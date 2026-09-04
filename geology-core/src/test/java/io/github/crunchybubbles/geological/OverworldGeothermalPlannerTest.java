package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GeothermalSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.GeothermalHostPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldGeothermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGeothermalPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverworldGeothermalPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void fixtureFormsAllGeothermalTypesWithClosedLedgers() {
    OverworldGeothermalPlanner planner = planner(0, 0, GeothermalHostPolicy.fixture());
    Set<GeothermalSystemState.GeothermalType> types =
        EnumSet.noneOf(GeothermalSystemState.GeothermalType.class);
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldGeothermalColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED) {
          types.add(column.system().family());
          assertEquals(
              column.system().releasedHeatFixedUnits(),
              column.system().transportLossFixedUnits()
                  + column.system().reservoirAllocationFixedUnits());
          assertEquals(3, column.system().horizons().size());
          assertTrue(column.hasGeothermalReservoir());
        }
      }
    }
    assertEquals(
        EnumSet.of(
            GeothermalSystemState.GeothermalType.VOLCANIC_HIGH_ENTHALPY,
            GeothermalSystemState.GeothermalType.FAULT_CONTROLLED,
            GeothermalSystemState.GeothermalType.SEDIMENTARY_AQUIFER,
            GeothermalSystemState.GeothermalType.HOT_DRY_ROCK),
        types);
  }

  @Test
  void defaultPlannerDoesNotInventHeatOrReservoir() {
    OverworldGeothermalColumnPlan column = planner(0, 0).plan(10_000L, 10_000L);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalColumns() {
    OverworldGeothermalColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldGeothermalColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldGeothermalPlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, GeothermalHostPolicy.none());
  }

  private static OverworldGeothermalPlanner planner(
      long chunkX, long chunkZ, GeothermalHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldGeothermalPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }
}
