package io.github.crunchybubbles.geological.petrology;

/** Authored protolith and reduced peak P-T-path state for a primary metamorphic rock recipe. */
public record PrimaryMetamorphicDefinition(
    String protolithRockId,
    MetamorphicGrade grade,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double minimumTemperatureCelsius,
    double maximumTemperatureCelsius,
    double minimumPressureMpa,
    double maximumPressureMpa) {
  public PrimaryMetamorphicDefinition {
    if (protolithRockId == null
        || protolithRockId.isBlank()
        || grade == null
        || facies == null
        || path == null) {
      throw new IllegalArgumentException("primary metamorphic definition must be complete");
    }
    if (grade == MetamorphicGrade.NONE
        || facies == MetamorphicFacies.NONE
        || path == MetamorphicPath.NONE) {
      throw new IllegalArgumentException("primary metamorphism requires non-NONE state classes");
    }
    requireOrdered(
        minimumTemperatureCelsius, maximumTemperatureCelsius, -100.0, 1_500.0, "temperature");
    requireOrdered(minimumPressureMpa, maximumPressureMpa, 0.0, 5_000.0, "pressure");
  }

  private static void requireOrdered(
      double minimum, double maximum, double floor, double ceiling, String name) {
    if (!Double.isFinite(minimum)
        || !Double.isFinite(maximum)
        || minimum < floor
        || maximum > ceiling
        || minimum > maximum) {
      throw new IllegalArgumentException("primary metamorphic " + name + " interval is invalid");
    }
  }
}
