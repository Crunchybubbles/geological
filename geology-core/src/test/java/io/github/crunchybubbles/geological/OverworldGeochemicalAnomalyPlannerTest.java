package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.GeochemicalAnomalyEstimate;
import io.github.crunchybubbles.geological.worldgen.GeochemicalIndicatorEstimate;
import io.github.crunchybubbles.geological.worldgen.OverworldGeochemicalAnomalyPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSample;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSampler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldGeochemicalAnomalyPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void estimatesAreDeterministicIntervalValuedAndCensored() {
    OverworldSedimentSampler sampler =
        OverworldSedimentSampler.from(OverworldRegolithPlanner.from(context()));
    OverworldSedimentSample sample = sampler.sampleStreamSediment(-2_048, -1_080);
    OverworldGeochemicalAnomalyPlanner planner = OverworldGeochemicalAnomalyPlanner.from(sampler);

    GeochemicalAnomalyEstimate first = planner.estimate(sample);
    GeochemicalAnomalyEstimate second = planner.estimate(sample);

    assertEquals(first, second);
    assertFalse(first.indicators().isEmpty());
    assertEquals(sample.sampleId(), first.sampleId());
    assertEquals(sample.provenanceBodyIds(), first.provenanceBodyIds());
    assertTrue(first.summary().contains("geochemical estimate id="));
    assertTrue(first.indicators().stream().allMatch(OverworldGeochemicalAnomalyPlannerTest::valid));
  }

  private static boolean valid(GeochemicalIndicatorEstimate indicator) {
    return indicator.reportedSignalPpm() >= indicator.lowerBoundPpm()
        && indicator.reportedSignalPpm() <= indicator.upperBoundPpm()
        && indicator.detectionLimitPpm() > 0
        && indicator.anomalyScorePpm() >= 0
        && indicator.anomalyScorePpm() <= 1_000_000
        && indicator.detected() != indicator.censored();
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
