package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetherResourcePacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableNetherMaterialAndResourceArtifact() throws Exception {
    Path first =
        new NetherResourcePacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new NetherResourcePacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase8_nether_material_history_resources_not_earth_geology"));
    assertTrue(firstJson.contains("NETHER_QUARTZ"));
    assertTrue(firstJson.contains("NETHER_GOLD"));
    assertTrue(firstJson.contains("GLOWSTONE"));
    assertTrue(firstJson.contains("ANCIENT_DEBRIS"));
    assertTrue(firstJson.contains("PYROCLASTIC_PACKAGE"));
    assertTrue(firstJson.contains("LAYERED_MAFIC_INTRUSION"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"BARREN_SYSTEM\""));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
