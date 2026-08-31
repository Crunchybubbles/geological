package io.github.crunchybubbles.geological.stratigraphy;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;

/** Finite erosional surface with bounded relief and a preserved weathering profile. */
public record UnconformityKernel(
    StableId id,
    AgeKey age,
    Point2 center,
    double radiusU,
    double radiusV,
    double datumElevation,
    double maximumRelief,
    double weatheringThickness) {
  public UnconformityKernel {
    if (id == null || age == null || center == null) {
      throw new IllegalArgumentException("unconformity identity and context are required");
    }
    requirePositive(radiusU, "radiusU");
    requirePositive(radiusV, "radiusV");
    requirePositive(maximumRelief, "maximumRelief");
    requirePositive(weatheringThickness, "weatheringThickness");
    if (!Double.isFinite(datumElevation)) {
      throw new IllegalArgumentException("datumElevation must be finite");
    }
  }

  public double footprintValue(Point2 local) {
    double u = (local.x() - center.x()) / radiusU;
    double v = (local.z() - center.z()) / radiusV;
    return u * u + v * v;
  }

  public boolean insideFootprint(Point2 local) {
    return footprintValue(local) < 1.0;
  }

  public double elevation(Point2 local) {
    double footprint = footprintValue(local);
    if (footprint >= 1.0) {
      return datumElevation;
    }
    double u = (local.x() - center.x()) / radiusU;
    double v = (local.z() - center.z()) / radiusV;
    double relief =
        0.58 * StrictMath.sin(2.4 * u + 0.7 * v) + 0.42 * StrictMath.cos(2.1 * v - 0.5 * u);
    double edgeTaper = 1.0 - footprint;
    return datumElevation + maximumRelief * relief * edgeTaper;
  }

  public boolean insideWeatheringProfile(Point3 local) {
    Point2 horizontal = new Point2(local.x(), local.z());
    if (!insideFootprint(horizontal)) {
      return false;
    }
    double surface = elevation(horizontal);
    return local.y() < surface && local.y() >= surface - weatheringThickness;
  }

  private static void requirePositive(double value, String name) {
    if (!(value > 0.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }
}
