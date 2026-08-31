package io.github.crunchybubbles.geological.registry;

/** Expected dimension and inclusive numeric bounds for one schema field. */
public record ParameterConstraint(
    QuantityDimension dimension, double minimumInclusive, double maximumInclusive) {
  public ParameterConstraint {
    if (dimension == null
        || !Double.isFinite(minimumInclusive)
        || !Double.isFinite(maximumInclusive)
        || maximumInclusive < minimumInclusive) {
      throw new IllegalArgumentException("parameter constraint must have finite ordered bounds");
    }
  }

  public boolean accepts(ScientificQuantity quantity) {
    return quantity.unit().dimension() == dimension
        && quantity.value() >= minimumInclusive
        && quantity.value() <= maximumInclusive;
  }
}
