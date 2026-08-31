package io.github.crunchybubbles.geological.stratigraphy;

/** Result of evaluating a package's implicit base, top, and normalized vertical coordinate. */
public record StratigraphicCoordinate(
    double baseElevation,
    double topElevation,
    double normalizedHeight,
    double thickness,
    boolean insidePackage) {
  public StratigraphicCoordinate {
    if (!Double.isFinite(baseElevation)
        || !Double.isFinite(topElevation)
        || !Double.isFinite(normalizedHeight)
        || !Double.isFinite(thickness)
        || thickness < 0.0
        || topElevation < baseElevation) {
      throw new IllegalArgumentException("stratigraphic coordinate must be finite and ordered");
    }
  }
}
