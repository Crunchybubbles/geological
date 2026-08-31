package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChronicleOrderingTest {
  @Test
  void eventsAreOldestToYoungestAndOnlyReferenceAvailableInputs() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    List<GeologicalEvent> events =
        query.atlas().provinceAt(new Point2(0.0, 0.0)).chronicle().events();
    assertEquals(17, events.size());
    assertEquals(EventType.ESTABLISH_BASEMENT, events.getFirst().type());
    assertEquals(EventType.ERODE_TRANSPORT_DEPOSIT, events.getLast().type());

    Set<StableId> available = new HashSet<>();
    double previousAge = Double.POSITIVE_INFINITY;
    for (GeologicalEvent event : events) {
      assertTrue(event.age().ageMa() <= previousAge);
      assertTrue(available.containsAll(event.inputs()));
      available.addAll(event.outputs());
      previousAge = event.age().ageMa();
    }

    GeologicalEvent fold =
        events.stream().filter(event -> event.type() == EventType.FOLD).findFirst().orElseThrow();
    GeologicalEvent fault =
        events.stream()
            .filter(event -> event.type() == EventType.FAULT_REACTIVATION)
            .findFirst()
            .orElseThrow();
    assertTrue(fault.age().youngerThan(fold.age()));
  }
}
