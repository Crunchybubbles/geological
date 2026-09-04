package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldAirFluidPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void planIsDeterministicAndKeepsWaterAboveSolidSurface() {
    OverworldAirFluidPlanner first =
        OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(-11, 17)));
    OverworldAirFluidPlanner second =
        OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(-11, 17)));

    OverworldAirFluidColumnPlan expected = first.plan(-161, 273);
    OverworldAirFluidColumnPlan actual = second.plan(-161, 273);

    assertEquals(expected, actual);
    assertEquals(OverworldAirFluidPlanner.DEFAULT_SEA_LEVEL, first.seaLevel());
    assertTrue(expected.surfaceWaterMaxYExclusive() >= expected.solidMaxYExclusive());
    assertTrue(expected.surfaceWaterMaxYExclusive() <= expected.maxYExclusive());
    assertEquals(expected.surfaceWaterMaxYExclusive(), expected.airMinYInclusive());
    assertTrue(expected.airMinYInclusive() >= expected.solidMaxYExclusive());
  }

  @Test
  void targetPlanHasStableFootprintAndFindsBothWetAndDryColumns() {
    OverworldAirFluidPlanner planner =
        OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(-11, 17)), 107);
    List<OverworldAirFluidColumnPlan> columns = planner.planTargetChunk();

    assertEquals(256, columns.size());
    assertEquals(-176, columns.getFirst().blockX());
    assertEquals(272, columns.getFirst().blockZ());
    assertEquals(-161, columns.getLast().blockX());
    assertEquals(287, columns.getLast().blockZ());
    ChunkBlockBounds bounds = planner.baseTerrain().context().targetBounds();
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    column.blockX() >= bounds.minX()
                        && column.blockX() < bounds.maxXExclusive()
                        && column.blockZ() >= bounds.minZ()
                        && column.blockZ() < bounds.maxZExclusive()));
    assertTrue(columns.stream().anyMatch(OverworldAirFluidColumnPlan::hasSurfaceWater));
    assertTrue(columns.stream().anyMatch(column -> !column.hasSurfaceWater()));
    assertTrue(columns.stream().allMatch(OverworldAirFluidColumnPlan::hasAir));
  }

  @Test
  void policyRejectsOutOfEnvelopeSeaLevelAndWaterlessProfiles() {
    OverworldBaseTerrainPlanner overworld = OverworldBaseTerrainPlanner.from(context(0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OverworldAirFluidPlanner.from(
                overworld, overworld.context().targetBounds().maxYExclusive()));

    DimensionGeologyProfile nether = DimensionGeologyProfiles.require("minecraft:the_nether");
    WorldgenExecutionContext netherContext =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(8_675_309L, nether, 0, 0, WorldgenStage.BASE_TERRAIN),
            WorldgenStage.BASE_TERRAIN,
            WorldgenSnapshot.forProfile(nether),
            Runnable::run);
    assertThrows(
        IllegalArgumentException.class,
        () -> OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(netherContext)));
  }

  @Test
  void emptySurfaceWaterIntervalStillStartsAirAtTheSolidBoundary() {
    OverworldAirFluidColumnPlan dry = new OverworldAirFluidColumnPlan(0, 0, -64, 320, 100, 100);

    assertFalse(dry.hasSurfaceWater());
    assertTrue(dry.hasAir());
    assertEquals(100, dry.airMinYInclusive());
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
