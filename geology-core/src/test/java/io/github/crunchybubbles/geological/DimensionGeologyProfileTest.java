package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DimensionGeologyProfileTest {
  @Test
  void canonicalProfilesExposeDistinctFrozenIdentities() {
    List<DimensionGeologyProfile> profiles = DimensionGeologyProfiles.all();

    assertEquals(
        List.of("minecraft:overworld", "minecraft:the_end", "minecraft:the_nether"),
        profiles.stream().map(DimensionGeologyProfile::dimensionKey).toList());
    assertEquals(3, profiles.stream().map(DimensionGeologyProfile::profileId).distinct().count());
    assertEquals(
        Set.of("phase4-alpha.2"),
        profiles.stream()
            .map(DimensionGeologyProfile::version)
            .collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        3, profiles.stream().map(DimensionGeologyProfile::scientificDigest).distinct().count());
    assertTrue(
        profiles.stream()
            .allMatch(profile -> profile.scientificDigest().matches("sha256:[0-9a-f]{64}")));
    assertEquals(
        "sha256:984be8310f9abc9a7188efb43632c46bcdca3fc1cabc6f49e1853be80da52625",
        DimensionGeologyProfiles.require("minecraft:overworld").scientificDigest());
    assertEquals(
        "sha256:ce670647aa18391b1631bbfe7aca8ba30429e4b09c076890ee0da29f7f318dec",
        DimensionGeologyProfiles.require("minecraft:the_nether").scientificDigest());
    assertEquals(
        "sha256:ab2a07dbbd7b509d875a2635b0430c3f56a4b28b13f107f2981f62e437c4000e",
        DimensionGeologyProfiles.require("minecraft:the_end").scientificDigest());
    assertEquals(
        DimensionProfile.SurfaceTopology.SINGLE_VALUED_SURFACE,
        DimensionGeologyProfiles.require("minecraft:overworld").atlasTopology());
    assertEquals(
        DimensionProfile.SurfaceTopology.CAVERN_VOLUME,
        DimensionGeologyProfiles.require("minecraft:the_nether").atlasTopology());
    assertEquals(
        DimensionProfile.SurfaceTopology.BOUNDED_BODIES_IN_VOID,
        DimensionGeologyProfiles.require("minecraft:the_end").atlasTopology());
  }

  @Test
  void fictionalProfilesForbidEarthSurfaceProcesses() {
    DimensionGeologyProfile nether = DimensionGeologyProfiles.require("minecraft:the_nether");
    DimensionGeologyProfile end = DimensionGeologyProfiles.require("minecraft:the_end");

    for (DimensionGeologyProfile profile : List.of(nether, end)) {
      assertEquals(
          Set.of(
              DimensionGeologyProfile.DimensionProcessFamily.SEDIMENTATION,
              DimensionGeologyProfile.DimensionProcessFamily.SURFACE_WATER_DRAINAGE,
              DimensionGeologyProfile.DimensionProcessFamily.SEDIMENT_TRANSPORT),
          profile.forbiddenProcessFamilies().stream()
              .filter(
                  family ->
                      family == DimensionGeologyProfile.DimensionProcessFamily.SEDIMENTATION
                          || family
                              == DimensionGeologyProfile.DimensionProcessFamily
                                  .SURFACE_WATER_DRAINAGE
                          || family
                              == DimensionGeologyProfile.DimensionProcessFamily.SEDIMENT_TRANSPORT)
              .collect(java.util.stream.Collectors.toSet()));
      assertTrue(
          java.util.Collections.disjoint(
              profile.allowedProcessFamilies(), profile.forbiddenProcessFamilies()));
      assertEquals(
          DimensionGeologyProfile.ConfidencePolicy.FICTIONAL_PREMISE_CONSTRAINED,
          profile.confidencePolicy());
    }
    assertEquals(
        Set.of(
            DimensionGeologyProfile.FluidMedium.LAVA,
            DimensionGeologyProfile.FluidMedium.MAGMATIC_VOLATILES),
        nether.fluidMedia());
    assertEquals(Set.of(DimensionGeologyProfile.FluidMedium.VOID), end.fluidMedia());
    assertTrue(
        end.forbiddenProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.ARC_MAGMATISM));
  }

  @Test
  void profileValidationRejectsOverlappingProcessesAndInvalidEnvelope() {
    assertThrows(
        IllegalArgumentException.class, () -> new DimensionGeologyProfile.VerticalEnvelope(10, 10));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DimensionGeologyProfile(
                "minecraft:test",
                "geological:test",
                "phase4-alpha.1",
                "test",
                DimensionGeologyProfile.ConfidencePolicy.FICTIONAL_PREMISE_CONSTRAINED,
                DimensionProfile.SurfaceTopology.CAVERN_VOLUME,
                new DimensionGeologyProfile.VerticalEnvelope(-64, 127),
                DimensionGeologyProfile.GravityFrame.WORLD_Y_UP,
                EnumSet.of(DimensionGeologyProfile.DimensionProcessFamily.BASEMENT),
                EnumSet.of(DimensionGeologyProfile.DimensionProcessFamily.BASEMENT),
                Set.of(DimensionGeologyProfile.FluidMedium.LAVA),
                "boundary",
                Set.of(),
                Set.of(),
                "biome",
                "structures",
                "scale",
                "sha256:0000000000000000000000000000000000000000000000000000000000000000"));
    assertThrows(
        IllegalArgumentException.class,
        () -> DimensionGeologyProfiles.require("minecraft:missing"));
    assertNotEquals(
        DimensionGeologyProfiles.require("minecraft:the_nether").scientificDigest(),
        DimensionGeologyProfiles.require("minecraft:the_end").scientificDigest());
    assertFalse(
        DimensionGeologyProfiles.require("minecraft:the_nether")
            .mineralSystemRegistryIds()
            .contains("geological:mineral/porphyry_cu"));
  }
}
