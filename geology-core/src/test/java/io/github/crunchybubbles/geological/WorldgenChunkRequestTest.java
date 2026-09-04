package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.registry.Phase2ScientificManifest;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldgenChunkRequestTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void stageSequenceMatchesAdapterContract() {
    assertEquals(
        List.of(
            WorldgenStage.ACQUIRE_CONTEXT,
            WorldgenStage.COARSE_TERRAIN_CONTROLS,
            WorldgenStage.BASE_TERRAIN,
            WorldgenStage.LITHOLOGY,
            WorldgenStage.STRUCTURES_DEPOSITS_ALTERATION,
            WorldgenStage.CAVES_AQUIFERS,
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenStage.BIOME_DECORATION,
            WorldgenStage.VALIDATE_METRICS),
        List.of(WorldgenStage.values()));
    assertFalse(WorldgenStage.ACQUIRE_CONTEXT.writesChunk());
    assertFalse(WorldgenStage.COARSE_TERRAIN_CONTROLS.writesChunk());
    assertTrue(WorldgenStage.BASE_TERRAIN.writesChunk());
    assertTrue(WorldgenStage.BIOME_DECORATION.writesChunk());
    assertFalse(WorldgenStage.VALIDATE_METRICS.writesChunk());
  }

  @Test
  void identityAndStageStreamsAreIndependentOfRequestOrder() {
    WorldgenChunkRequest first = WorldgenChunkRequest.forChunk(8_675_309L, OVERWORLD, -11, 17);
    WorldgenChunkRequest second = WorldgenChunkRequest.forChunk(8_675_309L, OVERWORLD, -11, 17);

    assertEquals(first.chunkId(), second.chunkId());
    assertEquals(
        first.stageStream(WorldgenStage.LITHOLOGY).unitDouble("probe", 4),
        second.stageStream(WorldgenStage.LITHOLOGY).unitDouble("probe", 4));
    assertArrayEquals(
        first.stageStream(WorldgenStage.LITHOLOGY).bytes("probe", 4),
        second.stageStream(WorldgenStage.LITHOLOGY).bytes("probe", 4));
    assertNotEquals(
        first.chunkId(), WorldgenChunkRequest.forChunk(8_675_310L, OVERWORLD, -11, 17).chunkId());
    assertNotEquals(
        first.chunkId(), WorldgenChunkRequest.forChunk(8_675_309L, OVERWORLD, -10, 17).chunkId());

    assertEquals(List.of(WorldgenStage.values()), first.requiredStages());
    assertTrue(first.canWrite(WorldgenStage.LITHOLOGY));
    assertFalse(first.canWrite(WorldgenStage.ACQUIRE_CONTEXT));
    assertFalse(first.canWrite(WorldgenStage.VALIDATE_METRICS));
  }

  @Test
  void targetBoundsUseNegativeChunkFloorAndProfileEnvelope() {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, -11, 17, WorldgenStage.BASE_TERRAIN);
    ChunkBlockBounds bounds = request.targetBounds();

    assertEquals(-176L, bounds.minX());
    assertEquals(-160L, bounds.maxXExclusive());
    assertEquals(272L, bounds.minZ());
    assertEquals(288L, bounds.maxZExclusive());
    assertEquals(-64, bounds.minY());
    assertEquals(320, bounds.maxYExclusive());
    assertEquals(16L, bounds.width());
    assertEquals(384, bounds.height());
    assertEquals(16L, bounds.depth());
    assertTrue(bounds.contains(-176, -64, 272));
    assertTrue(bounds.contains(-161, 319, 287));
    assertFalse(bounds.contains(-160, 319, 287));
    assertFalse(bounds.contains(-176, 320, 272));
  }

  @Test
  void partialStatusCannotAccessLaterStageOrNeighborChunk() {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forStage(8_675_309L, OVERWORLD, 0, 0, WorldgenStage.LITHOLOGY);

    assertTrue(request.includes(WorldgenStage.BASE_TERRAIN));
    assertTrue(request.includes(WorldgenStage.LITHOLOGY));
    assertFalse(request.includes(WorldgenStage.CAVES_AQUIFERS));
    assertThrows(
        IllegalArgumentException.class, () -> request.stageStream(WorldgenStage.CAVES_AQUIFERS));
    assertThrows(IllegalArgumentException.class, () -> request.requireTargetChunk(1, 0));
    request.requireTargetChunk(0, 0);
  }

  @Test
  void profileAndWorldIdentityMustMatchExactly() {
    WorldIdentity wrongDigest =
        new WorldIdentity(
            8_675_309L,
            OVERWORLD.version(),
            "sha256:0000000000000000000000000000000000000000000000000000000000000000",
            OVERWORLD.profileId());
    assertThrows(
        IllegalArgumentException.class,
        () -> WorldgenChunkRequest.from(wrongDigest, OVERWORLD, 0, 0, WorldgenStage.BASE_TERRAIN));

    WorldIdentity wrongVersion =
        new WorldIdentity(
            8_675_309L,
            "phase4-alpha.other",
            Phase2ScientificManifest.digest(),
            OVERWORLD.profileId());
    assertThrows(
        IllegalArgumentException.class,
        () -> WorldgenChunkRequest.from(wrongVersion, OVERWORLD, 0, 0, WorldgenStage.BASE_TERRAIN));
  }
}
