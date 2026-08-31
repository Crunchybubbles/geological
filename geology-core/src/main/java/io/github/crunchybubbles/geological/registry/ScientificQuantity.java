package io.github.crunchybubbles.geological.registry;

/** Finite value with an explicit unit and citation or calibration rationale. */
public record ScientificQuantity(double value, ScientificUnit unit, ParameterBasis basis) {
  public ScientificQuantity {
    if (!Double.isFinite(value) || unit == null || basis == null) {
      throw new IllegalArgumentException(
          "scientific quantities must be finite, unitful, and sourced");
    }
  }
}
