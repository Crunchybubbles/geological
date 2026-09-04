package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.ExplorationSampleKind;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSample;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSampler;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSampler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import org.junit.jupiter.api.Test;

class OverworldSedimentSamplerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void soilStreamAndHeavySamplesAreDeterministicAndCausallyBounded() {
    WorldgenExecutionContext context = context();
    OverworldRegolithPlanner regolith = OverworldRegolithPlanner.from(context);
    OverworldSedimentSampler sampler = OverworldSedimentSampler.from(regolith);
    OverworldTerrainControlSampler terrain = OverworldTerrainControlSampler.from(context);

    OverworldSedimentSample soil = findSoil(sampler);
    OverworldSedimentSample stream = sampler.sampleStreamSediment(-2_048, -1_080);
    OverworldSedimentSample heavy = sampler.sampleHeavyMineral(-2_048, -1_080);

    assertEquals(soil, sampler.sample(ExplorationSampleKind.SOIL, soil.blockX(), soil.blockZ()));
    assertEquals(
        stream,
        sampler.sample(ExplorationSampleKind.STREAM_SEDIMENT, stream.blockX(), stream.blockZ()));
    assertEquals(
        heavy, sampler.sample(ExplorationSampleKind.HEAVY_MINERAL, heavy.blockX(), heavy.blockZ()));
    assertEquals(ExplorationSampleKind.SOIL, soil.kind());
    assertTrue(
        soil.surfaceKind() == SurfaceMaterialKind.IN_SITU_REGOLITH
            || soil.surfaceKind() == SurfaceMaterialKind.COLLUVIAL_MANTLE);
    assertFalse(soil.reportedConstituentsPpm().isEmpty());
    assertFalse(soil.indicatorSignalsPpm().isEmpty());
    assertEquals(ExplorationSampleKind.STREAM_SEDIMENT, stream.kind());
    assertTrue(stream.flowAccumulation() >= 0.0 && stream.flowAccumulation() <= 1.0);
    assertTrue(stream.hydraulicTrapScore() >= 0.0 && stream.hydraulicTrapScore() <= 1.0);
    assertEquals(ExplorationSampleKind.HEAVY_MINERAL, heavy.kind());
    assertFalse(heavy.heavyMineralModesPpm().isEmpty());
    assertEquals(
        1_000_000L,
        heavy.heavyMineralModesPpm().values().stream().mapToLong(Long::longValue).sum());
    assertTrue(heavy.summary().contains("kind=HEAVY_MINERAL"));
    assertTrue(soil.provenanceBodyIds().contains(soil.material().rockBodyId()));
    assertTrue(stream.provenanceBodyIds().contains(stream.material().rockBodyId()));
    assertTrue(heavy.provenanceBodyIds().contains(heavy.material().rockBodyId()));
  }

  @Test
  void streamMethodsRejectOffChannelColumns() {
    WorldgenExecutionContext context = context();
    OverworldRegolithPlanner regolith = OverworldRegolithPlanner.from(context);
    OverworldSedimentSampler sampler = OverworldSedimentSampler.from(regolith);
    OverworldTerrainControlSampler terrain = OverworldTerrainControlSampler.from(context);
    long[] offChannel = findOffChannel(terrain);

    assertThrows(
        IllegalArgumentException.class,
        () -> sampler.sampleStreamSediment(offChannel[0], offChannel[1]));
    assertThrows(
        IllegalArgumentException.class,
        () -> sampler.sampleHeavyMineral(offChannel[0], offChannel[1]));
  }

  private static OverworldSedimentSample findSoil(OverworldSedimentSampler sampler) {
    for (long x = -176; x < -160; x++) {
      for (long z = 272; z < 288; z++) {
        try {
          return sampler.sampleSoil(x, z);
        } catch (IllegalArgumentException ignored) {
          // Continue through the bounded fixture footprint until a soil setting is found.
        }
      }
    }
    throw new AssertionError("fixture footprint did not contain a soil setting");
  }

  private static long[] findOffChannel(OverworldTerrainControlSampler terrain) {
    for (long x = -176; x < -160; x++) {
      for (long z = 272; z < 288; z++) {
        if (!terrain.sample(x, z).channel()) {
          return new long[] {x, z};
        }
      }
    }
    throw new AssertionError("fixture footprint did not contain an off-channel column");
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
