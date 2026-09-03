package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import java.util.Optional;

/** Bounded burial, strain, fluid, and reaction evidence for one metamorphic history. */
public record MetamorphicProcessState(
    BurialCurveClass burialCurveClass,
    StrainClass strainClass,
    FluidAvailabilityClass fluidAvailabilityClass,
    long strainIntensityPpm,
    MetamorphicStrainState strainState,
    long reactionProgressPpm,
    long massTransferPpm,
    long retrogressionPotentialPpm,
    MetamorphicReactionState reactionState) {
  /** Compatibility constructor for callers that only provide the original process proxies. */
  public MetamorphicProcessState(
      BurialCurveClass burialCurveClass,
      StrainClass strainClass,
      FluidAvailabilityClass fluidAvailabilityClass,
      long reactionProgressPpm,
      long massTransferPpm,
      long retrogressionPotentialPpm) {
    this(
        burialCurveClass,
        strainClass,
        fluidAvailabilityClass,
        0L,
        MetamorphicStrainState.none(),
        reactionProgressPpm,
        massTransferPpm,
        retrogressionPotentialPpm,
        MetamorphicReactionState.none());
  }

  /** Compatibility constructor for the alpha85 process shape without an explicit strain frame. */
  public MetamorphicProcessState(
      BurialCurveClass burialCurveClass,
      StrainClass strainClass,
      FluidAvailabilityClass fluidAvailabilityClass,
      long strainIntensityPpm,
      long reactionProgressPpm,
      long massTransferPpm,
      long retrogressionPotentialPpm,
      MetamorphicReactionState reactionState) {
    this(
        burialCurveClass,
        strainClass,
        fluidAvailabilityClass,
        strainIntensityPpm,
        MetamorphicStrainState.proofFor(MetamorphicPath.NONE, strainClass, strainIntensityPpm),
        reactionProgressPpm,
        massTransferPpm,
        retrogressionPotentialPpm,
        reactionState);
  }

  public MetamorphicProcessState {
    if (burialCurveClass == null
        || strainClass == null
        || fluidAvailabilityClass == null
        || strainState == null
        || reactionState == null
        || strainIntensityPpm < 0
        || strainIntensityPpm > MaterialAssemblage.SCALE
        || reactionProgressPpm < 0
        || reactionProgressPpm > MaterialAssemblage.SCALE
        || massTransferPpm < 0
        || massTransferPpm > MaterialAssemblage.SCALE
        || retrogressionPotentialPpm < 0
        || retrogressionPotentialPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "metamorphic process state is incomplete or out of bounds");
    }
    if (strainState.intensityPpm() != strainIntensityPpm) {
      throw new IllegalArgumentException("strain state intensity disagrees with process state");
    }
  }

  /** Derives the deterministic proof state from a P-T path and optional alteration response. */
  public static MetamorphicProcessState proofFor(
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      MaterialProcessClass processClass,
      long replacementPpm,
      Optional<ProcessFluidState> fluidState) {
    return proofFor(
        grade, facies, path, processClass, replacementPpm, fluidState, Optional.empty());
  }

  /** Derives process state with optional host context for carbonate-contact reactions. */
  public static MetamorphicProcessState proofFor(
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      MaterialProcessClass processClass,
      long replacementPpm,
      Optional<ProcessFluidState> fluidState,
      Optional<Lithology> hostLithology) {
    if (grade == null
        || facies == null
        || path == null
        || processClass == null
        || fluidState == null
        || hostLithology == null
        || replacementPpm < 0
        || replacementPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "metamorphic process inputs are incomplete or out of bounds");
    }
    if ((processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
            || processClass == MaterialProcessClass.WEATHERING)
        && fluidState.isEmpty()) {
      throw new IllegalArgumentException("mass-transfer metamorphism requires fluid evidence");
    }
    long gradeProgress =
        switch (grade) {
          case NONE -> 0L;
          case LOW -> 350_000L;
          case MEDIUM -> 650_000L;
          case HIGH -> 900_000L;
        };
    if (path == MetamorphicPath.CONTACT_LOW_P
        || (path == MetamorphicPath.NONE
            && processClass == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM)) {
      return new MetamorphicProcessState(
          BurialCurveClass.CONTACT_HEATING,
          StrainClass.THERMAL_RECRYSTALLIZATION,
          FluidAvailabilityClass.LIMITED_AQUEOUS,
          300_000L,
          MetamorphicStrainState.proofFor(path, StrainClass.THERMAL_RECRYSTALLIZATION, 300_000L),
          Math.max(700_000L, gradeProgress),
          0L,
          150_000L,
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    if (path == MetamorphicPath.BURIAL_HEATING
        || path == MetamorphicPath.SUBDUCTION_COLD
        || path == MetamorphicPath.EXTENSION_DECOMPRESSION
        || path == MetamorphicPath.POLYMETAMORPHIC) {
      return new MetamorphicProcessState(
          burialCurveFor(path),
          strainFor(facies),
          path == MetamorphicPath.SUBDUCTION_COLD
              ? FluidAvailabilityClass.LIMITED_AQUEOUS
              : FluidAvailabilityClass.BUFFERED_AQUEOUS,
          gradeProgress,
          MetamorphicStrainState.proofFor(path, strainFor(facies), gradeProgress),
          gradeProgress,
          0L,
          retrogressionFor(path),
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    if (path == MetamorphicPath.COLLISION_CLOCKWISE) {
      return new MetamorphicProcessState(
          BurialCurveClass.COLLISIONAL_THICKENING,
          strainFor(facies),
          FluidAvailabilityClass.BUFFERED_AQUEOUS,
          gradeProgress,
          MetamorphicStrainState.proofFor(path, strainFor(facies), gradeProgress),
          gradeProgress,
          0L,
          250_000L,
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    if (path == MetamorphicPath.HYDROTHERMAL_HYDRATION) {
      return new MetamorphicProcessState(
          BurialCurveClass.HYDROTHERMAL_HEATING,
          StrainClass.FRACTURE_CONTROLLED,
          FluidAvailabilityClass.HYDROTHERMAL_FLOW,
          700_000L,
          MetamorphicStrainState.proofFor(path, StrainClass.FRACTURE_CONTROLLED, 700_000L),
          Math.max(500_000L, gradeProgress),
          0L,
          700_000L,
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    if (processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM) {
      return new MetamorphicProcessState(
          BurialCurveClass.HYDROTHERMAL_HEATING,
          StrainClass.FRACTURE_CONTROLLED,
          FluidAvailabilityClass.HYDROTHERMAL_FLOW,
          650_000L,
          MetamorphicStrainState.proofFor(path, StrainClass.FRACTURE_CONTROLLED, 650_000L),
          replacementPpm,
          replacementPpm,
          700_000L,
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    if (processClass == MaterialProcessClass.WEATHERING) {
      return new MetamorphicProcessState(
          BurialCurveClass.SURFACE_WEATHERING,
          StrainClass.REGOLITH_DISAGGREGATION,
          FluidAvailabilityClass.SURFACE_METEORIC,
          250_000L,
          MetamorphicStrainState.proofFor(path, StrainClass.REGOLITH_DISAGGREGATION, 250_000L),
          replacementPpm,
          replacementPpm,
          850_000L,
          MetamorphicReactionState.proofFor(
              grade, facies, path, processClass, replacementPpm, hostLithology));
    }
    return new MetamorphicProcessState(
        BurialCurveClass.NONE,
        StrainClass.NONE,
        FluidAvailabilityClass.NONE,
        0L,
        MetamorphicStrainState.none(),
        0L,
        0L,
        0L,
        MetamorphicReactionState.proofFor(
            grade, facies, path, processClass, replacementPpm, hostLithology));
  }

  private static StrainClass strainFor(MetamorphicFacies facies) {
    return switch (facies) {
      case NONE -> StrainClass.NONE;
      case SUBGREENSCHIST, GREENSCHIST -> StrainClass.DIRECTED_FOLIATION;
      case AMPHIBOLITE -> StrainClass.NEMATOBLASTIC;
      case GRANULITE -> StrainClass.GRANOBLASTIC;
      case HORNBLENDE_HORNFELS -> StrainClass.THERMAL_RECRYSTALLIZATION;
    };
  }

  private static BurialCurveClass burialCurveFor(MetamorphicPath path) {
    return switch (path) {
      case BURIAL_HEATING -> BurialCurveClass.BURIAL_HEATING;
      case SUBDUCTION_COLD -> BurialCurveClass.SUBDUCTION_COOLING;
      case EXTENSION_DECOMPRESSION -> BurialCurveClass.EXHUMATION_DECOMPRESSION;
      case POLYMETAMORPHIC -> BurialCurveClass.POLYMETAMORPHIC_REWORKING;
      default -> throw new IllegalArgumentException("path is not an extended metamorphic path");
    };
  }

  private static long retrogressionFor(MetamorphicPath path) {
    return switch (path) {
      case EXTENSION_DECOMPRESSION -> 650_000L;
      case SUBDUCTION_COLD -> 400_000L;
      case POLYMETAMORPHIC -> 500_000L;
      case BURIAL_HEATING -> 300_000L;
      default -> throw new IllegalArgumentException("path is not an extended metamorphic path");
    };
  }

  public enum BurialCurveClass {
    NONE,
    COLLISIONAL_THICKENING,
    HYDROTHERMAL_HEATING,
    CONTACT_HEATING,
    SURFACE_WEATHERING,
    BURIAL_HEATING,
    SUBDUCTION_COOLING,
    EXHUMATION_DECOMPRESSION,
    POLYMETAMORPHIC_REWORKING
  }

  public enum StrainClass {
    NONE,
    DIRECTED_FOLIATION,
    NEMATOBLASTIC,
    GRANOBLASTIC,
    THERMAL_RECRYSTALLIZATION,
    FRACTURE_CONTROLLED,
    REGOLITH_DISAGGREGATION
  }

  public enum FluidAvailabilityClass {
    NONE,
    LIMITED_AQUEOUS,
    BUFFERED_AQUEOUS,
    HYDROTHERMAL_FLOW,
    SURFACE_METEORIC
  }
}
