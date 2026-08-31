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
    String residualFluidPotential) {
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
        || residualFluidPotential.isBlank()) {
      throw new IllegalArgumentException("magma lineage state must be complete");
    }
    if (!Double.isFinite(differentiationProgress)
        || differentiationProgress < 0.0
        || differentiationProgress > 1.0) {
      throw new IllegalArgumentException("differentiation progress must lie in [0, 1]");
    }
  }
}
