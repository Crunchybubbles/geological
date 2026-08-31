package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogAuthoringException;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogJsonLoader;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MineralAssemblage;
import io.github.crunchybubbles.geological.petrology.MineralDefinition;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MaterialCatalogTest {
  @Test
  void packagedCatalogCoversEveryImplementedMaterialAndClosesChemistry() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();

    assertEquals(22, catalog.minerals().size());
    assertEquals(Lithology.values().length, catalog.rocks().size());
    assertEquals(Overprint.values().length, catalog.alterations().size());
    assertEquals(
        "sha256:66ae96285f1fc9d71a0473550047ffde0f98d11d112442929462b4c901b7c5c7",
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
              for (GeneticFamily family : GeneticFamily.values()) {
                assertEquals(
                    alteration.replacementPpm() > 0,
                    alteration.targetAssemblage(family) != null,
                    alteration.overprint() + "/" + family);
              }
            });
  }

  @Test
  void canonicalCatalogIgnoresProtolithFamilyAuthoringOrder() throws Exception {
    String authored = packagedCatalogJson();
    String reordered =
        authored.replace(
            "\"IGNEOUS\", \"SEDIMENTARY\", \"METAMORPHIC\", \"HYDROTHERMAL\", \"SURFICIAL\"",
            "\"SURFICIAL\", \"HYDROTHERMAL\", \"METAMORPHIC\", \"SEDIMENTARY\", \"IGNEOUS\"");
    assertTrue(reordered.contains("\"SURFICIAL\", \"HYDROTHERMAL\""));

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

    String incompleteRecipes =
        authored.replace(
            "\"IGNEOUS\", \"SEDIMENTARY\", \"METAMORPHIC\", \"HYDROTHERMAL\", \"SURFICIAL\"",
            "\"IGNEOUS\"");
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
  }

  @Test
  void strictCatalogBoundaryRejectsUnknownFieldsAndUnclosedModes() {
    String unknown =
        """
        {"authoring_schema":"geological:material_catalog_authoring:v3","evidence":{},
        "minerals":[],"rocks":[],"overprints":[],"surprise":true}
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
          "authoring_schema":"geological:material_catalog_authoring:v3",
          "evidence":{"citation_id":"refs:test","parameter_basis":"test tunable",
            "publication_year":2000,"title":"Test","uri":"https://example.invalid/test"},
          "minerals":[{"density_g_cm3":2.65,"formula":{"Si":1,"O":2},
            "hardness_mohs":7.0,"id":"test:quartz","weathering_resistance":1.0}],
          "rocks":[{"erodibility_distribution":{"minimum":0.05,"mode":0.1,"maximum":0.2},
            "genetic_family":"IGNEOUS","id":"test:rock",
            "lithology":"GRANITIC_GNEISS","mineral_modes_ppm":{"test:quartz":999999},
            "modal_spread_fraction":0.1,
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
