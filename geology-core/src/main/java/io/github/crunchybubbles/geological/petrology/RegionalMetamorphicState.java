package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.Optional;

/**
 * Bounded spatial regional-metamorphism context derived from the authored fold field.
 *
 * <p>This is a deterministic field/context proof, not a regional equilibrium solver or a body
 * generator. The fold event remains the identity and timing anchor so the Phase 1 chronicle is
 * unchanged.
 */
public record RegionalMetamorphicState(
    StableId driverEventId,
    AgeKey eventAge,
    MetamorphicGrade grade,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double peakTemperatureCelsius,
    double peakPressureMpa,
    MetamorphicProcessState.StrainClass strainClass,
    long intensityPpm) {
  public RegionalMetamorphicState {
    if (driverEventId == null
        || eventAge == null
        || grade == null
        || facies == null
        || path == null
        || strainClass == null
        || grade == MetamorphicGrade.NONE
        || facies == MetamorphicFacies.NONE
        || path == MetamorphicPath.NONE
        || strainClass == MetamorphicProcessState.StrainClass.NONE
        || !Double.isFinite(peakTemperatureCelsius)
        || peakTemperatureCelsius <= 0.0
        || peakTemperatureCelsius > 2_000.0
        || !Double.isFinite(peakPressureMpa)
        || peakPressureMpa <= 0.0
        || peakPressureMpa > 5_000.0
        || intensityPpm <= 0
        || intensityPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "regional metamorphic state is incomplete or out of bounds");
    }
  }

  /** Returns the canonical bounded principal-axis/frame evidence for this regional field. */
  public MetamorphicStrainState strainState() {
    return MetamorphicStrainState.proofFor(path, strainClass, intensityPpm);
  }

  /**
   * Derives the bounded regional field at a world point when it lies inside the authored fold. The
   * fold taper is reused as the normalized intensity proof, keeping the response spatially
   * continuous and exactly reproducible.
   */
  public static Optional<RegionalMetamorphicState> proofFor(Province province, Point3 worldPoint) {
    if (province == null || worldPoint == null) {
      throw new IllegalArgumentException("province and world point are required");
    }
    Optional<GeologicalEvent> foldEvent =
        province.chronicle().events().stream()
            .filter(event -> event.type() == EventType.FOLD)
            .findFirst();
    if (foldEvent.isEmpty()) {
      return Optional.empty();
    }
    Point3 local = province.frame().toLocal(worldPoint);
    double radius = province.geometry().fold().radius();
    double radial = StrictMath.hypot(local.x(), local.z());
    if (!(radial < radius)) {
      return Optional.empty();
    }
    double normalizedRadius = radial / radius;
    double taper = 1.0 - normalizedRadius * normalizedRadius;
    long intensityPpm =
        Math.max(
            1L,
            Math.min(
                MaterialAssemblage.SCALE, Math.round(MaterialAssemblage.SCALE * taper * taper)));
    MetamorphicGrade grade = gradeFor(province, intensityPpm);
    MetamorphicFacies facies = faciesFor(grade);
    double intensity = intensityPpm / (double) MaterialAssemblage.SCALE;
    double peakTemperatureCelsius = temperatureFor(grade) + 35.0 * intensity;
    double peakPressureMpa = pressureFor(grade) + 90.0 * intensity;
    return Optional.of(
        new RegionalMetamorphicState(
            foldEvent.orElseThrow().id(),
            foldEvent.orElseThrow().age(),
            grade,
            facies,
            MetamorphicPath.COLLISION_CLOCKWISE,
            peakTemperatureCelsius,
            peakPressureMpa,
            strainFor(grade),
            intensityPpm));
  }

  private static MetamorphicGrade gradeFor(Province province, long intensityPpm) {
    MetamorphicGrade maximumGrade =
        switch (province.grammar()) {
          case EXHUMED_FERTILE_RIFT_TO_ARC -> MetamorphicGrade.MEDIUM;
          case BURIED_FERTILE_RIFT_TO_ARC -> MetamorphicGrade.HIGH;
          case BARREN_DRY_RIFT_TO_ARC -> MetamorphicGrade.LOW;
        };
    return switch (maximumGrade) {
      case HIGH ->
          intensityPpm >= 650_000L
              ? MetamorphicGrade.HIGH
              : intensityPpm >= 300_000L ? MetamorphicGrade.MEDIUM : MetamorphicGrade.LOW;
      case MEDIUM -> intensityPpm >= 500_000L ? MetamorphicGrade.MEDIUM : MetamorphicGrade.LOW;
      case LOW -> MetamorphicGrade.LOW;
      case NONE -> MetamorphicGrade.NONE;
    };
  }

  private static MetamorphicFacies faciesFor(MetamorphicGrade grade) {
    return switch (grade) {
      case LOW -> MetamorphicFacies.SUBGREENSCHIST;
      case MEDIUM -> MetamorphicFacies.GREENSCHIST;
      case HIGH -> MetamorphicFacies.AMPHIBOLITE;
      case NONE -> MetamorphicFacies.NONE;
    };
  }

  private static MetamorphicProcessState.StrainClass strainFor(MetamorphicGrade grade) {
    return switch (grade) {
      case LOW, MEDIUM -> MetamorphicProcessState.StrainClass.DIRECTED_FOLIATION;
      case HIGH -> MetamorphicProcessState.StrainClass.NEMATOBLASTIC;
      case NONE -> MetamorphicProcessState.StrainClass.NONE;
    };
  }

  private static double temperatureFor(MetamorphicGrade grade) {
    return switch (grade) {
      case LOW -> 430.0;
      case MEDIUM -> 560.0;
      case HIGH -> 700.0;
      case NONE -> 0.0;
    };
  }

  private static double pressureFor(MetamorphicGrade grade) {
    return switch (grade) {
      case LOW -> 480.0;
      case MEDIUM -> 720.0;
      case HIGH -> 980.0;
      case NONE -> 0.0;
    };
  }
}
