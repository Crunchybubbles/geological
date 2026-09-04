package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** Exact 16×16 target-chunk projection of the Nether thermal/cavern compiler. */
public record NetherThermalChunkPlan(
    long chunkX, long chunkZ, ChunkBlockBounds bounds, List<NetherThermalColumnPlan> columns) {
  public NetherThermalChunkPlan {
    if (bounds == null || columns == null || bounds.width() != 16 || bounds.depth() != 16) {
      throw new IllegalArgumentException("Nether thermal chunk bounds must be 16 by 16");
    }
    columns = List.copyOf(columns);
    if (columns.size() != 256) {
      throw new IllegalArgumentException("Nether thermal chunk must contain exactly 256 columns");
    }
    int index = 0;
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        NetherThermalColumnPlan column = columns.get(index++);
        if (column == null || column.blockX() != blockX || column.blockZ() != blockZ) {
          throw new IllegalArgumentException(
              "Nether thermal columns must use stable X-then-Z order");
        }
      }
    }
  }

  public NetherThermalColumnPlan at(long blockX, long blockZ) {
    if (!bounds.contains(blockX, bounds.minY(), blockZ)) {
      throw new IllegalArgumentException("Nether column is outside the target chunk");
    }
    int offsetX = Math.toIntExact(blockX - bounds.minX());
    int offsetZ = Math.toIntExact(blockZ - bounds.minZ());
    return columns.get(offsetX * 16 + offsetZ);
  }

  public long lavaColumnCount() {
    return columns.stream().filter(NetherThermalColumnPlan::hasLava).count();
  }

  public long bridgeColumnCount() {
    return columns.stream().filter(NetherThermalColumnPlan::hangingBridge).count();
  }

  public long provinceCount(StableId provinceId) {
    return columns.stream().filter(column -> column.provinceId().equals(provinceId)).count();
  }
}
