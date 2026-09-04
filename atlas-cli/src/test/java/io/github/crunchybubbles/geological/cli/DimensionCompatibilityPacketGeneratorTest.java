package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DimensionCompatibilityPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableCompatibilityAndLoreReview() throws Exception {
    Path first =
        new DimensionCompatibilityPacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("a"));
    Path second =
        new DimensionCompatibilityPacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase8_cross_dimensional_compatibility_lore_review"));
    assertTrue(firstJson.contains("\"allChecksPassed\": true"));
    assertTrue(firstJson.contains("\"failedChecks\": []"));
    assertTrue(firstJson.contains("portal_ratio_is_topology_only_not_shared_geology"));
    assertTrue(firstJson.contains("structures_remain_platform_owned"));
    assertTrue(firstJson.contains("\"expertReviewRequired\": true"));
  }
}
