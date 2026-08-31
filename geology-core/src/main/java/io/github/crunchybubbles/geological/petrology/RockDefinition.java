package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;

/** Bulk rock classification, central mineral recipe, and body-scale property distributions. */
public record RockDefinition(
    String id,
    Lithology lithology,
    GeneticFamily geneticFamily,
    RockTexture texture,
    MineralAssemblage primaryAssemblage,
    double modalSpreadFraction,
    UnitIntervalDistribution porosityDistribution,
    UnitIntervalDistribution permeabilityDistribution,
    UnitIntervalDistribution erodibilityDistribution) {
  public RockDefinition {
    if (id == null
        || id.isBlank()
        || lithology == null
        || geneticFamily == null
        || texture == null
        || primaryAssemblage == null
        || porosityDistribution == null
        || permeabilityDistribution == null
        || erodibilityDistribution == null) {
      throw new IllegalArgumentException("rock definition must be complete");
    }
    if (!Double.isFinite(modalSpreadFraction)
        || modalSpreadFraction < 0.0
        || modalSpreadFraction > 0.5) {
      throw new IllegalArgumentException("modal spread fraction must lie in [0, 0.5]");
    }
  }

  public double porosityFraction() {
    return porosityDistribution.mode();
  }

  public double permeabilityIndex() {
    return permeabilityDistribution.mode();
  }

  public double erodibilityIndex() {
    return erodibilityDistribution.mode();
  }
}
