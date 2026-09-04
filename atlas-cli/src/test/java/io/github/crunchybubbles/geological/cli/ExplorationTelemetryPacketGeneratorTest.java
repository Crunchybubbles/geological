package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExplorationTelemetryPacketGeneratorTest {
  @Test
  void writesDeterministicClueTelemetryArtifact(@TempDir Path temporaryDirectory) throws Exception {
    Path report = new ExplorationTelemetryPacketGenerator(8_675_309L).generate(temporaryDirectory);
    String json = Files.readString(report);

    assertTrue(
        json.contains(
            "phase5_exploration_clue_telemetry_engineering_observation_not_microbenchmark"));
    assertTrue(json.contains("\"cellsVisited\": 81"));
    assertTrue(json.contains("\"travelBurdenPpm\": "));
    assertTrue(json.contains("\"clueSufficiencyPpm\": "));
    assertTrue(json.contains("\"digest\": \"sha256:"));
  }
}
