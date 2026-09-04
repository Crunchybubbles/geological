package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SupergeneCopperState;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldSecondaryWeatheringPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void formedProfileProjectsToWorldCoordinatesAndRetainsTheSourceBudget() {
    OverworldSecondaryWeatheringPlanner planner =
        OverworldSecondaryWeatheringPlanner.from(
            io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner.from(
                context(0, 0)));
    Province province = formedProvince(planner);
    SupergeneCopperState state = planner.regolith().material().supergeneCopperState(province);
    assertEquals(FormationStatus.FORMED, state.status());

    Point3 worldCenter = province.frame().toWorld(state.localCenter());
    long blockX = (long) StrictMath.floor(worldCenter.x());
    long blockZ = (long) StrictMath.floor(worldCenter.z());
    OverworldSecondaryWeatheringColumnPlan plan = planner.plan(blockX, blockZ);

    assertEquals(province.id(), plan.provinceId());
    assertEquals(state.systemId(), plan.systemId());
    assertEquals(state.sourceBudgetFixedUnits(), plan.sourceBudgetFixedUnits());
    assertEquals(state.retainedHypogeneFixedUnits(), plan.retainedHypogeneFixedUnits());
    assertEquals(state.leachableCopperFixedUnits(), plan.leachableCopperFixedUnits());
    assertEquals(state.supergeneAllocationFixedUnits(), plan.supergeneAllocationFixedUnits());
    assertEquals(
        state.oxidizedAndDissolvedLossFixedUnits(), plan.oxidizedAndDissolvedLossFixedUnits());
    assertTrue(plan.hasSecondaryWeathering());
    assertTrue(plan.hasEnrichedSulfide());
    assertTrue(plan.at((int) StrictMath.floor(state.localCenter().y())).isPresent());
    assertTrue(
        plan.intervals().stream()
            .allMatch(
                interval ->
                    interval.systemId().equals(state.systemId())
                        && interval.primaryDepositId().equals(state.primaryDepositId())
                        && interval.weatheringProcessId().equals(state.weatheringProcessId())
                        && interval.allocatedCopperFixedUnits()
                            <= state.leachableCopperFixedUnits()));
    assertTrue(
        plan.intervals().stream()
                .mapToLong(OverworldSecondaryWeatheringInterval::allocatedCopperFixedUnits)
                .sum()
            <= state.leachableCopperFixedUnits());
  }

  @Test
  void targetChunkIsBoundedAndAdjacentContextsAgreeAtTheSeam() {
    OverworldSecondaryWeatheringPlanner planner =
        OverworldSecondaryWeatheringPlanner.from(
            io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner.from(
                context(-11, 17)));
    List<OverworldSecondaryWeatheringColumnPlan> columns = planner.planTargetChunk();
    ChunkBlockBounds bounds = planner.regolith().context().targetBounds();

    assertEquals(256, columns.size());
    assertEquals(bounds.minX(), columns.getFirst().blockX());
    assertEquals(bounds.minZ(), columns.getFirst().blockZ());
    assertEquals(bounds.maxXExclusive() - 1, columns.getLast().blockX());
    assertEquals(bounds.maxZExclusive() - 1, columns.getLast().blockZ());
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    bounds.contains(column.blockX(), column.minYInclusive(), column.blockZ())
                        && column.intervals().stream()
                            .allMatch(
                                interval ->
                                    interval.minYInclusive() >= column.minYInclusive()
                                        && interval.maxYExclusive()
                                            <= column.solidMaxYExclusive())));

    OverworldSecondaryWeatheringColumnPlan fromWest =
        OverworldSecondaryWeatheringPlanner.from(
                io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner.from(
                    context(-1, 0)))
            .plan(-1, 3);
    OverworldSecondaryWeatheringColumnPlan fromEast =
        OverworldSecondaryWeatheringPlanner.from(
                io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner.from(
                    context(0, 0)))
            .plan(-1, 3);
    assertEquals(fromWest, fromEast);
  }

  @Test
  void barrenProjectionCarriesNoSecondaryIntervalsOrBudget() {
    OverworldSecondaryWeatheringPlanner planner =
        OverworldSecondaryWeatheringPlanner.from(
            io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner.from(
                context(0, 0)));
    Province province = nonFormedProvince(planner);
    Point3 worldCenter = province.frame().toWorld(province.geometry().porphyryCenter().withY(35.0));
    OverworldSecondaryWeatheringColumnPlan plan =
        planner.plan(
            (long) StrictMath.floor(worldCenter.x()), (long) StrictMath.floor(worldCenter.z()));

    assertEquals(FormationStatus.BARREN_SYSTEM, plan.status());
    assertFalse(plan.hasSecondaryWeathering());
    assertEquals(0L, plan.sourceBudgetFixedUnits());
    assertEquals(0L, plan.leachableCopperFixedUnits());
    assertEquals(0L, plan.supergeneAllocationFixedUnits());
    assertEquals(0L, plan.oxidizedAndDissolvedLossFixedUnits());
  }

  private static WorldgenExecutionContext context(long chunkX, long chunkZ) {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
        WorldgenStage.REGOLITH_SURFACE_CLUES,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }

  private static Province formedProvince(OverworldSecondaryWeatheringPlanner planner) {
    var atlas = planner.regolith().material().geology().atlas();
    for (long cellX = -2; cellX <= 2; cellX++) {
      for (long cellZ = -2; cellZ <= 2; cellZ++) {
        Province candidate = atlas.province(new CellKey("province", cellX, cellZ));
        SupergeneCopperState state = planner.regolith().material().supergeneCopperState(candidate);
        if (state.status() != FormationStatus.FORMED) {
          continue;
        }
        Point3 worldCenter = candidate.frame().toWorld(state.localCenter());
        Province owner = atlas.provinceAt(new Point2(worldCenter.x(), worldCenter.z()));
        if (owner.id().equals(candidate.id())) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed supergene province found in the bounded fixture");
  }

  private static Province nonFormedProvince(OverworldSecondaryWeatheringPlanner planner) {
    var atlas = planner.regolith().material().geology().atlas();
    for (long cellX = -2; cellX <= 2; cellX++) {
      for (long cellZ = -2; cellZ <= 2; cellZ++) {
        Province candidate = atlas.province(new CellKey("province", cellX, cellZ));
        SupergeneCopperState state = planner.regolith().material().supergeneCopperState(candidate);
        if (state.status() != FormationStatus.FORMED) {
          Point3 worldCenter = candidate.frame().toWorld(state.localCenter());
          Province owner = atlas.provinceAt(new Point2(worldCenter.x(), worldCenter.z()));
          if (owner.id().equals(candidate.id())) {
            return candidate;
          }
        }
      }
    }
    throw new AssertionError("no barren supergene province found in the bounded fixture");
  }
}
