package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSample;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class GeologicalWorldgenAdapterTest {
  @Test
  void mapsOverworldDimensionAndChunkToCoreIdentity() {
    var request =
        GeologicalWorldgenAdapter.request(
            8_675_309L, Level.OVERWORLD, new ChunkPos(-11, 17), WorldgenStage.LITHOLOGY);

    assertEquals("minecraft:overworld", request.dimensionKey());
    assertEquals(-11, request.chunkX());
    assertEquals(17, request.chunkZ());
    assertEquals(WorldgenStage.LITHOLOGY, request.authorizedThrough());
  }

  @Test
  void rejectsUnknownDimensionAndMissingArgumentsBeforeCoreWork() {
    ResourceKey<Level> unknownDimension =
        ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("geological", "unknown"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GeologicalWorldgenAdapter.request(
                8_675_309L, unknownDimension, new ChunkPos(0, 0), WorldgenStage.BASE_TERRAIN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GeologicalWorldgenAdapter.request(
                8_675_309L, null, new ChunkPos(0, 0), WorldgenStage.BASE_TERRAIN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GeologicalWorldgenAdapter.request(
                8_675_309L, Level.OVERWORLD, null, WorldgenStage.BASE_TERRAIN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GeologicalWorldgenAdapter.request(
                8_675_309L, Level.OVERWORLD, new ChunkPos(0, 0), null));
  }

  @Test
  void bindsCoarseTerrainCallbackToSnapshotAndSuppliedExecutor() {
    var overworld = DimensionGeologyProfiles.require("minecraft:overworld");
    var context =
        GeologicalWorldgenAdapter.coarseTerrainContext(
            8_675_309L,
            Level.OVERWORLD,
            new ChunkPos(-11, 17),
            WorldgenSnapshot.forProfile(overworld),
            Runnable::run);

    OverworldTerrainControlSample sample =
        GeologicalWorldgenAdapter.coarseTerrainControls(context).sample(-161, 273);

    assertEquals(WorldgenStage.COARSE_TERRAIN_CONTROLS, context.stage());
    assertEquals(-161, sample.blockX());
    assertEquals(273, sample.blockZ());
    assertEquals(overworld.profileId(), context.request().profile().profileId());
  }

  @Test
  void bindsBaseTerrainCallbackToChunkLocalPlanWithoutWritingAChunk() {
    var overworld = DimensionGeologyProfiles.require("minecraft:overworld");
    var context =
        GeologicalWorldgenAdapter.baseTerrainContext(
            8_675_309L,
            Level.OVERWORLD,
            new ChunkPos(-11, 17),
            WorldgenSnapshot.forProfile(overworld),
            Runnable::run);

    OverworldBaseTerrainPlanner planner = GeologicalWorldgenAdapter.baseTerrainPlanner(context);
    OverworldBaseTerrainColumnPlan plan = planner.plan(-161, 273);

    assertEquals(WorldgenStage.BASE_TERRAIN, context.stage());
    assertTrue(context.canWriteTarget());
    assertTrue(context.targetBounds().contains(plan.blockX(), plan.minYInclusive(), plan.blockZ()));
    assertTrue(plan.solidMaxYExclusive() >= plan.minYInclusive());
    assertTrue(plan.solidMaxYExclusive() <= plan.maxYExclusive());
  }
}
