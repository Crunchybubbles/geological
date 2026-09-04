package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SkarnSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnPlanner;
import io.github.crunchybubbles.geological.worldgen.SkarnHostPolicy;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldSkarnPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void fixtureFormsClosedCarbonateContactSkarnProfile() {
    OverworldSkarnColumnPlan column = findFormed(planner(0, 0, SkarnHostPolicy.fixture()));
    SkarnSystemState system = column.system();

    assertEquals(FormationStatus.FORMED, system.status());
    assertEquals(SkarnSystemState.HostClass.LIMESTONE, system.hostClass());
    assertEquals(SkarnSystemState.FluidClass.MAGMATIC_HYDROTHERMAL_FLUID, system.fluidClass());
    assertEquals(
        system.releasedFluidFixedUnits(),
        system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
    assertEquals(3, system.horizons().size());
    assertTrue(column.hasSkarn());
  }

  @Test
  void defaultPlannerNeverInventsCarbonateHost() {
    OverworldSkarnColumnPlan column = planner(0, 0, SkarnHostPolicy.none()).plan(-4104, 628);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertEquals(SkarnSystemState.HostClass.NO_REACTIVE_HOST, column.system().hostClass());
    assertEquals("reactive_carbonate_host", column.system().failedGate().orElseThrow());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalFixtureSkarnColumns() {
    OverworldSkarnColumnPlan fromWest = planner(-1, 0, SkarnHostPolicy.fixture()).plan(-1, 3);
    OverworldSkarnColumnPlan fromEast = planner(0, 0, SkarnHostPolicy.fixture()).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldSkarnPlanner planner(long chunkX, long chunkZ, SkarnHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSkarnPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldSkarnColumnPlan findFormed(OverworldSkarnPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldSkarnColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.system().status() == FormationStatus.FORMED && candidate.hasSkarn()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed skarn profile found in bounded fixture");
  }
}
