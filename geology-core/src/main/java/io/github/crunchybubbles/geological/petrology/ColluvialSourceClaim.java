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
    long depositedFixedUnits,
    ColluvialSedimentBudget.GrainMass capacityGrainMass,
    ColluvialSedimentBudget.GrainMass mobilizedGrainMass,
    ColluvialSedimentBudget.GrainMass retainedGrainMass,
    ColluvialSedimentBudget.GrainMass transportLossGrainMass,
    ColluvialSedimentBudget.GrainMass bypassedGrainMass,
    ColluvialSedimentBudget.GrainMass depositedGrainMass) {
  public ColluvialSourceClaim(
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
    this(
        parcelPoint,
        parcelBodyId,
        sourceBodyId,
        upslopeDistanceBlocks,
        claimedCapacityFixedUnits,
        mobilizedFixedUnits,
        retainedFixedUnits,
        transportLossFixedUnits,
        bypassedFixedUnits,
        depositedFixedUnits,
        new ColluvialSedimentBudget.GrainMass(claimedCapacityFixedUnits, 0, 0),
        new ColluvialSedimentBudget.GrainMass(mobilizedFixedUnits, 0, 0),
        new ColluvialSedimentBudget.GrainMass(retainedFixedUnits, 0, 0),
        new ColluvialSedimentBudget.GrainMass(transportLossFixedUnits, 0, 0),
        new ColluvialSedimentBudget.GrainMass(bypassedFixedUnits, 0, 0),
        new ColluvialSedimentBudget.GrainMass(depositedFixedUnits, 0, 0));
  }

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
        || capacityGrainMass == null
        || mobilizedGrainMass == null
        || retainedGrainMass == null
        || transportLossGrainMass == null
        || bypassedGrainMass == null
        || depositedGrainMass == null
        || claimedCapacityFixedUnits != retainedFixedUnits + mobilizedFixedUnits
        || mobilizedFixedUnits != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
        || capacityGrainMass.totalFixedUnits() != claimedCapacityFixedUnits
        || mobilizedGrainMass.totalFixedUnits() != mobilizedFixedUnits
        || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
        || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
        || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
        || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
        || !capacityGrainMass.equals(retainedGrainMass.add(mobilizedGrainMass))
        || !mobilizedGrainMass.equals(
            transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
      throw new IllegalArgumentException("colluvial source claim does not close");
    }
  }
}
