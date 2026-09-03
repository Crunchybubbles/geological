package io.github.crunchybubbles.geological.petrology;

/** Bounded normalized production and delivery stages for one colluvial input tranche. */
public record ColluvialProductionState(
    double weatheringAvailability,
    double erodibilityResponse,
    double slopeMobilityResponse,
    double runoffMobilityResponse,
    double mobilizationPotential,
    double mobilizedFraction,
    double retainedFraction,
    double transportArrivalFraction,
    double depositionFraction,
    double netDepositionFraction) {
  private static final double WEATHERING_DEPTH_REFERENCE = 12.0;
  private static final double SLOPE_REFERENCE = 0.24;
  private static final double MINIMUM_SLOPE_RESPONSE = 0.25;
  private static final double MINIMUM_RUNOFF_RESPONSE = 0.65;

  public ColluvialProductionState {
    requireUnit(weatheringAvailability, "weathering availability");
    requireUnit(erodibilityResponse, "erodibility response");
    requireUnit(slopeMobilityResponse, "slope mobility response");
    requireUnit(runoffMobilityResponse, "runoff mobility response");
    requireUnit(mobilizationPotential, "mobilization potential");
    requireUnit(mobilizedFraction, "mobilized fraction");
    requireUnit(retainedFraction, "retained fraction");
    requireUnit(transportArrivalFraction, "transport-arrival fraction");
    requireUnit(depositionFraction, "deposition fraction");
    requireUnit(netDepositionFraction, "net-deposition fraction");
  }

  /** Derives the bounded stages from one exact input balance. */
  public static ColluvialProductionState from(ColluvialSedimentBudget.InputBalance balance) {
    if (balance == null) {
      throw new IllegalArgumentException("colluvial input balance is required");
    }
    ColluvialSedimentBudget.ProductionInput input = balance.input();
    double weatheringAvailability = clamp(input.weatheringDepth() / WEATHERING_DEPTH_REFERENCE);
    double erodibilityResponse = 0.5 + 0.5 * input.erodibilityIndex();
    double slopeMobilityResponse =
        MINIMUM_SLOPE_RESPONSE
            + (1.0 - MINIMUM_SLOPE_RESPONSE) * clamp(input.slope() / SLOPE_REFERENCE);
    double runoffMobilityResponse =
        MINIMUM_RUNOFF_RESPONSE + (1.0 - MINIMUM_RUNOFF_RESPONSE) * input.runoffIndex();
    double mobilizationPotential =
        weatheringAvailability
            * erodibilityResponse
            * slopeMobilityResponse
            * runoffMobilityResponse;
    long capacity = input.capacityFixedUnits();
    long mobilized = balance.mobilizedFixedUnits();
    long arrived = mobilized - balance.transportLossFixedUnits();
    long deposited = balance.depositedFixedUnits();
    return new ColluvialProductionState(
        weatheringAvailability,
        erodibilityResponse,
        slopeMobilityResponse,
        runoffMobilityResponse,
        mobilizationPotential,
        fraction(mobilized, capacity),
        fraction(balance.retainedFixedUnits(), capacity),
        fraction(arrived, mobilized),
        fraction(deposited, arrived),
        fraction(deposited, capacity));
  }

  private static double fraction(long numerator, long denominator) {
    return denominator > 0 ? clamp((double) numerator / denominator) : 0.0;
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
