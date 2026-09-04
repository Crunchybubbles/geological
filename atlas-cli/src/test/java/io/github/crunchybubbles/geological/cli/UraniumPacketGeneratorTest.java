package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UraniumPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableUraniumFamilyArtifact() throws Exception {
    Path first = new UraniumPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second = new UraniumPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase7_uranium_redox_groundwater_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("UNCONFORMITY_RELATED"));
    assertTrue(firstJson.contains("SANDSTONE_ROLL_FRONT"));
    assertTrue(firstJson.contains("OLD_U_FERTILE_BASEMENT"));
    assertTrue(firstJson.contains("OXIDIZED_URANIUM_GROUNDWATER_PROXY"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
  }
}
