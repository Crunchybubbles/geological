package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GreisenPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableResidualFluidGreisenArtifact() throws Exception {
    Path first = new GreisenPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second = new GreisenPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase7_greisen_residual_fluid_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("RESIDUAL_FELSIC_FLUID_PROXY"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"FORMED\""));
  }
}
