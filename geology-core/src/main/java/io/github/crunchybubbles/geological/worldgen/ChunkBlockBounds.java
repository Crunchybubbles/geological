package io.github.crunchybubbles.geological.worldgen;

/** Half-open integer block bounds for one authorized chunk write. */
public record ChunkBlockBounds(
    long minX, int minY, long minZ, long maxXExclusive, int maxYExclusive, long maxZExclusive) {
  public ChunkBlockBounds {
    if (maxXExclusive <= minX || maxYExclusive <= minY || maxZExclusive <= minZ) {
      throw new IllegalArgumentException("chunk block bounds must have positive dimensions");
    }
  }

  public long width() {
    return maxXExclusive - minX;
  }

  public int height() {
    return maxYExclusive - minY;
  }

  public long depth() {
    return maxZExclusive - minZ;
  }

  public boolean contains(long x, int y, long z) {
    return x >= minX
        && x < maxXExclusive
        && y >= minY
        && y < maxYExclusive
        && z >= minZ
        && z < maxZExclusive;
  }
}
