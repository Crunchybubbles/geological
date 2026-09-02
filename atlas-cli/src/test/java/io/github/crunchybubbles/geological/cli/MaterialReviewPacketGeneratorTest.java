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
    assertTrue(firstJson.contains("\"responseTexture\""));
    assertTrue(firstJson.contains("\"primaryTexture\""));
    assertTrue(firstJson.contains("\"resolvedTexture\""));
    assertTrue(firstJson.contains("\"HORNFELSIC\""));
    assertTrue(firstJson.contains("\"porosityDistribution\""));
    assertTrue(firstJson.contains("\"modalVariationAxes\""));
    assertTrue(firstJson.contains("\"loadingsPpm\""));
    assertTrue(firstJson.contains("\"solidSolutionCount\""));
    assertTrue(firstJson.contains("\"nonCrystallineConstituentCount\""));
    assertTrue(firstJson.contains("\"constituentCount\""));
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
    assertTrue(firstJson.contains("\"banded-iron-formation-catalog\""));
    assertTrue(firstJson.contains("geological:mineral/siderite"));
    assertTrue(firstJson.contains("\"gypsum-anhydrite-evaporite-catalog\""));
    assertTrue(firstJson.contains("\"halite-potash-evaporite-catalog\""));
    assertTrue(firstJson.contains("\"coal-catalog\""));
    assertTrue(firstJson.contains("geological:constituent/coal_organic_matter"));
    assertTrue(firstJson.contains("\"ORGANIC_MATTER\""));
    assertTrue(firstJson.contains("\"N\""));
    assertTrue(firstJson.contains("geological:mineral/gypsum"));
    assertTrue(firstJson.contains("geological:mineral/halite"));
    assertTrue(firstJson.contains("\"Cl\""));
    assertTrue(firstJson.contains("\"sedimentaryState\""));
    assertTrue(firstJson.contains("\"dolomitized_carbonate_platform\""));
    assertTrue(firstJson.contains("\"silica_precipitation_and_recrystallization\""));
    assertTrue(firstJson.contains("\"ancient_iron_silica_precipitation_basin\""));
    assertTrue(firstJson.contains("\"chemical_precipitate_redox_controlled\""));
    assertTrue(firstJson.contains("\"buried_peat_mire\""));
    assertTrue(firstJson.contains("\"peat_derived_rank_unresolved\""));
    assertTrue(firstJson.contains("\"slate-phyllite-catalog\""));
    assertTrue(firstJson.contains("\"mica-schist-catalog\""));
    assertTrue(firstJson.contains("\"greenschist-catalog\""));
    assertTrue(firstJson.contains("\"amphibolite-catalog\""));
    assertTrue(firstJson.contains("\"granulite-catalog\""));
    assertTrue(firstJson.contains("\"quartzite-catalog\""));
    assertTrue(firstJson.contains("\"marble-catalog\""));
    assertTrue(firstJson.contains("\"serpentinite-catalog\""));
    assertTrue(firstJson.contains("geological:mineral/lizardite"));
    assertTrue(firstJson.contains("geological:mineral/chrysotile"));
    assertTrue(firstJson.contains("geological:mineral/antigorite"));
    assertTrue(firstJson.contains("geological:mineral/brucite"));
    assertTrue(firstJson.contains("\"SERPENTINIZED_MESH\""));
    assertTrue(firstJson.contains("geological:mineral/magnesiohornblende"));
    assertTrue(firstJson.contains("geological:mineral/ferrohornblende"));
    assertTrue(firstJson.contains("geological:solid_solution/hornblende"));
    assertTrue(firstJson.contains("geological:mineral/graphite"));
    assertTrue(firstJson.contains("geological:mineral/almandine"));
    assertTrue(firstJson.contains("\"primaryMetamorphism\""));
    assertTrue(firstJson.contains("\"protolithRockId\""));
    assertTrue(firstJson.contains("\"grade\""));
    assertTrue(firstJson.contains("\"restricted_evaporite_basin_center\""));
    assertTrue(firstJson.contains("\"salt_recrystallization_dissolution_and_halokinesis\""));
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
