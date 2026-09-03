package io.github.crunchybubbles.geological.petrology;

/**
 * Coarse deterministic transport-process selection for one routed colluvial production input.
 *
 * <p>The scores are bounded proof proxies derived from the existing terrain and hydrologic
 * evidence. They select a dominant process label, but do not claim calibrated process rates or a
 * rainfall/runoff forcing model.
 */
public record ColluvialTransportProcess(
    ProcessClass processClass,
    double hillslopeCreepScore,
    double sheetwashScore,
    double dryRavelScore) {
  private static final double SCORE_TOLERANCE = 1.0e-12;

  public ColluvialTransportProcess {
    if (processClass == null) {
      throw new IllegalArgumentException("colluvial transport process class is required");
    }
    requireUnit(hillslopeCreepScore, "hillslope-creep score");
    requireUnit(sheetwashScore, "sheetwash score");
    requireUnit(dryRavelScore, "dry-ravel score");
    double maximum =
        StrictMath.max(hillslopeCreepScore, StrictMath.max(sheetwashScore, dryRavelScore));
    double selected =
        switch (processClass) {
          case HILLSLOPE_CREEP -> hillslopeCreepScore;
          case SHEETWASH -> sheetwashScore;
          case DRY_RAVEL -> dryRavelScore;
        };
    if (selected + SCORE_TOLERANCE < maximum) {
      throw new IllegalArgumentException(
          "colluvial transport process class must select a maximum score");
    }
  }

  /** Derives the dominant process proxy from one immutable production input. */
  public static ColluvialTransportProcess from(ColluvialSedimentBudget.ProductionInput input) {
    return from(input, ColluvialTransportPolicy.DEFAULT);
  }

  /** Derives the dominant process proxy with an explicit response policy. */
  public static ColluvialTransportProcess from(
      ColluvialSedimentBudget.ProductionInput input, ColluvialTransportPolicy policy) {
    if (input == null) {
      throw new IllegalArgumentException("colluvial production input is required");
    }
    if (policy == null) {
      throw new IllegalArgumentException("colluvial transport policy is required");
    }
    double slopeIndex = clamp(input.slope() / policy.slopeMobilityReference());
    double runoff = input.runoffIndex();
    double routeContinuity = 0.5 + 0.5 * input.terrainPath().downslopeContinuityIndex();
    double routeGrade = input.terrainPath().routeGradeIndex(policy.slopeMobilityReference());
    double gradeSupport = 0.5 + 0.5 * routeGrade;

    double hillslopeCreepScore = clamp((1.0 - slopeIndex) * (1.0 - 0.5 * runoff) * gradeSupport);
    double sheetwashScore = clamp(runoff * (0.70 + 0.30 * slopeIndex) * routeContinuity);
    double dryRavelScore = clamp(slopeIndex * (1.0 - runoff) * gradeSupport * routeContinuity);

    ProcessClass processClass = ProcessClass.HILLSLOPE_CREEP;
    double selected = hillslopeCreepScore;
    if (sheetwashScore > selected) {
      processClass = ProcessClass.SHEETWASH;
      selected = sheetwashScore;
    }
    if (dryRavelScore > selected) {
      processClass = ProcessClass.DRY_RAVEL;
    }
    return new ColluvialTransportProcess(
        processClass, hillslopeCreepScore, sheetwashScore, dryRavelScore);
  }

  /** Score for the selected process class. */
  public double selectedScore() {
    return switch (processClass) {
      case HILLSLOPE_CREEP -> hillslopeCreepScore;
      case SHEETWASH -> sheetwashScore;
      case DRY_RAVEL -> dryRavelScore;
    };
  }

  /** Difference between the selected score and the strongest non-selected score. */
  public double selectionMargin() {
    double second;
    switch (processClass) {
      case HILLSLOPE_CREEP -> second = StrictMath.max(sheetwashScore, dryRavelScore);
      case SHEETWASH -> second = StrictMath.max(hillslopeCreepScore, dryRavelScore);
      case DRY_RAVEL -> second = StrictMath.max(hillslopeCreepScore, sheetwashScore);
      default -> throw new IllegalStateException("unmapped colluvial transport process");
    }
    return StrictMath.max(0.0, selectedScore() - second);
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  /** Dominant coarse transport process selected by the bounded proxy scores. */
  public enum ProcessClass {
    HILLSLOPE_CREEP,
    SHEETWASH,
    DRY_RAVEL
  }
}
