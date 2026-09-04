package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.ExplorationObservationKind;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservation;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservationPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldExplorationObservationTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void observationsAreDeterministicBoundedAndProvenanceRich() {
    OverworldExplorationObservationPlanner planner =
        OverworldExplorationObservationPlanner.from(
            OverworldRegolithPlanner.from(
                new WorldgenExecutionContext(
                    WorldgenChunkRequest.forStage(
                        8_675_309L, OVERWORLD, -11, 17, WorldgenStage.REGOLITH_SURFACE_CLUES),
                    WorldgenStage.REGOLITH_SURFACE_CLUES,
                    WorldgenSnapshot.forProfile(OVERWORLD),
                    Runnable::run)));

    List<OverworldExplorationObservation> first = planner.plan(-161, 273);
    List<OverworldExplorationObservation> second = planner.plan(-161, 273);

    assertEquals(first, second);
    assertFalse(first.isEmpty());
    assertTrue(
        first.stream()
            .anyMatch(observation -> observation.kind() == ExplorationObservationKind.CONTACT));
    assertTrue(first.stream().allMatch(OverworldExplorationObservationTest::isValid));
  }

  private static boolean isValid(OverworldExplorationObservation observation) {
    return observation.provenanceBodyIds().contains(observation.material().rockBodyId())
        && observation.confidencePpm() >= 0
        && observation.confidencePpm() <= 1_000_000
        && observation.observationScaleBlocks() > 0
        && (observation.kind() != ExplorationObservationKind.CONTACT
            || observation.adjacentMaterial() != null);
  }
}
