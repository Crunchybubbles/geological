package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionWorldgenTrace;
import io.github.crunchybubbles.geological.worldgen.DimensionWorldgenTracePlanner;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DimensionWorldgenTracePlannerTest {
  @Test
  void preservesDistinctDimensionIdentityForTheSameSeedAndChunk() {
    List<DimensionWorldgenTrace> traces =
        DimensionWorldgenTracePlanner.fromSeed(8_675_309L).traceAll(-7L, 11L);

    assertEquals(3, traces.size());
    assertEquals(3, traces.stream().map(DimensionWorldgenTrace::profileId).distinct().count());
    assertEquals(3, traces.stream().map(DimensionWorldgenTrace::chunkId).distinct().count());
    assertEquals(
        3,
        new HashSet<>(traces.stream().map(DimensionWorldgenTrace::dimensionKey).toList()).size());
    assertTrue(traces.stream().allMatch(trace -> trace.columnsVisited() == 256));
    assertTrue(traces.stream().allMatch(DimensionWorldgenTrace::seamStable));
    assertTrue(traces.stream().allMatch(DimensionWorldgenTrace::topologyValid));
  }

  @Test
  void retainsNativeProcessAndBoundaryEvidence() {
    DimensionWorldgenTracePlanner planner = DimensionWorldgenTracePlanner.fromSeed(8_675_309L);
    DimensionWorldgenTrace overworld = planner.trace("minecraft:overworld", -7L, 11L);
    DimensionWorldgenTrace nether = planner.trace("minecraft:the_nether", -7L, 11L);
    DimensionWorldgenTrace end = planner.trace("minecraft:the_end", -7L, 11L);

    assertEquals("province", overworld.ownerKind());
    assertEquals("province", nether.ownerKind());
    assertEquals("parent_body", end.ownerKind());
    assertTrue(nether.fluidMedia().contains("LAVA"));
    assertTrue(end.fluidMedia().contains("VOID"));
    assertTrue(nether.forbiddenProcessFamilies().contains("SEDIMENTATION"));
    assertTrue(end.forbiddenProcessFamilies().contains("SURFACE_WATER_DRAINAGE"));
    assertTrue(end.voidColumns() > 0);
    assertTrue(end.protectedColumnCount() >= 0);
    assertNotEquals(overworld.scientificDigest(), nether.scientificDigest());
    assertNotEquals(nether.scientificDigest(), end.scientificDigest());
  }

  @Test
  void repeatedTraceAndSummaryAreDeterministic() {
    DimensionWorldgenTracePlanner planner = DimensionWorldgenTracePlanner.fromSeed(8_675_309L);

    DimensionWorldgenTrace first = planner.trace("minecraft:the_nether", 3L, -4L);
    DimensionWorldgenTrace second = planner.trace("minecraft:the_nether", 3L, -4L);

    assertEquals(first, second);
    assertEquals(first.summary(), second.summary());
  }
}
