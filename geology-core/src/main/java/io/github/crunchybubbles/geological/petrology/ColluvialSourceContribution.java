package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;

/** One bounded upslope sampling tranche contributing to a colluvial parcel. */
public record ColluvialSourceContribution(
    Point2 sourcePoint,
    StableId sourceProvinceId,
    StableId sourceBodyId,
    Lithology sourceLithology,
    Overprint sourceOverprint,
    int upslopeDistanceBlocks,
    long assemblageFractionPpm) {
  public ColluvialSourceContribution {
    if (sourcePoint == null
        || sourceProvinceId == null
        || sourceBodyId == null
        || sourceLithology == null
        || sourceOverprint == null) {
      throw new IllegalArgumentException("colluvial source contribution must be complete");
    }
    if (upslopeDistanceBlocks < 0) {
      throw new IllegalArgumentException("colluvial source distance must be non-negative");
    }
    if (assemblageFractionPpm <= 0 || assemblageFractionPpm >= MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("colluvial source fraction must lie inside (0, scale)");
    }
  }
}
