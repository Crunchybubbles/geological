package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Overprint;

/** Authored response for a younger metamorphic, hydrothermal, or weathering overprint. */
public record AlterationDefinition(
    Overprint overprint,
    MaterialProcessClass processClass,
    long replacementPpm,
    MineralAssemblage targetAssemblage,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double minimumTemperatureCelsius,
    double maximumTemperatureCelsius,
    double minimumPressureMpa,
    double maximumPressureMpa,
    double porosityMultiplier,
    double erodibilityDelta) {
  public AlterationDefinition {
    if (overprint == null || processClass == null || facies == null || path == null) {
      throw new IllegalArgumentException("alteration definition identity must be complete");
    }
    if (replacementPpm < 0 || replacementPpm > MineralAssemblage.SCALE) {
      throw new IllegalArgumentException("replacement fraction must lie in [0, 1000000]");
    }
    if ((replacementPpm == 0) != (targetAssemblage == null)) {
      throw new IllegalArgumentException(
          "target assemblage is required exactly when replacement is non-zero");
    }
    requireOrdered(
        minimumTemperatureCelsius, maximumTemperatureCelsius, -100.0, 1_500.0, "temperature");
    requireOrdered(minimumPressureMpa, maximumPressureMpa, 0.0, 5_000.0, "pressure");
    if (!Double.isFinite(porosityMultiplier) || porosityMultiplier < 0.0) {
      throw new IllegalArgumentException("porosity multiplier must be finite and non-negative");
    }
    if (!Double.isFinite(erodibilityDelta) || erodibilityDelta < -1.0 || erodibilityDelta > 1.0) {
      throw new IllegalArgumentException("erodibility delta must lie in [-1, 1]");
    }
  }

  private static void requireOrdered(
      double minimum, double maximum, double floor, double ceiling, String name) {
    if (!Double.isFinite(minimum)
        || !Double.isFinite(maximum)
        || minimum < floor
        || maximum > ceiling
        || minimum > maximum) {
      throw new IllegalArgumentException(name + " interval is invalid");
    }
  }
}
