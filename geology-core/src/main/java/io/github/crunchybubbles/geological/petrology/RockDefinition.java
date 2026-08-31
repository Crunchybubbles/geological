package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;

/** Bulk rock behavior and its interned primary mineral-mode recipe. */
public record RockDefinition(
    String id,
    Lithology lithology,
    GeneticFamily geneticFamily,
    MineralAssemblage primaryAssemblage,
    double porosityFraction,
    double permeabilityIndex,
    double erodibilityIndex) {
  public RockDefinition {
    if (id == null
        || id.isBlank()
        || lithology == null
        || geneticFamily == null
        || primaryAssemblage == null) {
      throw new IllegalArgumentException("rock definition must be complete");
    }
    requireUnit(porosityFraction, "porosity fraction");
    requireUnit(permeabilityIndex, "permeability index");
    requireUnit(erodibilityIndex, "erodibility index");
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
