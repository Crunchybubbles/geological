package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.DrillCoreInterval;
import io.github.crunchybubbles.geological.worldgen.DrillCoreLog;
import io.github.crunchybubbles.geological.worldgen.OverworldDrillCorePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldDrillCorePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void coreLogsAreDeterministicContiguousAndProvenanceRich() {
    OverworldDrillCorePlanner planner =
        OverworldDrillCorePlanner.from(OverworldRegolithPlanner.from(context()));

    DrillCoreLog first = planner.logSurface(-161, 273, 64);
    DrillCoreLog second = planner.logSurface(-161, 273, 64);

    assertEquals(first, second);
    assertEquals(64, first.maxYExclusive() - first.minYInclusive());
    assertFalse(first.intervals().isEmpty());
    assertFalse(first.provenanceBodyIds().isEmpty());
    assertTrue(first.materialEvaluations() >= first.intervals().size());
    assertTrue(first.summary().contains("drill-log id="));
    assertTrue(first.intervals().stream().allMatch(OverworldDrillCorePlannerTest::valid));
  }

  @Test
  void coreRejectsAirAndUnboundedDepth() {
    OverworldDrillCorePlanner planner =
        OverworldDrillCorePlanner.from(OverworldRegolithPlanner.from(context()));

    assertThrows(IllegalArgumentException.class, () -> planner.logSurface(-161, 273, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> planner.logSurface(-161, 273, OverworldDrillCorePlanner.MAX_CORE_DEPTH_BLOCKS + 1));
    assertThrows(IllegalArgumentException.class, () -> planner.log(-161, 273, 100, 200));
  }

  private static boolean valid(DrillCoreInterval interval) {
    return interval.maxYExclusive() > interval.minYInclusive()
        && !interval.visibleConstituentsPpm().isEmpty()
        && interval.provenanceBodyIds().contains(interval.material().rockBodyId())
        && interval.confidencePpm() >= 0
        && interval.confidencePpm() <= 1_000_000;
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
