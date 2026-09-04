package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;

/** Dimension-native thermal/magmatic province descriptor for the fictional Nether. */
public record NetherThermalProvinceState(
    StableId provinceId,
    StableId refractoryBasementId,
    StableId magmaProvinceId,
    NetherProvinceKind kind,
    long provinceCellX,
    long provinceCellZ,
    long heatPotentialFixedUnits,
    long volatilePotentialFixedUnits) {
  public NetherThermalProvinceState {
    if (provinceId == null
        || refractoryBasementId == null
        || magmaProvinceId == null
        || kind == null) {
      throw new IllegalArgumentException("Nether thermal province identities are required");
    }
    if (heatPotentialFixedUnits < 0L
        || heatPotentialFixedUnits > 1_000_000L
        || volatilePotentialFixedUnits < 0L
        || volatilePotentialFixedUnits > 1_000_000L) {
      throw new IllegalArgumentException("Nether thermal province potentials are out of bounds");
    }
  }

  public enum NetherProvinceKind {
    NETHERRACK_VOLCANIC_WASTE,
    BASALT_DELTA_COMPLEX,
    SOUL_ASH_VALLEY,
    VOLATILE_VENT_FIELD
  }
}
