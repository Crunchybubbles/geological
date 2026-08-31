package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.query.Phase2World;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialReviewPacketGeneratorTest {
  @Test
  void generatedReviewIsDeterministicAndCarriesPhase2ProofState(@TempDir Path temporaryDirectory)
      throws Exception {
    Path first =
        new MaterialReviewPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new MaterialReviewPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains(Phase2World.SCIENTIFIC_DIGEST));
    assertTrue(firstJson.contains("\"materialProcess\""));
    assertTrue(firstJson.contains("\"elementReservoirLedgers\""));
    assertTrue(firstJson.contains("\"surfacePlacerContext\""));
    assertTrue(firstJson.contains("\"normalizedExchangeMagnitudePpm\""));
    assertTrue(firstJson.contains("\"texture\""));
    assertTrue(firstJson.contains("\"porosityDistribution\""));
    assertTrue(firstJson.contains("\"protolithFamilies\""));
  }
}
