package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndFragmentPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableEndParentFragmentArtifact() throws Exception {
    Path first =
        new EndFragmentPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new EndFragmentPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase8_end_parent_fragment_impact_regolith_projection_not_earth_geology"));
    assertTrue(firstJson.contains("CENTRAL_PROGRESSION"));
    assertTrue(firstJson.contains("GATEWAY_RING"));
    assertTrue(firstJson.contains("OUTER_ISLAND"));
    assertTrue(firstJson.contains("SILICATE_DIFFERENTIATED"));
    assertTrue(firstJson.contains("METAL_SEPARATED"));
    assertTrue(firstJson.contains("POLYMICT_BRECCIA"));
    assertTrue(firstJson.contains("REGOLITH_REACCUMULATION"));
    assertTrue(firstJson.contains("\"parentBudgetClosed\": true"));
    assertTrue(firstJson.contains("\"regolithBudgetClosed\": true"));
    assertTrue(firstJson.contains("\"voidSamples\":"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
