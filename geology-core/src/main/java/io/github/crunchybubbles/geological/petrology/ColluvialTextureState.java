package io.github.crunchybubbles.geological.petrology;

/** Coarse grain-size and fabric state for a generated colluvial mantle. */
public record ColluvialTextureState(
    SedimentGrainSize grainSize,
    SedimentSorting sorting,
    SedimentSupport support,
    ClastShape clastShape) {
  private static final long MATRIX_SUPPORTED_MAXIMUM_COARSE_PPM = 250_000L;
  private static final long CLAST_SUPPORTED_MINIMUM_COARSE_PPM = 500_000L;
  private static final double MODERATE_SORTING_DOMINANT_FRACTION = 0.55;
  private static final double WELL_SORTED_DOMINANT_FRACTION = 0.75;

  public ColluvialTextureState {
    if (grainSize == null || sorting == null || support == null || clastShape == null) {
      throw new IllegalArgumentException("colluvial texture state must be complete");
    }
  }

  public static ColluvialTextureState from(SedimentGrainSize grainSize) {
    if (grainSize == null) {
      throw new IllegalArgumentException("colluvial grain-size state is required");
    }
    SedimentSupport support =
        grainSize.gravelAndCoarserPpm() <= MATRIX_SUPPORTED_MAXIMUM_COARSE_PPM
            ? SedimentSupport.MATRIX_SUPPORTED
            : grainSize.gravelAndCoarserPpm() >= CLAST_SUPPORTED_MINIMUM_COARSE_PPM
                ? SedimentSupport.CLAST_SUPPORTED
                : SedimentSupport.MIXED_SUPPORT;
    return new ColluvialTextureState(
        grainSize, sorting(grainSize), support, ClastShape.ANGULAR_TO_SUBROUNDED);
  }

  /** Continuous dominance proxy for the coarse three-bin sorting class. */
  public double sortingDominanceIndex() {
    double dominantFraction =
        StrictMath.max(
                StrictMath.max(grainSize.gravelAndCoarserPpm(), grainSize.sandPpm()),
                grainSize.finesPpm())
            / (double) MaterialAssemblage.SCALE;
    return StrictMath.max(0.0, StrictMath.min(1.0, (dominantFraction - (1.0 / 3.0)) / (2.0 / 3.0)));
  }

  private static SedimentSorting sorting(SedimentGrainSize grainSize) {
    double dominantFraction =
        StrictMath.max(
                StrictMath.max(grainSize.gravelAndCoarserPpm(), grainSize.sandPpm()),
                grainSize.finesPpm())
            / (double) MaterialAssemblage.SCALE;
    if (dominantFraction >= WELL_SORTED_DOMINANT_FRACTION) {
      return SedimentSorting.WELL_SORTED;
    }
    if (dominantFraction >= MODERATE_SORTING_DOMINANT_FRACTION) {
      return SedimentSorting.MODERATELY_SORTED;
    }
    return SedimentSorting.UNSORTED_TO_POORLY_SORTED;
  }
}
