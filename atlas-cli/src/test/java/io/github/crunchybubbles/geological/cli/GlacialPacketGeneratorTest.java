package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GlacialPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableOptInGlacialArtifact() throws Exception {
    Path first = new GlacialPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second = new GlacialPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase6_glacial_transport_opt_in_source_budgeted_prototype"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"defaultNoIceGate\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
