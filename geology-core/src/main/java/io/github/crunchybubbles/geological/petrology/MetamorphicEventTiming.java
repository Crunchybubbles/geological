package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;

/** Stable event/age pair for a metamorphic history. */
public record MetamorphicEventTiming(StableId eventId, AgeKey age) {
  public MetamorphicEventTiming {
    if (eventId == null || age == null) {
      throw new IllegalArgumentException("metamorphic event timing requires an ID and age");
    }
  }
}
