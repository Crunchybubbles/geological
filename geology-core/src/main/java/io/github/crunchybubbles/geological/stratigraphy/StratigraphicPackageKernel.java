package io.github.crunchybubbles.geological.stratigraphy;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.List;

/** Finite onlapping package expressed by implicit base/top surfaces and a local coordinate. */
public record StratigraphicPackageKernel(
    StableId id,
    StableId basinId,
    AgeKey birthAge,
    Point2 center,
    double radiusU,
    double radiusV,
    double maximumThickness,
    List<Double> memberBoundaryFractions,
    UnconformityKernel unconformity) {
  public StratigraphicPackageKernel {
    if (id == null
        || basinId == null
        || birthAge == null
        || center == null
        || unconformity == null) {
      throw new IllegalArgumentException("stratigraphic package identity and context are required");
    }
    requirePositive(radiusU, "radiusU");
    requirePositive(radiusV, "radiusV");
    requirePositive(maximumThickness, "maximumThickness");
    memberBoundaryFractions = List.copyOf(memberBoundaryFractions);
    if (memberBoundaryFractions.size() != 3
        || memberBoundaryFractions.get(0) <= 0.0
        || memberBoundaryFractions.get(0) >= memberBoundaryFractions.get(1)
        || memberBoundaryFractions.get(1) >= memberBoundaryFractions.get(2)
        || memberBoundaryFractions.get(2) >= 1.0) {
      throw new IllegalArgumentException(
          "stratigraphic package requires three strictly ordered member boundaries");
    }
  }

  public double footprintValue(Point2 local) {
    double u = (local.x() - center.x()) / radiusU;
    double v = (local.z() - center.z()) / radiusV;
    return u * u + v * v;
  }

  public StratigraphicCoordinate evaluate(Point3 local) {
    Point2 horizontal = new Point2(local.x(), local.z());
    double footprint = footprintValue(horizontal);
    double base = unconformity.elevation(horizontal);
    if (footprint >= 1.0) {
      return new StratigraphicCoordinate(base, base, 0.0, 0.0, false);
    }
    double thickness = maximumThickness * StrictMath.pow(1.0 - footprint, 0.42);
    double top = base + thickness;
    double normalized = (local.y() - base) / thickness;
    boolean inside = normalized >= 0.0 && normalized <= 1.0;
    return new StratigraphicCoordinate(base, top, normalized, thickness, inside);
  }

  public Lithology lithologyAt(Point3 local) {
    StratigraphicCoordinate coordinate = evaluate(local);
    if (!coordinate.insidePackage()) {
      return null;
    }
    double fraction = coordinate.normalizedHeight();
    if (fraction < memberBoundaryFractions.get(0)) {
      return Lithology.BASAL_CONGLOMERATE;
    }
    if (fraction < memberBoundaryFractions.get(1)) {
      return Lithology.MARINE_VOLCANICLASTIC;
    }
    if (fraction < memberBoundaryFractions.get(2)) {
      return Lithology.BASIN_SHALE;
    }
    return Lithology.BASIN_SANDSTONE;
  }

  /** Formation-space Y values at which package membership or member identity can change. */
  public List<Double> formationBoundaries(Point2 local) {
    double footprint = footprintValue(local);
    if (footprint >= 1.0) {
      return List.of();
    }
    double base = unconformity.elevation(local);
    double thickness = maximumThickness * StrictMath.pow(1.0 - footprint, 0.42);
    return List.of(
        base,
        base + memberBoundaryFractions.get(0) * thickness,
        base + memberBoundaryFractions.get(1) * thickness,
        base + memberBoundaryFractions.get(2) * thickness,
        base + thickness);
  }

  private static void requirePositive(double value, String name) {
    if (!(value > 0.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }
}
