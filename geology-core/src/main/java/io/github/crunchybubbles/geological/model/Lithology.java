package io.github.crunchybubbles.geological.model;

public enum Lithology {
  GRANITIC_GNEISS(0.82),
  BASAL_CONGLOMERATE(0.58),
  MARINE_VOLCANICLASTIC(0.55),
  BASIN_SHALE(0.35),
  BASIN_SANDSTONE(0.65),
  SILTSTONE(0.50),
  LIMESTONE(0.72),
  DOLOSTONE(0.78),
  CHERT(0.92),
  BANDED_IRON_FORMATION(0.86),
  GYPSUM_ANHYDRITE_EVAPORITE(0.42),
  HALITE_POTASH_EVAPORITE(0.30),
  COAL(0.32),
  KOMATIITIC_ULTRAMAFIC(0.88),
  BASALTIC(0.82),
  GABBROIC(0.88),
  DIORITE_PULSE(0.86),
  GRANODIORITE_PULSE(0.90),
  FELSIC_STOCK(0.92),
  VMS_MASSIVE_SULFIDE(0.72),
  ALLUVIAL_GRAVEL(0.28);

  private final double strength;

  Lithology(double strength) {
    this.strength = strength;
  }

  public double strength() {
    return strength;
  }
}
