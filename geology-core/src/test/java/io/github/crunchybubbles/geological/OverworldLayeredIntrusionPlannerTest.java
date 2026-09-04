package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LayeredIntrusionSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.LayeredIntrusionHostPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldLayeredIntrusionColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldLayeredIntrusionPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverworldLayeredIntrusionPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultPlannerDoesNotInventLayeredHost() {
    OverworldLayeredIntrusionColumnPlan column = planner(0, 0).plan(10_000L, 10_000L);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertEquals("layered_intrusion", column.system().failedGate().orElseThrow());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void fixtureFormsAllThreeLayeredFamiliesWithClosedLedger() {
    Set<LayeredIntrusionSystemState.DepositFamily> families =
        EnumSet.noneOf(LayeredIntrusionSystemState.DepositFamily.class);
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldLayeredIntrusionColumnPlan column =
            planner(0, 0, LayeredIntrusionHostPolicy.fixture()).plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED) {
          LayeredIntrusionSystemState system = column.system();
          families.add(system.family());
          assertEquals(
              system.releasedMeltFixedUnits(),
              system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
          assertEquals(3, system.horizons().size());
          assertTrue(column.hasLayeredIntrusion());
        }
      }
    }
    assertEquals(
        Set.of(
            LayeredIntrusionSystemState.DepositFamily.STRATIFORM_CHROMITE,
            LayeredIntrusionSystemState.DepositFamily.NI_CU_PGE_SULFIDE,
            LayeredIntrusionSystemState.DepositFamily.LAYERED_PGE_REEF),
        families);
  }

  @Test
  void adjacentChunkContextsProduceIdenticalLayeredColumns() {
    OverworldLayeredIntrusionColumnPlan fromWest =
        planner(-1, 0, LayeredIntrusionHostPolicy.fixture()).plan(-1, 3);
    OverworldLayeredIntrusionColumnPlan fromEast =
        planner(0, 0, LayeredIntrusionHostPolicy.fixture()).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldLayeredIntrusionPlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, LayeredIntrusionHostPolicy.none());
  }

  private static OverworldLayeredIntrusionPlanner planner(
      long chunkX, long chunkZ, LayeredIntrusionHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldLayeredIntrusionPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }
}
