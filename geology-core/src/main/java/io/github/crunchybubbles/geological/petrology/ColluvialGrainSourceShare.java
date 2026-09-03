package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Optional;

/** Source provenance for one deposited grain-class share. */
public record ColluvialGrainSourceShare(
    SourceRole sourceRole,
    Optional<StableId> sourceBodyId,
    int upslopeDistanceBlocks,
    ColluvialSedimentBudget.GrainMass depositedGrainMass,
    long gravelAndCoarserFractionPpm,
    long sandFractionPpm,
    long finesFractionPpm) {
  public ColluvialGrainSourceShare {
    if (sourceRole == null
        || sourceBodyId == null
        || upslopeDistanceBlocks < 0
        || depositedGrainMass == null
        || depositedGrainMass.totalFixedUnits() <= 0
        || gravelAndCoarserFractionPpm < 0
        || sandFractionPpm < 0
        || finesFractionPpm < 0
        || gravelAndCoarserFractionPpm > MaterialAssemblage.SCALE
        || sandFractionPpm > MaterialAssemblage.SCALE
        || finesFractionPpm > MaterialAssemblage.SCALE
        || sourceRole == SourceRole.WEATHERED_MATRIX
            && (sourceBodyId.isPresent() || upslopeDistanceBlocks != 0)
        || sourceRole == SourceRole.TRANSPORTED_SOURCE && sourceBodyId.isEmpty()) {
      throw new IllegalArgumentException("colluvial grain-source share is invalid");
    }
  }

  public long depositedFixedUnits() {
    return depositedGrainMass.totalFixedUnits();
  }

  public enum SourceRole {
    WEATHERED_MATRIX,
    TRANSPORTED_SOURCE
  }
}
