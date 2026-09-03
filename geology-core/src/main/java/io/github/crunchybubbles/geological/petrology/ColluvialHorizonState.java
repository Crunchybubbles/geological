package io.github.crunchybubbles.geological.petrology;

/** Coarse, source-aware profile state for a generated colluvial parcel. */
public record ColluvialHorizonState(
    ProfileClass profileClass,
    double weatheringIndex,
    long weatheredMatrixFractionPpm,
    long transportedSourceFractionPpm) {
  private static final double WEATHERING_DEPTH_REFERENCE = 12.0;
  private static final long MATRIX_RICH_MINIMUM_PPM = 500_000L;
  private static final long SOURCE_CLAST_RICH_MINIMUM_PPM = 650_000L;

  public ColluvialHorizonState {
    if (profileClass == null
        || !Double.isFinite(weatheringIndex)
        || weatheringIndex < 0.0
        || weatheringIndex > 1.0
        || weatheredMatrixFractionPpm < 0
        || transportedSourceFractionPpm < 0
        || Math.addExact(weatheredMatrixFractionPpm, transportedSourceFractionPpm)
            != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("colluvial horizon state is invalid");
    }
  }

  public static ColluvialHorizonState from(ColluvialSedimentBudget budget) {
    if (budget == null) {
      throw new IllegalArgumentException("colluvial sediment budget is required");
    }
    ColluvialSedimentBudget.ProductionInput matrixInput = budget.weatheredMatrixBalance().input();
    double weatheringIndex = clamp(matrixInput.weatheringDepth() / WEATHERING_DEPTH_REFERENCE);
    long matrixFraction = budget.weatheredMatrixFractionPpm();
    long sourceFraction = Math.subtractExact(MaterialAssemblage.SCALE, matrixFraction);
    return new ColluvialHorizonState(
        profileClass(weatheringIndex, matrixFraction, sourceFraction),
        weatheringIndex,
        matrixFraction,
        sourceFraction);
  }

  public boolean matches(ColluvialSedimentBudget budget) {
    if (budget == null) {
      return false;
    }
    return equals(from(budget));
  }

  private static ProfileClass profileClass(
      double weatheringIndex, long matrixFraction, long sourceFraction) {
    if (weatheringIndex < 0.5) {
      return ProfileClass.THIN_MIXED_PROFILE;
    }
    if (matrixFraction >= MATRIX_RICH_MINIMUM_PPM) {
      return ProfileClass.WEATHERED_MATRIX_RICH_PROFILE;
    }
    if (sourceFraction >= SOURCE_CLAST_RICH_MINIMUM_PPM) {
      return ProfileClass.SOURCE_CLAST_RICH_PROFILE;
    }
    return ProfileClass.BALANCED_MIXED_PROFILE;
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  public enum ProfileClass {
    THIN_MIXED_PROFILE,
    BALANCED_MIXED_PROFILE,
    WEATHERED_MATRIX_RICH_PROFILE,
    SOURCE_CLAST_RICH_PROFILE
  }
}
