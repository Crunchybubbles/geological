package io.github.crunchybubbles.geological.petrology;

/**
 * Bounded within-bin grain-dispersion proxies for a deposited colluvial spectrum.
 *
 * <p>The authoritative grain state remains the three normalized bins. These indices expose how much
 * unresolved spread each bin should carry for review and later refinement; they are not measured
 * grain diameters or mineral-specific distributions.
 */
public record ColluvialGrainDispersionState(
    DispersionClass dispersionClass,
    double coarseSpreadIndex,
    double sandSpreadIndex,
    double finesSpreadIndex,
    double weightedSpreadIndex) {
  private static final double COARSE_BASE_SPREAD = 0.85;
  private static final double COARSE_SORTING_REDUCTION = 0.55;
  private static final double SAND_BASE_SPREAD = 0.70;
  private static final double SAND_SORTING_REDUCTION = 0.40;
  private static final double FINES_BASE_SPREAD = 0.55;
  private static final double FINES_SORTING_REDUCTION = 0.25;
  private static final double BROAD_SPREAD_THRESHOLD = 0.58;
  private static final double NARROW_SPREAD_THRESHOLD = 0.38;
  private static final double CLASS_TOLERANCE = 1.0e-12;

  public ColluvialGrainDispersionState {
    if (dispersionClass == null) {
      throw new IllegalArgumentException("colluvial grain-dispersion class is required");
    }
    requireUnit(coarseSpreadIndex, "coarse spread index");
    requireUnit(sandSpreadIndex, "sand spread index");
    requireUnit(finesSpreadIndex, "fines spread index");
    requireUnit(weightedSpreadIndex, "weighted spread index");
    DispersionClass expected = dispersionClass(weightedSpreadIndex);
    if (expected != dispersionClass
        && StrictMath.abs(weightedSpreadIndex - BROAD_SPREAD_THRESHOLD) > CLASS_TOLERANCE
        && StrictMath.abs(weightedSpreadIndex - NARROW_SPREAD_THRESHOLD) > CLASS_TOLERANCE) {
      throw new IllegalArgumentException(
          "colluvial grain-dispersion class must match its weighted spread");
    }
  }

  /** Derives bounded unresolved-spread proxies from the coarse sorting state. */
  public static ColluvialGrainDispersionState from(ColluvialTextureState textureState) {
    if (textureState == null) {
      throw new IllegalArgumentException("colluvial texture state is required");
    }
    double sorting = textureState.sortingDominanceIndex();
    double coarse = clamp(COARSE_BASE_SPREAD - COARSE_SORTING_REDUCTION * sorting);
    double sand = clamp(SAND_BASE_SPREAD - SAND_SORTING_REDUCTION * sorting);
    double fines = clamp(FINES_BASE_SPREAD - FINES_SORTING_REDUCTION * sorting);
    SedimentGrainSize grainSize = textureState.grainSize();
    double weighted =
        (grainSize.gravelAndCoarserPpm() * coarse
                + grainSize.sandPpm() * sand
                + grainSize.finesPpm() * fines)
            / MaterialAssemblage.SCALE;
    return new ColluvialGrainDispersionState(
        dispersionClass(weighted), coarse, sand, fines, weighted);
  }

  private static DispersionClass dispersionClass(double weightedSpread) {
    if (weightedSpread >= BROAD_SPREAD_THRESHOLD) {
      return DispersionClass.BROAD_WITHIN_BIN_PROXY;
    }
    if (weightedSpread <= NARROW_SPREAD_THRESHOLD) {
      return DispersionClass.NARROW_WITHIN_BIN_PROXY;
    }
    return DispersionClass.MODERATE_WITHIN_BIN_PROXY;
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  /** Coarse class for the unresolved within-bin spread proxy. */
  public enum DispersionClass {
    NARROW_WITHIN_BIN_PROXY,
    MODERATE_WITHIN_BIN_PROXY,
    BROAD_WITHIN_BIN_PROXY
  }
}
