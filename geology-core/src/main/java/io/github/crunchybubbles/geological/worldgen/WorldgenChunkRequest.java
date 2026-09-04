package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.RandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable adapter request authorized through one logical chunk-generation stage. */
public record WorldgenChunkRequest(WorldgenChunkIdentity chunk, WorldgenStage authorizedThrough) {
  public WorldgenChunkRequest {
    Objects.requireNonNull(chunk, "worldgen chunk identity");
    Objects.requireNonNull(authorizedThrough, "authorized worldgen stage");
  }

  /** Creates a request authorized through the complete logical stage sequence. */
  public static WorldgenChunkRequest forChunk(
      long worldSeed, DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    return forStage(worldSeed, profile, chunkX, chunkZ, WorldgenStage.VALIDATE_METRICS);
  }

  /** Creates a request authorized through the supplied logical stage. */
  public static WorldgenChunkRequest forStage(
      long worldSeed,
      DimensionGeologyProfile profile,
      long chunkX,
      long chunkZ,
      WorldgenStage authorizedThrough) {
    return new WorldgenChunkRequest(
        WorldgenChunkIdentity.forSeed(worldSeed, profile, chunkX, chunkZ), authorizedThrough);
  }

  /** Rebinds an already-frozen identity to a platform status stage. */
  public static WorldgenChunkRequest from(
      WorldIdentity worldIdentity,
      DimensionGeologyProfile profile,
      long chunkX,
      long chunkZ,
      WorldgenStage authorizedThrough) {
    return new WorldgenChunkRequest(
        new WorldgenChunkIdentity(worldIdentity, profile, chunkX, chunkZ), authorizedThrough);
  }

  public WorldIdentity worldIdentity() {
    return chunk.worldIdentity();
  }

  public DimensionGeologyProfile profile() {
    return chunk.profile();
  }

  public long chunkX() {
    return chunk.chunkX();
  }

  public long chunkZ() {
    return chunk.chunkZ();
  }

  public String dimensionKey() {
    return chunk.dimensionKey();
  }

  public StableId chunkId() {
    return chunk.chunkId();
  }

  public ChunkBlockBounds targetBounds() {
    return chunk.targetBounds();
  }

  /** Returns the exact ordered prefix of stages authorized for this request. */
  public List<WorldgenStage> requiredStages() {
    return List.copyOf(
        Arrays.stream(WorldgenStage.values())
            .filter(stage -> stage.isAtOrBefore(authorizedThrough))
            .toList());
  }

  public boolean includes(WorldgenStage stage) {
    Objects.requireNonNull(stage, "worldgen stage");
    return stage.isAtOrBefore(authorizedThrough);
  }

  /** True only for an included stage whose adapter work is allowed to write the target chunk. */
  public boolean canWrite(WorldgenStage stage) {
    Objects.requireNonNull(stage, "worldgen stage");
    return includes(stage) && stage.writesChunk();
  }

  /** Returns a stage stream only when that stage is authorized by this request. */
  public RandomStream stageStream(WorldgenStage stage) {
    if (!includes(stage)) {
      throw new IllegalArgumentException(
          "stage " + stage + " is not authorized through " + authorizedThrough);
    }
    return chunk.stageStream(stage);
  }

  public void requireStage(WorldgenStage stage) {
    if (!includes(stage)) {
      throw new IllegalArgumentException(
          "stage " + stage + " is not authorized through " + authorizedThrough);
    }
  }

  /** Enforces the chunk-local write rule at the adapter boundary. */
  public void requireTargetChunk(long targetChunkX, long targetChunkZ) {
    if (targetChunkX != chunkX() || targetChunkZ != chunkZ()) {
      throw new IllegalArgumentException(
          "worldgen request may write only its authorized target chunk");
    }
  }
}
