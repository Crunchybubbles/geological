package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
import io.github.crunchybubbles.geological.worldgen.OverworldVerticalSectionPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldVerticalSectionTrace;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldVerticalSectionPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void verticalSectionsAreDeterministicContiguousAndBounded() {
    OverworldVerticalSectionPlanner planner =
        OverworldVerticalSectionPlanner.from(OverworldRegolithPlanner.from(context()));

    OverworldVerticalSectionTrace first = planner.section(Axis.X, -176, 272, 4, 32);
    OverworldVerticalSectionTrace second = planner.section(Axis.X, -176, 272, 4, 32);

    assertEquals(first, second);
    assertEquals(4, first.columns().size());
    assertEquals(4, first.length());
    assertEquals(32, first.depthBlocks());
    assertFalse(first.provenanceBodyIds().isEmpty());
    assertTrue(first.materialEvaluations() > 0);
    assertTrue(first.summary().contains("vertical-section id="));
    assertEquals(-176, first.columns().getFirst().blockX());
    assertEquals(-173, first.columns().getLast().blockX());
  }

  @Test
  void verticalSectionsRejectInvalidBounds() {
    OverworldVerticalSectionPlanner planner =
        OverworldVerticalSectionPlanner.from(OverworldRegolithPlanner.from(context()));

    assertThrows(IllegalArgumentException.class, () -> planner.section(Axis.Z, 0, 0, 0, 16));
    assertThrows(IllegalArgumentException.class, () -> planner.section(Axis.Z, 0, 0, 65, 16));
    assertThrows(IllegalArgumentException.class, () -> planner.section(Axis.Z, 0, 0, 4, 257));
  }

  private static WorldgenExecutionContext context() {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            8_675_309L, OVERWORLD, -11, 17, WorldgenStage.REGOLITH_SURFACE_CLUES),
        WorldgenStage.REGOLITH_SURFACE_CLUES,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }
}
