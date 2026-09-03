package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;

/** One linked pulse in the reduced source-to-differentiated-melt lineage. */
public record MagmaLineageState(
    StableId systemId,
    StableId pulseId,
    int pulseOrder,
    double differentiationProgress,
    String sourceReservoirClass,
    String waterClass,
    String oxidationClass,
    String residualFluidPotential,
    MagmaDifferentiationState differentiationState) {
  public MagmaLineageState(
      StableId systemId,
      StableId pulseId,
      int pulseOrder,
      double differentiationProgress,
      String sourceReservoirClass,
      String waterClass,
      String oxidationClass,
      String residualFluidPotential) {
    this(
        systemId,
        pulseId,
        pulseOrder,
        differentiationProgress,
        sourceReservoirClass,
        waterClass,
        oxidationClass,
        residualFluidPotential,
        MagmaDifferentiationState.arcProofFor(pulseOrder, java.util.List.of(systemId)));
  }

  public MagmaLineageState {
    if (systemId == null
        || pulseId == null
        || pulseOrder < 0
        || sourceReservoirClass == null
        || sourceReservoirClass.isBlank()
        || waterClass == null
        || waterClass.isBlank()
        || oxidationClass == null
        || oxidationClass.isBlank()
        || residualFluidPotential == null
        || residualFluidPotential.isBlank()
        || differentiationState == null) {
      throw new IllegalArgumentException("magma lineage state must be complete");
    }
    if (!Double.isFinite(differentiationProgress)
        || differentiationProgress < 0.0
        || differentiationProgress > 1.0) {
      throw new IllegalArgumentException("differentiation progress must lie in [0, 1]");
    }
    double normalizedCrystalFraction =
        differentiationState.cumulativeCrystalFractionPpm() / (double) MaterialAssemblage.SCALE;
    if (StrictMath.abs(differentiationProgress - normalizedCrystalFraction) > 1.0e-12) {
      throw new IllegalArgumentException(
          "lineage progress must match the normalized differentiation ledger");
    }
  }
}
