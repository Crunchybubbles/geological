package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Thin NeoForge bridge from Minecraft's dimension/chunk identity to the platform-neutral core. */
public final class GeologicalWorldgenAdapter {
  private GeologicalWorldgenAdapter() {}

  public static WorldgenChunkRequest request(
      long worldSeed,
      ResourceKey<Level> dimension,
      ChunkPos chunk,
      WorldgenStage authorizedThrough) {
    if (dimension == null || chunk == null || authorizedThrough == null) {
      throw new IllegalArgumentException("dimension, chunk, and stage are required");
    }
    DimensionGeologyProfile profile =
        DimensionGeologyProfiles.require(dimension.location().toString());
    return WorldgenChunkRequest.forStage(worldSeed, profile, chunk.x, chunk.z, authorizedThrough);
  }
}
