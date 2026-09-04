package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionBiomeSubstrateAdapter;
import io.github.crunchybubbles.geological.worldgen.DimensionBiomeSubstrateState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSample;
import io.github.crunchybubbles.geological.worldgen.OverworldTerrainControlSampler;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DimensionBiomeSubstrateAdapterTest {
  private static final long SEED = 8_675_309L;
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");

  @Test
  void projectsEachCanonicalDimensionToBoundedReadOnlyControls() {
    DimensionBiomeSubstrateState overworld =
        DimensionBiomeSubstrateAdapter.overworld(
            OVERWORLD, OverworldTerrainControlSampler.from(overworldContext()).sample(-112L, 176L));
    DimensionBiomeSubstrateState nether =
        DimensionBiomeSubstrateAdapter.nether(
            NETHER, NetherThermalTerrainCompiler.from(identity(NETHER)).provinceAt(-112L, 176L));
    EndFragmentTerrainCompiler endCompiler = EndFragmentTerrainCompiler.from(identity(END));
    DimensionBiomeSubstrateState endBody =
        DimensionBiomeSubstrateAdapter.end(END, endCompiler.planColumn(0L, 0L));
    DimensionBiomeSubstrateState endVoid =
        DimensionBiomeSubstrateAdapter.end(END, endCompiler.planColumn(320L, 0L));

    assertEquals("minecraft:overworld", overworld.dimensionKey());
    assertEquals("minecraft:overworld_climate_bridge", overworld.adapterId());
    assertTrue(overworld.ownerId().isPresent());
    assertTrue(overworld.surfaceWaterEligible());
    assertFalse(overworld.voidMedium());
    assertTrue(overworld.semanticTags().contains("earth_surface"));

    assertEquals("minecraft:the_nether", nether.dimensionKey());
    assertEquals("geological:nether_thermal_substrate_bridge", nether.adapterId());
    assertTrue(nether.ownerId().isPresent());
    assertFalse(nether.surfaceWaterEligible());
    assertFalse(nether.voidMedium());
    assertTrue(nether.semanticTags().contains("thermal"));
    assertTrue(nether.substrateId().startsWith("nether:thermal/"));

    assertTrue(endBody.ownerId().isPresent());
    assertFalse(endBody.surfaceWaterEligible());
    assertFalse(endBody.voidMedium());
    assertTrue(endBody.semanticTags().contains("fragment"));
    assertTrue(endBody.substrateId().startsWith("end:fragment/"));

    assertTrue(endVoid.ownerId().isEmpty());
    assertEquals("end:void", endVoid.substrateId());
    assertEquals(0.0, endVoid.primarySignal());
    assertEquals(1.0, endVoid.secondarySignal());
    assertFalse(endVoid.surfaceWaterEligible());
    assertTrue(endVoid.voidMedium());
  }

  @Test
  void repeatedProjectionIsDeterministicAndProfileLocked() {
    OverworldTerrainControlSample sample =
        OverworldTerrainControlSampler.from(overworldContext()).sample(23L, -41L);
    assertEquals(
        DimensionBiomeSubstrateAdapter.overworld(OVERWORLD, sample),
        DimensionBiomeSubstrateAdapter.overworld(OVERWORLD, sample));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DimensionBiomeSubstrateAdapter.nether(
                OVERWORLD, NetherThermalTerrainCompiler.from(identity(NETHER)).provinceAt(0L, 0L)));
  }

  @Test
  void stateRejectsUnownedSolidAndWaterVoidContradictions() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DimensionBiomeSubstrateState(
                "minecraft:the_end",
                "geological:end_fragment_provenance_bridge",
                Optional.empty(),
                "end:void",
                0.0,
                1.0,
                java.util.Set.of("void"),
                false,
                false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DimensionBiomeSubstrateState(
                "minecraft:the_end",
                "geological:end_fragment_provenance_bridge",
                Optional.empty(),
                "end:void",
                0.0,
                1.0,
                java.util.Set.of("void"),
                true,
                true));
  }

  private static WorldgenExecutionContext overworldContext() {
    return new WorldgenExecutionContext(
        WorldgenChunkRequest.forStage(
            SEED, OVERWORLD, -7L, 11L, WorldgenStage.COARSE_TERRAIN_CONTROLS),
        WorldgenStage.COARSE_TERRAIN_CONTROLS,
        WorldgenSnapshot.forProfile(OVERWORLD),
        Runnable::run);
  }

  private static WorldIdentity identity(DimensionGeologyProfile profile) {
    return new WorldIdentity(
        SEED, profile.version(), profile.scientificDigest(), profile.profileId());
  }
}
