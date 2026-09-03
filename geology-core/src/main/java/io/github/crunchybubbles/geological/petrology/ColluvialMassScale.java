package io.github.crunchybubbles.geological.petrology;

/** Caller-supplied conversion from normalized colluvial fixed units to a mass/time basis. */
public record ColluvialMassScale(
    String massUnit, double normalizedCapacityMass, double durationYears) {
  public ColluvialMassScale {
    if (massUnit == null || massUnit.isBlank()) {
      throw new IllegalArgumentException("colluvial mass unit is required");
    }
    if (!Double.isFinite(normalizedCapacityMass) || normalizedCapacityMass <= 0.0) {
      throw new IllegalArgumentException("normalized colluvial capacity mass must be positive");
    }
    if (!Double.isFinite(durationYears) || durationYears <= 0.0) {
      throw new IllegalArgumentException("colluvial calibration duration must be positive");
    }
  }

  /** Converts a non-negative normalized fixed-unit inventory to the selected mass unit. */
  public double mass(long fixedUnits) {
    if (fixedUnits < 0) {
      throw new IllegalArgumentException("colluvial fixed-unit inventory must be non-negative");
    }
    return normalizedCapacityMass * fixedUnits / MaterialAssemblage.SCALE;
  }

  /** Converts a non-negative normalized fixed-unit inventory to mass per calibration year. */
  public double productionRate(long fixedUnits) {
    return mass(fixedUnits) / durationYears;
  }
}
