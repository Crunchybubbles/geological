package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSample;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSampler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldTerrainControlSamplerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void sampleIsDeterministicAcrossConstructionOrderAndCacheEviction() {
    WorldgenExecutionContext firstContext = context(-11, 17);
    WorldgenExecutionContext secondContext = context(-10, 17);
    OverworldTerrainControlSampler first = OverworldTerrainControlSampler.from(firstContext);
    OverworldTerrainControlSampler second = OverworldTerrainControlSampler.from(secondContext);

    OverworldTerrainControlSample expected = first.sample(-161, 273);
    second.sample(-160, 273);
    first.clearCaches();
    second.clearCaches();
    OverworldTerrainControlSample actual = first.sample(-161, 273);

    assertEquals(expected, actual);
    assertEquals(expected.provinceId(), actual.provinceId());
    assertEquals(expected.macroDomainId(), actual.macroDomainId());
    assertTrue(Double.isFinite(expected.elevation()));
    assertTrue(expected.slope() >= 0.0);
    assertTrue(expected.flowAccumulation() >= 0.0 && expected.flowAccumulation() <= 1.0);
    assertTrue(expected.channelDistance() >= 0.0);
  }

  @Test
  void adjacentChunkContextsAgreeAtTheSameWorldColumn() {
    OverworldTerrainControlSample fromWest =
        OverworldTerrainControlSampler.from(context(-1, 0)).sample(-1, 3);
    OverworldTerrainControlSample fromEast =
        OverworldTerrainControlSampler.from(context(0, 0)).sample(-1, 3);

    assertEquals(fromWest, fromEast);
    assertEquals(-1, fromWest.blockX());
    assertEquals(-1, fromEast.blockX());
  }

  @Test
  void samplerIsReadOnlyAndAvailableAfterCoarseStage() {
    WorldgenExecutionContext context = context(0, 0);
    OverworldTerrainControlSampler sampler = OverworldTerrainControlSampler.from(context);

    assertFalse(context.canWriteTarget());
    WorldgenExecutionContext base =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.BASE_TERRAIN),
            WorldgenStage.BASE_TERRAIN,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    assertTrue(OverworldTerrainControlSampler.from(base).sample(0, 0).elevation() > 0.0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OverworldTerrainControlSampler.from(
                new WorldgenExecutionContext(
                    WorldgenChunkRequest.forStage(
                        8_675_309L, OVERWORLD, 0, 0, WorldgenStage.ACQUIRE_CONTEXT),
                    WorldgenStage.ACQUIRE_CONTEXT,
                    WorldgenSnapshot.forProfile(OVERWORLD),
                    Runnable::run)));
    assertEquals(context, sampler.context());
  }

  @Test
  void samplerRejectsFictionalDimensions() {
    DimensionGeologyProfile nether = DimensionGeologyProfiles.require("minecraft:the_nether");
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, nether, 0, 0, WorldgenStage.COARSE_TERRAIN_CONTROLS),
            WorldgenStage.COARSE_TERRAIN_CONTROLS,
            WorldgenSnapshot.forProfile(nether),
            Runnable::run);

    assertThrows(
        IllegalArgumentException.class, () -> OverworldTerrainControlSampler.from(context));
  }

  private static WorldgenExecutionContext context(long chunkX, long chunkZ) {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.COARSE_TERRAIN_CONTROLS),
        WorldgenStage.COARSE_TERRAIN_CONTROLS,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }
}
