package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;

/** One exact source-tranche claim made by a finite queried colluvial parcel. */
public record ColluvialSourceClaim(
    Point2 parcelPoint,
    StableId parcelBodyId,
    StableId sourceBodyId,
    int upslopeDistanceBlocks,
    long claimedCapacityFixedUnits,
    long mobilizedFixedUnits,
    long retainedFixedUnits,
    long transportLossFixedUnits,
    long bypassedFixedUnits,
    long depositedFixedUnits) {
  public ColluvialSourceClaim {
    if (parcelPoint == null
        || parcelBodyId == null
        || sourceBodyId == null
        || upslopeDistanceBlocks < 0
        || claimedCapacityFixedUnits <= 0
        || mobilizedFixedUnits < 0
        || retainedFixedUnits < 0
        || transportLossFixedUnits < 0
        || bypassedFixedUnits < 0
        || depositedFixedUnits < 0
        || claimedCapacityFixedUnits != retainedFixedUnits + mobilizedFixedUnits
        || mobilizedFixedUnits
            != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits) {
      throw new IllegalArgumentException("colluvial source claim does not close");
    }
  }
}
