package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SedimentaryResourcePacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableSedimentaryResourceArtifact() throws Exception {
    Path first =
        new SedimentaryResourcePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("a"));
    Path second =
        new SedimentaryResourcePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase7_phosphorite_manganese_coal_brine_gas_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("PHOSPHORITE"));
    assertTrue(firstJson.contains("SEDIMENTARY_MANGANESE"));
    assertTrue(firstJson.contains("COAL"));
    assertTrue(firstJson.contains("LITHIUM_BRINE"));
    assertTrue(firstJson.contains("POTASH_BORATE_BRINE"));
    assertTrue(firstJson.contains("HELIUM_GAS"));
    assertTrue(firstJson.contains("deterministic-sedimentary-resource-facies-fixture"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
