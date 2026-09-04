package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.HandSampleIdentification;
import io.github.crunchybubbles.geological.worldgen.OverworldHandSamplePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldHandSamplePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void surfaceAndSubsurfaceIdentificationsAreDeterministicAndCoarse() {
    OverworldHandSamplePlanner planner =
        OverworldHandSamplePlanner.from(
            OverworldRegolithPlanner.from(
                new WorldgenExecutionContext(
                    WorldgenChunkRequest.forStage(
                        8_675_309L, OVERWORLD, -11, 17, WorldgenStage.REGOLITH_SURFACE_CLUES),
                    WorldgenStage.REGOLITH_SURFACE_CLUES,
                    WorldgenSnapshot.forProfile(OVERWORLD),
                    Runnable::run)));

    HandSampleIdentification first = planner.identifySurface(-161, 273);
    HandSampleIdentification second = planner.identifySurface(-161, 273);
    HandSampleIdentification subsurface = planner.identify(-161, first.blockY() - 32, 273);

    assertEquals(first, second);
    assertTrue(first.visibleConstituentsPpm().size() <= 8);
    assertTrue(first.visibleConstituentsPpm().size() > 0);
    assertTrue(first.assayRequired());
    assertEquals("SUBSURFACE", subsurface.samplingContext());
    assertTrue(subsurface.summary().contains("hand-sample id="));
  }

  @Test
  void airAndFluidBlocksCannotBeCollectedAsHandSamples() {
    OverworldHandSamplePlanner planner =
        OverworldHandSamplePlanner.from(
            OverworldRegolithPlanner.from(
                new WorldgenExecutionContext(
                    WorldgenChunkRequest.forStage(
                        8_675_309L, OVERWORLD, -11, 17, WorldgenStage.REGOLITH_SURFACE_CLUES),
                    WorldgenStage.REGOLITH_SURFACE_CLUES,
                    WorldgenSnapshot.forProfile(OVERWORLD),
                    Runnable::run)));

    assertThrows(IllegalArgumentException.class, () -> planner.identify(-161, 200, 273));
  }
}
