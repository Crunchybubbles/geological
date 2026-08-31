package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Compiles exact vertical transition bounds for the current bounded proof kernels. */
final class ColumnTransitionPlanner {
  private static final AgeKey VMS_AGE = new AgeKey(241.0, 0);
  private static final AgeKey AUREOLE_AGE = new AgeKey(96.0, 0);
  private static final AgeKey PORPHYRY_AGE = new AgeKey(92.0, 0);

  private ColumnTransitionPlanner() {}

  static ColumnIntervalProof plan(
      Province province, ColumnRequest request, List<SpatialCandidate> candidates) {
    RiftArcGeometry geometry = province.geometry();
    Point2 local = province.frame().toLocal(request.horizontalPoint());
    List<Double> transitions = new ArrayList<>();
    for (SpatialCandidate candidate : candidates) {
      if (!candidate.affectsColumnState()) {
        continue;
      }
      switch (candidate.kind()) {
        case STRATIGRAPHIC_PACKAGE -> addPackage(transitions, province, local);
        case UNCONFORMITY -> addUnconformity(transitions, geometry, local);
        case PLUTON_PULSE ->
            geometry.plutonPulses().stream()
                .filter(pulse -> pulse.id().equals(candidate.id()))
                .forEach(pulse -> addPulse(transitions, geometry, local, pulse));
        case FAULT_DAMAGE_ZONE -> {
          transitions.add(-geometry.fault().halfHeight());
          transitions.add(geometry.fault().halfHeight());
        }
        case FOLD_WARP -> {
          // Fold influence changes coordinates, not material state; body boundaries include it.
        }
        case CONTACT_AUREOLE -> addAureole(transitions, geometry, local);
        case VMS_DEPOSIT -> addVms(transitions, geometry, local);
        case PORPHYRY_SYSTEM -> addPorphyry(transitions, geometry, local);
      }
    }

    List<Double> boundedTransitions =
        transitions.stream()
            .filter(Double::isFinite)
            .filter(
                transition ->
                    transition >= request.minYInclusive() - 1.0
                        && transition <= request.maxYExclusive() + 1.0)
            .distinct()
            .sorted()
            .toList();
    TreeSet<Integer> splits = new TreeSet<>();
    splits.add(request.minYInclusive());
    splits.add(request.maxYExclusive());
    for (double transition : boundedTransitions) {
      long lowerBlock = (long) StrictMath.floor(transition - 0.5);
      addSplit(splits, request, lowerBlock);
      addSplit(splits, request, lowerBlock + 1L);
    }
    return new ColumnIntervalProof(
        "analytic kernel transition bounds with boundary-block isolation",
        boundedTransitions,
        List.copyOf(splits));
  }

  private static void addPackage(List<Double> transitions, Province province, Point2 local) {
    RiftArcGeometry geometry = province.geometry();
    boolean explicit =
        province.chronicle().events().stream()
            .anyMatch(event -> event.type() == EventType.ERODE_UNCONFORMITY);
    List<Double> formationBoundaries;
    if (explicit) {
      formationBoundaries = geometry.stratigraphicPackage().formationBoundaries(local);
    } else if (geometry.basin().footprintValue(local) < 1.0) {
      double base = geometry.basin().baseElevation();
      double thickness = geometry.basin().topElevation(local) - base;
      formationBoundaries =
          List.of(
              base,
              base + 0.22 * thickness,
              base + 0.58 * thickness,
              base + 0.82 * thickness,
              base + thickness);
    } else {
      formationBoundaries = List.of();
    }
    formationBoundaries.forEach(
        y -> transitions.add(pushY(geometry, local, y, new AgeKey(250.0, 0))));
  }

  private static void addUnconformity(
      List<Double> transitions, RiftArcGeometry geometry, Point2 local) {
    double surface = geometry.unconformity().elevation(local);
    transitions.add(pushY(geometry, local, surface, geometry.unconformity().age()));
    transitions.add(
        pushY(
            geometry,
            local,
            surface - geometry.unconformity().weatheringThickness(),
            geometry.unconformity().age()));
  }

  private static void addPulse(
      List<Double> transitions,
      RiftArcGeometry geometry,
      Point2 local,
      RiftArcGeometry.PlutonPulse pulse) {
    addEllipsoid(
        transitions,
        geometry,
        local,
        pulse.center(),
        pulse.radiusU(),
        pulse.radiusY(),
        pulse.radiusV(),
        1.0,
        pulse.birthAge());
  }

  private static void addAureole(List<Double> transitions, RiftArcGeometry geometry, Point2 local) {
    RiftArcGeometry.PlutonPulse youngest = geometry.plutonPulses().getLast();
    double meanRadius = (youngest.radiusU() + youngest.radiusY() + youngest.radiusV()) / 3.0;
    addEllipsoid(
        transitions,
        geometry,
        local,
        youngest.center(),
        youngest.radiusU(),
        youngest.radiusY(),
        youngest.radiusV(),
        1.0,
        AUREOLE_AGE);
    addEllipsoid(
        transitions,
        geometry,
        local,
        youngest.center(),
        youngest.radiusU(),
        youngest.radiusY(),
        youngest.radiusV(),
        1.0 + 128.0 / meanRadius,
        AUREOLE_AGE);
  }

  private static void addVms(List<Double> transitions, RiftArcGeometry geometry, Point2 local) {
    Point3 center = geometry.vmsCenter();
    addEllipsoid(transitions, geometry, local, center, 112.0, 15.0, 72.0, 1.0, VMS_AGE);
    double feederU = (local.x() - center.x()) / 28.0;
    double feederV = (local.z() - center.z()) / 34.0;
    if (feederU * feederU + feederV * feederV <= 1.0) {
      transitions.add(pushY(geometry, local, center.y() - 95.0, VMS_AGE));
      transitions.add(pushY(geometry, local, center.y(), VMS_AGE));
    }
  }

  private static void addPorphyry(
      List<Double> transitions, RiftArcGeometry geometry, Point2 local) {
    for (double radius : List.of(65.0, 125.0, 205.0)) {
      double deltaX = local.x() - geometry.porphyryCenter().x();
      double deltaZ = local.z() - geometry.porphyryCenter().z();
      double remaining = radius * radius - deltaX * deltaX - deltaZ * deltaZ;
      if (remaining >= 0.0) {
        double halfHeight = StrictMath.sqrt(remaining);
        transitions.add(
            pushY(geometry, local, geometry.porphyryCenter().y() - halfHeight, PORPHYRY_AGE));
        transitions.add(
            pushY(geometry, local, geometry.porphyryCenter().y() + halfHeight, PORPHYRY_AGE));
      }
    }
  }

  private static void addEllipsoid(
      List<Double> transitions,
      RiftArcGeometry geometry,
      Point2 local,
      Point3 center,
      double radiusU,
      double radiusY,
      double radiusV,
      double scale,
      AgeKey age) {
    double u = (local.x() - center.x()) / radiusU;
    double v = (local.z() - center.z()) / radiusV;
    double remaining = scale * scale - u * u - v * v;
    if (remaining < 0.0) {
      return;
    }
    double halfHeight = radiusY * StrictMath.sqrt(remaining);
    transitions.add(pushY(geometry, local, center.y() - halfHeight, age));
    transitions.add(pushY(geometry, local, center.y() + halfHeight, age));
  }

  private static double pushY(
      RiftArcGeometry geometry, Point2 local, double formationY, AgeKey age) {
    return geometry.pushForward(new Point3(local.x(), formationY, local.z()), age).y();
  }

  private static void addSplit(TreeSet<Integer> splits, ColumnRequest request, long split) {
    if (split > request.minYInclusive() && split < request.maxYExclusive()) {
      splits.add((int) split);
    }
  }
}
