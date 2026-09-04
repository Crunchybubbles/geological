package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.registry.Phase2ScientificManifest;
import java.util.List;

/**
 * Frozen catalog of canonical-dimension identity contracts consumed by a future platform adapter.
 */
public final class DimensionGeologyProfiles {
  private static final List<DimensionGeologyProfile> PROFILES =
      List.of(
              DimensionGeologyProfile.overworldPhase4(Phase2ScientificManifest.digest()),
              DimensionGeologyProfile.netherPhase4(),
              DimensionGeologyProfile.endPhase4())
          .stream()
          .sorted(java.util.Comparator.comparing(DimensionGeologyProfile::dimensionKey))
          .toList();

  private DimensionGeologyProfiles() {}

  public static List<DimensionGeologyProfile> all() {
    return PROFILES;
  }

  public static DimensionGeologyProfile require(String dimensionKey) {
    return PROFILES.stream()
        .filter(profile -> profile.dimensionKey().equals(dimensionKey))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown dimension key " + dimensionKey));
  }
}
