package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;

/** Stable handles required by mineral-system evidence and source ledgers. */
public record ProvinceProofIds(
    StableId magmaLineageId,
    StableId vmsSystemId,
    StableId vmsDepositId,
    StableId porphyrySystemId,
    StableId porphyryDepositId,
    StableId placerSystemId,
    StableId placerDepositId,
    StableId rejectedPorphyryCandidateId,
    StableId rejectedVmsCandidateId,
    StableId rejectedPlacerCandidateId,
    StableId upliftId,
    StableId weatheringId) {
  public ProvinceProofIds {
    if (magmaLineageId == null
        || vmsSystemId == null
        || vmsDepositId == null
        || porphyrySystemId == null
        || porphyryDepositId == null
        || placerSystemId == null
        || placerDepositId == null
        || rejectedPorphyryCandidateId == null
        || rejectedVmsCandidateId == null
        || rejectedPlacerCandidateId == null
        || upliftId == null
        || weatheringId == null) {
      throw new IllegalArgumentException("proof IDs must be complete");
    }
  }
}
