package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarbonatiteKimberlitePacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableCarbonatiteAndKimberliteArtifact() throws Exception {
    Path first =
        new CarbonatiteKimberlitePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("a"));
    Path second =
        new CarbonatiteKimberlitePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase7_carbonatite_peralkaline_ree_kimberlite_diamond_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("CARBONATITE_REE"));
    assertTrue(firstJson.contains("PERALKALINE_REE"));
    assertTrue(firstJson.contains("KIMBERLITE_DIAMOND"));
    assertTrue(firstJson.contains("DIAMOND_BEARING"));
    assertTrue(firstJson.contains("deterministic-carbonatite-peralkaline-kimberlite-fixture"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
