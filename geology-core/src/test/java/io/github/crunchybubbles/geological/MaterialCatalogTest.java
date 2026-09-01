package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogAuthoringException;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogJsonLoader;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MineralAssemblage;
import io.github.crunchybubbles.geological.petrology.MineralDefinition;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.petrology.SolidSolutionState;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MaterialCatalogTest {
  @Test
  void packagedCatalogCoversEveryImplementedMaterialAndClosesChemistry() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();

    assertEquals(31, catalog.minerals().size());
    assertEquals(6, catalog.solidSolutions().size());
    assertEquals(19, catalog.rocks().size());
    assertEquals(Lithology.values().length, catalog.rocks().size());
    assertEquals(Overprint.values().length, catalog.alterations().size());
    assertEquals(
        "sha256:6ae5fa63d8d7e8d6a1df02c87bb628f634ae592fbf34707c760065965893f3b1",
        catalog.digest());

    for (MineralDefinition mineral : catalog.minerals()) {
      assertEquals(
          1.0,
          mineral.elementMassFractions().values().stream().mapToDouble(Double::doubleValue).sum(),
          1.0e-12,
          mineral.id());
    }
    catalog
        .rocks()
        .forEach(
            rock -> {
              assertTrue(
                  rock.modalSpreadFraction() > 0.0 && rock.modalSpreadFraction() <= 0.5, rock.id());
              assertTrue(!rock.modalVariationAxes().isEmpty(), rock.id());
              rock.modalVariationAxes()
                  .forEach(
                      axis ->
                          assertEquals(
                              0L,
                              axis.loadingsPpm().values().stream().mapToLong(Long::longValue).sum(),
                              rock.id() + "/" + axis.id()));
              assertTrue(rock.porosityDistribution().contains(rock.porosityFraction()), rock.id());
              assertTrue(
                  rock.permeabilityDistribution().contains(rock.permeabilityIndex()), rock.id());
              assertTrue(
                  rock.erodibilityDistribution().contains(rock.erodibilityIndex()), rock.id());
              assertEquals(
                  MineralAssemblage.SCALE,
                  rock.primaryAssemblage().modesPpm().values().stream()
                      .mapToLong(Long::longValue)
                      .sum(),
                  rock.id());
              assertEquals(
                  MineralAssemblage.SCALE,
                  catalog.composition(rock.primaryAssemblage()).elementMassPpm().values().stream()
                      .mapToLong(Long::longValue)
                      .sum(),
                  rock.id());
            });
    catalog
        .alterations()
        .forEach(
            alteration -> {
              boolean requiresFluid =
                  alteration.processClass() == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
                      || alteration.processClass() == MaterialProcessClass.WEATHERING;
              assertEquals(requiresFluid, alteration.fluidState().isPresent());
              if (alteration.replacementPpm() > 0) {
                assertTrue(alteration.targetRecipes().size() >= 3, alteration.overprint().name());
                assertEquals(
                    alteration.targetRecipes().size(),
                    alteration.targetRecipes().stream()
                        .map(recipe -> recipe.targetAssemblage())
                        .distinct()
                        .count(),
                    alteration.overprint().name());
              }
              for (GeneticFamily family : GeneticFamily.values()) {
                assertEquals(
                    alteration.replacementPpm() > 0,
                    alteration.targetAssemblage(family) != null,
                    alteration.overprint() + "/" + family);
              }
            });
    assertFalse(
        catalog
            .requireAlteration(Overprint.OXIDIZED_GOSSAN)
            .targetAssemblage(GeneticFamily.HYDROTHERMAL)
            .modesPpm()
            .containsKey("geological:mineral/native_gold"));
    assertEquals(
        1L,
        catalog
            .requireAlteration(Overprint.OXIDIZED_GOSSAN)
            .targetAssemblage(GeneticFamily.SURFICIAL)
            .modesPpm()
            .get("geological:mineral/native_gold"));

    var granodiorite = catalog.requireRock(Lithology.GRANODIORITE_PULSE);
    SolidSolutionState plagioclase =
        catalog.solidSolutionStates(granodiorite.primaryAssemblage()).stream()
            .filter(state -> state.definitionId().equals("geological:solid_solution/plagioclase"))
            .findFirst()
            .orElseThrow();
    assertEquals(
        granodiorite.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            + granodiorite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite"),
        plagioclase.phaseModePpm());
    assertEquals(
        MineralAssemblage.SCALE,
        plagioclase.endmemberVolumeFractionsPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertEquals(
        MineralAssemblage.SCALE,
        plagioclase.endmemberMoleFractionsPpm().values().stream().mapToLong(Long::longValue).sum());
    assertEquals(
        MineralAssemblage.SCALE,
        plagioclase.bulkComposition().elementMassPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertEquals(
        1L,
        catalog
            .requireAlteration(Overprint.WEATHERED_REGOLITH)
            .targetAssemblage(GeneticFamily.SURFICIAL)
            .modesPpm()
            .get("geological:mineral/native_gold"));
  }

  @Test
  void maficUltramaficSliceHasDistinctTexturesAndCompositionalPhases() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var ultramafic = catalog.requireRock(Lithology.KOMATIITIC_ULTRAMAFIC);
    var basalt = catalog.requireRock(Lithology.BASALTIC);
    var gabbro = catalog.requireRock(Lithology.GABBROIC);

    assertEquals(RockTexture.ULTRAMAFIC_CRYSTALLINE, ultramafic.texture());
    assertEquals(RockTexture.APHANITIC_CRYSTALLINE, basalt.texture());
    assertEquals(RockTexture.PHANERITIC_CRYSTALLINE, gabbro.texture());
    for (var rock : java.util.List.of(ultramafic, basalt, gabbro)) {
      assertEquals(GeneticFamily.IGNEOUS, rock.geneticFamily());
      var states = catalog.solidSolutionStates(rock.primaryAssemblage());
      assertTrue(
          states.stream()
              .anyMatch(state -> state.definitionId().equals("geological:solid_solution/olivine")));
      assertTrue(
          states.stream()
              .anyMatch(
                  state -> state.definitionId().equals("geological:solid_solution/orthopyroxene")));
    }

    SolidSolutionState olivine =
        catalog.solidSolutionStates(ultramafic.primaryAssemblage()).stream()
            .filter(state -> state.definitionId().equals("geological:solid_solution/olivine"))
            .findFirst()
            .orElseThrow();
    assertEquals(600_000L, olivine.phaseModePpm());
    assertEquals(4.0, olivine.idealFormulaAtoms().get(ChemicalElement.O));
    assertTrue(
        olivine.endmemberMoleFractionsPpm().get("geological:mineral/forsterite")
            > olivine.endmemberMoleFractionsPpm().get("geological:mineral/fayalite"));
  }

  @Test
  void sedimentarySliceSeparatesGrainSizeCarbonateChemistryAndSilica() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var siltstone = catalog.requireRock(Lithology.SILTSTONE);
    var limestone = catalog.requireRock(Lithology.LIMESTONE);
    var dolostone = catalog.requireRock(Lithology.DOLOSTONE);
    var chert = catalog.requireRock(Lithology.CHERT);

    assertEquals(RockTexture.CLASTIC_SILT, siltstone.texture());
    assertEquals(RockTexture.BEDDED_CARBONATE, limestone.texture());
    assertEquals(RockTexture.BEDDED_CARBONATE, dolostone.texture());
    assertEquals(RockTexture.MICROCRYSTALLINE_SILICA, chert.texture());
    for (var rock : java.util.List.of(siltstone, limestone, dolostone, chert)) {
      assertEquals(GeneticFamily.SEDIMENTARY, rock.geneticFamily());
    }

    MineralDefinition dolomite = catalog.requireMineral("geological:mineral/dolomite");
    assertEquals(1.0, dolomite.formula().get(ChemicalElement.CA).doubleValue());
    assertEquals(1.0, dolomite.formula().get(ChemicalElement.MG).doubleValue());
    assertEquals(2.0, dolomite.formula().get(ChemicalElement.C).doubleValue());
    assertEquals(6.0, dolomite.formula().get(ChemicalElement.O).doubleValue());
    assertTrue(
        catalog.composition(dolostone.primaryAssemblage()).elementMassPpm().get(ChemicalElement.MG)
            > catalog
                .composition(limestone.primaryAssemblage())
                .elementMassPpm()
                .get(ChemicalElement.MG));
    assertTrue(
        limestone.primaryAssemblage().modesPpm().get("geological:mineral/calcite")
            > dolostone.primaryAssemblage().modesPpm().get("geological:mineral/calcite"));
    assertEquals(920_000L, chert.primaryAssemblage().modesPpm().get("geological:mineral/quartz"));
    assertTrue(
        siltstone.primaryAssemblage().modesPpm().get("geological:mineral/kaolinite")
            > catalog
                .requireRock(Lithology.BASIN_SANDSTONE)
                .primaryAssemblage()
                .modesPpm()
                .get("geological:mineral/kaolinite"));
  }

  @Test
  void evaporiteSliceSeparatesHydratedSulfateFromLateChlorideSalts() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var sulfate = catalog.requireRock(Lithology.GYPSUM_ANHYDRITE_EVAPORITE);
    var chloride = catalog.requireRock(Lithology.HALITE_POTASH_EVAPORITE);

    assertEquals(ChemicalElement.CL, ChemicalElement.fromSymbol("Cl"));
    assertEquals(RockTexture.BEDDED_EVAPORITE, sulfate.texture());
    assertEquals(RockTexture.BEDDED_EVAPORITE, chloride.texture());
    assertEquals(GeneticFamily.SEDIMENTARY, sulfate.geneticFamily());
    assertEquals(GeneticFamily.SEDIMENTARY, chloride.geneticFamily());

    MineralDefinition gypsum = catalog.requireMineral("geological:mineral/gypsum");
    MineralDefinition anhydrite = catalog.requireMineral("geological:mineral/anhydrite");
    MineralDefinition halite = catalog.requireMineral("geological:mineral/halite");
    MineralDefinition sylvite = catalog.requireMineral("geological:mineral/sylvite");
    assertEquals(4.0, gypsum.formula().get(ChemicalElement.H).doubleValue());
    assertEquals(6.0, gypsum.formula().get(ChemicalElement.O).doubleValue());
    assertEquals(4.0, anhydrite.formula().get(ChemicalElement.O).doubleValue());
    assertEquals(1.0, halite.formula().get(ChemicalElement.NA).doubleValue());
    assertEquals(1.0, halite.formula().get(ChemicalElement.CL).doubleValue());
    assertEquals(1.0, sylvite.formula().get(ChemicalElement.K).doubleValue());
    assertEquals(1.0, sylvite.formula().get(ChemicalElement.CL).doubleValue());

    assertEquals(550_000L, sulfate.primaryAssemblage().modesPpm().get("geological:mineral/gypsum"));
    assertEquals(
        760_000L, chloride.primaryAssemblage().modesPpm().get("geological:mineral/halite"));
    assertEquals(
        150_000L, chloride.primaryAssemblage().modesPpm().get("geological:mineral/sylvite"));
    assertTrue(
        catalog.composition(chloride.primaryAssemblage()).elementMassPpm().get(ChemicalElement.CL)
            > catalog
                .composition(sulfate.primaryAssemblage())
                .elementMassPpm()
                .get(ChemicalElement.CL));
  }

  @Test
  void canonicalCatalogIgnoresProtolithFamilyAuthoringOrder() throws Exception {
    String authored = packagedCatalogJson();
    String reordered =
        authored.replace("\"IGNEOUS\", \"METAMORPHIC\"", "\"METAMORPHIC\", \"IGNEOUS\"");
    reordered =
        reordered.replace(
            "\"geological:mineral/albite\", \"geological:mineral/anorthite\"",
            "\"geological:mineral/anorthite\", \"geological:mineral/albite\"");
    reordered =
        reordered.replace(
            "\"geological:mineral/forsterite\", \"geological:mineral/fayalite\"",
            "\"geological:mineral/fayalite\", \"geological:mineral/forsterite\"");
    reordered =
        reordered.replace(
            "\"geological:mineral/enstatite\", \"geological:mineral/ferrosilite\"",
            "\"geological:mineral/ferrosilite\", \"geological:mineral/enstatite\"");
    assertTrue(reordered.contains("\"METAMORPHIC\", \"IGNEOUS\""));

    MaterialCatalogSnapshot loaded =
        new MaterialCatalogJsonLoader()
            .load(
                new ByteArrayInputStream(reordered.getBytes(StandardCharsets.UTF_8)),
                "reordered.json");

    assertEquals(Phase2World.materialCatalog().canonicalJson(), loaded.canonicalJson());
    assertEquals(Phase2World.materialCatalog().digest(), loaded.digest());
  }

  @Test
  void strictCatalogBoundaryRejectsInvalidDistributionsAndIncompleteRecipeCoverage()
      throws Exception {
    String authored = packagedCatalogJson();
    String invalidDistribution =
        authored.replace(
            "{\"minimum\": 0.14, \"mode\": 0.18, \"maximum\": 0.22}",
            "{\"minimum\": 0.14, \"mode\": 0.25, \"maximum\": 0.22}");
    MaterialCatalogAuthoringException distributionFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            invalidDistribution.getBytes(StandardCharsets.UTF_8)),
                        "invalid-distribution.json"));
    assertTrue(distributionFailure.getMessage().contains("minimum <= mode <= maximum"));

    String incompleteRecipes = authored.replace("\"IGNEOUS\", \"METAMORPHIC\"", "\"IGNEOUS\"");
    MaterialCatalogAuthoringException recipeFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            incompleteRecipes.getBytes(StandardCharsets.UTF_8)),
                        "incomplete-recipes.json"));
    assertTrue(recipeFailure.getMessage().contains("cover every protolith family"));

    String invalidLigand =
        authored.replace(
            "{\"chloride\": 3, \"reduced_sulfur\": 2, \"carbonate\": 1, \"fluorine_boron\": 2}",
            "{\"chloride\": 4, \"reduced_sulfur\": 2, \"carbonate\": 1, \"fluorine_boron\": 2}");
    MaterialCatalogAuthoringException ligandFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(invalidLigand.getBytes(StandardCharsets.UTF_8)),
                        "invalid-ligand.json"));
    assertTrue(ligandFailure.getMessage().contains("ligand capacity must lie in [0, 3]"));

    String unbalancedAxis =
        authored.replace(
            "{\"geological:mineral/quartz\": 30000, \"geological:mineral/orthoclase\": -30000}",
            "{\"geological:mineral/quartz\": 30000, \"geological:mineral/orthoclase\": -29999}");
    MaterialCatalogAuthoringException axisFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unbalancedAxis.getBytes(StandardCharsets.UTF_8)),
                        "unbalanced-axis.json"));
    assertTrue(axisFailure.getMessage().contains("axis loadings must sum to zero"));

    String unknownEndmember =
        authored.replace(
            "[\"geological:mineral/albite\", \"geological:mineral/anorthite\"]",
            "[\"geological:mineral/unknown\", \"geological:mineral/anorthite\"]");
    MaterialCatalogAuthoringException endmemberFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unknownEndmember.getBytes(StandardCharsets.UTF_8)),
                        "unknown-endmember.json"));
    assertTrue(endmemberFailure.getMessage().contains("references unknown mineral"));

    String overlappingEndmember =
        authored.replace(
            "[\"geological:mineral/diopside\", \"geological:mineral/hedenbergite\"]",
            "[\"geological:mineral/albite\", \"geological:mineral/hedenbergite\"]");
    MaterialCatalogAuthoringException overlapFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            overlappingEndmember.getBytes(StandardCharsets.UTF_8)),
                        "overlapping-endmember.json"));
    assertTrue(overlapFailure.getMessage().contains("belongs to both"));
  }

  @Test
  void strictCatalogBoundaryRejectsUnknownFieldsAndUnclosedModes() {
    String unknown =
        """
        {"authoring_schema":"geological:material_catalog_authoring:v5","evidence":{},
        "minerals":[],"rocks":[],"solid_solutions":[],"overprints":[],"surprise":true}
        """;
    MaterialCatalogAuthoringException unknownFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unknown.getBytes(StandardCharsets.UTF_8)),
                        "unknown.json"));
    assertTrue(unknownFailure.getMessage().contains("$.surprise: unknown field"));

    String unclosed =
        """
        {
          "authoring_schema":"geological:material_catalog_authoring:v5",
          "evidence":{"citation_id":"refs:test","parameter_basis":"test tunable",
            "publication_year":2000,"title":"Test","uri":"https://example.invalid/test"},
          "minerals":[{"density_g_cm3":2.65,"formula":{"Si":1,"O":2},
            "hardness_mohs":7.0,"id":"test:quartz","weathering_resistance":1.0}],
          "solid_solutions":[],
          "rocks":[{"erodibility_distribution":{"minimum":0.05,"mode":0.1,"maximum":0.2},
            "genetic_family":"IGNEOUS","id":"test:rock",
            "lithology":"GRANITIC_GNEISS","mineral_modes_ppm":{"test:quartz":999999},
            "modal_spread_fraction":0.1,
            "modal_variation_axes":[],
            "permeability_distribution":{"minimum":0.05,"mode":0.1,"maximum":0.2},
            "porosity_distribution":{"minimum":0.05,"mode":0.1,"maximum":0.2},
            "texture":"PHANERITIC_CRYSTALLINE"}],
          "overprints":[]
        }
        """;
    MaterialCatalogAuthoringException modeFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unclosed.getBytes(StandardCharsets.UTF_8)),
                        "unclosed.json"));
    assertTrue(modeFailure.getMessage().contains("mineral modes must close"));
  }

  private static String packagedCatalogJson() throws Exception {
    try (InputStream input =
        Phase2World.class.getResourceAsStream("/data/geological/registry/phase2-materials.json")) {
      if (input == null) {
        throw new IllegalStateException("packaged material catalog is missing");
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
