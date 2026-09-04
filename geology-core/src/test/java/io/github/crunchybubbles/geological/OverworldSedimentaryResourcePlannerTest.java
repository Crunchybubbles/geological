package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SedimentaryResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentaryResourceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentaryResourcePlanner;
import io.github.crunchybubbles.geological.worldgen.SedimentaryResourceHostPolicy;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverworldSedimentaryResourcePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void fixtureFormsAllSedimentaryResourceFamiliesWithClosedLedgers() {
    OverworldSedimentaryResourcePlanner planner =
        planner(0, 0, SedimentaryResourceHostPolicy.fixture());
    Set<SedimentaryResourceSystemState.ResourceFamily> families =
        EnumSet.noneOf(SedimentaryResourceSystemState.ResourceFamily.class);
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldSedimentaryResourceColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED) {
          families.add(column.system().family());
          assertEquals(
              column.system().releasedResourceFixedUnits(),
              column.system().transportLossFixedUnits()
                  + column.system().depositAllocationFixedUnits());
          assertEquals(3, column.system().horizons().size());
          assertTrue(column.hasSedimentaryResource());
        }
      }
    }
    assertEquals(
        EnumSet.of(
            SedimentaryResourceSystemState.ResourceFamily.PHOSPHORITE,
            SedimentaryResourceSystemState.ResourceFamily.SEDIMENTARY_MANGANESE,
            SedimentaryResourceSystemState.ResourceFamily.COAL,
            SedimentaryResourceSystemState.ResourceFamily.LITHIUM_BRINE,
            SedimentaryResourceSystemState.ResourceFamily.POTASH_BORATE_BRINE,
            SedimentaryResourceSystemState.ResourceFamily.HELIUM_GAS),
        families);
  }

  @Test
  void defaultPlannerDoesNotInventResourceHost() {
    OverworldSedimentaryResourceColumnPlan column = planner(0, 0).plan(10_000L, 10_000L);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalColumns() {
    OverworldSedimentaryResourceColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldSedimentaryResourceColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldSedimentaryResourcePlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, SedimentaryResourceHostPolicy.none());
  }

  private static OverworldSedimentaryResourcePlanner planner(
      long chunkX, long chunkZ, SedimentaryResourceHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSedimentaryResourcePlanner.from(OverworldRegolithPlanner.from(context), policy);
  }
}
