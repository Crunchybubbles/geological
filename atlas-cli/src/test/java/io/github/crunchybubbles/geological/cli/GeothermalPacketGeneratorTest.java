package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeothermalPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableGeothermalArtifact() throws Exception {
    Path first =
        new GeothermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new GeothermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase7_geothermal_heat_fluid_reservoir_proxy_not_assay"));
    assertTrue(firstJson.contains("VOLCANIC_HIGH_ENTHALPY"));
    assertTrue(firstJson.contains("FAULT_CONTROLLED"));
    assertTrue(firstJson.contains("SEDIMENTARY_AQUIFER"));
    assertTrue(firstJson.contains("HOT_DRY_ROCK"));
    assertTrue(firstJson.contains("deterministic-geothermal-heat-reservoir-fixture"));
    assertTrue(firstJson.contains("\"defaultNegativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
