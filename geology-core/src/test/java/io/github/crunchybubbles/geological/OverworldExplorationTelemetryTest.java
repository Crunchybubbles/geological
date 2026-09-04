package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.ExplorationTelemetryReport;
import io.github.crunchybubbles.geological.worldgen.ExplorationTelemetryRequest;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationTelemetry;
import org.junit.jupiter.api.Test;

class OverworldExplorationTelemetryTest {
  @Test
  void telemetryIsDeterministicBoundedAndHypothesisAware() {
    ExplorationTelemetryRequest request = new ExplorationTelemetryRequest(-176, 272, 32, 16);

    ExplorationTelemetryReport first = OverworldExplorationTelemetry.run(8_675_309L, request);
    ExplorationTelemetryReport second = OverworldExplorationTelemetry.run(8_675_309L, request);

    assertEquals(first, second);
    assertEquals(25, first.cellsVisited());
    assertTrue(first.observationCells() >= 0 && first.observationCells() <= 25);
    assertTrue(first.handSampleCells() >= 0 && first.handSampleCells() <= 25);
    assertTrue(first.sampledCells() >= 0 && first.sampledCells() <= 25);
    assertTrue(first.anomalousCells() <= first.anomalyCells());
    assertTrue(first.hypothesisCells() <= first.cellsVisited());
    assertTrue(first.travelBurdenPpm() >= 0 && first.travelBurdenPpm() <= 1_000_000);
    assertTrue(first.clueSufficiencyPpm() >= 0 && first.clueSufficiencyPpm() <= 1_000_000);
    assertTrue(first.summary().contains("exploration-telemetry"));
  }

  @Test
  void telemetryWindowRejectsUnboundedSurveys() {
    assertThrows(
        IllegalArgumentException.class, () -> new ExplorationTelemetryRequest(0, 0, 257, 16));
    assertThrows(
        IllegalArgumentException.class, () -> new ExplorationTelemetryRequest(0, 0, 256, 1));
  }
}
