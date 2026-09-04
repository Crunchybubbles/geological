package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.RandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.Objects;

/** Frozen seed, dimension, and chunk identity consumed by a platform adapter. */
public record WorldgenChunkIdentity(
    WorldIdentity worldIdentity, DimensionGeologyProfile profile, long chunkX, long chunkZ) {
  public static final int CHUNK_SIZE_BLOCKS = 16;
  private static final String CHUNK_CELL_LEVEL = "minecraft:chunk";
  private static final String WORLDGEN_NAMESPACE = "geological:worldgen";

  public WorldgenChunkIdentity {
    Objects.requireNonNull(worldIdentity, "world identity");
    Objects.requireNonNull(profile, "dimension geology profile");
    if (!profile.profileId().equals(worldIdentity.dimensionProfileId())) {
      throw new IllegalArgumentException(
          "world identity profile does not match dimension geology profile");
    }
    if (!profile.version().equals(worldIdentity.modelVersion())) {
      throw new IllegalArgumentException(
          "world identity model version does not match dimension geology profile");
    }
    if (!profile.scientificDigest().equals(worldIdentity.scientificDigest())) {
      throw new IllegalArgumentException(
          "world identity scientific digest does not match dimension geology profile");
    }
  }

  /** Creates a chunk identity from the frozen profile and the Minecraft world seed. */
  public static WorldgenChunkIdentity forSeed(
      long worldSeed, DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    Objects.requireNonNull(profile, "dimension geology profile");
    WorldIdentity identity =
        new WorldIdentity(
            worldSeed, profile.version(), profile.scientificDigest(), profile.profileId());
    return new WorldgenChunkIdentity(identity, profile, chunkX, chunkZ);
  }

  public String dimensionKey() {
    return profile.dimensionKey();
  }

  /** Stable identity for the chunk itself, independent of the stage at which it is queried. */
  public StableId chunkId() {
    return worldIdentity.stream(
            WORLDGEN_NAMESPACE, "chunk", new CellKey(CHUNK_CELL_LEVEL, chunkX, chunkZ), 0L)
        .stableId();
  }

  /** Domain-separated random-access stream for one logical stage of this chunk. */
  public RandomStream stageStream(WorldgenStage stage) {
    Objects.requireNonNull(stage, "worldgen stage");
    return worldIdentity.stream(
        WORLDGEN_NAMESPACE, stage.id(), new CellKey(CHUNK_CELL_LEVEL, chunkX, chunkZ), 0L);
  }

  /** Half-open target bounds; the profile's maximum Y is an inclusive world envelope value. */
  public ChunkBlockBounds targetBounds() {
    long minX = Math.multiplyExact(chunkX, (long) CHUNK_SIZE_BLOCKS);
    long minZ = Math.multiplyExact(chunkZ, (long) CHUNK_SIZE_BLOCKS);
    int maxYExclusive = Math.addExact(profile.verticalEnvelope().maximumY(), 1);
    return new ChunkBlockBounds(
        minX,
        profile.verticalEnvelope().minimumY(),
        minZ,
        Math.addExact(minX, CHUNK_SIZE_BLOCKS),
        maxYExclusive,
        Math.addExact(minZ, CHUNK_SIZE_BLOCKS));
  }

  public CellKey chunkCell() {
    return new CellKey(CHUNK_CELL_LEVEL, chunkX, chunkZ);
  }
}
