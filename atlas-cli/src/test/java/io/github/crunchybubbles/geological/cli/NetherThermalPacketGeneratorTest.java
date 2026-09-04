package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetherThermalPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableNetherThermalArtifact() throws Exception {
    Path first =
        new NetherThermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new NetherThermalPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase8_nether_thermal_cavern_roof_floor_lava_projection_not_earth_geology"));
    assertTrue(firstJson.contains("NETHERRACK_VOLCANIC_WASTE"));
    assertTrue(firstJson.contains("BASALT_DELTA_COMPLEX"));
    assertTrue(firstJson.contains("SOUL_ASH_VALLEY"));
    assertTrue(firstJson.contains("VOLATILE_VENT_FIELD"));
    assertTrue(firstJson.contains("\"lavaColumns\":"));
    assertTrue(firstJson.contains("\"bridgeColumns\":"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
