package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.DescriptorCache;
import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.ColumnPlanBudget;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.spatial.ProvinceSpatialIndex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ColumnQueryTest {
  @Test
  void compressedRunsExactlyMatchPointQueriesAtEveryBlockCenter() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Point2 contact =
        province
            .frame()
            .toWorld(
                new Point2(
                    province.geometry().porphyryCenter().x(),
                    province.geometry().porphyryCenter().z()));
    ColumnRequest request = new ColumnRequest(contact.x(), contact.z(), -64, 320);
    ColumnQueryResult result = query.column(request);

    for (int y = request.minYInclusive(); y < request.maxYExclusive(); y++) {
      assertEquals(
          MaterialState.from(query.sample(new Point3(request.x(), y + 0.5, request.z()))),
          result.stateAt(y));
    }
    assertTrue(result.runs().size() < request.height());
    assertTrue(result.pointEvaluations() < 64);
    assertEquals(result.pointEvaluations(), result.intervalProof().provenUniformIntervals());
    assertTrue(result.complexity().within(ColumnPlanBudget.PHASE1_REVIEW));
    assertCandidatesUseStableOrder(result);
  }

  @Test
  void analyticIntervalProofsMatchPointQueriesAcrossProvinceGrammars() {
    GeologyQueryEngine query = Phase1World.create(31_415_926L);
    int checkedColumns = 0;
    for (int z = -3600; z <= 3600; z += 1200) {
      for (int x = -3600; x <= 3600; x += 1200) {
        ColumnRequest request = new ColumnRequest(x + 0.25, z - 0.25, -64, 320);
        assertColumnMatchesPointQueries(query, request);
        checkedColumns++;
      }
    }
    Province fertile =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    List<Point2> targetedLocal = new java.util.ArrayList<>();
    targetedLocal.add(fertile.geometry().basin().center());
    targetedLocal.add(
        new Point2(fertile.geometry().vmsCenter().x(), fertile.geometry().vmsCenter().z()));
    targetedLocal.add(
        new Point2(
            fertile.geometry().porphyryCenter().x(), fertile.geometry().porphyryCenter().z()));
    targetedLocal.add(new Point2(fertile.geometry().fault().planeU(), 0.0));
    fertile
        .geometry()
        .plutonPulses()
        .forEach(pulse -> targetedLocal.add(new Point2(pulse.center().x(), pulse.center().z())));
    for (Point2 local : targetedLocal) {
      Point2 world = fertile.frame().toWorld(local);
      assertColumnMatchesPointQueries(query, new ColumnRequest(world.x(), world.z(), -64, 320));
      checkedColumns++;
    }
    assertEquals(56, checkedColumns);
  }

  @Test
  void adaptiveEvaluationSkipsProvenUniformIntervalsAndIgnoresQueryOrder() {
    GeologyQueryEngine query = Phase1World.create(6_161L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    ColumnQueryResult best = null;
    for (int u = -900; u <= 900; u += 300) {
      for (int v = -900; v <= 900; v += 300) {
        Point2 point = province.frame().toWorld(new Point2(u, v));
        ColumnQueryResult candidate =
            query.column(new ColumnRequest(point.x(), point.z(), -64, 320));
        if (candidate.provinceId().equals(province.id())
            && (best == null
                || candidate.skippedPointEvaluations() > best.skippedPointEvaluations())) {
          best = candidate;
        }
      }
    }
    if (best == null) {
      throw new AssertionError("no column remained inside the selected province");
    }
    assertTrue(best.skippedPointEvaluations() > 300);

    ColumnRequest request = best.request();
    query.column(new ColumnRequest(request.x() + 40_000.0, request.z() - 40_000.0, -64, 320));
    query.clearCaches();
    assertEquals(best, query.column(request));
    assertEquals(best, uncachedQuery(6_161L).column(request));
  }

  @Test
  void completeChunkPlansAreIndependentOfColumnOrderAndCacheState() {
    GeologyQueryEngine query = Phase1World.create(73_731L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Point2 contact =
        province
            .frame()
            .toWorld(
                new Point2(
                    province.geometry().porphyryCenter().x(),
                    province.geometry().porphyryCenter().z()));
    long originX = Math.floorDiv((long) StrictMath.floor(contact.x()), 16L) * 16L;
    long originZ = Math.floorDiv((long) StrictMath.floor(contact.z()), 16L) * 16L;
    List<ColumnRequest> requests = new ArrayList<>();
    for (int z = 0; z < 16; z++) {
      for (int x = 0; x < 16; x++) {
        requests.add(new ColumnRequest(originX + x + 0.5, originZ + z + 0.5, -64, 320));
      }
    }
    Map<ColumnRequest, ColumnQueryResult> expected = new HashMap<>();
    requests.forEach(request -> expected.put(request, query.column(request)));

    Collections.reverse(requests);
    query.clearCaches();
    requests.forEach(request -> assertEquals(expected.get(request), query.column(request)));
  }

  private static void assertCandidatesUseStableOrder(ColumnQueryResult result) {
    assertEquals(
        result.candidates(),
        result.candidates().stream()
            .sorted(java.util.Comparator.comparing(candidate -> candidate.id()))
            .toList());
  }

  private static void assertColumnMatchesPointQueries(
      GeologyQueryEngine query, ColumnRequest request) {
    ColumnQueryResult result = query.column(request);
    for (int y = request.minYInclusive(); y < request.maxYExclusive(); y++) {
      assertEquals(
          MaterialState.from(query.sample(new Point3(request.x(), y + 0.5, request.z()))),
          result.stateAt(y),
          "interval proof crossed a transition at " + request + " Y=" + y);
    }
    assertTrue(result.pointEvaluations() < request.height());
  }

  private static GeologyQueryEngine uncachedQuery(long seed) {
    DimensionProfile profile = DimensionProfile.overworldPhase1();
    WorldIdentity identity =
        new WorldIdentity(
            seed, Phase1World.MODEL_VERSION, Phase1World.SCIENTIFIC_DIGEST, profile.id());
    DescriptorCache<StableId, ProvinceSpatialIndex> noIndexes = DescriptorCache.none();
    return new GeologyQueryEngine(new GeologyAtlas(identity, profile), 0, noIndexes);
  }
}
