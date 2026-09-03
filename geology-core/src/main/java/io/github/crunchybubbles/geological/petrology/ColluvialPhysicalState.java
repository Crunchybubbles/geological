package io.github.crunchybubbles.geological.petrology;

/** Source-texture-conditioned physical properties for a generated colluvial mantle. */
public record ColluvialPhysicalState(
    ColluvialTextureState textureState,
    double porosityQuantile,
    double permeabilityQuantile,
    double erodibilityQuantile,
    double porosityFraction,
    double permeabilityIndex,
    double erodibilityIndex) {
  private static final double ANGULAR_POROSITY_ADDITION = 0.10;
  private static final double MODERATE_SORTING_POROSITY_ADDITION = 0.20;
  private static final double WELL_SORTED_POROSITY_ADDITION = 0.35;
  private static final double MATRIX_SUPPORT_POROSITY_ADDITION = 0.05;
  private static final double CLAST_SUPPORT_POROSITY_ADDITION = 0.20;

  private static final double GRAVEL_PERMEABILITY_WEIGHT = 0.90;
  private static final double SAND_PERMEABILITY_WEIGHT = 0.55;
  private static final double FINES_PERMEABILITY_WEIGHT = 0.05;
  private static final double POOR_SORTING_PERMEABILITY_PENALTY = 0.15;
  private static final double WELL_SORTED_PERMEABILITY_ADDITION = 0.15;
  private static final double MATRIX_SUPPORT_PERMEABILITY_PENALTY = 0.10;
  private static final double CLAST_SUPPORT_PERMEABILITY_ADDITION = 0.10;

  private static final double GRAVEL_ERODIBILITY_WEIGHT = 0.25;
  private static final double SAND_ERODIBILITY_WEIGHT = 0.90;
  private static final double FINES_ERODIBILITY_WEIGHT = 0.55;
  private static final double MIXED_SUPPORT_ERODIBILITY_ADDITION = 0.05;
  private static final double CLAST_SUPPORT_ERODIBILITY_PENALTY = 0.10;

  public ColluvialPhysicalState {
    if (textureState == null) {
      throw new IllegalArgumentException("colluvial physical state requires its texture state");
    }
    requireUnit(porosityQuantile, "porosity quantile");
    requireUnit(permeabilityQuantile, "permeability quantile");
    requireUnit(erodibilityQuantile, "erodibility quantile");
    requireUnit(porosityFraction, "porosity fraction");
    requireUnit(permeabilityIndex, "permeability index");
    requireUnit(erodibilityIndex, "erodibility index");
  }

  public ColluvialCohesionState cohesionState() {
    return ColluvialCohesionState.from(textureState, erodibilityIndex);
  }

  public static ColluvialPhysicalState derive(
      ColluvialTextureState textureState,
      UnitIntervalDistribution porosityDistribution,
      UnitIntervalDistribution permeabilityDistribution,
      UnitIntervalDistribution erodibilityDistribution) {
    if (textureState == null
        || porosityDistribution == null
        || permeabilityDistribution == null
        || erodibilityDistribution == null) {
      throw new IllegalArgumentException(
          "colluvial texture and physical-property distributions are required");
    }
    SedimentGrainSize grainSize = textureState.grainSize();
    double gravel = fraction(grainSize.gravelAndCoarserPpm());
    double sand = fraction(grainSize.sandPpm());
    double fines = fraction(grainSize.finesPpm());

    double porosityQuantile =
        quantile(
            0.25
                + sortingPorosityAdjustment(textureState.sorting())
                + supportPorosityAdjustment(textureState.support())
                + shapePorosityAdjustment(textureState.clastShape()));
    double permeabilityQuantile =
        quantile(
            gravel * GRAVEL_PERMEABILITY_WEIGHT
                + sand * SAND_PERMEABILITY_WEIGHT
                + fines * FINES_PERMEABILITY_WEIGHT
                + sortingPermeabilityAdjustment(textureState.sorting())
                + supportPermeabilityAdjustment(textureState.support()));
    double erodibilityQuantile =
        quantile(
            gravel * GRAVEL_ERODIBILITY_WEIGHT
                + sand * SAND_ERODIBILITY_WEIGHT
                + fines * FINES_ERODIBILITY_WEIGHT
                + supportErodibilityAdjustment(textureState.support()));
    return new ColluvialPhysicalState(
        textureState,
        porosityQuantile,
        permeabilityQuantile,
        erodibilityQuantile,
        porosityDistribution.sample(porosityQuantile),
        permeabilityDistribution.sample(permeabilityQuantile),
        erodibilityDistribution.sample(erodibilityQuantile));
  }

  private static double sortingPorosityAdjustment(SedimentSorting sorting) {
    return switch (sorting) {
      case UNSORTED_TO_POORLY_SORTED -> 0.0;
      case MODERATELY_SORTED -> MODERATE_SORTING_POROSITY_ADDITION;
      case WELL_SORTED -> WELL_SORTED_POROSITY_ADDITION;
    };
  }

  private static double supportPorosityAdjustment(SedimentSupport support) {
    return switch (support) {
      case MATRIX_SUPPORTED -> MATRIX_SUPPORT_POROSITY_ADDITION;
      case MIXED_SUPPORT -> 0.0;
      case CLAST_SUPPORTED -> CLAST_SUPPORT_POROSITY_ADDITION;
    };
  }

  private static double shapePorosityAdjustment(ClastShape shape) {
    return switch (shape) {
      case ANGULAR_TO_SUBROUNDED -> ANGULAR_POROSITY_ADDITION;
      case SUBROUNDED_TO_ROUNDED -> 0.0;
    };
  }

  private static double sortingPermeabilityAdjustment(SedimentSorting sorting) {
    return switch (sorting) {
      case UNSORTED_TO_POORLY_SORTED -> -POOR_SORTING_PERMEABILITY_PENALTY;
      case MODERATELY_SORTED -> 0.0;
      case WELL_SORTED -> WELL_SORTED_PERMEABILITY_ADDITION;
    };
  }

  private static double supportPermeabilityAdjustment(SedimentSupport support) {
    return switch (support) {
      case MATRIX_SUPPORTED -> -MATRIX_SUPPORT_PERMEABILITY_PENALTY;
      case MIXED_SUPPORT -> 0.0;
      case CLAST_SUPPORTED -> CLAST_SUPPORT_PERMEABILITY_ADDITION;
    };
  }

  private static double supportErodibilityAdjustment(SedimentSupport support) {
    return switch (support) {
      case MATRIX_SUPPORTED -> 0.0;
      case MIXED_SUPPORT -> MIXED_SUPPORT_ERODIBILITY_ADDITION;
      case CLAST_SUPPORTED -> -CLAST_SUPPORT_ERODIBILITY_PENALTY;
    };
  }

  private static double fraction(long ppm) {
    return ppm / (double) MaterialAssemblage.SCALE;
  }

  private static double quantile(double value) {
    return StrictMath.max(0.0, StrictMath.min(StrictMath.nextDown(1.0), value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
