package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
