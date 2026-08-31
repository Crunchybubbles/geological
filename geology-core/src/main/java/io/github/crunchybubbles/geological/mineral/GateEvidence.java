package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

public record GateEvidence(
    String gate, GateStatus status, String explanation, List<StableId> upstreamObjectIds) {
  public GateEvidence {
    if (gate == null
        || gate.isBlank()
        || status == null
        || explanation == null
        || explanation.isBlank()) {
      throw new IllegalArgumentException("gate evidence must be complete");
    }
    upstreamObjectIds = List.copyOf(upstreamObjectIds);
  }
}
