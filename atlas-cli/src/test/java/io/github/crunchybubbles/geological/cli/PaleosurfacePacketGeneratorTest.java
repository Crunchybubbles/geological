package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaleosurfacePacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableStructuralRefinementArtifact() throws Exception {
    Path first =
        new PaleosurfacePacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new PaleosurfacePacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    String secondJson = Files.readString(second);
    assertEquals(firstJson, secondJson);
    assertTrue(firstJson.contains("phase6_paleosurface_structural_refinement_not_ore_inventory"));
    assertTrue(firstJson.contains("structural_refinement_no_ore_inventory"));
    assertTrue(firstJson.contains("EXPOSED_RESIDUAL_REGOLITH:FORMED"));
    assertTrue(firstJson.contains("BURIED_PALEOSURFACE:BARREN_SYSTEM"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
