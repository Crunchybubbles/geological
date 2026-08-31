package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogAuthoringException;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogJsonLoader;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.MineralAssemblage;
import io.github.crunchybubbles.geological.petrology.MineralDefinition;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.io.ByteArrayInputStream;
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
        "sha256:fcd78f23997e677682276abc083b588ca88e1fddffa48a28ffc00f24abf8ef52",
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
  }

  @Test
  void strictCatalogBoundaryRejectsUnknownFieldsAndUnclosedModes() {
    String unknown =
        """
        {"authoring_schema":"geological:material_catalog_authoring:v1","evidence":{},
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
          "authoring_schema":"geological:material_catalog_authoring:v1",
          "evidence":{"citation_id":"refs:test","parameter_basis":"test tunable",
            "publication_year":2000,"title":"Test","uri":"https://example.invalid/test"},
          "minerals":[{"density_g_cm3":2.65,"formula":{"Si":1,"O":2},
            "hardness_mohs":7.0,"id":"test:quartz","weathering_resistance":1.0}],
          "rocks":[{"erodibility_index":0.1,"genetic_family":"IGNEOUS","id":"test:rock",
            "lithology":"GRANITIC_GNEISS","mineral_modes_ppm":{"test:quartz":999999},
            "permeability_index":0.1,"porosity_fraction":0.1}],
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
}
