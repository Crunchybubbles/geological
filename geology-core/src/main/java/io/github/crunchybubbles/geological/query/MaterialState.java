package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import java.util.List;

/** Coordinate-independent geological state compressed into a vertical run. */
public record MaterialState(
    StableId rockBodyId,
    Lithology lithology,
    AgeKey formationAge,
    Overprint overprint,
    boolean faultDamageZone,
    List<StableId> depositIds) {
  public MaterialState {
    if (rockBodyId == null || lithology == null || formationAge == null || overprint == null) {
      throw new IllegalArgumentException("material state fields must be present");
    }
    depositIds = List.copyOf(depositIds).stream().sorted().toList();
  }

  public static MaterialState from(GeologicalSample sample) {
    return new MaterialState(
        sample.rockBodyId(),
        sample.lithology(),
        sample.formationAge(),
        sample.overprint(),
        sample.faultDamageZone(),
        sample.depositIds());
  }
}
