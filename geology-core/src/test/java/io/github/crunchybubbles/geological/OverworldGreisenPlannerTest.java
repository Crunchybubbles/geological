package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GreisenSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldGreisenColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGreisenPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldGreisenPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void evolvedFelsicPulseFormsClosedResidualFluidGreisenProfile() {
    OverworldGreisenPlanner planner = planner(0, 0);
    OverworldGreisenColumnPlan column = findFormed(planner);
    GreisenSystemState system = column.system();

    assertEquals(FormationStatus.FORMED, system.status());
    assertEquals(GreisenSystemState.ParentClass.EVOLVED_FELSIC_PULSE, system.parentClass());
    assertEquals(GreisenSystemState.SourceBasis.RESIDUAL_FELSIC_FLUID_PROXY, system.sourceBasis());
    assertEquals(
        system.releasedFluidFixedUnits(),
        system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
    assertTrue(system.depositAllocationFixedUnits() > 0);
    assertEquals(3, system.horizons().size());
    assertTrue(column.hasGreisen());
  }

  @Test
  void barrenColumnsRetainResidualFluidGate() {
    OverworldGreisenColumnPlan column = planner(-11, 17).plan(-176, 272);

    assertTrue(column.system().status() != FormationStatus.FORMED);
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalGreisenColumns() {
    OverworldGreisenColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldGreisenColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldGreisenPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldGreisenPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldGreisenColumnPlan findFormed(OverworldGreisenPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldGreisenColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.system().status() == FormationStatus.FORMED && candidate.hasGreisen()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed greisen profile found in bounded fixture");
  }
}
