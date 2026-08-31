package io.github.crunchybubbles.geological.spatial;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds3D;

/** Stable finite candidate reference; full descriptors remain owned by their province. */
public record SpatialCandidate(
    StableId id, CandidateKind kind, Bounds3D bounds, AgeKey birthAge, boolean affectsColumnState) {
  public SpatialCandidate {
    if (id == null || kind == null || bounds == null || birthAge == null) {
      throw new IllegalArgumentException("spatial candidate fields must be present");
    }
  }
}
