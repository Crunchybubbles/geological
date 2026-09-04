package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkarnPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableCarbonateContactSkarnArtifact() throws Exception {
    Path first = new SkarnPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second = new SkarnPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase7_skarn_carbonate_contact_fixture_projection_not_assay"));
    assertTrue(firstJson.contains("deterministic-carbonate-contact-fixture"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"defaultActualHostGate\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"FORMED\""));
  }
}
