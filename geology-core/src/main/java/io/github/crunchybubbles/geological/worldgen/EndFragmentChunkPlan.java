package io.github.crunchybubbles.geological.worldgen;

import java.util.List;

/** Exact 16×16 target-chunk projection of End islands and void. */
public record EndFragmentChunkPlan(
    long chunkX, long chunkZ, ChunkBlockBounds bounds, List<EndFragmentColumnPlan> columns) {
  public EndFragmentChunkPlan {
    if (bounds == null || columns == null || bounds.width() != 16 || bounds.depth() != 16) {
      throw new IllegalArgumentException("End fragment chunk bounds must be 16 by 16");
    }
    columns = List.copyOf(columns);
    if (columns.size() != 256) {
      throw new IllegalArgumentException("End fragment chunk must contain exactly 256 columns");
    }
    int index = 0;
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        EndFragmentColumnPlan column = columns.get(index++);
        if (column == null || column.blockX() != blockX || column.blockZ() != blockZ) {
          throw new IllegalArgumentException("End fragment columns must use stable X-then-Z order");
        }
      }
    }
  }

  public EndFragmentColumnPlan at(long blockX, long blockZ) {
    if (!bounds.contains(blockX, bounds.minY(), blockZ)) {
      throw new IllegalArgumentException("End fragment column is outside the target chunk");
    }
    int offsetX = Math.toIntExact(blockX - bounds.minX());
    int offsetZ = Math.toIntExact(blockZ - bounds.minZ());
    return columns.get(offsetX * 16 + offsetZ);
  }

  public long islandColumnCount() {
    return columns.stream().filter(column -> !column.isVoid()).count();
  }

  public long voidColumnCount() {
    return columns.stream().filter(EndFragmentColumnPlan::isVoid).count();
  }

  public long impactColumnCount() {
    return columns.stream().filter(column -> !column.impactMeltIntervals().isEmpty()).count();
  }

  public long regolithColumnCount() {
    return columns.stream().filter(column -> !column.regolithIntervals().isEmpty()).count();
  }
}
