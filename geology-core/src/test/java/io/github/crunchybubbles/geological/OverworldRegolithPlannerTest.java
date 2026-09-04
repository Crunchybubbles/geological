package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.SurfaceClueKind;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldRegolithPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void planIsDeterministicAndRetainsSurfaceProvenance() {
    OverworldRegolithColumnPlan first =
        OverworldRegolithPlanner.from(context(-11, 17)).plan(-161, 273);
    OverworldRegolithColumnPlan second =
        OverworldRegolithPlanner.from(context(-11, 17)).plan(-161, 273);

    assertEquals(first, second);
    assertEquals(first.surfaceMaterial().rockBodyId(), first.materialBodyId());
    assertFalse(first.sourceBodyIds().isEmpty());
    assertTrue(first.regolithMinYInclusive() <= first.solidMaxYExclusive());
    assertTrue(first.regolithMinYInclusive() >= first.minYInclusive());
    assertTrue(first.surfaceMaterial().depositIds().equals(first.depositIds()));
    if (first.clueKind() == SurfaceClueKind.BEDROCK_OUTCROP) {
      assertFalse(first.hasRegolith());
    }
  }

  @Test
  void targetPlanHasStableFootprintAndClippedRegolithIntervals() {
    OverworldRegolithPlanner planner = OverworldRegolithPlanner.from(context(-11, 17));
    List<OverworldRegolithColumnPlan> columns = planner.planTargetChunk();
    ChunkBlockBounds bounds = planner.context().targetBounds();

    assertEquals(256, columns.size());
    assertEquals(-176, columns.getFirst().blockX());
    assertEquals(272, columns.getFirst().blockZ());
    assertEquals(-161, columns.getLast().blockX());
    assertEquals(287, columns.getLast().blockZ());
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    bounds.contains(column.blockX(), column.minYInclusive(), column.blockZ())
                        && column.regolithMinYInclusive() >= bounds.minY()
                        && column.solidMaxYExclusive() <= bounds.maxYExclusive()));
    assertTrue(columns.stream().anyMatch(OverworldRegolithColumnPlan::hasRegolith));
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    column.clueKind() != SurfaceClueKind.BEDROCK_OUTCROP || !column.hasRegolith()));
  }

  @Test
  void adjacentStageContextsAgreeAtTheSameWorldColumn() {
    OverworldRegolithColumnPlan fromWest =
        OverworldRegolithPlanner.from(context(-1, 0)).plan(-1, 3);
    OverworldRegolithColumnPlan fromEast = OverworldRegolithPlanner.from(context(0, 0)).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  @Test
  void plannerRequiresTheExactRegolithStageAndOverworld() {
    WorldgenExecutionContext base =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.BASE_TERRAIN),
            WorldgenStage.BASE_TERRAIN,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    assertThrows(IllegalArgumentException.class, () -> OverworldRegolithPlanner.from(base));

    DimensionGeologyProfile nether = DimensionGeologyProfiles.require("minecraft:the_nether");
    WorldgenExecutionContext netherContext =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, nether, 0, 0, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(nether),
            Runnable::run);
    assertThrows(
        IllegalArgumentException.class, () -> OverworldRegolithPlanner.from(netherContext));
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
