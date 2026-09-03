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
    long residualMeltFractionPpm) {
  public MagmaDifferentiationState {
    if (tectonicSetting == null
        || meltingMechanism == null
        || sourceLithologyClass == null
        || meltFractionClass == null
        || sulfurSaturationHistory == null
        || crustalAssimilationClass == null
        || differentiationPath == null
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
              250_000L);
      case 1 ->
          proof(
              sourceReservoirIds,
              SulfurSaturationHistory.APPROACHING_SATURATION,
              CrustalAssimilationClass.LIMITED_LOWER_CRUST,
              DifferentiationPath.FRACTIONAL_CRYSTALLIZATION,
              550_000L);
      default ->
          proof(
              sourceReservoirIds,
              SulfurSaturationHistory.SATURATED,
              CrustalAssimilationClass.INCREASING_LOWER_CRUST,
              DifferentiationPath.RESIDUAL_FELSIC_FRACTIONATION,
              850_000L);
    };
  }

  private static MagmaDifferentiationState proof(
      List<StableId> sourceReservoirIds,
      SulfurSaturationHistory sulfurSaturationHistory,
      CrustalAssimilationClass crustalAssimilationClass,
      DifferentiationPath differentiationPath,
      long cumulativeCrystalFractionPpm) {
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
        MaterialAssemblage.SCALE - cumulativeCrystalFractionPpm);
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
}
