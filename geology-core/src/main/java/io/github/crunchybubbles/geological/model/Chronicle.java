package io.github.crunchybubbles.geological.model;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validated oldest-to-youngest event sequence for one province proof. */
public final class Chronicle {
  private final List<GeologicalEvent> events;

  public Chronicle(List<GeologicalEvent> events) {
    this.events = List.copyOf(events);
    validate(this.events);
  }

  public List<GeologicalEvent> events() {
    return events;
  }

  public GeologicalEvent event(EventType type) {
    return events.stream()
        .filter(event -> event.type() == type)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("chronicle does not contain " + type));
  }

  private static void validate(List<GeologicalEvent> events) {
    if (events.isEmpty()) {
      throw new IllegalArgumentException("chronicle must not be empty");
    }
    Set<StableId> eventIds = new HashSet<>();
    Set<StableId> availableObjects = new HashSet<>();
    AgeKey previous = null;
    for (GeologicalEvent event : events) {
      if (!eventIds.add(event.id())) {
        throw new IllegalArgumentException("duplicate event ID " + event.id());
      }
      if (previous != null && previous.compareTo(event.age()) > 0) {
        throw new IllegalArgumentException("chronicle is not ordered oldest to youngest");
      }
      for (StableId input : event.inputs()) {
        if (!availableObjects.contains(input)) {
          throw new IllegalArgumentException(
              "event " + event.id() + " refers to unavailable or younger input " + input);
        }
      }
      for (StableId output : event.outputs()) {
        if (!availableObjects.add(output)) {
          throw new IllegalArgumentException("duplicate chronicle output ID " + output);
        }
      }
      previous = event.age();
    }
  }
}
