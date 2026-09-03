package io.github.crunchybubbles.geological.petrology;

/** Bounded principal-axis and orientation evidence for a metamorphic fabric response. */
public record MetamorphicStrainState(
    FrameClass frameClass,
    long intensityPpm,
    long shorteningAxisPpm,
    long flatteningAxisPpm,
    long stretchingAxisPpm,
    double foliationAzimuthDegrees,
    double lineationTrendDegrees,
    double lineationPlungeDegrees) {
  public MetamorphicStrainState {
    if (frameClass == null
        || intensityPpm < 0
        || intensityPpm > MaterialAssemblage.SCALE
        || shorteningAxisPpm < 0
        || shorteningAxisPpm > MaterialAssemblage.SCALE
        || flatteningAxisPpm < 0
        || flatteningAxisPpm > MaterialAssemblage.SCALE
        || stretchingAxisPpm < 0
        || stretchingAxisPpm > MaterialAssemblage.SCALE
        || !Double.isFinite(foliationAzimuthDegrees)
        || foliationAzimuthDegrees < 0.0
        || foliationAzimuthDegrees >= 360.0
        || !Double.isFinite(lineationTrendDegrees)
        || lineationTrendDegrees < 0.0
        || lineationTrendDegrees >= 360.0
        || !Double.isFinite(lineationPlungeDegrees)
        || lineationPlungeDegrees < -90.0
        || lineationPlungeDegrees > 90.0) {
      throw new IllegalArgumentException("metamorphic strain state is incomplete or out of bounds");
    }
    long axisTotal =
        Math.addExact(Math.addExact(shorteningAxisPpm, flatteningAxisPpm), stretchingAxisPpm);
    if (frameClass == FrameClass.NONE) {
      if (intensityPpm != 0L
          || axisTotal != 0L
          || foliationAzimuthDegrees != 0.0
          || lineationTrendDegrees != 0.0
          || lineationPlungeDegrees != 0.0) {
        throw new IllegalArgumentException("inactive strain state must be all zero");
      }
    } else if (intensityPpm == 0L || axisTotal != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "active strain state requires positive intensity and closed principal axes");
    }
  }

  public static MetamorphicStrainState none() {
    return new MetamorphicStrainState(FrameClass.NONE, 0L, 0L, 0L, 0L, 0.0, 0.0, 0.0);
  }

  /** Derives a deterministic bounded frame from a path, fabric class, and normalized intensity. */
  public static MetamorphicStrainState proofFor(
      MetamorphicPath path, MetamorphicProcessState.StrainClass strainClass, long intensityPpm) {
    if (path == null
        || strainClass == null
        || intensityPpm < 0
        || intensityPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "metamorphic strain inputs are incomplete or out of bounds");
    }
    if (intensityPpm == 0L) {
      return none();
    }
    if (strainClass == MetamorphicProcessState.StrainClass.NONE) {
      throw new IllegalArgumentException("active strain intensity requires a fabric class");
    }
    Frame frame = frameFor(path, strainClass);
    return new MetamorphicStrainState(
        frame.frameClass(),
        intensityPpm,
        frame.shorteningAxisPpm(),
        frame.flatteningAxisPpm(),
        frame.stretchingAxisPpm(),
        frame.foliationAzimuthDegrees(),
        frame.lineationTrendDegrees(),
        frame.lineationPlungeDegrees());
  }

  private static Frame frameFor(
      MetamorphicPath path, MetamorphicProcessState.StrainClass strainClass) {
    double azimuth = 0.0;
    double trend = 0.0;
    double plunge = 0.0;
    if (strainClass == MetamorphicProcessState.StrainClass.DIRECTED_FOLIATION
        || strainClass == MetamorphicProcessState.StrainClass.NEMATOBLASTIC
        || strainClass == MetamorphicProcessState.StrainClass.FRACTURE_CONTROLLED) {
      switch (path) {
        case COLLISION_CLOCKWISE, POLYMETAMORPHIC -> {
          azimuth = 45.0;
          trend = 135.0;
          plunge = path == MetamorphicPath.POLYMETAMORPHIC ? 20.0 : 15.0;
        }
        case SUBDUCTION_COLD -> {
          azimuth = 135.0;
          trend = 45.0;
          plunge = 30.0;
        }
        case EXTENSION_DECOMPRESSION -> {
          azimuth = 0.0;
          trend = 90.0;
          plunge = 0.0;
        }
        case BURIAL_HEATING -> {
          azimuth = 90.0;
          trend = 0.0;
          plunge = 0.0;
        }
        case HYDROTHERMAL_HYDRATION -> {
          azimuth = 45.0;
          trend = 90.0;
          plunge = 10.0;
        }
        case CONTACT_LOW_P, NONE -> {}
      }
    }
    return switch (strainClass) {
      case DIRECTED_FOLIATION ->
          new Frame(FrameClass.FOLIATION, 600_000L, 300_000L, 100_000L, azimuth, trend, plunge);
      case NEMATOBLASTIC ->
          new Frame(FrameClass.LINEATION, 700_000L, 200_000L, 100_000L, azimuth, trend, plunge);
      case GRANOBLASTIC ->
          new Frame(FrameClass.ISOTROPIC, 333_333L, 333_333L, 333_334L, 0.0, 0.0, 0.0);
      case THERMAL_RECRYSTALLIZATION ->
          new Frame(FrameClass.THERMAL, 333_333L, 333_333L, 333_334L, 0.0, 0.0, 0.0);
      case FRACTURE_CONTROLLED ->
          new Frame(FrameClass.FRACTURE, 100_000L, 200_000L, 700_000L, azimuth, trend, plunge);
      case REGOLITH_DISAGGREGATION ->
          new Frame(FrameClass.REGOLITH, 250_000L, 250_000L, 500_000L, 0.0, 0.0, 0.0);
      case NONE -> throw new IllegalArgumentException("inactive strain class has no frame");
    };
  }

  private record Frame(
      FrameClass frameClass,
      long shorteningAxisPpm,
      long flatteningAxisPpm,
      long stretchingAxisPpm,
      double foliationAzimuthDegrees,
      double lineationTrendDegrees,
      double lineationPlungeDegrees) {}

  public enum FrameClass {
    NONE,
    ISOTROPIC,
    FOLIATION,
    LINEATION,
    FRACTURE,
    THERMAL,
    REGOLITH
  }
}
