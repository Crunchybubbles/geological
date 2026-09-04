package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.OrogenicGoldSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldOrogenicGoldColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldOrogenicGoldPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldOrogenicGoldPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void deformationAndMetamorphicFluidFormClosedOrogenicProfile() {
    OverworldOrogenicGoldColumnPlan column = findFormed(planner(0, 0));
    OrogenicGoldSystemState system = column.system();

    assertEquals(FormationStatus.FORMED, system.status());
    assertEquals(
        OrogenicGoldSystemState.FluidSourceClass.METAMORPHIC_AQUEOUS_CARBONIC_PROXY,
        system.fluidSourceClass());
    assertEquals(
        system.releasedFluidFixedUnits(),
        system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
    assertEquals(3, system.horizons().size());
    assertTrue(column.hasOrogenicGold());
  }

  @Test
  void barrenColumnsRetainDeformationGate() {
    OverworldOrogenicGoldColumnPlan column = planner(-11, 17).plan(-176, 272);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalOrogenicColumns() {
    OverworldOrogenicGoldColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldOrogenicGoldColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldOrogenicGoldPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldOrogenicGoldPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldOrogenicGoldColumnPlan findFormed(OverworldOrogenicGoldPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldOrogenicGoldColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.system().status() == FormationStatus.FORMED && candidate.hasOrogenicGold()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed orogenic-gold profile found in bounded fixture");
  }
}
