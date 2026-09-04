package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Objects;

/** Applies bounded regolith intervals while preserving their coarse surface-clue relationship. */
public final class OverworldRegolithWriter {
  private OverworldRegolithWriter() {}

  @FunctionalInterface
  public interface BlockSink {
    void set(
        long blockX, int blockY, long blockZ, MaterialState material, SurfaceClueKind clueKind);
  }

  /** Writes only the authorized target footprint in deterministic X-then-Z-then-Y order. */
  public static int write(OverworldRegolithPlanner planner, BlockSink sink) {
    Objects.requireNonNull(planner, "regolith planner");
    Objects.requireNonNull(sink, "regolith block sink");

    WorldgenExecutionContext context = planner.context();
    context.requireWritableTargetChunk(context.request().chunkX(), context.request().chunkZ());
    ChunkBlockBounds bounds = context.targetBounds();
    int writes = 0;
    for (OverworldRegolithColumnPlan column : planner.planTargetChunk()) {
      for (int blockY = column.regolithMinYInclusive();
          blockY < column.solidMaxYExclusive();
          blockY++) {
        if (!bounds.contains(column.blockX(), blockY, column.blockZ())) {
          throw new IllegalStateException("regolith plan escaped the authorized target bounds");
        }
        sink.set(
            column.blockX(), blockY, column.blockZ(), column.surfaceMaterial(), column.clueKind());
        writes++;
      }
    }
    return writes;
  }
}
