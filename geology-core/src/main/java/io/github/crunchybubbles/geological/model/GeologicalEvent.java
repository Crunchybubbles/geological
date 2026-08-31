package io.github.crunchybubbles.geological.model;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** Consequential event in the compact province chronicle. */
public record GeologicalEvent(
    StableId id,
    EventType type,
    AgeKey age,
    List<StableId> inputs,
    List<StableId> outputs,
    String description) {
  public GeologicalEvent {
    if (id == null || type == null || age == null) {
      throw new IllegalArgumentException("event identity, type, and age are required");
    }
    inputs = List.copyOf(inputs);
    outputs = List.copyOf(outputs);
    if (outputs.isEmpty()) {
      throw new IllegalArgumentException("events must create or annotate at least one output");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("event description must be present");
    }
  }
}
