package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;

/** Deposited grain-spectrum provenance for one routed colluvial source tranche. */
public record ColluvialSourceGrainShare(
    StableId sourceBodyId,
    int upslopeDistanceBlocks,
    long depositedFixedUnits,
    ColluvialSedimentBudget.GrainMass depositedGrainMass) {
  public ColluvialSourceGrainShare {
    if (sourceBodyId == null
        || upslopeDistanceBlocks < 0
        || depositedFixedUnits <= 0
        || depositedGrainMass == null
        || depositedGrainMass.totalFixedUnits() != depositedFixedUnits) {
      throw new IllegalArgumentException("colluvial source grain share is incomplete");
    }
  }

  public SedimentGrainSize depositedGrainSize() {
    return depositedGrainMass.normalizedPpm();
  }
}
