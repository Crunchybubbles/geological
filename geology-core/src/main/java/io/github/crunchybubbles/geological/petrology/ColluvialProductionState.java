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
    double netDepositionFraction,
    double processResponse) {
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
    requireUnit(processResponse, "process response");
  }

  /** Derives the bounded stages from one exact input balance. */
  public static ColluvialProductionState from(ColluvialSedimentBudget.InputBalance balance) {
    return from(balance, ColluvialTransportPolicy.DEFAULT);
  }

  /** Derives the bounded stages with an explicit response policy. */
  public static ColluvialProductionState from(
      ColluvialSedimentBudget.InputBalance balance, ColluvialTransportPolicy policy) {
    if (balance == null) {
      throw new IllegalArgumentException("colluvial input balance is required");
    }
    if (policy == null) {
      throw new IllegalArgumentException("colluvial transport policy is required");
    }
    if (!policy.equals(balance.transportPolicy())) {
      throw new IllegalArgumentException("colluvial production state must use the balance policy");
    }
    ColluvialSedimentBudget.ProductionInput input = balance.input();
    double weatheringAvailability =
        clamp(input.weatheringDepth() / policy.weatheringDepthReference());
    double erodibilityResponse = 0.5 + 0.5 * input.erodibilityIndex();
    double slopeMobilityResponse =
        policy.minimumSlopeMobility()
            + (1.0 - policy.minimumSlopeMobility())
                * clamp(input.slope() / policy.slopeMobilityReference());
    double runoffMobilityResponse =
        policy.minimumRunoffMobilityResponse()
            + (1.0 - policy.minimumRunoffMobilityResponse()) * input.runoffIndex();
    double processResponse = policy.processResponse(balance.transportProcess().processClass());
    double mobilizationPotential =
        weatheringAvailability
            * erodibilityResponse
            * slopeMobilityResponse
            * runoffMobilityResponse
            * processResponse;
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
        fraction(deposited, capacity),
        processResponse);
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
