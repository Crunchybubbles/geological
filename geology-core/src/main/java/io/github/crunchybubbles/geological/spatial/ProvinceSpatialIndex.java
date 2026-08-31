package io.github.crunchybubbles.geological.spatial;

import io.github.crunchybubbles.geological.atlas.LocalFrame;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.Bounds3D;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.ArrayList;
import java.util.List;

/** Pure per-province candidate index compiled from immutable descriptor bounds. */
public final class ProvinceSpatialIndex {
  public static final double GRID_CELL_SIZE = 256.0;

  private final Province province;
  private final StableId provinceId;
  private final UniformGridSpatialIndex index;

  private ProvinceSpatialIndex(
      Province province, StableId provinceId, UniformGridSpatialIndex index) {
    this.province = province;
    this.provinceId = provinceId;
    this.index = index;
  }

  public static ProvinceSpatialIndex compile(Province province) {
    RiftArcGeometry geometry = province.geometry();
    double deformationMargin =
        geometry.fold().amplitude() + geometry.fault().verticalSlip() * 0.5 + 1.0;
    List<SpatialCandidate> candidates = new ArrayList<>();

    RiftArcGeometry.Basin basin = geometry.basin();
    candidates.add(
        candidate(
            basin.packageId(),
            CandidateKind.STRATIGRAPHIC_PACKAGE,
            bounds(
                province.frame(),
                basin.center().x() - basin.radiusU(),
                basin.baseElevation() - deformationMargin,
                basin.center().z() - basin.radiusV(),
                basin.center().x() + basin.radiusU(),
                basin.baseElevation() + basin.maximumThickness() + deformationMargin,
                basin.center().z() + basin.radiusV()),
            basin.birthAge(),
            true));

    for (RiftArcGeometry.PlutonPulse pulse : geometry.plutonPulses()) {
      candidates.add(
          candidate(
              pulse.id(),
              CandidateKind.PLUTON_PULSE,
              bounds(
                  province.frame(),
                  pulse.center().x() - pulse.radiusU(),
                  pulse.center().y() - pulse.radiusY() - deformationMargin,
                  pulse.center().z() - pulse.radiusV(),
                  pulse.center().x() + pulse.radiusU(),
                  pulse.center().y() + pulse.radiusY() + deformationMargin,
                  pulse.center().z() + pulse.radiusV()),
              pulse.birthAge(),
              true));
    }

    RiftArcGeometry.Fault fault = geometry.fault();
    candidates.add(
        candidate(
            fault.id(),
            CandidateKind.FAULT_DAMAGE_ZONE,
            bounds(
                province.frame(),
                fault.planeU() - fault.damageHalfWidth(),
                -fault.halfHeight(),
                -fault.halfLength(),
                fault.planeU() + fault.damageHalfWidth(),
                fault.halfHeight(),
                fault.halfLength()),
            fault.reactivationAge(),
            true));

    RiftArcGeometry.Fold fold = geometry.fold();
    candidates.add(
        candidate(
            fold.id(),
            CandidateKind.FOLD_WARP,
            bounds(
                province.frame(),
                -fold.radius(),
                basin.baseElevation() - deformationMargin,
                -fold.radius(),
                fold.radius(),
                basin.baseElevation() + basin.maximumThickness() + deformationMargin,
                fold.radius()),
            fold.age(),
            false));

    RiftArcGeometry.PlutonPulse youngest = geometry.plutonPulses().getLast();
    double aureole = 128.0;
    double meanRadius = (youngest.radiusU() + youngest.radiusY() + youngest.radiusV()) / 3.0;
    double aureoleScale = 1.0 + aureole / meanRadius;
    candidates.add(
        candidate(
            geometry.aureoleId(),
            CandidateKind.CONTACT_AUREOLE,
            bounds(
                province.frame(),
                youngest.center().x() - youngest.radiusU() * aureoleScale,
                youngest.center().y() - youngest.radiusY() * aureoleScale - deformationMargin,
                youngest.center().z() - youngest.radiusV() * aureoleScale,
                youngest.center().x() + youngest.radiusU() * aureoleScale,
                youngest.center().y() + youngest.radiusY() * aureoleScale + deformationMargin,
                youngest.center().z() + youngest.radiusV() * aureoleScale),
            new AgeKey(96.0, 0),
            true));

    if (province.grammar().formsVms()) {
      candidates.add(
          candidate(
              province.proofIds().vmsDepositId(),
              CandidateKind.VMS_DEPOSIT,
              bounds(
                  province.frame(),
                  geometry.vmsCenter().x() - 112.0,
                  geometry.vmsCenter().y() - 95.0 - deformationMargin,
                  geometry.vmsCenter().z() - 72.0,
                  geometry.vmsCenter().x() + 112.0,
                  geometry.vmsCenter().y() + 15.0 + deformationMargin,
                  geometry.vmsCenter().z() + 72.0),
              new AgeKey(241.0, 0),
              true));
    }

    if (province.grammar().formsPorphyry()) {
      Point2 center = new Point2(geometry.porphyryCenter().x(), geometry.porphyryCenter().z());
      double radius = 205.0;
      candidates.add(
          candidate(
              province.proofIds().porphyryDepositId(),
              CandidateKind.PORPHYRY_SYSTEM,
              bounds(
                  province.frame(),
                  center.x() - radius,
                  geometry.porphyryCenter().y() - radius - deformationMargin,
                  center.z() - radius,
                  center.x() + radius,
                  geometry.porphyryCenter().y() + radius + deformationMargin,
                  center.z() + radius),
              new AgeKey(92.0, 0),
              true));
    }

    return new ProvinceSpatialIndex(
        province, province.id(), new UniformGridSpatialIndex(GRID_CELL_SIZE, candidates));
  }

  public StableId provinceId() {
    return provinceId;
  }

  public List<SpatialCandidate> at(Point2 point) {
    Point2 local = province.frame().toLocal(point);
    return index.at(point).stream()
        .filter(candidate -> mayIntersectColumn(candidate, local))
        .toList();
  }

  public List<SpatialCandidate> intersecting(Bounds2D bounds) {
    return index.intersecting(bounds);
  }

  public List<SpatialCandidate> allCandidates() {
    return index.allCandidates();
  }

  private boolean mayIntersectColumn(SpatialCandidate candidate, Point2 local) {
    RiftArcGeometry geometry = province.geometry();
    return switch (candidate.kind()) {
      case STRATIGRAPHIC_PACKAGE -> geometry.basin().footprintValue(local) < 1.0;
      case PLUTON_PULSE ->
          geometry.plutonPulses().stream()
              .filter(pulse -> pulse.id().equals(candidate.id()))
              .anyMatch(
                  pulse -> {
                    double u = (local.x() - pulse.center().x()) / pulse.radiusU();
                    double v = (local.z() - pulse.center().z()) / pulse.radiusV();
                    return u * u + v * v <= 1.0;
                  });
      case FAULT_DAMAGE_ZONE ->
          StrictMath.abs(local.x() - geometry.fault().planeU())
                  <= geometry.fault().damageHalfWidth()
              && StrictMath.abs(local.z()) <= geometry.fault().halfLength();
      case FOLD_WARP -> StrictMath.hypot(local.x(), local.z()) <= geometry.fold().radius();
      case CONTACT_AUREOLE -> {
        RiftArcGeometry.PlutonPulse youngest = geometry.plutonPulses().getLast();
        double meanRadius = (youngest.radiusU() + youngest.radiusY() + youngest.radiusV()) / 3.0;
        double scale = 1.0 + 128.0 / meanRadius;
        double u = (local.x() - youngest.center().x()) / youngest.radiusU();
        double v = (local.z() - youngest.center().z()) / youngest.radiusV();
        yield u * u + v * v <= scale * scale;
      }
      case VMS_DEPOSIT -> {
        double lensU = (local.x() - geometry.vmsCenter().x()) / 112.0;
        double lensV = (local.z() - geometry.vmsCenter().z()) / 72.0;
        double feederU = (local.x() - geometry.vmsCenter().x()) / 28.0;
        double feederV = (local.z() - geometry.vmsCenter().z()) / 34.0;
        yield lensU * lensU + lensV * lensV <= 1.0 || feederU * feederU + feederV * feederV <= 1.0;
      }
      case PORPHYRY_SYSTEM -> {
        double x = local.x() - geometry.porphyryCenter().x();
        double z = local.z() - geometry.porphyryCenter().z();
        yield x * x + z * z <= 205.0 * 205.0;
      }
    };
  }

  private static SpatialCandidate candidate(
      StableId id, CandidateKind kind, Bounds3D bounds, AgeKey age, boolean affectsColumnState) {
    return new SpatialCandidate(id, kind, bounds, age, affectsColumnState);
  }

  private static Bounds3D bounds(
      LocalFrame frame,
      double minU,
      double minY,
      double minV,
      double maxU,
      double maxY,
      double maxV) {
    Point2 first = frame.toWorld(new Point2(minU, minV));
    Point2 second = frame.toWorld(new Point2(minU, maxV));
    Point2 third = frame.toWorld(new Point2(maxU, minV));
    Point2 fourth = frame.toWorld(new Point2(maxU, maxV));
    double minX =
        StrictMath.min(
            StrictMath.min(first.x(), second.x()), StrictMath.min(third.x(), fourth.x()));
    double minZ =
        StrictMath.min(
            StrictMath.min(first.z(), second.z()), StrictMath.min(third.z(), fourth.z()));
    double maxX =
        StrictMath.max(
            StrictMath.max(first.x(), second.x()), StrictMath.max(third.x(), fourth.x()));
    double maxZ =
        StrictMath.max(
            StrictMath.max(first.z(), second.z()), StrictMath.max(third.z(), fourth.z()));
    return new Bounds3D(minX, minY, minZ, maxX, maxY, maxZ);
  }
}
