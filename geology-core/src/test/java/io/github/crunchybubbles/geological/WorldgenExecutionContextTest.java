package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorldgenExecutionContextTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultSnapshotIsStableAndProfileBound() {
    WorldgenSnapshot first = WorldgenSnapshot.forProfile(OVERWORLD);
    WorldgenSnapshot second = WorldgenSnapshot.forProfile(OVERWORLD);

    assertEquals(first, second);
    assertEquals("phase4-alpha.2", first.modelVersion());
    assertEquals(OVERWORLD.scientificDigest(), first.scientificDigest());
    assertTrue(first.configurationDigest().matches("sha256:[0-9a-f]{64}"));
    assertTrue(first.presentationDigest().matches("sha256:[0-9a-f]{64}"));
    assertTrue(first.matches(OVERWORLD));
    assertNotEquals(
        first.configurationDigest(),
        WorldgenSnapshot.forProfile(DimensionGeologyProfiles.require("minecraft:the_nether"))
            .configurationDigest());
  }

  @Test
  void contextUsesSuppliedExecutorAndAllowsOnlyWritableTargetStage() {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, -4, 9, WorldgenStage.LITHOLOGY);
    WorldgenSnapshot snapshot = WorldgenSnapshot.forProfile(OVERWORLD);
    AtomicInteger executions = new AtomicInteger();
    AtomicInteger taskRuns = new AtomicInteger();
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            request,
            WorldgenStage.LITHOLOGY,
            snapshot,
            task -> {
              executions.incrementAndGet();
              task.run();
            });

    assertTrue(context.canWriteTarget());
    context.requireWritableTargetChunk(-4, 9);
    context.execute(taskRuns::incrementAndGet);
    assertEquals(1, executions.get());
    assertEquals(1, taskRuns.get());
  }

  @Test
  void contextRejectsNonWritableStageAndNeighborWrites() {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.ACQUIRE_CONTEXT);
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            request,
            WorldgenStage.ACQUIRE_CONTEXT,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);

    assertFalse(context.canWriteTarget());
    assertThrows(IllegalStateException.class, context::requireWritableTarget);
    assertThrows(IllegalStateException.class, () -> context.requireWritableTargetChunk(0, 0));

    WorldgenExecutionContext writable =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.BASE_TERRAIN),
            WorldgenStage.BASE_TERRAIN,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    assertThrows(IllegalArgumentException.class, () -> writable.requireWritableTargetChunk(1, 0));
  }

  @Test
  void contextRejectsUnauthorizedStageAndMismatchedSnapshot() {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.LITHOLOGY);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new WorldgenExecutionContext(
                request,
                WorldgenStage.CAVES_AQUIFERS,
                WorldgenSnapshot.forProfile(OVERWORLD),
                Runnable::run));

    WorldgenSnapshot altered =
        new WorldgenSnapshot(
            OVERWORLD.version(),
            "sha256:0000000000000000000000000000000000000000000000000000000000000000",
            WorldgenSnapshot.forProfile(OVERWORLD).configurationDigest(),
            WorldgenSnapshot.forProfile(OVERWORLD).presentationDigest(),
            OVERWORLD.scaleProfileId());
    assertFalse(altered.matches(OVERWORLD));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new WorldgenExecutionContext(request, WorldgenStage.LITHOLOGY, altered, Runnable::run));
  }
}
