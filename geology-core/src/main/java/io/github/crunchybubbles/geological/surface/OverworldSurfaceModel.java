package io.github.crunchybubbles.geological.surface;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.Noise2D;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Point2;

/** Coarse Phase 0 uplift, terrain, catchment, drainage, and weathering response. */
public final class OverworldSurfaceModel {
  private final Noise2D terrainNoise;
  private final Noise2D weatheringNoise;

  public OverworldSurfaceModel(WorldIdentity identity) {
    this(identity, 4096);
  }

  public OverworldSurfaceModel(WorldIdentity identity, int noiseCacheSize) {
    terrainNoise = new Noise2D(identity, "overworld-terrain", noiseCacheSize);
    weatheringNoise = new Noise2D(identity, "overworld-weathering", noiseCacheSize);
  }

  public void clearCaches() {
    terrainNoise.clearCache();
    weatheringNoise.clearCache();
  }

  public SurfaceFields evaluate(Province province, Point2 worldPoint) {
    Point2 local = province.frame().toLocal(worldPoint);
    DrainageSample drainage = drainage(province, local);
    double elevation = elevation(province, worldPoint, local, drainage);
    double step = 4.0;
    Point2 worldEast = province.frame().toWorld(local.add(step, 0.0));
    Point2 worldWest = province.frame().toWorld(local.add(-step, 0.0));
    Point2 worldSouth = province.frame().toWorld(local.add(0.0, step));
    Point2 worldNorth = province.frame().toWorld(local.add(0.0, -step));
    double gradientU =
        (elevation(
                    province,
                    worldEast,
                    local.add(step, 0.0),
                    drainage(province, local.add(step, 0.0)))
                - elevation(
                    province,
                    worldWest,
                    local.add(-step, 0.0),
                    drainage(province, local.add(-step, 0.0))))
            / (2.0 * step);
    double gradientV =
        (elevation(
                    province,
                    worldSouth,
                    local.add(0.0, step),
                    drainage(province, local.add(0.0, step)))
                - elevation(
                    province,
                    worldNorth,
                    local.add(0.0, -step),
                    drainage(province, local.add(0.0, -step))))
            / (2.0 * step);
    double slope = StrictMath.hypot(gradientU, gradientV);
    double uplift = uplift(province, local);
    double weathering =
        clamp(
            2.2
                + 7.0 / (1.0 + 3.5 * slope)
                + 1.3 * weatheringNoise.value(worldPoint.x(), worldPoint.z(), 190.0)
                - 1.4 * drainage.flowAccumulation(),
            0.4,
            12.0);
    boolean outcrop = slope >= 0.24 || weathering <= 3.2 || drainage.channelDistance() <= 16.0;
    return new SurfaceFields(worldPoint, elevation, uplift, slope, weathering, outcrop, drainage);
  }

  private double elevation(
      Province province, Point2 worldPoint, Point2 local, DrainageSample drainage) {
    RiftArcGeometry geometry = province.geometry();
    double uplift = uplift(province, local);
    double basinValue = geometry.basin().footprintValue(local);
    double basinSubsidence = basinValue < 1.0 ? -17.0 * (1.0 - basinValue) : 0.0;
    double strength = strengthProxy(geometry, local);
    double resistantRelief = (strength - 0.55) * 20.0 * clamp(uplift / 80.0, 0.0, 1.0);
    double roughness = 13.0 * terrainNoise.fractal(worldPoint.x(), worldPoint.z(), 510.0, 3);
    double channelIncision = -19.0 * StrictMath.exp(-drainage.channelDistance() / 52.0);
    return 76.0 + uplift + basinSubsidence + resistantRelief + roughness + channelIncision;
  }

  private static double uplift(Province province, Point2 local) {
    RiftArcGeometry geometry = province.geometry();
    Point2 pluton =
        new Point2(
            geometry.plutonPulses().getLast().center().x(),
            geometry.plutonPulses().getLast().center().z());
    double radial = StrictMath.sqrt(local.squaredDistance(pluton)) / (0.42 * province.cellSize());
    double arcUplift = 54.0 * StrictMath.exp(-2.4 * radial * radial);
    double faultRidge =
        14.0
            * StrictMath.exp(
                -StrictMath.abs(local.x() - geometry.fault().planeU())
                    / geometry.fault().damageHalfWidth());
    double regionalTilt = 10.0 * clamp((local.z() / province.cellSize()) + 0.5, 0.0, 1.0);
    return 24.0 + arcUplift + faultRidge + regionalTilt;
  }

  private static double strengthProxy(RiftArcGeometry geometry, Point2 local) {
    for (RiftArcGeometry.PlutonPulse pulse : geometry.plutonPulses().reversed()) {
      double u = (local.x() - pulse.center().x()) / pulse.radiusU();
      double v = (local.z() - pulse.center().z()) / pulse.radiusV();
      if (u * u + v * v <= 1.0) {
        return pulse.lithology().strength();
      }
    }
    double basinValue = geometry.basin().footprintValue(local);
    return basinValue < 1.0 ? 0.48 : 0.82;
  }

  private static DrainageSample drainage(Province province, Point2 local) {
    RiftArcGeometry geometry = province.geometry();
    double scale = province.cellSize();
    double sourceU = geometry.porphyryCenter().x();
    double phase = geometry.drainagePhase();
    double trunkU =
        sourceU + 0.075 * scale * StrictMath.sin(2.0 * StrictMath.PI * local.z() / scale + phase);
    double channelDistance = StrictMath.abs(local.x() - trunkU);
    double longitudinal = clamp(local.z() / scale + 0.5, 0.0, 1.0);
    double catchmentFocus = StrictMath.exp(-channelDistance / (0.17 * scale));
    double accumulation = clamp(longitudinal * catchmentFocus, 0.0, 1.0);
    double channelWidth = 7.0 + 30.0 * accumulation;
    boolean channel = channelDistance <= channelWidth;

    double derivative =
        0.15 * StrictMath.PI * StrictMath.cos(2.0 * StrictMath.PI * local.z() / scale + phase);
    double length = StrictMath.hypot(derivative, 1.0);
    double localFlowU = derivative / length;
    double localFlowV = 1.0 / length;
    double cosine = StrictMath.cos(province.frame().rotationRadians());
    double sine = StrictMath.sin(province.frame().rotationRadians());
    Point2 worldFlow =
        new Point2(
            cosine * localFlowU - sine * localFlowV, sine * localFlowU + cosine * localFlowV);

    Point2 placer = geometry.placerCenter();
    double trapAlong =
        0.5 + 0.5 * StrictMath.cos(2.0 * StrictMath.PI * (local.z() - placer.z()) / 230.0);
    double trapAcross = StrictMath.exp(-channelDistance / 20.0);
    double trapScore = clamp(trapAlong * trapAcross, 0.0, 1.0);
    double placerU = (local.x() - placer.x()) / 68.0;
    double placerV = (local.z() - placer.z()) / 105.0;
    boolean sourceLinkedPlacer =
        province.grammar().formsPlacer()
            && local.z() > geometry.porphyryCenter().z()
            && placerU * placerU + placerV * placerV <= 1.0
            && trapScore >= 0.42;
    return new DrainageSample(
        channelDistance,
        accumulation,
        worldFlow,
        local.z(),
        trapScore,
        channel,
        sourceLinkedPlacer);
  }

  private static double clamp(double value, double minimum, double maximum) {
    return StrictMath.max(minimum, StrictMath.min(maximum, value));
  }
}
