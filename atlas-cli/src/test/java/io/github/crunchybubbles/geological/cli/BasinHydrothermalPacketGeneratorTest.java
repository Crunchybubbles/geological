package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BasinHydrothermalPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableBasinFamilyArtifact() throws Exception {
    Path first =
        new BasinHydrothermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new BasinHydrothermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase7_basin_hydrothermal_redox_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("MVT_PB_ZN"));
    assertTrue(firstJson.contains("SEDEX_ZN_PB_AG") || firstJson.contains("SEDIMENT_HOSTED_CU"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
