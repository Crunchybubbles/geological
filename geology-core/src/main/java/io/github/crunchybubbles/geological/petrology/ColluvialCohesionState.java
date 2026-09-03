package io.github.crunchybubbles.geological.petrology;

/**
 * Coarse cohesion-versus-erodibility separation for a generated colluvial parcel.
 *
 * <p>The cohesion index is a texture proxy for fines and matrix support. The adjusted detachment
 * index is deliberately dimensionless and is not a physical shear-strength or conductivity
 * estimate.
 */
public record ColluvialCohesionState(
    CohesionClass cohesionClass,
    double finesFraction,
    double cohesionIndex,
    double cohesionAdjustedErodibilityIndex) {
  private static final double MATRIX_SUPPORT_WEIGHT = 0.15;
  private static final double FINES_WEIGHT = 0.85;
  private static final double COHESION_ERODIBILITY_REDUCTION = 0.30;

  public ColluvialCohesionState {
    if (cohesionClass == null) {
      throw new IllegalArgumentException("colluvial cohesion class is required");
    }
    requireUnit(finesFraction, "fines fraction");
    requireUnit(cohesionIndex, "cohesion index");
    requireUnit(cohesionAdjustedErodibilityIndex, "cohesion-adjusted erodibility index");
  }

  /** Derives cohesion and detachment proxies from the resolved texture and erodibility. */
  public static ColluvialCohesionState from(
      ColluvialTextureState textureState, double erodibilityIndex) {
    if (textureState == null) {
      throw new IllegalArgumentException("colluvial texture state is required");
    }
    requireUnit(erodibilityIndex, "erodibility index");
    double finesFraction = textureState.grainSize().finesPpm() / (double) MaterialAssemblage.SCALE;
    double matrixSupport =
        switch (textureState.support()) {
          case MATRIX_SUPPORTED -> 1.0;
          case MIXED_SUPPORT -> 0.55;
          case CLAST_SUPPORTED -> 0.15;
        };
    double cohesionIndex =
        clamp(FINES_WEIGHT * finesFraction + MATRIX_SUPPORT_WEIGHT * matrixSupport);
    CohesionClass cohesionClass =
        cohesionIndex < 0.20
            ? CohesionClass.NON_COHESIVE
            : cohesionIndex < 0.50
                ? CohesionClass.MIXED_COHESION
                : CohesionClass.COHESIVE_FINE_MATRIX;
    double adjustedErodibilityIndex =
        clamp(erodibilityIndex * (1.0 - COHESION_ERODIBILITY_REDUCTION * cohesionIndex));
    return new ColluvialCohesionState(
        cohesionClass, finesFraction, cohesionIndex, adjustedErodibilityIndex);
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  /** Coarse textural cohesion class, not a soil-engineering classification. */
  public enum CohesionClass {
    NON_COHESIVE,
    MIXED_COHESION,
    COHESIVE_FINE_MATRIX
  }
}
