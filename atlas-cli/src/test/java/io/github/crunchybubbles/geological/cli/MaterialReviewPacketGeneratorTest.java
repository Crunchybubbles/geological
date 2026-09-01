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
    assertTrue(firstJson.contains("\"modalVariationAxes\""));
    assertTrue(firstJson.contains("\"loadingsPpm\""));
    assertTrue(firstJson.contains("\"solidSolutionCount\""));
    assertTrue(firstJson.contains("geological:solid_solution/plagioclase"));
    assertTrue(firstJson.contains("geological:solid_solution/olivine"));
    assertTrue(firstJson.contains("\"komatiitic-ultramafic-catalog\""));
    assertTrue(firstJson.contains("\"basaltic-catalog\""));
    assertTrue(firstJson.contains("\"gabbroic-catalog\""));
    assertTrue(firstJson.contains("geological:mineral/dolomite"));
    assertTrue(firstJson.contains("\"siltstone-catalog\""));
    assertTrue(firstJson.contains("\"limestone-catalog\""));
    assertTrue(firstJson.contains("\"dolostone-catalog\""));
    assertTrue(firstJson.contains("\"chert-catalog\""));
    assertTrue(firstJson.contains("\"sedimentaryState\""));
    assertTrue(firstJson.contains("\"dolomitized_carbonate_platform\""));
    assertTrue(firstJson.contains("\"silica_precipitation_and_recrystallization\""));
    assertTrue(firstJson.contains("\"magmaLineage\""));
    assertTrue(firstJson.contains("\"endmemberMoleFractionsPpm\""));
    assertTrue(firstJson.contains("\"idealFormulaAtoms\""));
    assertTrue(firstJson.contains("\"protolithFamilies\""));
    assertTrue(firstJson.contains("\"geneticFamily\""));
    assertTrue(firstJson.contains("\"basin-sandstone-phyllic\""));
    assertTrue(firstJson.contains("\"vms-massive-sulfide-gossan\""));
    assertTrue(firstJson.contains("\"ligandCapacities\""));
  }
}
