package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSampler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.Objects;
import java.util.concurrent.Executor;
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

  /** Binds a platform-supplied executor and frozen snapshot to the read-only terrain stage. */
  public static WorldgenExecutionContext coarseTerrainContext(
      long worldSeed,
      ResourceKey<Level> dimension,
      ChunkPos chunk,
      WorldgenSnapshot snapshot,
      Executor executor) {
    Objects.requireNonNull(snapshot, "worldgen snapshot");
    Objects.requireNonNull(executor, "stage-supplied executor");
    WorldgenChunkRequest request =
        request(worldSeed, dimension, chunk, WorldgenStage.COARSE_TERRAIN_CONTROLS);
    return new WorldgenExecutionContext(
        request, WorldgenStage.COARSE_TERRAIN_CONTROLS, snapshot, executor);
  }

  /** Exposes the platform-neutral read-only sampler for a validated coarse-terrain callback. */
  public static OverworldTerrainControlSampler coarseTerrainControls(
      WorldgenExecutionContext context) {
    return OverworldTerrainControlSampler.from(context);
  }
}
