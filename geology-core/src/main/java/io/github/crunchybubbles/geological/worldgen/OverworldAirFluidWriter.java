package io.github.crunchybubbles.geological.worldgen;

import java.util.Objects;

/** Applies explicit air and surface-water intervals to a caller-owned block sink. */
public final class OverworldAirFluidWriter {
  private OverworldAirFluidWriter() {}

  public enum BlockKind {
    SURFACE_WATER,
    AIR
  }

  @FunctionalInterface
  public interface BlockSink {
    void set(long blockX, int blockY, long blockZ, BlockKind kind);
  }

  /** Writes only the authorized target footprint in deterministic X-then-Z-then-Y order. */
  public static int write(OverworldAirFluidPlanner planner, BlockSink sink) {
    Objects.requireNonNull(planner, "air/fluid planner");
    Objects.requireNonNull(sink, "air/fluid block sink");

    WorldgenExecutionContext context = planner.baseTerrain().context();
    context.requireWritableTargetChunk(context.request().chunkX(), context.request().chunkZ());
    ChunkBlockBounds bounds = context.targetBounds();
    int writes = 0;
    for (OverworldAirFluidColumnPlan column : planner.planTargetChunk()) {
      for (int blockY = column.solidMaxYExclusive(); blockY < column.maxYExclusive(); blockY++) {
        if (!bounds.contains(column.blockX(), blockY, column.blockZ())) {
          throw new IllegalStateException("air/fluid plan escaped the authorized target bounds");
        }
        BlockKind kind =
            blockY < column.surfaceWaterMaxYExclusive() ? BlockKind.SURFACE_WATER : BlockKind.AIR;
        sink.set(column.blockX(), blockY, column.blockZ(), kind);
        writes++;
      }
    }
    return writes;
  }
}
