package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldBaseTerrainPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void planIsDeterministicAndClipsMaterialRunsToTheSurface() {
    OverworldBaseTerrainPlanner first = OverworldBaseTerrainPlanner.from(context(-11, 17));
    OverworldBaseTerrainPlanner second = OverworldBaseTerrainPlanner.from(context(-11, 17));

    OverworldBaseTerrainColumnPlan expected = first.plan(-161, 273);
    OverworldBaseTerrainColumnPlan actual = second.plan(-161, 273);

    assertEquals(expected, actual);
    assertEquals(-64, expected.minYInclusive());
    assertEquals(320, expected.maxYExclusive());
    assertTrue(expected.solidMaxYExclusive() >= expected.minYInclusive());
    assertTrue(expected.solidMaxYExclusive() <= expected.maxYExclusive());
    assertEquals(
        expected.solidMaxYExclusive(),
        expected.lithologyRuns().isEmpty()
            ? expected.minYInclusive()
            : expected.lithologyRuns().getLast().maxYExclusive());
    assertTrue(expected.geologicalPointEvaluations() > 0);
    assertNotNull(expected.terrainControls().provinceId());
  }

  @Test
  void plannerRequiresWritableOverworldBaseStage() {
    WorldgenExecutionContext coarse =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, 0, 0, WorldgenStage.COARSE_TERRAIN_CONTROLS),
            WorldgenStage.COARSE_TERRAIN_CONTROLS,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    assertFalse(coarse.canWriteTarget());
    assertThrows(IllegalArgumentException.class, () -> OverworldBaseTerrainPlanner.from(coarse));
  }

  @Test
  void planUsesOnlyTheAuthorizedTargetBounds() {
    OverworldBaseTerrainPlanner planner = OverworldBaseTerrainPlanner.from(context(-11, 17));
    OverworldBaseTerrainColumnPlan plan = planner.plan(-161, 287);

    assertTrue(
        planner
            .context()
            .targetBounds()
            .contains(plan.blockX(), plan.minYInclusive(), plan.blockZ()));
    assertTrue(
        planner
            .context()
            .targetBounds()
            .contains(plan.blockX(), plan.maxYExclusive() - 1, plan.blockZ()));
    assertEquals(-161, plan.blockX());
    assertEquals(287, plan.blockZ());
  }

  @Test
  void targetChunkPlanHasEveryColumnInStableOrder() {
    List<OverworldBaseTerrainColumnPlan> columns =
        OverworldBaseTerrainPlanner.from(context(-11, 17)).planTargetChunk();

    assertEquals(256, columns.size());
    assertEquals(-176, columns.getFirst().blockX());
    assertEquals(272, columns.getFirst().blockZ());
    assertEquals(-161, columns.getLast().blockX());
    assertEquals(287, columns.getLast().blockZ());
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    column.blockX() >= -176
                        && column.blockX() < -160
                        && column.blockZ() >= 272
                        && column.blockZ() < 288));
  }

  private static WorldgenExecutionContext context(long chunkX, long chunkZ) {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.BASE_TERRAIN),
        WorldgenStage.BASE_TERRAIN,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }
}
