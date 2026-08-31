package io.github.crunchybubbles.geological.trace;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** One ordered edge in a compact source-to-present explanation. */
public record ProvenanceStep(
    int order,
    String process,
    String explanation,
    List<StableId> inputIds,
    List<StableId> outputIds) {
  public ProvenanceStep {
    if (order < 0
        || process == null
        || process.isBlank()
        || explanation == null
        || explanation.isBlank()) {
      throw new IllegalArgumentException("provenance step must be ordered and explained");
    }
    inputIds = List.copyOf(inputIds);
    outputIds = List.copyOf(outputIds);
  }
}
