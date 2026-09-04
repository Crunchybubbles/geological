package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithWriter;
import io.github.crunchybubbles.geological.worldgen.SurfaceClueKind;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldRegolithWriterTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void emitsOnlyRegolithBlocksInStableOrderWithClueKind() {
    OverworldRegolithPlanner planner = OverworldRegolithPlanner.from(context(-11, 17));
    List<WrittenBlock> first = new ArrayList<>();
    int count =
        OverworldRegolithWriter.write(
            planner,
            (x, y, z, material, clue) -> first.add(new WrittenBlock(x, y, z, material, clue)));

    assertTrue(count > 0);
    assertEquals(count, first.size());
    assertTrue(
        first.stream()
            .allMatch(
                block ->
                    planner.context().targetBounds().contains(block.x(), block.y(), block.z())));
    assertTrue(first.stream().allMatch(block -> block.material() != null));
    assertTrue(first.stream().allMatch(block -> block.clueKind() != null));
    for (int index = 1; index < first.size(); index++) {
      WrittenBlock previous = first.get(index - 1);
      WrittenBlock current = first.get(index);
      assertTrue(compare(previous, current) <= 0);
    }

    List<WrittenBlock> second = new ArrayList<>();
    assertEquals(
        count,
        OverworldRegolithWriter.write(
            planner,
            (x, y, z, material, clue) -> second.add(new WrittenBlock(x, y, z, material, clue))));
    assertEquals(first, second);
  }

  @Test
  void requiresPlannerAndSink() {
    OverworldRegolithPlanner planner = OverworldRegolithPlanner.from(context(0, 0));
    assertThrows(
        NullPointerException.class,
        () -> OverworldRegolithWriter.write(null, (x, y, z, material, clue) -> {}));
    assertThrows(NullPointerException.class, () -> OverworldRegolithWriter.write(planner, null));
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
            8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
        WorldgenStage.REGOLITH_SURFACE_CLUES,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }

  private record WrittenBlock(
      long x, int y, long z, MaterialState material, SurfaceClueKind clueKind) {}
}
