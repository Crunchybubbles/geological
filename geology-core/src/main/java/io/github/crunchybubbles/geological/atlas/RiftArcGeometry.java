package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.stratigraphy.StratigraphicPackageKernel;
import io.github.crunchybubbles.geological.stratigraphy.UnconformityKernel;
import java.util.List;

/** Immutable 2.5-D bodies and modifiers for the synthetic Phase 0 province. */
public record RiftArcGeometry(
    StableId basementId,
    Basin basin,
    StratigraphicPackageKernel stratigraphicPackage,
    UnconformityKernel unconformity,
    List<PlutonPulse> plutonPulses,
    Fault fault,
    Fold fold,
    StableId aureoleId,
    Point3 porphyryCenter,
    Point3 vmsCenter,
    Point2 placerCenter,
    double drainagePhase) {
  public RiftArcGeometry {
    if (basementId == null
        || basin == null
        || stratigraphicPackage == null
        || unconformity == null
        || fault == null
        || fold == null
        || aureoleId == null
        || porphyryCenter == null
        || vmsCenter == null
        || placerCenter == null
        || !Double.isFinite(drainagePhase)) {
      throw new IllegalArgumentException("rift-to-arc geometry must be complete and finite");
    }
    plutonPulses = List.copyOf(plutonPulses);
    if (plutonPulses.size() < 2) {
      throw new IllegalArgumentException("the proof pluton must contain multiple pulses");
    }
  }

  public Point3 pullBack(Point3 presentPoint, AgeKey bodyAge) {
    Point3 beforeFault = fault.pullBack(presentPoint, bodyAge);
    return fold.pullBack(beforeFault, bodyAge);
  }

  public Point3 pushForward(Point3 formationPoint, AgeKey bodyAge) {
    Point3 afterFold = fold.pushForward(formationPoint, bodyAge);
    return fault.pushForward(afterFold, bodyAge);
  }

  public double roundTripResidual(Point3 presentPoint, AgeKey bodyAge) {
    Point3 reconstructed = pushForward(pullBack(presentPoint, bodyAge), bodyAge);
    double deltaX = reconstructed.x() - presentPoint.x();
    double deltaY = reconstructed.y() - presentPoint.y();
    double deltaZ = reconstructed.z() - presentPoint.z();
    return StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
  }

  public record Basin(
      StableId id,
      StableId packageId,
      Point2 center,
      double radiusU,
      double radiusV,
      double baseElevation,
      double maximumThickness,
      AgeKey birthAge) {
    public Basin {
      if (id == null || packageId == null || center == null || birthAge == null) {
        throw new IllegalArgumentException("basin identity and context are required");
      }
      requirePositive(radiusU, "radiusU");
      requirePositive(radiusV, "radiusV");
      requirePositive(maximumThickness, "maximumThickness");
    }

    public double footprintValue(Point2 local) {
      double u = (local.x() - center.x()) / radiusU;
      double v = (local.z() - center.z()) / radiusV;
      return u * u + v * v;
    }

    public double topElevation(Point2 local) {
      double footprint = footprintValue(local);
      if (footprint >= 1.0) {
        return baseElevation;
      }
      return baseElevation + maximumThickness * StrictMath.pow(1.0 - footprint, 0.42);
    }

    public Lithology lithologyAt(Point3 local) {
      Point2 horizontal = new Point2(local.x(), local.z());
      double top = topElevation(horizontal);
      if (footprintValue(horizontal) >= 1.0 || local.y() < baseElevation || local.y() > top) {
        return null;
      }
      double fraction = (local.y() - baseElevation) / (top - baseElevation);
      if (fraction < 0.22) {
        return Lithology.BASAL_CONGLOMERATE;
      }
      if (fraction < 0.58) {
        return Lithology.MARINE_VOLCANICLASTIC;
      }
      if (fraction < 0.82) {
        return Lithology.BASIN_SHALE;
      }
      return Lithology.BASIN_SANDSTONE;
    }
  }

  public record PlutonPulse(
      StableId id,
      Point3 center,
      double radiusU,
      double radiusY,
      double radiusV,
      AgeKey birthAge,
      Lithology lithology) {
    public PlutonPulse {
      if (id == null || center == null || birthAge == null || lithology == null) {
        throw new IllegalArgumentException("pluton pulse identity and context are required");
      }
      requirePositive(radiusU, "radiusU");
      requirePositive(radiusY, "radiusY");
      requirePositive(radiusV, "radiusV");
    }

    public double implicitValue(Point3 local) {
      double u = (local.x() - center.x()) / radiusU;
      double y = (local.y() - center.y()) / radiusY;
      double v = (local.z() - center.z()) / radiusV;
      return u * u + y * y + v * v - 1.0;
    }

    public double approximateOutsideDistance(Point3 local) {
      double normalizedRadius = StrictMath.sqrt(StrictMath.max(0.0, implicitValue(local) + 1.0));
      double meanRadius = (radiusU + radiusY + radiusV) / 3.0;
      return (normalizedRadius - 1.0) * meanRadius;
    }
  }

  public record Fault(
      StableId id,
      double planeU,
      double halfLength,
      double halfHeight,
      double damageHalfWidth,
      double verticalSlip,
      AgeKey inheritedAge,
      AgeKey reactivationAge) {
    public Fault {
      if (id == null || inheritedAge == null || reactivationAge == null) {
        throw new IllegalArgumentException("fault identity and ages are required");
      }
      requirePositive(halfLength, "halfLength");
      requirePositive(halfHeight, "halfHeight");
      requirePositive(damageHalfWidth, "damageHalfWidth");
      requirePositive(verticalSlip, "verticalSlip");
    }

    public boolean intersectsDamageZone(Point3 local) {
      return StrictMath.abs(local.x() - planeU) <= damageHalfWidth
          && StrictMath.abs(local.z()) <= halfLength
          && StrictMath.abs(local.y()) <= halfHeight;
    }

    public Point3 pullBack(Point3 local, AgeKey bodyAge) {
      return local.withY(local.y() - displacement(local, bodyAge));
    }

    public Point3 pushForward(Point3 local, AgeKey bodyAge) {
      if (!reactivationAge.youngerThan(bodyAge) || StrictMath.abs(local.z()) > halfLength) {
        return local;
      }
      double presentY = local.y() + displacement(local, bodyAge);
      for (int iteration = 0; iteration < 8; iteration++) {
        Point3 present = local.withY(presentY);
        double residual = presentY - displacement(present, bodyAge) - local.y();
        if (StrictMath.abs(residual) < 1.0 / 256.0) {
          return present;
        }
        double derivative = 1.0 - displacementDerivative(present, bodyAge);
        if (!(derivative >= 0.25 && derivative <= 4.0)) {
          throw new IllegalStateException("fault inverse exceeds its Jacobian budget");
        }
        presentY -= residual / derivative;
      }
      Point3 present = local.withY(presentY);
      double residual = presentY - displacement(present, bodyAge) - local.y();
      if (StrictMath.abs(residual) >= 1.0 / 256.0) {
        throw new IllegalStateException("fault inverse did not converge within eight iterations");
      }
      return present;
    }

    public double pullBackVerticalJacobianDeterminant(Point3 present, AgeKey bodyAge) {
      return 1.0 - displacementDerivative(present, bodyAge);
    }

    private double displacement(Point3 present, AgeKey bodyAge) {
      if (!reactivationAge.youngerThan(bodyAge)
          || StrictMath.abs(present.z()) > halfLength
          || StrictMath.abs(present.y()) >= halfHeight) {
        return 0.0;
      }
      double alongStrikeTaper =
          0.5 + 0.5 * StrictMath.cos(StrictMath.PI * present.z() / halfLength);
      double verticalCosine = StrictMath.cos(StrictMath.PI * present.y() / (2.0 * halfHeight));
      double verticalTaper = verticalCosine * verticalCosine;
      double side = present.x() >= planeU ? 1.0 : -1.0;
      return side * verticalSlip * 0.5 * alongStrikeTaper * verticalTaper;
    }

    private double displacementDerivative(Point3 present, AgeKey bodyAge) {
      if (!reactivationAge.youngerThan(bodyAge)
          || StrictMath.abs(present.z()) > halfLength
          || StrictMath.abs(present.y()) >= halfHeight) {
        return 0.0;
      }
      double alongStrikeTaper =
          0.5 + 0.5 * StrictMath.cos(StrictMath.PI * present.z() / halfLength);
      double side = present.x() >= planeU ? 1.0 : -1.0;
      double coefficient = side * verticalSlip * 0.5 * alongStrikeTaper;
      return -coefficient
          * StrictMath.sin(StrictMath.PI * present.y() / halfHeight)
          * StrictMath.PI
          / (2.0 * halfHeight);
    }
  }

  public record Fold(StableId id, double radius, double amplitude, double wavelength, AgeKey age) {
    public Fold {
      if (id == null || age == null) {
        throw new IllegalArgumentException("fold identity and age are required");
      }
      requirePositive(radius, "radius");
      requirePositive(amplitude, "amplitude");
      requirePositive(wavelength, "wavelength");
    }

    public Point3 pullBack(Point3 local, AgeKey bodyAge) {
      if (!age.youngerThan(bodyAge)) {
        return local;
      }
      return local.withY(local.y() - warp(local));
    }

    public Point3 pushForward(Point3 local, AgeKey bodyAge) {
      if (!age.youngerThan(bodyAge)) {
        return local;
      }
      return local.withY(local.y() + warp(local));
    }

    private double warp(Point3 local) {
      double radial = StrictMath.hypot(local.x(), local.z()) / radius;
      if (radial >= 1.0) {
        return 0.0;
      }
      double taper = (1.0 - radial * radial) * (1.0 - radial * radial);
      return amplitude * StrictMath.sin(2.0 * StrictMath.PI * local.x() / wavelength) * taper;
    }
  }

  private static void requirePositive(double value, String name) {
    if (!(value > 0.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }
}
