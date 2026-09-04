package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentChunkPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.NetherResourceChunkPlan;
import io.github.crunchybubbles.geological.worldgen.NetherResourcePlanner;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DimensionNativeShuffleTest {
  private static final long SEED = 8_675_309L;
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");

  @Test
  void netherChunkPlansDoNotDependOnRequestOrder() {
    NetherResourcePlanner planner =
        NetherResourcePlanner.from(NetherThermalTerrainCompiler.from(identity(NETHER)));
    List<ChunkCoordinate> coordinates =
        List.of(
            new ChunkCoordinate(-1L, 0L),
            new ChunkCoordinate(0L, 0L),
            new ChunkCoordinate(0L, -1L),
            new ChunkCoordinate(1L, 0L));
    Map<ChunkCoordinate, NetherResourceChunkPlan> expected = new LinkedHashMap<>();
    for (ChunkCoordinate coordinate : coordinates) {
      expected.put(coordinate, planner.plan(coordinate.chunkX(), coordinate.chunkZ()));
    }
    List<ChunkCoordinate> shuffled = new ArrayList<>(coordinates);
    Collections.shuffle(shuffled, new Random(0x4E45544845524CL));
    for (ChunkCoordinate coordinate : shuffled) {
      assertEquals(
          expected.get(coordinate), planner.plan(coordinate.chunkX(), coordinate.chunkZ()));
    }
  }

  @Test
  void endChunkPlansDoNotDependOnRequestOrderAcrossVoidAndIsland() {
    EndFragmentTerrainCompiler compiler = EndFragmentTerrainCompiler.from(identity(END));
    List<ChunkCoordinate> coordinates =
        List.of(
            new ChunkCoordinate(0L, 0L),
            new ChunkCoordinate(13L, 0L),
            new ChunkCoordinate(20L, 0L),
            new ChunkCoordinate(-1L, -1L));
    Map<ChunkCoordinate, EndFragmentChunkPlan> expected = new LinkedHashMap<>();
    for (ChunkCoordinate coordinate : coordinates) {
      expected.put(coordinate, compiler.plan(coordinate.chunkX(), coordinate.chunkZ()));
    }
    List<ChunkCoordinate> shuffled = new ArrayList<>(coordinates);
    Collections.shuffle(shuffled, new Random(0x454E445348554646L));
    for (ChunkCoordinate coordinate : shuffled) {
      assertEquals(
          expected.get(coordinate), compiler.plan(coordinate.chunkX(), coordinate.chunkZ()));
    }
  }

  private static WorldIdentity identity(DimensionGeologyProfile profile) {
    return new WorldIdentity(
        SEED, profile.version(), profile.scientificDigest(), profile.profileId());
  }

  private record ChunkCoordinate(long chunkX, long chunkZ) {}
}
