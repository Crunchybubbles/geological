package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.UraniumSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldUraniumColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldUraniumPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldUraniumPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultPlannerFormsUraniumFamilyWithClosedGroundwaterLedger() {
    OverworldUraniumColumnPlan column = findFormed(planner(0, 0));

    assertEquals(FormationStatus.FORMED, column.system().status());
    assertNotEquals(UraniumSystemState.DepositFamily.NONE, column.system().family());
    assertEquals(
        column.system().releasedFluidFixedUnits(),
        column.system().transportLossFixedUnits() + column.system().depositAllocationFixedUnits());
    assertEquals(3, column.system().horizons().size());
    assertTrue(column.hasUranium());
  }

  @Test
  void boundedScanRetainsBothUraniumFamilyBranches() {
    boolean unconformity = false;
    boolean rollFront = false;
    OverworldUraniumPlanner planner = planner(0, 0);
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        UraniumSystemState.DepositFamily family = planner.plan(blockX, blockZ).system().family();
        unconformity |= family == UraniumSystemState.DepositFamily.UNCONFORMITY_RELATED;
        rollFront |= family == UraniumSystemState.DepositFamily.SANDSTONE_ROLL_FRONT;
      }
    }
    assertTrue(unconformity);
    assertTrue(rollFront);
  }

  @Test
  void barrenColumnsRetainANamedUraniumGate() {
    OverworldUraniumColumnPlan column = planner(-11, 17).plan(-176, 272);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalUraniumColumns() {
    OverworldUraniumColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldUraniumColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldUraniumPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldUraniumPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldUraniumColumnPlan findFormed(OverworldUraniumPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldUraniumColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.system().status() == FormationStatus.FORMED && candidate.hasUranium()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed uranium profile found in bounded fixture");
  }
}
