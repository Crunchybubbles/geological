package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainWriter;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldBaseTerrainWriterTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void emitsEverySolidBlockInStableOrderAndPreservesMaterialState() {
    OverworldBaseTerrainPlanner planner = OverworldBaseTerrainPlanner.from(context(-11, 17));
    List<WrittenBlock> first = new ArrayList<>();

    int count =
        OverworldBaseTerrainWriter.write(
            planner, (x, y, z, material) -> first.add(new WrittenBlock(x, y, z, material)));

    assertTrue(count > 0);
    assertEquals(count, first.size());
    assertTrue(
        first.stream()
            .allMatch(
                block ->
                    planner.context().targetBounds().contains(block.x(), block.y(), block.z())));
    assertTrue(first.stream().allMatch(block -> block.material() != null));
    for (int index = 1; index < first.size(); index++) {
      WrittenBlock previous = first.get(index - 1);
      WrittenBlock current = first.get(index);
      assertTrue(compare(previous, current) <= 0);
    }

    List<WrittenBlock> second = new ArrayList<>();
    assertEquals(
        count,
        OverworldBaseTerrainWriter.write(
            planner, (x, y, z, material) -> second.add(new WrittenBlock(x, y, z, material))));
    assertEquals(first, second);
  }

  @Test
  void requiresPlannerAndSink() {
    OverworldBaseTerrainPlanner planner = OverworldBaseTerrainPlanner.from(context(0, 0));
    assertThrows(
        NullPointerException.class,
        () -> OverworldBaseTerrainWriter.write(null, (x, y, z, m) -> {}));
    assertThrows(NullPointerException.class, () -> OverworldBaseTerrainWriter.write(planner, null));
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

  private record WrittenBlock(long x, int y, long z, MaterialState material) {}
}
