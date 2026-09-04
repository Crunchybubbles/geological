package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecondaryPlacerPacketGeneratorTest {
  @Test
  void writesDeterministicSourceBudgetedReviewArtifact(@TempDir Path temporaryDirectory)
      throws Exception {
    Path first =
        new SecondaryPlacerPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new SecondaryPlacerPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));
    String firstJson = Files.readString(first);

    assertEquals(firstJson, Files.readString(second));
    assertTrue(
        firstJson.contains(
            "phase6_secondary_placer_source_budgeted_projection_not_voxel_inventory"));
    assertTrue(firstJson.contains("\"budgetClosed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
    assertTrue(firstJson.contains("\"CASSITERITE\""));
    assertTrue(firstJson.contains("\"HEAVY_MINERAL_SAND\""));
    assertTrue(firstJson.contains("\"diamondFormedProfiles\": 0"));
    assertTrue(firstJson.contains("\"digest\": \"sha256:"));
  }
}
