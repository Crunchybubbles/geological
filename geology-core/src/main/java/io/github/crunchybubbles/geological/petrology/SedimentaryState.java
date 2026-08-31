package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** Source-to-sink and diagenetic summary for one resolved stratigraphic member. */
public record SedimentaryState(
    String faciesClass,
    String grainSizeClass,
    String maturityClass,
    String diagenesisClass,
    List<StableId> sourceBodyIds) {
  public SedimentaryState {
    if (faciesClass == null
        || faciesClass.isBlank()
        || grainSizeClass == null
        || grainSizeClass.isBlank()
        || maturityClass == null
        || maturityClass.isBlank()
        || diagenesisClass == null
        || diagenesisClass.isBlank()) {
      throw new IllegalArgumentException("sedimentary state must be complete");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()) {
      throw new IllegalArgumentException("sedimentary state must name a source body");
    }
  }
}
