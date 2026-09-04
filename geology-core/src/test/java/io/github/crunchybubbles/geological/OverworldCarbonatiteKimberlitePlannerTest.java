package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.petrology.MantleCargoStatus;
import io.github.crunchybubbles.geological.worldgen.CarbonatiteKimberliteHostPolicy;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldCarbonatiteKimberliteColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldCarbonatiteKimberlitePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverworldCarbonatiteKimberlitePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultPlannerDoesNotInventAlkalineComplexOrDiamondCargo() {
    OverworldCarbonatiteKimberliteColumnPlan column = planner(0, 0).plan(10_000L, 10_000L);

    assertEquals(FormationStatus.BARREN_SYSTEM, column.system().status());
    assertEquals("alkaline_or_carrier_host", column.system().failedGate().orElseThrow());
    assertTrue(column.intervals().isEmpty());
  }

  @Test
  void fixtureFormsCarbonatitePeralkalineAndKimberliteFamilies() {
    Set<CarbonatiteKimberliteSystemState.DepositFamily> families =
        EnumSet.noneOf(CarbonatiteKimberliteSystemState.DepositFamily.class);
    boolean cargoChecked = false;
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldCarbonatiteKimberliteColumnPlan column =
            planner(0, 0, CarbonatiteKimberliteHostPolicy.fixture()).plan(blockX, blockZ);
        if (column.system().status() != FormationStatus.FORMED) {
          continue;
        }
        CarbonatiteKimberliteSystemState system = column.system();
        families.add(system.family());
        assertEquals(
            system.releasedBudgetFixedUnits(),
            system.transportLossFixedUnits() + system.depositAllocationFixedUnits());
        assertEquals(3, system.horizons().size());
        assertTrue(column.hasAlkalineComplex());
        if (system.family() == CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND) {
          assertTrue(system.mantleCargo().isPresent());
          assertEquals(
              MantleCargoStatus.DIAMOND_BEARING, system.mantleCargo().orElseThrow().status());
          assertTrue(
              system.mantleCargo().orElseThrow().carrierBodyId().equals(system.hostBodyId()));
          cargoChecked = true;
        } else {
          assertTrue(system.mantleCargo().isEmpty());
        }
      }
    }
    assertEquals(
        Set.of(
            CarbonatiteKimberliteSystemState.DepositFamily.CARBONATITE_REE,
            CarbonatiteKimberliteSystemState.DepositFamily.PERALKALINE_REE,
            CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND),
        families);
    assertTrue(cargoChecked);
  }

  @Test
  void adjacentChunkContextsProduceIdenticalAlkalineColumns() {
    OverworldCarbonatiteKimberliteColumnPlan fromWest =
        planner(-1, 0, CarbonatiteKimberliteHostPolicy.fixture()).plan(-1, 3);
    OverworldCarbonatiteKimberliteColumnPlan fromEast =
        planner(0, 0, CarbonatiteKimberliteHostPolicy.fixture()).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldCarbonatiteKimberlitePlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, CarbonatiteKimberliteHostPolicy.none());
  }

  private static OverworldCarbonatiteKimberlitePlanner planner(
      long chunkX, long chunkZ, CarbonatiteKimberliteHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldCarbonatiteKimberlitePlanner.from(
        OverworldRegolithPlanner.from(context), policy);
  }
}
