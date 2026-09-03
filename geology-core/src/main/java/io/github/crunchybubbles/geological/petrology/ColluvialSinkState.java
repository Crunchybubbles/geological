package io.github.crunchybubbles.geological.petrology;

/**
 * Coarse receiving-role state for the two explicit non-deposit sinks in one colluvial tranche.
 *
 * <p>The roles identify what leaves the modeled parcel, not an exact receiving body or coordinate.
 * Transport loss is retained along the intervening route; bypass continues beyond the deposition
 * target.
 */
public record ColluvialSinkState(
    SinkRole transportLossSink,
    SinkRole bypassSink,
    double transportLossFraction,
    double bypassFraction) {
  public ColluvialSinkState {
    if (transportLossSink == null || bypassSink == null) {
      throw new IllegalArgumentException("colluvial sink roles are required");
    }
    requireUnit(transportLossFraction, "transport-loss fraction");
    requireUnit(bypassFraction, "bypass fraction");
    if ((transportLossSink == SinkRole.NONE) != (transportLossFraction == 0.0)
        || (bypassSink == SinkRole.NONE) != (bypassFraction == 0.0)) {
      throw new IllegalArgumentException("colluvial sink roles must agree with their fractions");
    }
  }

  /** Derives sink roles from the exact closed input balance. */
  public static ColluvialSinkState from(ColluvialSedimentBudget.InputBalance balance) {
    if (balance == null) {
      throw new IllegalArgumentException("colluvial input balance is required");
    }
    long mobilized = balance.mobilizedFixedUnits();
    long arrived = mobilized - balance.transportLossFixedUnits();
    double transportLossFraction =
        mobilized > 0 ? balance.transportLossFixedUnits() / (double) mobilized : 0.0;
    double bypassFraction = arrived > 0 ? balance.bypassedFixedUnits() / (double) arrived : 0.0;
    return new ColluvialSinkState(
        transportLossFraction == 0.0 ? SinkRole.NONE : SinkRole.INTERMEDIATE_ROUTE_STORAGE,
        bypassFraction == 0.0 ? SinkRole.NONE : SinkRole.DOWNSTREAM_CONTINUATION,
        transportLossFraction,
        bypassFraction);
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  /** Receiving role for material that leaves the modeled colluvial parcel. */
  public enum SinkRole {
    NONE,
    INTERMEDIATE_ROUTE_STORAGE,
    DOWNSTREAM_CONTINUATION
  }
}
