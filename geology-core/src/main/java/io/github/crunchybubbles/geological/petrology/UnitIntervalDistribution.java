package io.github.crunchybubbles.geological.petrology;

/** Bounded triangular distribution for a dimensionless material property in [0, 1]. */
public record UnitIntervalDistribution(double minimum, double mode, double maximum) {
  public UnitIntervalDistribution {
    if (!Double.isFinite(minimum)
        || !Double.isFinite(mode)
        || !Double.isFinite(maximum)
        || minimum < 0.0
        || maximum > 1.0
        || minimum > mode
        || mode > maximum) {
      throw new IllegalArgumentException(
          "unit-interval distribution must satisfy 0 <= minimum <= mode <= maximum <= 1");
    }
  }

  public double sample(double unitUniform) {
    if (!Double.isFinite(unitUniform) || unitUniform < 0.0 || unitUniform >= 1.0) {
      throw new IllegalArgumentException("triangular sample input must lie in [0, 1)");
    }
    if (minimum == maximum) {
      return minimum;
    }
    double span = maximum - minimum;
    double risingFraction = (mode - minimum) / span;
    if (unitUniform < risingFraction) {
      return minimum + StrictMath.sqrt(unitUniform * span * (mode - minimum));
    }
    return maximum - StrictMath.sqrt((1.0 - unitUniform) * span * (maximum - mode));
  }

  public boolean contains(double value) {
    return Double.isFinite(value) && value >= minimum && value <= maximum;
  }
}
