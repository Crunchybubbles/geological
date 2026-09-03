package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.HashSet;
import java.util.List;

/**
 * Typed, bounded source-to-differentiated-melt state for one magma-system pulse. The fractions are
 * a normalized proof ledger, not a calibrated MELTS calculation or an absolute mass inventory.
 */
public record MagmaDifferentiationState(
    TectonicSetting tectonicSetting,
    List<StableId> sourceReservoirIds,
    MeltingMechanism meltingMechanism,
    SourceLithologyClass sourceLithologyClass,
    MeltFractionClass meltFractionClass,
    SulfurSaturationHistory sulfurSaturationHistory,
    CrustalAssimilationClass crustalAssimilationClass,
    DifferentiationPath differentiationPath,
    long cumulativeCrystalFractionPpm,
    long residualMeltFractionPpm,
    ResidualFluidPotential residualFluidPotential,
    List<String> fertilityTags) {
  /** Compatibility constructor for callers that only provide the original fraction ledger. */
  public MagmaDifferentiationState(
      TectonicSetting tectonicSetting,
      List<StableId> sourceReservoirIds,
      MeltingMechanism meltingMechanism,
      SourceLithologyClass sourceLithologyClass,
      MeltFractionClass meltFractionClass,
      SulfurSaturationHistory sulfurSaturationHistory,
      CrustalAssimilationClass crustalAssimilationClass,
      DifferentiationPath differentiationPath,
      long cumulativeCrystalFractionPpm,
      long residualMeltFractionPpm) {
    this(
        tectonicSetting,
        sourceReservoirIds,
        meltingMechanism,
        sourceLithologyClass,
        meltFractionClass,
        sulfurSaturationHistory,
        crustalAssimilationClass,
        differentiationPath,
        cumulativeCrystalFractionPpm,
        residualMeltFractionPpm,
        ResidualFluidPotential.UNRESOLVED,
        List.of());
  }

  public MagmaDifferentiationState {
    if (tectonicSetting == null
        || meltingMechanism == null
        || sourceLithologyClass == null
        || meltFractionClass == null
        || sulfurSaturationHistory == null
        || crustalAssimilationClass == null
        || differentiationPath == null
        || residualFluidPotential == null
        || fertilityTags == null
        || sourceReservoirIds == null
        || cumulativeCrystalFractionPpm < 0
        || cumulativeCrystalFractionPpm > MaterialAssemblage.SCALE
        || residualMeltFractionPpm < 0
        || residualMeltFractionPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "magma differentiation state is incomplete or out of bounds");
    }
    sourceReservoirIds = List.copyOf(sourceReservoirIds).stream().sorted().toList();
    if (sourceReservoirIds.isEmpty()
        || sourceReservoirIds.stream().anyMatch(id -> id == null)
        || sourceReservoirIds.size() != new HashSet<>(sourceReservoirIds).size()) {
      throw new IllegalArgumentException("magma source reservoirs must be non-empty and unique");
    }
    fertilityTags = List.copyOf(fertilityTags).stream().sorted().toList();
    if (fertilityTags.stream().anyMatch(tag -> tag == null || tag.isBlank())
        || fertilityTags.size() != new HashSet<>(fertilityTags).size()) {
      throw new IllegalArgumentException("magma fertility tags must be non-blank and unique");
    }
    if (cumulativeCrystalFractionPpm + residualMeltFractionPpm != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "magma crystal and residual-melt fractions must close to " + MaterialAssemblage.SCALE);
    }
  }

  /** Returns the deterministic arc proof state for an ordered pluton pulse. */
  public static MagmaDifferentiationState arcProofFor(
      int pulseOrder, List<StableId> sourceReservoirIds) {
    if (pulseOrder < 0) {
      throw new IllegalArgumentException("magma pulse order must be non-negative");
    }
    return switch (pulseOrder) {
      case 0 ->
          proof(
              sourceReservoirIds,
              SulfurSaturationHistory.UNDERSATURATED,
              CrustalAssimilationClass.NONE,
              DifferentiationPath.FRACTIONAL_CRYSTALLIZATION,
              250_000L,
              ResidualFluidPotential.MODERATE,
              List.of("OXIDIZED_ARC"));
      case 1 ->
          proof(
              sourceReservoirIds,
              SulfurSaturationHistory.APPROACHING_SATURATION,
              CrustalAssimilationClass.LIMITED_LOWER_CRUST,
              DifferentiationPath.FRACTIONAL_CRYSTALLIZATION,
              550_000L,
              ResidualFluidPotential.HIGH,
              List.of("OXIDIZED_ARC", "SULFIDE_APPROACHING_SATURATION"));
      default ->
          proof(
              sourceReservoirIds,
              SulfurSaturationHistory.SATURATED,
              CrustalAssimilationClass.INCREASING_LOWER_CRUST,
              DifferentiationPath.RESIDUAL_FELSIC_FRACTIONATION,
              850_000L,
              ResidualFluidPotential.VERY_HIGH,
              List.of("EVOLVED_RESIDUAL_MELT", "OXIDIZED_ARC", "VOLATILE_ENRICHED"));
    };
  }

  private static MagmaDifferentiationState proof(
      List<StableId> sourceReservoirIds,
      SulfurSaturationHistory sulfurSaturationHistory,
      CrustalAssimilationClass crustalAssimilationClass,
      DifferentiationPath differentiationPath,
      long cumulativeCrystalFractionPpm,
      ResidualFluidPotential residualFluidPotential,
      List<String> fertilityTags) {
    return new MagmaDifferentiationState(
        TectonicSetting.VOLCANIC_ARC,
        sourceReservoirIds,
        MeltingMechanism.FLUX_MELTING,
        SourceLithologyClass.HYDRATED_MANTLE_WEDGE_WITH_LOWER_CRUSTAL_INPUT,
        MeltFractionClass.MODERATE,
        sulfurSaturationHistory,
        crustalAssimilationClass,
        differentiationPath,
        cumulativeCrystalFractionPpm,
        MaterialAssemblage.SCALE - cumulativeCrystalFractionPpm,
        residualFluidPotential,
        fertilityTags);
  }

  public enum TectonicSetting {
    VOLCANIC_ARC
  }

  public enum MeltingMechanism {
    FLUX_MELTING
  }

  public enum SourceLithologyClass {
    HYDRATED_MANTLE_WEDGE_WITH_LOWER_CRUSTAL_INPUT
  }

  public enum MeltFractionClass {
    MODERATE
  }

  public enum SulfurSaturationHistory {
    UNDERSATURATED,
    APPROACHING_SATURATION,
    SATURATED
  }

  public enum CrustalAssimilationClass {
    NONE,
    LIMITED_LOWER_CRUST,
    INCREASING_LOWER_CRUST
  }

  public enum DifferentiationPath {
    FRACTIONAL_CRYSTALLIZATION,
    RESIDUAL_FELSIC_FRACTIONATION
  }

  /** Coarse residual-fluid potential used for pathway eligibility, not an ore-fertility claim. */
  public enum ResidualFluidPotential {
    UNRESOLVED,
    MODERATE,
    HIGH,
    VERY_HIGH;

    public String wireValue() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }
}
