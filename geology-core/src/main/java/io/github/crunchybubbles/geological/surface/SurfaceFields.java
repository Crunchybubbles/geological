package io.github.crunchybubbles.geological.surface;

import io.github.crunchybubbles.geological.model.Point2;

/** Physical surface fields before bedrock/outcrop presentation is evaluated. */
public record SurfaceFields(
    Point2 point,
    double elevation,
    double uplift,
    double slope,
    double weatheringDepth,
    boolean outcrop,
    DrainageSample drainage) {
  public SurfaceFields {
    if (point == null
        || !Double.isFinite(elevation)
        || !Double.isFinite(uplift)
        || !Double.isFinite(slope)
        || slope < 0.0
        || !Double.isFinite(weatheringDepth)
        || weatheringDepth < 0.0
        || drainage == null) {
      throw new IllegalArgumentException("surface fields must be complete and finite");
    }
  }
}
