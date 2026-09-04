package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldColumnDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldColumnDebugTraceTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void joinsTheThreeColumnPlansDeterministically() {
    OverworldRegolithPlanner planner = OverworldRegolithPlanner.from(context(-11, 17));
    OverworldBaseTerrainColumnPlan base = planner.baseTerrain().plan(-161, 273);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(planner.baseTerrain()).plan(-161, 273);
    OverworldRegolithColumnPlan regolith = planner.plan(-161, 273);

    OverworldColumnDebugTrace first = OverworldColumnDebugTrace.from(base, air, regolith);
    OverworldColumnDebugTrace second =
        OverworldColumnDebugTrace.from(
            OverworldBaseTerrainPlanner.from(context(-11, 17)).plan(-161, 273),
            OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(-11, 17)))
                .plan(-161, 273),
            OverworldRegolithPlanner.from(context(-11, 17)).plan(-161, 273));

    assertEquals(first, second);
    assertEquals(first.summary(), second.summary());
    assertTrue(first.summary().contains("column x=-161 z=273"));
    assertTrue(first.baseLithologyRuns().size() > 0);
    assertEquals(first.surfaceMaterial().depositIds(), first.depositIds());
  }

  @Test
  void rejectsPlansFromDifferentColumns() {
    OverworldRegolithPlanner planner = OverworldRegolithPlanner.from(context(-11, 17));
    OverworldBaseTerrainColumnPlan base = planner.baseTerrain().plan(-161, 273);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(planner.baseTerrain()).plan(-160, 273);
    OverworldRegolithColumnPlan regolith = planner.plan(-161, 273);

    assertThrows(
        IllegalArgumentException.class, () -> OverworldColumnDebugTrace.from(base, air, regolith));
  }

  private static WorldgenExecutionContext context(long chunkX, long chunkZ) {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
        WorldgenStage.REGOLITH_SURFACE_CLUES,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }
}
