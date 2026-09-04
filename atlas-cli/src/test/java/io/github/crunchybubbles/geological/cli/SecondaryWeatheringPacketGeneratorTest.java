package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecondaryWeatheringPacketGeneratorTest {
  @Test
  void writesDeterministicSourceBudgetedReviewArtifact(@TempDir Path temporaryDirectory)
      throws Exception {
    Path first =
        new SecondaryWeatheringPacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("a"));
    Path second =
        new SecondaryWeatheringPacketGenerator(8_675_309L)
            .generate(temporaryDirectory.resolve("b"));
    String firstJson = Files.readString(first);

    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase6_secondary_weathering_source_budgeted_projection_not_voxel_inventory"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"SUPERGENE_SULFIDE\""));
    assertTrue(firstJson.contains("\"digest\": \"sha256:"));
  }
}
