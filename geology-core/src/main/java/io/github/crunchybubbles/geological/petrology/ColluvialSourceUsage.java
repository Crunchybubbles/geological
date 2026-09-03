package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;

/** Exact per-source aggregation of all routed colluvial tranches in one parcel. */
public record ColluvialSourceUsage(
    StableId sourceBodyId,
    int trancheCount,
    long claimedCapacityFixedUnits,
    long mobilizedFixedUnits,
    long retainedFixedUnits,
    long transportLossFixedUnits,
    long bypassedFixedUnits,
    long depositedFixedUnits) {
  public ColluvialSourceUsage {
    if (sourceBodyId == null
        || trancheCount <= 0
        || claimedCapacityFixedUnits <= 0
        || mobilizedFixedUnits < 0
        || retainedFixedUnits < 0
        || transportLossFixedUnits < 0
        || bypassedFixedUnits < 0
        || depositedFixedUnits < 0
        || claimedCapacityFixedUnits != retainedFixedUnits + mobilizedFixedUnits
        || mobilizedFixedUnits
            != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits) {
      throw new IllegalArgumentException("colluvial source usage does not close");
    }
  }
}
