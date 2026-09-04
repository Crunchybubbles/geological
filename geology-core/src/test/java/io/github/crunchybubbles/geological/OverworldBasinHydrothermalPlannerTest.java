package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.BasinHydrothermalSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.worldgen.BasinHydrothermalHostPolicy;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldBasinHydrothermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldBasinHydrothermalPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldBasinHydrothermalPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultPlannerFormsBasinFamilyWithClosedBrineLedger() {
    OverworldBasinHydrothermalColumnPlan column = findFormed(planner(0, 0));
    BasinHydrothermalSystemState system = column.system();

    assertEquals(FormationStatus.FORMED, system.status());
    assertTrue(system.family() != BasinHydrothermalSystemState.DepositFamily.NONE);
    assertEquals(
        system.releasedFluidFixedUnits(),
        system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
    assertEquals(3, system.horizons().size());
    assertTrue(column.hasBasinHydrothermal());
  }

  @Test
  void carbonateFixtureFormsMvtWithoutChangingDefaultHostPolicy() {
    OverworldBasinHydrothermalColumnPlan column =
        findFormed(planner(0, 0, BasinHydrothermalHostPolicy.fixture()));

    assertEquals(BasinHydrothermalSystemState.DepositFamily.MVT_PB_ZN, column.system().family());
    assertEquals(FormationStatus.FORMED, column.system().status());
    assertTrue(column.hasBasinHydrothermal());
    assertTrue(
        planner(0, 0).plan(column.blockX(), column.blockZ()).system().family()
            != BasinHydrothermalSystemState.DepositFamily.MVT_PB_ZN);
  }

  @Test
  void barrenColumnsRetainBasinFamilyGate() {
    OverworldBasinHydrothermalColumnPlan column = planner(-11, 17).plan(-176, 272);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertTrue(column.system().failedGate().isPresent());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalBasinColumns() {
    OverworldBasinHydrothermalColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldBasinHydrothermalColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldBasinHydrothermalPlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, BasinHydrothermalHostPolicy.none());
  }

  private static OverworldBasinHydrothermalPlanner planner(
      long chunkX, long chunkZ, BasinHydrothermalHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldBasinHydrothermalPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldBasinHydrothermalColumnPlan findFormed(
      OverworldBasinHydrothermalPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldBasinHydrothermalColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.system().status() == FormationStatus.FORMED
            && candidate.hasBasinHydrothermal()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed basin hydrothermal profile found in bounded fixture");
  }
}
