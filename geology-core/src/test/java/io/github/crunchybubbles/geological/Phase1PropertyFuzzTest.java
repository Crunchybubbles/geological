package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GateStatus;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.ColumnPlanBudget;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.query.Phase1World;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Phase1PropertyFuzzTest {
  private static final double RESIDUAL_LIMIT = 1.0 / 256.0;
  private static final List<CellKey> CELLS =
      List.of(
          new CellKey("province", 0, 0),
          new CellKey("province", -1, 0),
          new CellKey("province", 0, -1),
          new CellKey("province", 1, 1),
          new CellKey("province", -1_000_000, 1_000_000));

  @Test
  void generatedChroniclesAndMineralProofsPreserveTheirCausalInvariants() {
    for (long seed = 0; seed < 16; seed++) {
      GeologyQueryEngine query = Phase1World.create(seed);
      for (CellKey cell : CELLS) {
        Province province = query.atlas().province(cell);
        String replay = replay(seed, cell);
        assertChronicle(province, replay);
        query
            .mineralDecisions(province)
            .forEach(
                decision -> {
                  assertFalse(decision.gates().isEmpty(), replay);
                  if (decision.status() == FormationStatus.FORMED) {
                    assertTrue(
                        decision.gates().stream()
                            .allMatch(gate -> gate.status() == GateStatus.PASS),
                        replay + " model=" + decision.modelId());
                    assertFalse(decision.provenance().isEmpty(), replay);
                    assertEquals(
                        decision.ledger().sourceAmount(),
                        decision.ledger().allocations().values().stream()
                            .mapToLong(Long::longValue)
                            .sum(),
                        replay + " model=" + decision.modelId());
                  } else {
                    assertTrue(
                        decision.gates().stream()
                            .anyMatch(gate -> gate.status() == GateStatus.FAIL),
                        replay + " model=" + decision.modelId());
                  }
                });
      }
    }
  }

  @Test
  void intervalProofsMatchEveryBlockAndRemainWithinReviewComplexity() {
    for (long seed = 0; seed < 12; seed++) {
      GeologyQueryEngine query = Phase1World.create(seed);
      int caseIndex = 0;
      for (CellKey cell : CELLS) {
        Province province = query.atlas().province(cell);
        double u = signedUnit(seed, caseIndex * 2) * 720.0;
        double v = signedUnit(seed, caseIndex * 2 + 1) * 720.0;
        Point3 world = province.frame().toWorld(new Point3(u, 0.0, v));
        ColumnRequest request = new ColumnRequest(world.x(), world.z(), -64, 320);
        ColumnQueryResult column = query.column(request);
        String replay = replay(seed, cell) + " column=" + request;

        assertTrue(
            column.complexity().within(ColumnPlanBudget.PHASE1_REVIEW),
            replay
                + " violations="
                + column.complexity().violations(ColumnPlanBudget.PHASE1_REVIEW));
        for (int y = request.minYInclusive(); y < request.maxYExclusive(); y++) {
          assertEquals(
              MaterialState.from(query.sample(new Point3(request.x(), y + 0.5, request.z()))),
              column.stateAt(y),
              replay + " y=" + y);
        }
        caseIndex++;
      }
    }
  }

  @Test
  void deformationRoundTripsRemainFiniteAndInsideTheInverseBudget() {
    AgeKey oldBody = new AgeKey(500.0, 0);
    for (long seed = 0; seed < 16; seed++) {
      GeologyQueryEngine query = Phase1World.create(seed);
      for (int caseIndex = 0; caseIndex < CELLS.size(); caseIndex++) {
        CellKey cell = CELLS.get(caseIndex);
        Province province = query.atlas().province(cell);
        Point3 present =
            new Point3(
                signedUnit(seed, caseIndex * 3) * 800.0,
                signedUnit(seed, caseIndex * 3 + 1) * 300.0,
                signedUnit(seed, caseIndex * 3 + 2) * 900.0);
        Point3 formation = province.geometry().pullBack(present, oldBody);
        Point3 reconstructed = province.geometry().pushForward(formation, oldBody);
        double determinant =
            province.geometry().fault().pullBackVerticalJacobianDeterminant(present, oldBody);
        String replay = replay(seed, cell) + " point=" + present;

        assertTrue(finite(formation) && finite(reconstructed), replay);
        assertTrue(distance(present, reconstructed) < RESIDUAL_LIMIT, replay);
        assertTrue(determinant >= 0.25 && determinant <= 4.0, replay + " jacobian=" + determinant);
      }
    }
  }

  private static void assertChronicle(Province province, String replay) {
    Set<StableId> eventIds = new HashSet<>();
    Set<StableId> available = new HashSet<>();
    AgeKey previous = null;
    for (GeologicalEvent event : province.chronicle().events()) {
      assertTrue(eventIds.add(event.id()), replay + " duplicate event=" + event.id());
      if (previous != null) {
        assertTrue(previous.compareTo(event.age()) <= 0, replay + " event=" + event.id());
      }
      assertTrue(available.containsAll(event.inputs()), replay + " event=" + event.id());
      assertTrue(available.addAll(event.outputs()), replay + " event=" + event.id());
      previous = event.age();
    }
  }

  private static String replay(long seed, CellKey cell) {
    return "replay_seed=" + seed + " cell=" + cell;
  }

  private static double signedUnit(long seed, int index) {
    long value = seed + 0x9e3779b97f4a7c15L * (index + 1L);
    value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
    value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
    value ^= value >>> 31;
    return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
  }

  private static boolean finite(Point3 point) {
    return Double.isFinite(point.x()) && Double.isFinite(point.y()) && Double.isFinite(point.z());
  }

  private static double distance(Point3 first, Point3 second) {
    double x = first.x() - second.x();
    double y = first.y() - second.y();
    double z = first.z() - second.z();
    return StrictMath.sqrt(x * x + y * y + z * z);
  }
}
