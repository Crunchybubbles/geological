package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidWriter;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldAirFluidWriterTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void emitsBoundedAirAndWaterInStableOrder() {
    OverworldAirFluidPlanner planner =
        OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(-11, 17)), 107);
    List<WrittenBlock> first = new ArrayList<>();

    int count =
        OverworldAirFluidWriter.write(
            planner, (x, y, z, kind) -> first.add(new WrittenBlock(x, y, z, kind)));

    assertTrue(count > 0);
    assertEquals(count, first.size());
    assertTrue(
        first.stream()
            .allMatch(
                block ->
                    planner
                        .baseTerrain()
                        .context()
                        .targetBounds()
                        .contains(block.x(), block.y(), block.z())));
    assertTrue(
        first.stream()
            .anyMatch(block -> block.kind() == OverworldAirFluidWriter.BlockKind.SURFACE_WATER));
    assertTrue(
        first.stream().anyMatch(block -> block.kind() == OverworldAirFluidWriter.BlockKind.AIR));
    for (int index = 1; index < first.size(); index++) {
      WrittenBlock previous = first.get(index - 1);
      WrittenBlock current = first.get(index);
      assertTrue(compare(previous, current) <= 0);
    }

    List<WrittenBlock> second = new ArrayList<>();
    assertEquals(
        count,
        OverworldAirFluidWriter.write(
            planner, (x, y, z, kind) -> second.add(new WrittenBlock(x, y, z, kind))));
    assertEquals(first, second);
  }

  @Test
  void requiresPlannerAndSink() {
    OverworldAirFluidPlanner planner =
        OverworldAirFluidPlanner.from(OverworldBaseTerrainPlanner.from(context(0, 0)));
    assertThrows(
        NullPointerException.class,
        () -> OverworldAirFluidWriter.write(null, (x, y, z, kind) -> {}));
    assertThrows(NullPointerException.class, () -> OverworldAirFluidWriter.write(planner, null));
  }

  private static int compare(WrittenBlock left, WrittenBlock right) {
    int x = Long.compare(left.x(), right.x());
    if (x != 0) {
      return x;
    }
    int z = Long.compare(left.z(), right.z());
    if (z != 0) {
      return z;
    }
    return Integer.compare(left.y(), right.y());
  }

  private static WorldgenExecutionContext context(long chunkX, long chunkZ) {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.BASE_TERRAIN),
        WorldgenStage.BASE_TERRAIN,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }

  private record WrittenBlock(long x, int y, long z, OverworldAirFluidWriter.BlockKind kind) {}
}
