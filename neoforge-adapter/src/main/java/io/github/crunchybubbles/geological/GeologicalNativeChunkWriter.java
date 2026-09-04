package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.worldgen.EndFragmentChunkPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentColumnPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.EndProgressionPlanner;
import io.github.crunchybubbles.geological.worldgen.EndTerrainInterval;
import io.github.crunchybubbles.geological.worldgen.NetherResourceChunkPlan;
import io.github.crunchybubbles.geological.worldgen.NetherResourceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.NetherResourcePlanner;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Writes native Nether/End plans into one authorized target chunk. */
public final class GeologicalNativeChunkWriter {
  private GeologicalNativeChunkWriter() {}

  /** Writes Nether air, thermal host rock, lava, and source-linked resource horizons. */
  public static int writeNetherTerrain(ChunkAccess target, WorldgenChunkRequest request) {
    requireTarget(target, request, "minecraft:the_nether");
    NetherResourcePlanner planner =
        NetherResourcePlanner.from(NetherThermalTerrainCompiler.from(request.worldIdentity()));
    NetherResourceChunkPlan plan = planner.plan(request.chunkX(), request.chunkZ());
    int writes = 0;
    for (NetherResourceColumnPlan column : plan.columns()) {
      writes =
          Math.addExact(writes, writeAir(target, plan.bounds(), column.blockX(), column.blockZ()));
      for (var interval : column.thermal().solidIntervals()) {
        writes =
            Math.addExact(
                writes,
                writeInterval(
                    target,
                    column.blockX(),
                    column.blockZ(),
                    interval.minYInclusive(),
                    interval.maxYExclusive(),
                    GeologicalNativeBlockPalette.netherHost(column.thermal().provinceKind())));
      }
      if (column.resource().status()
          == io.github.crunchybubbles.geological.mineral.FormationStatus.FORMED) {
        for (var interval : column.intervals()) {
          writes =
              Math.addExact(
                  writes,
                  writeInterval(
                      target,
                      column.blockX(),
                      column.blockZ(),
                      interval.minYInclusive(),
                      interval.maxYExclusive(),
                      GeologicalNativeBlockPalette.netherResource(column.resource().family())));
        }
      }
      for (var interval : column.thermal().lavaIntervals()) {
        writes =
            Math.addExact(
                writes,
                writeInterval(
                    target,
                    column.blockX(),
                    column.blockZ(),
                    interval.minYInclusive(),
                    interval.maxYExclusive(),
                    Blocks.LAVA.defaultBlockState()));
      }
    }
    return writes;
  }

  /**
   * Writes End void, parent-fragment host rock, regolith, and impact melt while leaving protected
   * progression columns to the platform structure system.
   */
  public static int writeEndTerrain(ChunkAccess target, WorldgenChunkRequest request) {
    requireTarget(target, request, "minecraft:the_end");
    EndFragmentTerrainCompiler compiler = EndFragmentTerrainCompiler.from(request.worldIdentity());
    EndProgressionPlanner progression = EndProgressionPlanner.from(compiler);
    EndFragmentChunkPlan plan = compiler.plan(request.chunkX(), request.chunkZ());
    int writes = 0;
    for (EndFragmentColumnPlan column : plan.columns()) {
      if (!progression.canWriteTerrain(column.blockX(), column.blockZ())) {
        continue;
      }
      writes =
          Math.addExact(writes, writeAir(target, plan.bounds(), column.blockX(), column.blockZ()));
      if (column.body().isEmpty()) {
        continue;
      }
      var body = column.body().orElseThrow();
      for (EndTerrainInterval interval : column.solidIntervals()) {
        writes =
            Math.addExact(
                writes,
                writeInterval(
                    target,
                    column.blockX(),
                    column.blockZ(),
                    interval.minYInclusive(),
                    interval.maxYExclusive(),
                    GeologicalNativeBlockPalette.endHost(body.parentFamily())));
      }
      for (EndTerrainInterval interval : column.regolithIntervals()) {
        writes =
            Math.addExact(
                writes,
                writeInterval(
                    target,
                    column.blockX(),
                    column.blockZ(),
                    interval.minYInclusive(),
                    interval.maxYExclusive(),
                    GeologicalNativeBlockPalette.endRegolith()));
      }
      for (EndTerrainInterval interval : column.impactMeltIntervals()) {
        writes =
            Math.addExact(
                writes,
                writeInterval(
                    target,
                    column.blockX(),
                    column.blockZ(),
                    interval.minYInclusive(),
                    interval.maxYExclusive(),
                    GeologicalNativeBlockPalette.endImpactMelt()));
      }
    }
    return writes;
  }

  private static void requireTarget(
      ChunkAccess target, WorldgenChunkRequest request, String dimensionKey) {
    Objects.requireNonNull(target, "target chunk");
    Objects.requireNonNull(request, "worldgen chunk request");
    if (!dimensionKey.equals(request.dimensionKey())) {
      throw new IllegalArgumentException("native writer dimension does not match request");
    }
    request.requireStage(WorldgenStage.BASE_TERRAIN);
    if (!request.canWrite(WorldgenStage.BASE_TERRAIN)) {
      throw new IllegalStateException(
          "native terrain writer requires a writable base-terrain stage");
    }
    ChunkPos targetPos = Objects.requireNonNull(target.getPos(), "target chunk position");
    request.requireTargetChunk(targetPos.x, targetPos.z);
  }

  private static int writeAir(
      ChunkAccess target,
      io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds bounds,
      long blockX,
      long blockZ) {
    return writeInterval(
        target,
        blockX,
        blockZ,
        bounds.minY(),
        bounds.maxYExclusive(),
        Blocks.AIR.defaultBlockState());
  }

  private static int writeInterval(
      ChunkAccess target,
      long blockX,
      long blockZ,
      int minYInclusive,
      int maxYExclusive,
      BlockState state) {
    int writes = 0;
    for (int blockY = minYInclusive; blockY < maxYExclusive; blockY++) {
      target.setBlockState(
          new BlockPos(Math.toIntExact(blockX), blockY, Math.toIntExact(blockZ)), state, false);
      writes++;
    }
    return writes;
  }
}
