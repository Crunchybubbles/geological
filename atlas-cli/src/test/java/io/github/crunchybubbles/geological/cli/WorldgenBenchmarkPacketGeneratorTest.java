package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldgenBenchmarkPacketGeneratorTest {
  @Test
  void writesInvariantWorldgenObservation(@TempDir Path temporaryDirectory) throws Exception {
    Path report = new WorldgenBenchmarkPacketGenerator(8_675_309L).generate(temporaryDirectory);
    String json = Files.readString(report);

    assertTrue(json.contains("phase4_server_worldgen_engineering_observation_not_microbenchmark"));
    assertTrue(json.contains("\"generationOrderStable\": true"));
    assertTrue(json.contains("\"warmResultStable\": true"));
    assertTrue(json.contains("\"seamStable\": true"));
    assertTrue(json.contains("\"seamColumnsChecked\": 32"));
    assertTrue(json.contains("\"stableSignatureHex\": \""));
  }
}
