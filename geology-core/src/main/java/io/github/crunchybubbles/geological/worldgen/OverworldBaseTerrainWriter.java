package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.query.MaterialRun;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Objects;

/**
 * Applies a validated Overworld base-terrain plan to a caller-owned block sink.
 *
 * <p>The writer invokes the sink once for every solid geological block in deterministic
 * X-then-Z-then-Y order. It deliberately knows nothing about Minecraft block states, air, fluids,
 * or palette registration; a platform adapter supplies those concerns through the sink.
 */
public final class OverworldBaseTerrainWriter {
  private OverworldBaseTerrainWriter() {}

  @FunctionalInterface
  public interface BlockSink {
    void set(long blockX, int blockY, long blockZ, MaterialState material);
  }

  /** Writes the planner's authorized target footprint and returns the number of blocks emitted. */
  public static int write(OverworldBaseTerrainPlanner planner, BlockSink sink) {
    Objects.requireNonNull(planner, "base-terrain planner");
    Objects.requireNonNull(sink, "base-terrain block sink");

    WorldgenExecutionContext context = planner.context();
    context.requireWritableTargetChunk(context.request().chunkX(), context.request().chunkZ());
    ChunkBlockBounds bounds = context.targetBounds();
    int writes = 0;
    for (OverworldBaseTerrainColumnPlan column : planner.planTargetChunk()) {
      for (MaterialRun run : column.lithologyRuns()) {
        for (int blockY = run.minYInclusive(); blockY < run.maxYExclusive(); blockY++) {
          if (!bounds.contains(column.blockX(), blockY, column.blockZ())) {
            throw new IllegalStateException(
                "base-terrain plan escaped the authorized target bounds");
          }
          sink.set(column.blockX(), blockY, column.blockZ(), run.state());
          writes++;
        }
      }
    }
    return writes;
  }
}
