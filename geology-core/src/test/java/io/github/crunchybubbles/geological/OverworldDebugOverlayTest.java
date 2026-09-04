package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldColumnDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldMapDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldDebugOverlayTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void mapAndSectionOverlaysAreDeterministicAndOrdered() {
    OverworldRegolithPlanner planner = planner();
    OverworldMapDebugTrace first = map(planner, -1, 2, 1);
    OverworldMapDebugTrace second = map(planner, -1, 2, 1);

    assertEquals(first, second);
    assertEquals(9, first.columns().size());
    assertTrue(first.summary().startsWith("map center=(-1,2) radius=1 columns=9"));

    List<OverworldColumnDebugTrace> sectionColumns =
        List.of(first.columns().get(0), first.columns().get(1), first.columns().get(2));
    OverworldSectionDebugTrace section =
        new OverworldSectionDebugTrace(Axis.Z, -2, 1, 3, sectionColumns);
    assertTrue(section.summary().startsWith("section axis=Z origin=(-2,1) length=3"));
  }

  @Test
  void overlaysRejectIncompleteOrMisorderedFootprints() {
    OverworldRegolithPlanner planner = planner();
    OverworldColumnDebugTrace column = trace(planner, -1, 2);

    assertThrows(
        IllegalArgumentException.class,
        () -> new OverworldMapDebugTrace(-1, 2, 1, List.of(column)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OverworldSectionDebugTrace(Axis.X, -1, 2, 2, List.of(column, column)));
  }

  private static OverworldMapDebugTrace map(
      OverworldRegolithPlanner planner, long centerX, long centerZ, int radius) {
    List<OverworldColumnDebugTrace> columns = new ArrayList<>();
    for (long blockX = centerX - radius; blockX <= centerX + radius; blockX++) {
      for (long blockZ = centerZ - radius; blockZ <= centerZ + radius; blockZ++) {
        columns.add(trace(planner, blockX, blockZ));
      }
    }
    return new OverworldMapDebugTrace(centerX, centerZ, radius, columns);
  }

  private static OverworldColumnDebugTrace trace(
      OverworldRegolithPlanner planner, long blockX, long blockZ) {
    return OverworldColumnDebugTrace.from(
        planner.baseTerrain().plan(blockX, blockZ),
        OverworldAirFluidPlanner.from(planner.baseTerrain()).plan(blockX, blockZ),
        planner.plan(blockX, blockZ));
  }

  private static OverworldRegolithPlanner planner() {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, -1, 2, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldRegolithPlanner.from(context);
  }
}
