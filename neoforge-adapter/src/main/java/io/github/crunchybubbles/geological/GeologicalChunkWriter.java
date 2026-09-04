package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainWriter;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** NeoForge bridge that writes only the authorized solid geology of one target chunk. */
public final class GeologicalChunkWriter {
  private GeologicalChunkWriter() {}

  /**
   * Writes the bounded base-terrain plan into {@code target} using a caller-owned material palette.
   *
   * <p>The resolver is memoized by immutable material state for this chunk. Air, fluids, surface
   * decoration, and palette registration are intentionally outside this increment.
   */
  public static int writeBaseTerrain(
      ChunkAccess target,
      WorldgenExecutionContext context,
      Function<MaterialState, BlockState> blockResolver) {
    Objects.requireNonNull(target, "target chunk");
    Objects.requireNonNull(context, "worldgen execution context");
    Objects.requireNonNull(blockResolver, "material block resolver");

    ChunkPos targetPos = Objects.requireNonNull(target.getPos(), "target chunk position");
    context.requireWritableTargetChunk(targetPos.x, targetPos.z);
    OverworldBaseTerrainPlanner planner = GeologicalWorldgenAdapter.baseTerrainPlanner(context);
    Map<MaterialState, BlockState> paletteCache = new HashMap<>();
    return OverworldBaseTerrainWriter.write(
        planner,
        (blockX, blockY, blockZ, material) -> {
          BlockState blockState =
              paletteCache.computeIfAbsent(
                  material,
                  key ->
                      Objects.requireNonNull(
                          blockResolver.apply(key), "material palette returned null"));
          target.setBlockState(
              new BlockPos(Math.toIntExact(blockX), blockY, Math.toIntExact(blockZ)),
              blockState,
              false);
        });
  }
}
