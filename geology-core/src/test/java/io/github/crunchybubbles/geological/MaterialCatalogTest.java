package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogAuthoringException;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogJsonLoader;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.MaterialConstituentKind;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MetamorphicGrade;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
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

    assertEquals(43, catalog.minerals().size());
    assertEquals(1, catalog.nonCrystallineConstituents().size());
    assertEquals(44, catalog.constituents().size());
    assertEquals(7, catalog.solidSolutions().size());
    assertEquals(33, catalog.rocks().size());
    assertEquals(Lithology.values().length, catalog.rocks().size());
    assertEquals(Overprint.values().length, catalog.alterations().size());
    assertEquals(
        "sha256:248a2031415f222752975c6e96d4920e760b8c8d8cc0142f53808f3e58777a49",
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
                  rock.geneticFamily() == GeneticFamily.METAMORPHIC,
                  rock.primaryMetamorphism().isPresent(),
                  rock.id());
              assertEquals(
                  MaterialAssemblage.SCALE,
                  rock.primaryAssemblage().modesPpm().values().stream()
                      .mapToLong(Long::longValue)
                      .sum(),
                  rock.id());
              assertEquals(
                  MaterialAssemblage.SCALE,
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
              assertEquals(
                  alteration.processClass() == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM,
                  alteration.responseTexture().isPresent());
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
    assertEquals(
        RockTexture.HORNFELSIC,
        catalog.requireAlteration(Overprint.CONTACT_HORNFELS).responseTexture().orElseThrow());
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
        MaterialAssemblage.SCALE,
        plagioclase.endmemberVolumeFractionsPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertEquals(
        MaterialAssemblage.SCALE,
        plagioclase.endmemberMoleFractionsPpm().values().stream().mapToLong(Long::longValue).sum());
    assertEquals(
        MaterialAssemblage.SCALE,
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
  void andesiteAndRhyoliteExtendVolcanicSilicaAndTextureRange() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var basalt = catalog.requireRock(Lithology.BASALTIC);
    var andesite = catalog.requireRock(Lithology.ANDESITIC);
    var rhyolite = catalog.requireRock(Lithology.RHYOLITIC);

    assertEquals(RockTexture.APHANITIC_CRYSTALLINE, basalt.texture());
    assertEquals(RockTexture.PORPHYRITIC_VOLCANIC, andesite.texture());
    assertEquals(RockTexture.FELSITIC_FLOW_BANDED, rhyolite.texture());
    assertEquals(
        500_000L,
        andesite.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            + andesite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite"));

    long rhyoliteQuartz = rhyolite.primaryAssemblage().modesPpm().get("geological:mineral/quartz");
    long rhyoliteAlkaliFeldspar =
        rhyolite.primaryAssemblage().modesPpm().get("geological:mineral/orthoclase");
    long rhyolitePlagioclase =
        rhyolite.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            + rhyolite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite");
    assertTrue(
        (double) rhyoliteQuartz / (rhyoliteQuartz + rhyoliteAlkaliFeldspar + rhyolitePlagioclase)
            >= 0.2);
    assertTrue(
        (double) rhyolitePlagioclase / (rhyoliteAlkaliFeldspar + rhyolitePlagioclase) <= 0.35);

    var andesiteComposition = catalog.composition(andesite.primaryAssemblage());
    var rhyoliteComposition = catalog.composition(rhyolite.primaryAssemblage());
    assertTrue(
        rhyoliteComposition.elementMassPpm().get(ChemicalElement.SI)
            > andesiteComposition.elementMassPpm().get(ChemicalElement.SI));
    assertTrue(
        rhyoliteComposition.elementMassPpm().get(ChemicalElement.K)
            > andesiteComposition.elementMassPpm().get(ChemicalElement.K));
    assertTrue(
        andesiteComposition.elementMassPpm().get(ChemicalElement.FE)
                + andesiteComposition.elementMassPpm().get(ChemicalElement.MG)
            > rhyoliteComposition.elementMassPpm().get(ChemicalElement.FE)
                + rhyoliteComposition.elementMassPpm().get(ChemicalElement.MG));

    var andesiteSolutions =
        catalog.solidSolutionStates(andesite.primaryAssemblage()).stream()
            .map(SolidSolutionState::definitionId)
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(andesiteSolutions.contains("geological:solid_solution/plagioclase"));
    assertTrue(andesiteSolutions.contains("geological:solid_solution/hornblende"));
    assertTrue(andesiteSolutions.contains("geological:solid_solution/calcic_clinopyroxene"));
    assertTrue(andesiteSolutions.contains("geological:solid_solution/orthopyroxene"));
    assertTrue(andesiteSolutions.contains("geological:solid_solution/biotite"));
    var rhyoliteSolutions =
        catalog.solidSolutionStates(rhyolite.primaryAssemblage()).stream()
            .map(SolidSolutionState::definitionId)
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(rhyoliteSolutions.contains("geological:solid_solution/plagioclase"));
    assertTrue(rhyoliteSolutions.contains("geological:solid_solution/biotite"));
    assertFalse(rhyoliteSolutions.contains("geological:solid_solution/calcic_clinopyroxene"));
  }

  @Test
  void alkalineAndCarbonatiteRecipesSeparateFeldspathoidAndCarbonateMagmatism() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var alkaline = catalog.requireRock(Lithology.ALKALINE);
    var carbonatite = catalog.requireRock(Lithology.CARBONATITIC);

    assertEquals(GeneticFamily.IGNEOUS, alkaline.geneticFamily());
    assertEquals(GeneticFamily.IGNEOUS, carbonatite.geneticFamily());
    assertEquals(RockTexture.FELDSPATHOID_BEARING_CRYSTALLINE, alkaline.texture());
    assertEquals(RockTexture.MAGMATIC_CARBONATE_CRYSTALLINE, carbonatite.texture());
    assertFalse(alkaline.primaryAssemblage().modesPpm().containsKey("geological:mineral/quartz"));
    assertEquals(
        750_000L,
        alkaline.primaryAssemblage().modesPpm().get("geological:mineral/orthoclase")
            + alkaline.primaryAssemblage().modesPpm().get("geological:mineral/nepheline"));
    assertTrue(
        carbonatite.primaryAssemblage().modesPpm().get("geological:mineral/calcite")
                + carbonatite.primaryAssemblage().modesPpm().get("geological:mineral/dolomite")
            > 500_000L);

    MineralDefinition nepheline = catalog.requireMineral("geological:mineral/nepheline");
    assertEquals(1.0, nepheline.formula().get(ChemicalElement.NA).doubleValue());
    assertEquals(1.0, nepheline.formula().get(ChemicalElement.AL).doubleValue());
    assertEquals(1.0, nepheline.formula().get(ChemicalElement.SI).doubleValue());
    assertEquals(4.0, nepheline.formula().get(ChemicalElement.O).doubleValue());
    MineralDefinition aegirine = catalog.requireMineral("geological:mineral/aegirine");
    assertEquals(1.0, aegirine.formula().get(ChemicalElement.NA).doubleValue());
    assertEquals(1.0, aegirine.formula().get(ChemicalElement.FE).doubleValue());
    MineralDefinition fluorapatite = catalog.requireMineral("geological:mineral/fluorapatite");
    assertEquals(3.0, fluorapatite.formula().get(ChemicalElement.P).doubleValue());
    assertEquals(1.0, fluorapatite.formula().get(ChemicalElement.F).doubleValue());

    var alkalineComposition = catalog.composition(alkaline.primaryAssemblage());
    var carbonatiteComposition = catalog.composition(carbonatite.primaryAssemblage());
    assertTrue(
        alkalineComposition.elementMassPpm().get(ChemicalElement.SI)
            > carbonatiteComposition.elementMassPpm().get(ChemicalElement.SI));
    assertTrue(
        carbonatiteComposition.elementMassPpm().get(ChemicalElement.C)
            > alkalineComposition.elementMassPpm().getOrDefault(ChemicalElement.C, 0L));
    assertTrue(
        carbonatiteComposition.elementMassPpm().get(ChemicalElement.P)
            > alkalineComposition.elementMassPpm().get(ChemicalElement.P));
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
  void bandedIronFormationBalancesSilicaOxidesAndIronCarbonate() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var bif = catalog.requireRock(Lithology.BANDED_IRON_FORMATION);
    MineralDefinition siderite = catalog.requireMineral("geological:mineral/siderite");

    assertEquals(RockTexture.BANDED_IRON_SILICA, bif.texture());
    assertEquals(GeneticFamily.SEDIMENTARY, bif.geneticFamily());
    assertEquals(1.0, siderite.formula().get(ChemicalElement.FE).doubleValue());
    assertEquals(1.0, siderite.formula().get(ChemicalElement.C).doubleValue());
    assertEquals(3.0, siderite.formula().get(ChemicalElement.O).doubleValue());
    assertEquals(580_000L, bif.primaryAssemblage().modesPpm().get("geological:mineral/quartz"));
    assertEquals(180_000L, bif.primaryAssemblage().modesPpm().get("geological:mineral/magnetite"));
    assertEquals(120_000L, bif.primaryAssemblage().modesPpm().get("geological:mineral/hematite"));
    assertEquals(70_000L, bif.primaryAssemblage().modesPpm().get("geological:mineral/siderite"));
    long ironPpm =
        catalog.composition(bif.primaryAssemblage()).elementMassPpm().get(ChemicalElement.FE);
    assertTrue(ironPpm > 300_000L);
    assertTrue(ironPpm < 400_000L);
  }

  @Test
  void coalUsesClosedOrganicBulkChemistryInsteadOfAFictitiousMineralFormula() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var coal = catalog.requireRock(Lithology.COAL);
    var organic =
        catalog.requireNonCrystallineConstituent("geological:constituent/coal_organic_matter");

    assertEquals(MaterialConstituentKind.ORGANIC_MATTER, organic.kind());
    assertEquals(RockTexture.ORGANIC_BEDDED, coal.texture());
    assertEquals(GeneticFamily.SEDIMENTARY, coal.geneticFamily());
    assertEquals(
        MaterialAssemblage.SCALE,
        organic.elementMassPpm().values().stream().mapToLong(Long::longValue).sum());
    assertEquals(800_000L, organic.elementMassPpm().get(ChemicalElement.C));
    assertEquals(15_000L, organic.elementMassPpm().get(ChemicalElement.N));
    assertEquals(
        820_000L,
        coal.primaryAssemblage().modesPpm().get("geological:constituent/coal_organic_matter"));
    var composition = catalog.composition(coal.primaryAssemblage());
    assertTrue(composition.elementMassPpm().get(ChemicalElement.C) > 500_000L);
    assertTrue(composition.elementMassPpm().get(ChemicalElement.N) > 0L);
    assertTrue(catalog.solidSolutionStates(coal.primaryAssemblage()).isEmpty());
  }

  @Test
  void peliticMetamorphicSliceAuthorsProtolithGradeAndIndexMinerals() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var slate = catalog.requireRock(Lithology.SLATE_PHYLLITE);
    var schist = catalog.requireRock(Lithology.MICA_SCHIST);
    var slateMetamorphism = slate.primaryMetamorphism().orElseThrow();
    var schistMetamorphism = schist.primaryMetamorphism().orElseThrow();
    MineralDefinition graphite = catalog.requireMineral("geological:mineral/graphite");
    MineralDefinition almandine = catalog.requireMineral("geological:mineral/almandine");

    assertEquals(RockTexture.SLATY_PHYLLITIC, slate.texture());
    assertEquals(RockTexture.SCHISTOSE, schist.texture());
    assertEquals("geological:rock/basin_shale", slateMetamorphism.protolithRockId());
    assertEquals("geological:rock/basin_shale", schistMetamorphism.protolithRockId());
    assertEquals(MetamorphicGrade.LOW, slateMetamorphism.grade());
    assertEquals(MetamorphicGrade.MEDIUM, schistMetamorphism.grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, slateMetamorphism.facies());
    assertEquals(MetamorphicFacies.AMPHIBOLITE, schistMetamorphism.facies());
    assertEquals(
        "geological:rock/felsic_stock",
        catalog
            .requireRock(Lithology.GRANITIC_GNEISS)
            .primaryMetamorphism()
            .orElseThrow()
            .protolithRockId());
    assertTrue(
        slateMetamorphism.maximumTemperatureCelsius()
            < schistMetamorphism.maximumTemperatureCelsius());
    assertEquals(1.0, graphite.formula().get(ChemicalElement.C).doubleValue());
    assertEquals(3.0, almandine.formula().get(ChemicalElement.FE).doubleValue());
    assertEquals(2.0, almandine.formula().get(ChemicalElement.AL).doubleValue());
    assertEquals(3.0, almandine.formula().get(ChemicalElement.SI).doubleValue());
    assertEquals(12.0, almandine.formula().get(ChemicalElement.O).doubleValue());
    assertEquals(10_000L, slate.primaryAssemblage().modesPpm().get("geological:mineral/graphite"));
    assertEquals(
        50_000L, schist.primaryAssemblage().modesPpm().get("geological:mineral/almandine"));
    assertTrue(
        catalog.composition(slate.primaryAssemblage()).elementMassPpm().get(ChemicalElement.C)
            > 0L);
  }

  @Test
  void maficMetamorphicSliceSeparatesActinoliteGreenschistFromHornblendeAmphibolite() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var greenschist = catalog.requireRock(Lithology.GREENSCHIST);
    var amphibolite = catalog.requireRock(Lithology.AMPHIBOLITE);
    var greenschistMetamorphism = greenschist.primaryMetamorphism().orElseThrow();
    var amphiboliteMetamorphism = amphibolite.primaryMetamorphism().orElseThrow();
    MineralDefinition magnesiohornblende =
        catalog.requireMineral("geological:mineral/magnesiohornblende");
    MineralDefinition ferrohornblende =
        catalog.requireMineral("geological:mineral/ferrohornblende");

    assertEquals(RockTexture.SCHISTOSE, greenschist.texture());
    assertEquals(RockTexture.NEMATOBLASTIC, amphibolite.texture());
    assertEquals("geological:rock/basaltic", greenschistMetamorphism.protolithRockId());
    assertEquals("geological:rock/basaltic", amphiboliteMetamorphism.protolithRockId());
    assertEquals(MetamorphicGrade.LOW, greenschistMetamorphism.grade());
    assertEquals(MetamorphicGrade.MEDIUM, amphiboliteMetamorphism.grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, greenschistMetamorphism.facies());
    assertEquals(MetamorphicFacies.AMPHIBOLITE, amphiboliteMetamorphism.facies());
    assertTrue(
        greenschistMetamorphism.maximumTemperatureCelsius()
            < amphiboliteMetamorphism.maximumTemperatureCelsius());

    assertEquals(2.0, magnesiohornblende.formula().get(ChemicalElement.CA).doubleValue());
    assertEquals(4.0, magnesiohornblende.formula().get(ChemicalElement.MG).doubleValue());
    assertEquals(2.0, magnesiohornblende.formula().get(ChemicalElement.AL).doubleValue());
    assertEquals(7.0, magnesiohornblende.formula().get(ChemicalElement.SI).doubleValue());
    assertEquals(24.0, magnesiohornblende.formula().get(ChemicalElement.O).doubleValue());
    assertEquals(2.0, magnesiohornblende.formula().get(ChemicalElement.H).doubleValue());
    assertEquals(4.0, ferrohornblende.formula().get(ChemicalElement.FE).doubleValue());

    SolidSolutionState actinolite =
        catalog.solidSolutionStates(greenschist.primaryAssemblage()).stream()
            .filter(
                state -> state.definitionId().equals("geological:solid_solution/calcic_amphibole"))
            .findFirst()
            .orElseThrow();
    SolidSolutionState hornblende =
        catalog.solidSolutionStates(amphibolite.primaryAssemblage()).stream()
            .filter(state -> state.definitionId().equals("geological:solid_solution/hornblende"))
            .findFirst()
            .orElseThrow();
    assertEquals(180_000L, actinolite.phaseModePpm());
    assertEquals(440_000L, hornblende.phaseModePpm());
    assertEquals(8.0, actinolite.idealFormulaAtoms().get(ChemicalElement.SI), 1.0e-12);
    assertEquals(7.0, hornblende.idealFormulaAtoms().get(ChemicalElement.SI), 1.0e-12);
    assertEquals(2.0, hornblende.idealFormulaAtoms().get(ChemicalElement.AL), 1.0e-12);
    assertEquals(
        330_000L,
        amphibolite.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            + amphibolite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite"));
  }

  @Test
  void granuliteCompletesMaficProgressionWithDryTwoPyroxeneAssemblage() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var amphibolite = catalog.requireRock(Lithology.AMPHIBOLITE);
    var granulite = catalog.requireRock(Lithology.GRANULITE);
    var amphiboliteMetamorphism = amphibolite.primaryMetamorphism().orElseThrow();
    var granuliteMetamorphism = granulite.primaryMetamorphism().orElseThrow();

    assertEquals(RockTexture.GRANOBLASTIC, granulite.texture());
    assertEquals("geological:rock/basaltic", granuliteMetamorphism.protolithRockId());
    assertEquals(MetamorphicGrade.HIGH, granuliteMetamorphism.grade());
    assertEquals(MetamorphicFacies.GRANULITE, granuliteMetamorphism.facies());
    assertTrue(
        amphiboliteMetamorphism.maximumTemperatureCelsius()
            < granuliteMetamorphism.maximumTemperatureCelsius());
    assertEquals(
        0L,
        catalog
            .composition(granulite.primaryAssemblage())
            .elementMassPpm()
            .getOrDefault(ChemicalElement.H, 0L));

    var solutionIds =
        catalog.solidSolutionStates(granulite.primaryAssemblage()).stream()
            .map(SolidSolutionState::definitionId)
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(solutionIds.contains("geological:solid_solution/plagioclase"));
    assertTrue(solutionIds.contains("geological:solid_solution/orthopyroxene"));
    assertTrue(solutionIds.contains("geological:solid_solution/calcic_clinopyroxene"));
    assertFalse(solutionIds.contains("geological:solid_solution/hornblende"));
    assertFalse(solutionIds.contains("geological:solid_solution/calcic_amphibole"));
    assertEquals(
        360_000L,
        granulite.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            + granulite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite"));
    assertEquals(
        260_000L,
        granulite.primaryAssemblage().modesPpm().get("geological:mineral/enstatite")
            + granulite.primaryAssemblage().modesPpm().get("geological:mineral/ferrosilite"));
    assertEquals(
        230_000L,
        granulite.primaryAssemblage().modesPpm().get("geological:mineral/diopside")
            + granulite.primaryAssemblage().modesPpm().get("geological:mineral/hedenbergite"));
  }

  @Test
  void quartziteAndMarbleRetainQuartzRichAndCarbonateProtoliths() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var sandstone = catalog.requireRock(Lithology.BASIN_SANDSTONE);
    var quartzite = catalog.requireRock(Lithology.QUARTZITE);
    var marble = catalog.requireRock(Lithology.MARBLE);
    var quartziteMetamorphism = quartzite.primaryMetamorphism().orElseThrow();
    var marbleMetamorphism = marble.primaryMetamorphism().orElseThrow();

    assertEquals(RockTexture.GRANOBLASTIC, quartzite.texture());
    assertEquals(RockTexture.GRANOBLASTIC, marble.texture());
    assertEquals("geological:rock/basin_sandstone", quartziteMetamorphism.protolithRockId());
    assertEquals("geological:rock/limestone", marbleMetamorphism.protolithRockId());
    assertEquals(MetamorphicGrade.LOW, quartziteMetamorphism.grade());
    assertEquals(MetamorphicGrade.LOW, marbleMetamorphism.grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, quartziteMetamorphism.facies());
    assertEquals(MetamorphicFacies.GREENSCHIST, marbleMetamorphism.facies());

    assertEquals(
        840_000L, quartzite.primaryAssemblage().modesPpm().get("geological:mineral/quartz"));
    assertTrue(
        quartzite.primaryAssemblage().modesPpm().get("geological:mineral/quartz")
            > sandstone.primaryAssemblage().modesPpm().get("geological:mineral/quartz"));
    assertEquals(
        920_000L,
        marble.primaryAssemblage().modesPpm().get("geological:mineral/calcite")
            + marble.primaryAssemblage().modesPpm().get("geological:mineral/dolomite"));
    assertTrue(
        catalog.composition(quartzite.primaryAssemblage()).elementMassPpm().get(ChemicalElement.SI)
            > catalog
                .composition(marble.primaryAssemblage())
                .elementMassPpm()
                .get(ChemicalElement.SI));
    assertTrue(
        catalog.composition(marble.primaryAssemblage()).elementMassPpm().get(ChemicalElement.C)
            > catalog
                .composition(quartzite.primaryAssemblage())
                .elementMassPpm()
                .get(ChemicalElement.C));
    assertTrue(quartzite.lithology().strength() > marble.lithology().strength());
    assertTrue(
        catalog.solidSolutionStates(quartzite.primaryAssemblage()).stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/plagioclase")));
    assertTrue(catalog.solidSolutionStates(marble.primaryAssemblage()).isEmpty());
  }

  @Test
  void serpentiniteRetainsUltramaficProtolithAndHydratedMeshAssemblage() {
    MaterialCatalogSnapshot catalog = Phase2World.materialCatalog();
    var ultramafic = catalog.requireRock(Lithology.KOMATIITIC_ULTRAMAFIC);
    var serpentinite = catalog.requireRock(Lithology.SERPENTINITE);
    var metamorphism = serpentinite.primaryMetamorphism().orElseThrow();

    assertEquals(RockTexture.SERPENTINIZED_MESH, serpentinite.texture());
    assertEquals("geological:rock/komatiitic_ultramafic", metamorphism.protolithRockId());
    assertEquals(MetamorphicGrade.LOW, metamorphism.grade());
    assertEquals(MetamorphicFacies.SUBGREENSCHIST, metamorphism.facies());
    assertEquals(MetamorphicPath.HYDROTHERMAL_HYDRATION, metamorphism.path());
    assertEquals(25.0, metamorphism.minimumTemperatureCelsius());
    assertEquals(300.0, metamorphism.maximumTemperatureCelsius());

    long serpentineMode =
        serpentinite.primaryAssemblage().modesPpm().get("geological:mineral/lizardite")
            + serpentinite.primaryAssemblage().modesPpm().get("geological:mineral/chrysotile")
            + serpentinite.primaryAssemblage().modesPpm().get("geological:mineral/antigorite");
    assertEquals(850_000L, serpentineMode);
    for (String mineralId :
        java.util.List.of(
            "geological:mineral/lizardite",
            "geological:mineral/chrysotile",
            "geological:mineral/antigorite")) {
      MineralDefinition mineral = catalog.requireMineral(mineralId);
      assertEquals(3.0, mineral.formula().get(ChemicalElement.MG).doubleValue());
      assertEquals(2.0, mineral.formula().get(ChemicalElement.SI).doubleValue());
      assertEquals(9.0, mineral.formula().get(ChemicalElement.O).doubleValue());
      assertEquals(4.0, mineral.formula().get(ChemicalElement.H).doubleValue());
    }
    MineralDefinition brucite = catalog.requireMineral("geological:mineral/brucite");
    assertEquals(2.0, brucite.formula().get(ChemicalElement.H).doubleValue());
    assertTrue(
        catalog
                .composition(serpentinite.primaryAssemblage())
                .elementMassPpm()
                .get(ChemicalElement.H)
            > catalog
                .composition(ultramafic.primaryAssemblage())
                .elementMassPpm()
                .getOrDefault(ChemicalElement.H, 0L));
    assertTrue(
        catalog.composition(serpentinite.primaryAssemblage()).density()
            < catalog.composition(ultramafic.primaryAssemblage()).density());
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
    reordered =
        reordered.replace(
            "\"geological:mineral/magnesiohornblende\", \"geological:mineral/ferrohornblende\"",
            "\"geological:mineral/ferrohornblende\", \"geological:mineral/magnesiohornblende\"");
    reordered =
        reordered.replace(
            "{\"C\": 800000, \"H\": 50000, \"N\": 15000, \"O\": 120000, \"S\": 15000}",
            "{\"S\": 15000, \"O\": 120000, \"N\": 15000, \"H\": 50000, \"C\": 800000}");
    reordered =
        reordered.replace(
            "\"facies\": \"AMPHIBOLITE\",\n        \"grade\": \"HIGH\"",
            "\"grade\": \"HIGH\",\n        \"facies\": \"AMPHIBOLITE\"");
    assertTrue(reordered.contains("\"METAMORPHIC\", \"IGNEOUS\""));
    assertTrue(reordered.contains("\"grade\": \"HIGH\",\n        \"facies\": \"AMPHIBOLITE\""));

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

    String missingResponseTexture =
        authored.replace("\"response_texture\": \"HORNFELSIC\"", "\"response_texture\": null");
    MaterialCatalogAuthoringException textureFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            missingResponseTexture.getBytes(StandardCharsets.UTF_8)),
                        "missing-response-texture.json"));
    assertTrue(textureFailure.getMessage().contains("explicit response texture"));

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

    String unclosedOrganic = authored.replace("\"N\": 15000", "\"N\": 14999");
    MaterialCatalogAuthoringException organicFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unclosedOrganic.getBytes(StandardCharsets.UTF_8)),
                        "unclosed-organic.json"));
    assertTrue(organicFailure.getMessage().contains("element mass must close"));

    String sharedConstituentId =
        authored.replace(
            "\"id\": \"geological:constituent/coal_organic_matter\"",
            "\"id\": \"geological:mineral/quartz\"");
    MaterialCatalogAuthoringException sharedIdFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            sharedConstituentId.getBytes(StandardCharsets.UTF_8)),
                        "shared-constituent-id.json"));
    assertTrue(sharedIdFailure.getMessage().contains("shared by mineral and non-crystalline"));

    String missingPrimaryMetamorphism =
        authored.replace("\"genetic_family\": \"METAMORPHIC\"", "\"genetic_family\": \"IGNEOUS\"");
    MaterialCatalogAuthoringException primaryFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(
                            missingPrimaryMetamorphism.getBytes(StandardCharsets.UTF_8)),
                        "invalid-primary-metamorphism.json"));
    assertTrue(primaryFailure.getMessage().contains("required exactly for metamorphic"));

    String unknownProtolith =
        authored.replace(
            "\"protolith_rock_id\": \"geological:rock/felsic_stock\"",
            "\"protolith_rock_id\": \"geological:rock/unknown\"");
    MaterialCatalogAuthoringException protolithFailure =
        assertThrows(
            MaterialCatalogAuthoringException.class,
            () ->
                new MaterialCatalogJsonLoader()
                    .load(
                        new ByteArrayInputStream(unknownProtolith.getBytes(StandardCharsets.UTF_8)),
                        "unknown-protolith.json"));
    assertTrue(protolithFailure.getMessage().contains("unknown metamorphic protolith"));
  }

  @Test
  void strictCatalogBoundaryRejectsUnknownFieldsAndUnclosedModes() {
    String unknown =
        """
        {"authoring_schema":"geological:material_catalog_authoring:v8","evidence":{},
        "minerals":[],"non_crystalline_constituents":[],"rocks":[],
        "solid_solutions":[],"overprints":[],"surprise":true}
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
          "authoring_schema":"geological:material_catalog_authoring:v8",
          "evidence":{"citation_id":"refs:test","parameter_basis":"test tunable",
            "publication_year":2000,"title":"Test","uri":"https://example.invalid/test"},
          "minerals":[{"density_g_cm3":2.65,"formula":{"Si":1,"O":2},
            "hardness_mohs":7.0,"id":"test:quartz","weathering_resistance":1.0}],
          "non_crystalline_constituents":[],
          "solid_solutions":[],
          "rocks":[{"erodibility_distribution":{"minimum":0.05,"mode":0.1,"maximum":0.2},
            "genetic_family":"IGNEOUS","id":"test:rock",
            "lithology":"GRANITIC_GNEISS","constituent_modes_ppm":{"test:quartz":999999},
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
    assertTrue(modeFailure.getMessage().contains("constituent modes must close"));
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
