package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DimensionWorldgenTracePacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableCrossDimensionalTraceArtifact() throws Exception {
    Path first =
        new DimensionWorldgenTracePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("a"));
    Path second =
        new DimensionWorldgenTracePacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase8_cross_dimensional_identity_adapter_trace"));
    assertTrue(firstJson.contains("\"profileCount\": 3"));
    assertTrue(firstJson.contains("\"crossDimensionalIdentityDistinct\": true"));
    assertTrue(firstJson.contains("\"minecraft:the_nether\": \"geological:nether\""));
    assertTrue(firstJson.contains("\"minecraft:the_end\": \"geological:end\""));
    assertTrue(firstJson.contains("\"allSeamsStable\": true"));
    assertTrue(firstJson.contains("\"allTopologyValid\": true"));
    assertTrue(firstJson.contains("minecraft:the_nether"));
    assertTrue(firstJson.contains("minecraft:the_end"));
  }
}
