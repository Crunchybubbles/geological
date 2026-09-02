package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;

/** Exact proof-level mixture of local parent material and generic weathered matrix. */
public record ColluvialSourceMix(
    StableId sourceBodyId,
    Lithology sourceLithology,
    Overprint sourceOverprint,
    long sourceAssemblageFractionPpm,
    long weatheredMatrixFractionPpm) {
  public ColluvialSourceMix {
    if (sourceBodyId == null || sourceLithology == null || sourceOverprint == null) {
      throw new IllegalArgumentException("colluvial source identity must be complete");
    }
    if (sourceAssemblageFractionPpm <= 0
        || sourceAssemblageFractionPpm >= MaterialAssemblage.SCALE
        || weatheredMatrixFractionPpm <= 0
        || weatheredMatrixFractionPpm >= MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("colluvial mixture fractions must lie inside (0, scale)");
    }
    if (sourceAssemblageFractionPpm + weatheredMatrixFractionPpm != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "colluvial mixture fractions must close to " + MaterialAssemblage.SCALE);
    }
  }
}
