package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrogenicGoldPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableMetamorphicFluidOrogenicArtifact() throws Exception {
    Path first =
        new OrogenicGoldPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new OrogenicGoldPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains("phase7_orogenic_gold_metamorphic_fluid_proxy_projection_not_assay"));
    assertTrue(firstJson.contains("METAMORPHIC_AQUEOUS_CARBONIC_PROXY"));
    assertTrue(firstJson.contains("\"negativeProof\""));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"FORMED\""));
  }
}
