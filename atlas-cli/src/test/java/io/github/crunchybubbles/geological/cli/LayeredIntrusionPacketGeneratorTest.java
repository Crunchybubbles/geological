package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LayeredIntrusionPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableLayeredIntrusionArtifact() throws Exception {
    Path first =
        new LayeredIntrusionPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new LayeredIntrusionPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase7_layered_intrusion_chromite_ni_cu_pge_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("STRATIFORM_CHROMITE"));
    assertTrue(firstJson.contains("NI_CU_PGE_SULFIDE"));
    assertTrue(firstJson.contains("LAYERED_PGE_REEF"));
    assertTrue(firstJson.contains("deterministic-layered-mafic-ultramafic-chamber-fixture"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
