package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.List;

/** Reconstructed point state; this value is never stored per block or chunk. */
public record GeologicalSample(
    Point3 point,
    StableId macroDomainId,
    StableId provinceId,
    StableId rockBodyId,
    Lithology lithology,
    AgeKey formationAge,
    Overprint overprint,
    boolean faultDamageZone,
    List<StableId> depositIds) {
  public GeologicalSample {
    if (point == null
        || macroDomainId == null
        || provinceId == null
        || rockBodyId == null
        || lithology == null
        || formationAge == null
        || overprint == null) {
      throw new IllegalArgumentException("geological sample must be complete");
    }
    depositIds = List.copyOf(depositIds).stream().sorted().toList();
  }
}
