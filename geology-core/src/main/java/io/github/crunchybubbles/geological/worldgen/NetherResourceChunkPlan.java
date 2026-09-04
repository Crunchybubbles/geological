package io.github.crunchybubbles.geological.worldgen;

import java.util.List;

/** Exact 16×16 target-chunk projection of Nether material history and resource bodies. */
public record NetherResourceChunkPlan(
    long chunkX, long chunkZ, ChunkBlockBounds bounds, List<NetherResourceColumnPlan> columns) {
  public NetherResourceChunkPlan {
    if (bounds == null || columns == null || bounds.width() != 16 || bounds.depth() != 16) {
      throw new IllegalArgumentException("Nether resource chunk bounds must be 16 by 16");
    }
    columns = List.copyOf(columns);
    if (columns.size() != 256) {
      throw new IllegalArgumentException("Nether resource chunk must contain exactly 256 columns");
    }
    int index = 0;
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        NetherResourceColumnPlan column = columns.get(index++);
        if (column == null || column.blockX() != blockX || column.blockZ() != blockZ) {
          throw new IllegalArgumentException("Nether resource columns must use X-then-Z order");
        }
      }
    }
  }

  public NetherResourceColumnPlan at(long blockX, long blockZ) {
    if (!bounds.contains(blockX, bounds.minY(), blockZ)) {
      throw new IllegalArgumentException("Nether resource column is outside the target chunk");
    }
    int offsetX = Math.toIntExact(blockX - bounds.minX());
    int offsetZ = Math.toIntExact(blockZ - bounds.minZ());
    return columns.get(offsetX * 16 + offsetZ);
  }

  public long columnsWithResource() {
    return columns.stream().filter(NetherResourceColumnPlan::hasResource).count();
  }

  public long formedColumnCount() {
    return columns.stream()
        .filter(
            column ->
                column.resource().status()
                    == io.github.crunchybubbles.geological.mineral.FormationStatus.FORMED)
        .count();
  }

  public long intervalCount() {
    return columns.stream().mapToLong(column -> column.intervals().size()).sum();
  }
}
