package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.BodyCompositionSampler;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ClastShape;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceContribution;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceMix;
import io.github.crunchybubbles.geological.petrology.ColluvialTextureState;
import io.github.crunchybubbles.geological.petrology.ElementReservoirLedger;
import io.github.crunchybubbles.geological.petrology.FluidMedium;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MantleCargoStatus;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MetamorphicGrade;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
import io.github.crunchybubbles.geological.petrology.PetrologicColumnResult;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.PetrologicState;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentGrainSize;
import io.github.crunchybubbles.geological.petrology.SedimentSorting;
import io.github.crunchybubbles.geological.petrology.SolidSolutionState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialQueryTest {
  @Test
  void phase2IdentityComposesFrozenPhase1ScienceWithMaterialContent() {
    assertEquals("phase2.0-alpha.29", Phase2World.MODEL_VERSION);
    assertEquals(
        "sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4",
        Phase1World.SCIENTIFIC_DIGEST);
    assertEquals(Phase1World.SCIENTIFIC_DIGEST, Phase2World.baseScientificSnapshot().digest());
    assertNotEquals(Phase1World.SCIENTIFIC_DIGEST, Phase2World.SCIENTIFIC_DIGEST);
    assertEquals(
        "sha256:984be8310f9abc9a7188efb43632c46bcdca3fc1cabc6f49e1853be80da52625",
        Phase2World.SCIENTIFIC_DIGEST);
    assertTrue(
        Phase2World.scientificManifestJson().contains(Phase2World.materialCatalog().digest()));
  }

  @Test
  void linkedPlutonPulsesAdvanceOneMagmaLineageInsteadOfRollingRockNames() {
    MaterialQueryEngine query = Phase2World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);

    double previousProgress = -1.0;
    for (int index = 0; index < province.geometry().plutonPulses().size(); index++) {
      RiftArcGeometry.PlutonPulse pulse = province.geometry().plutonPulses().get(index);
      GeologicalSample geological =
          sample(
              province,
              pulse.center(),
              pulse.id(),
              pulse.lithology(),
              pulse.birthAge(),
              Overprint.NONE);
      PetrologicSample material = query.resolve(province, geological);

      assertTrue(material.magmaLineage().isPresent());
      assertEquals(
          province.proofIds().magmaLineageId(), material.magmaLineage().orElseThrow().systemId());
      assertEquals(index, material.magmaLineage().orElseThrow().pulseOrder());
      assertTrue(
          material.magmaLineage().orElseThrow().differentiationProgress() > previousProgress);
      previousProgress = material.magmaLineage().orElseThrow().differentiationProgress();
    }
  }

  @Test
  void intermediateAndFelsicVolcanicQueriesRemainDistinctFromIntrusiveLineage() {
    MaterialQueryEngine query = Phase2World.create(8_183L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample andesite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000104"),
                Lithology.ANDESITIC,
                new AgeKey(245.0, 0),
                Overprint.NONE));
    PetrologicSample rhyolite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000105"),
                Lithology.RHYOLITIC,
                new AgeKey(240.0, 0),
                Overprint.NONE));

    assertEquals(RockTexture.PORPHYRITIC_VOLCANIC, andesite.resolvedTexture());
    assertEquals(RockTexture.FELSITIC_FLOW_BANDED, rhyolite.resolvedTexture());
    assertEquals(MetamorphicGrade.NONE, andesite.metamorphism().grade());
    assertEquals(MetamorphicGrade.NONE, rhyolite.metamorphism().grade());
    assertTrue(andesite.magmaLineage().isEmpty());
    assertTrue(rhyolite.magmaLineage().isEmpty());
    assertTrue(
        rhyolite.primaryComposition().elementMassPpm().get(ChemicalElement.SI)
            > andesite.primaryComposition().elementMassPpm().get(ChemicalElement.SI));
    assertTrue(
        rhyolite.primaryComposition().elementMassPpm().get(ChemicalElement.K)
            > andesite.primaryComposition().elementMassPpm().get(ChemicalElement.K));
  }

  @Test
  void rareAlkalineAndCarbonatiticQueriesRetainDistinctPrimaryChemistry() {
    MaterialQueryEngine query = Phase2World.create(8_183L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample alkaline =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000106"),
                Lithology.ALKALINE,
                new AgeKey(235.0, 0),
                Overprint.NONE));
    PetrologicSample carbonatite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000107"),
                Lithology.CARBONATITIC,
                new AgeKey(230.0, 0),
                Overprint.NONE));

    assertEquals(RockTexture.FELDSPATHOID_BEARING_CRYSTALLINE, alkaline.resolvedTexture());
    assertEquals(RockTexture.MAGMATIC_CARBONATE_CRYSTALLINE, carbonatite.resolvedTexture());
    assertEquals(MetamorphicGrade.NONE, alkaline.metamorphism().grade());
    assertEquals(MetamorphicGrade.NONE, carbonatite.metamorphism().grade());
    assertTrue(alkaline.magmaLineage().isEmpty());
    assertTrue(carbonatite.magmaLineage().isEmpty());
    assertTrue(
        alkaline.primaryComposition().elementMassPpm().get(ChemicalElement.SI)
            > carbonatite.primaryComposition().elementMassPpm().get(ChemicalElement.SI));
    assertTrue(
        carbonatite.primaryComposition().elementMassPpm().get(ChemicalElement.C)
            > alkaline.primaryComposition().elementMassPpm().getOrDefault(ChemicalElement.C, 0L));
    assertTrue(
        carbonatite.primaryComposition().elementMassPpm().get(ChemicalElement.P)
            > alkaline.primaryComposition().elementMassPpm().get(ChemicalElement.P));
  }

  @Test
  void kimberliteCarrierDoesNotInventUnresolvedDiamondFertility() {
    MaterialQueryEngine query = Phase2World.create(8_183L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    StableId bodyId = StableId.parse("00000000000000000000000000000108");
    PetrologicSample kimberlite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                bodyId,
                Lithology.KIMBERLITIC,
                new AgeKey(225.0, 0),
                Overprint.NONE));

    assertEquals(RockTexture.MACROCRYSTIC_VOLATILE_RICH, kimberlite.resolvedTexture());
    assertTrue(kimberlite.magmaLineage().isEmpty());
    assertFalse(
        kimberlite.primaryAssemblage().modesPpm().containsKey("geological:mineral/diamond"));
    var cargo = kimberlite.mantleCargo().orElseThrow();
    assertEquals(bodyId, cargo.carrierBodyId());
    assertEquals(MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED, cargo.status());
    assertTrue(cargo.sourceReservoirId().isEmpty());
    assertEquals("geological:mineral/diamond", cargo.diamondMineralId());
    assertEquals(0L, cargo.diamondGradePpbByMass());
    assertEquals(
        List.of(
            "geological:mineral/chromite",
            "geological:mineral/diopside",
            "geological:mineral/ilmenite",
            "geological:mineral/pyrope"),
        cargo.candidateIndicatorMineralIds());
    assertEquals(cargo, PetrologicState.from(kimberlite).mantleCargo().orElseThrow());
  }

  @Test
  void pyroclasticQueryKeepsPrimaryEruptiveIdentitySeparateFromMarineReworking() {
    MaterialQueryEngine query = Phase2World.create(8_183L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample pyroclastic =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000109"),
                Lithology.PYROCLASTIC,
                new AgeKey(220.0, 0),
                Overprint.NONE));
    PetrologicSample marineVolcaniclastic =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.MARINE_VOLCANICLASTIC,
                new AgeKey(215.0, 0),
                Overprint.NONE));

    assertEquals(RockTexture.WELDED_FRAGMENTAL, pyroclastic.resolvedTexture());
    assertTrue(pyroclastic.sedimentaryState().isEmpty());
    assertTrue(pyroclastic.magmaLineage().isEmpty());
    assertTrue(
        pyroclastic
            .primaryAssemblage()
            .modesPpm()
            .containsKey("geological:constituent/rhyolitic_volcanic_glass"));
    assertTrue(marineVolcaniclastic.sedimentaryState().isPresent());
    assertFalse(
        marineVolcaniclastic
            .primaryAssemblage()
            .modesPpm()
            .containsKey("geological:constituent/rhyolitic_volcanic_glass"));
  }

  @Test
  void surficialCatalogQueriesKeepDistinctTransportAndResidualBehavior() {
    MaterialQueryEngine query = Phase2World.create(8_184L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample laterite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a01"),
                Lithology.LATERITE_BAUXITE,
                new AgeKey(1.5, 0),
                Overprint.NONE));
    PetrologicSample colluvium =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a02"),
                Lithology.SOIL_COLLUVIUM,
                new AgeKey(0.02, 0),
                Overprint.NONE));
    PetrologicSample till =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a03"),
                Lithology.GLACIAL_TILL,
                new AgeKey(0.015, 0),
                Overprint.NONE));

    assertEquals(RockTexture.LATERITIC_PISOLITIC, laterite.resolvedTexture());
    assertEquals(RockTexture.SOIL_COLLUVIAL, colluvium.resolvedTexture());
    assertEquals(RockTexture.GLACIAL_DIAMICTIC, till.resolvedTexture());
    for (PetrologicSample material : java.util.List.of(laterite, colluvium, till)) {
      assertTrue(material.sedimentaryState().isEmpty());
      assertEquals(MetamorphicGrade.NONE, material.metamorphism().grade());
      assertEquals(MaterialProcessClass.NONE, material.processClass());
      assertTrue(material.magmaLineage().isEmpty());
      assertTrue(material.mantleCargo().isEmpty());
    }
    assertTrue(
        laterite.primaryComposition().elementMassPpm().get(ChemicalElement.AL)
            > colluvium.primaryComposition().elementMassPpm().get(ChemicalElement.AL));
    assertTrue(
        till.permeabilityIndex()
            < query
                .resolve(
                    province,
                    sample(
                        province,
                        new Point3(0.0, 0.0, 0.0),
                        StableId.parse("00000000000000000000000000000a04"),
                        Lithology.ALLUVIAL_GRAVEL,
                        new AgeKey(0.01, 0),
                        Overprint.NONE))
                .permeabilityIndex());
  }

  @Test
  void mantleCargoRequiresResolvedSourceAndPositiveGradeToClaimDiamonds() {
    StableId carrier = StableId.parse("00000000000000000000000000000108");
    StableId source = StableId.parse("00000000000000000000000000000901");
    List<String> indicators = List.of("geological:mineral/pyrope", "geological:mineral/chromite");

    MantleCargoState bearing =
        new MantleCargoState(
            carrier,
            Optional.of(source),
            MantleCargoStatus.DIAMOND_BEARING,
            "geological:mineral/diamond",
            100L,
            indicators);
    assertEquals(source, bearing.sourceReservoirId().orElseThrow());
    assertEquals(
        List.of("geological:mineral/chromite", "geological:mineral/pyrope"),
        bearing.candidateIndicatorMineralIds());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MantleCargoState(
                carrier,
                Optional.empty(),
                MantleCargoStatus.DIAMOND_BEARING,
                "geological:mineral/diamond",
                100L,
                indicators));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MantleCargoState(
                carrier,
                Optional.of(source),
                MantleCargoStatus.BARREN,
                "geological:mineral/diamond",
                1L,
                indicators));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MantleCargoState(
                carrier,
                Optional.of(source),
                MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED,
                "geological:mineral/diamond",
                0L,
                indicators));
  }

  @Test
  void bodyModalDistributionsAreStableBoundedAndIndependentOfOverprints() {
    MaterialQueryEngine query = Phase2World.create(7_777L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    RiftArcGeometry.PlutonPulse first = province.geometry().plutonPulses().getFirst();
    RiftArcGeometry.PlutonPulse second = province.geometry().plutonPulses().get(1);
    GeologicalSample firstUnaltered =
        sample(
            province,
            first.center(),
            first.id(),
            Lithology.FELSIC_STOCK,
            first.birthAge(),
            Overprint.NONE);
    GeologicalSample firstAltered =
        sample(
            province,
            first.center(),
            first.id(),
            Lithology.FELSIC_STOCK,
            first.birthAge(),
            Overprint.POTASSIC_ALTERATION);
    GeologicalSample secondUnaltered =
        sample(
            province,
            second.center(),
            second.id(),
            Lithology.FELSIC_STOCK,
            second.birthAge(),
            Overprint.NONE);

    PetrologicSample firstMaterial = query.resolve(province, firstUnaltered);
    PetrologicSample alteredMaterial = query.resolve(province, firstAltered);
    PetrologicSample secondMaterial = query.resolve(province, secondUnaltered);

    assertEquals(firstMaterial.primaryAssemblage(), alteredMaterial.primaryAssemblage());
    assertNotEquals(firstMaterial.primaryAssemblage(), secondMaterial.primaryAssemblage());
    assertNotEquals(
        query.catalog().requireRock(Lithology.FELSIC_STOCK).primaryAssemblage(),
        firstMaterial.primaryAssemblage());
    MaterialAssemblage central =
        query.catalog().requireRock(Lithology.FELSIC_STOCK).primaryAssemblage();
    long quartzDelta =
        firstMaterial.primaryAssemblage().modesPpm().get("geological:mineral/quartz")
            - central.modesPpm().get("geological:mineral/quartz");
    long orthoclaseDelta =
        firstMaterial.primaryAssemblage().modesPpm().get("geological:mineral/orthoclase")
            - central.modesPpm().get("geological:mineral/orthoclase");
    long albiteDelta =
        firstMaterial.primaryAssemblage().modesPpm().get("geological:mineral/albite")
            - central.modesPpm().get("geological:mineral/albite");
    long anorthiteDelta =
        firstMaterial.primaryAssemblage().modesPpm().get("geological:mineral/anorthite")
            - central.modesPpm().get("geological:mineral/anorthite");
    assertEquals(-quartzDelta, orthoclaseDelta);
    assertEquals(-albiteDelta, anorthiteDelta);
    assertEquals(
        central.modesPpm().get("geological:mineral/muscovite"),
        firstMaterial.primaryAssemblage().modesPpm().get("geological:mineral/muscovite"));
    assertTrue(
        firstMaterial.rock().porosityDistribution().contains(firstMaterial.porosityFraction()));
    assertTrue(
        firstMaterial
            .rock()
            .permeabilityDistribution()
            .contains(firstMaterial.permeabilityIndex()));
    assertTrue(
        firstMaterial.rock().erodibilityDistribution().contains(firstMaterial.erodibilityIndex()));
    assertNotEquals(firstMaterial.porosityFraction(), secondMaterial.porosityFraction());
    assertEquals(firstMaterial.porosityFraction() * 0.9, alteredMaterial.porosityFraction());
    assertEquals(
        firstMaterial.permeabilityIndex() * StrictMath.sqrt(0.9),
        alteredMaterial.permeabilityIndex());
    assertEquals(firstMaterial.erodibilityIndex() - 0.04, alteredMaterial.erodibilityIndex());
    assertEquals(
        MaterialAssemblage.SCALE,
        firstMaterial.primaryAssemblage().modesPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertTrue(query.bodyRecipeCacheSize() >= 3);
    query.clearCaches();
    assertEquals(0, query.bodyRecipeCacheSize());
    assertEquals(firstMaterial, query.resolve(province, firstUnaltered));
  }

  @Test
  void massTransferResponsesSelectTheTargetForTheHostGeneticFamily() {
    MaterialQueryEngine query = Phase2World.create(6_006L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    var phyllic = query.catalog().requireAlteration(Overprint.PHYLLIC_ALTERATION);

    assertNotEquals(
        phyllic.targetAssemblage(GeneticFamily.IGNEOUS),
        phyllic.targetAssemblage(GeneticFamily.SEDIMENTARY));
    assertNotEquals(
        phyllic.targetAssemblage(GeneticFamily.SEDIMENTARY),
        phyllic.targetAssemblage(GeneticFamily.HYDROTHERMAL));

    for (Lithology lithology :
        List.of(
            Lithology.FELSIC_STOCK,
            Lithology.BASIN_SANDSTONE,
            Lithology.GRANITIC_GNEISS,
            Lithology.VMS_MASSIVE_SULFIDE,
            Lithology.ALLUVIAL_GRAVEL)) {
      PetrologicSample material =
          query.resolve(
              province,
              sample(
                  province,
                  new Point3(0.0, 0.0, 0.0),
                  province.geometry().basementId(),
                  lithology,
                  new AgeKey(100.0, 0),
                  Overprint.PHYLLIC_ALTERATION));
      MaterialAssemblage expected =
          MaterialAssemblage.blend(
              material.primaryAssemblage(),
              phyllic.targetAssemblage(material.rock().geneticFamily()),
              phyllic.replacementPpm());

      assertEquals(expected, material.resolvedAssemblage(), lithology.name());
    }
  }

  @Test
  void solidSolutionStatesExposeBodyCompositionAndSurviveAlterationProjection() {
    MaterialQueryEngine query = Phase2World.create(7_701L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    RiftArcGeometry.PlutonPulse firstBody = province.geometry().plutonPulses().getFirst();
    RiftArcGeometry.PlutonPulse secondBody = province.geometry().plutonPulses().get(1);
    GeologicalSample first =
        sample(
            province,
            firstBody.center(),
            firstBody.id(),
            Lithology.GRANODIORITE_PULSE,
            firstBody.birthAge(),
            Overprint.NONE);
    GeologicalSample second =
        sample(
            province,
            secondBody.center(),
            secondBody.id(),
            Lithology.GRANODIORITE_PULSE,
            secondBody.birthAge(),
            Overprint.NONE);
    GeologicalSample altered =
        sample(
            province,
            firstBody.center(),
            firstBody.id(),
            Lithology.GRANODIORITE_PULSE,
            firstBody.birthAge(),
            Overprint.POTASSIC_ALTERATION);

    PetrologicSample firstMaterial = query.resolve(province, first);
    PetrologicSample secondMaterial = query.resolve(province, second);
    PetrologicSample alteredMaterial = query.resolve(province, altered);
    SolidSolutionState firstPlagioclase =
        solidSolution(
            firstMaterial.primarySolidSolutions(), "geological:solid_solution/plagioclase");
    SolidSolutionState secondPlagioclase =
        solidSolution(
            secondMaterial.primarySolidSolutions(), "geological:solid_solution/plagioclase");
    SolidSolutionState alteredPrimary =
        solidSolution(
            alteredMaterial.primarySolidSolutions(), "geological:solid_solution/plagioclase");
    SolidSolutionState alteredResolved =
        solidSolution(
            alteredMaterial.resolvedSolidSolutions(), "geological:solid_solution/plagioclase");

    assertEquals(firstPlagioclase, alteredPrimary);
    assertNotEquals(
        firstPlagioclase.endmemberMoleFractionsPpm(),
        secondPlagioclase.endmemberMoleFractionsPpm());
    assertTrue(alteredResolved.phaseModePpm() < alteredPrimary.phaseModePpm());
    for (String endmember : firstPlagioclase.endmemberMoleFractionsPpm().keySet()) {
      assertTrue(
          StrictMath.abs(
                  firstPlagioclase.endmemberMoleFractionsPpm().get(endmember)
                      - alteredResolved.endmemberMoleFractionsPpm().get(endmember))
              <= 2L);
    }
    assertEquals(8.0, firstPlagioclase.idealFormulaAtoms().get(ChemicalElement.O), 1.0e-12);
    assertTrue(firstPlagioclase.idealFormulaAtoms().get(ChemicalElement.NA) > 0.0);
    assertTrue(firstPlagioclase.idealFormulaAtoms().get(ChemicalElement.CA) > 0.0);
    assertEquals(
        MaterialAssemblage.SCALE,
        firstPlagioclase.bulkComposition().elementMassPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
  }

  @Test
  void sedimentaryMembersRetainNamedSourceToSinkAndDiageneticState() {
    MaterialQueryEngine query = Phase2World.create(42L);
    Province province =
        query
            .geology()
            .atlas()
            .provinceAt(new io.github.crunchybubbles.geological.model.Point2(0.0, 0.0));
    GeologicalSample geological =
        sample(
            province,
            new Point3(0.0, 0.0, 0.0),
            province.geometry().basin().packageId(),
            Lithology.MARINE_VOLCANICLASTIC,
            new AgeKey(250.0, 0),
            Overprint.NONE);

    PetrologicSample material = query.resolve(province, geological);

    assertTrue(material.sedimentaryState().isPresent());
    assertEquals(
        "submarine_volcanic_apron", material.sedimentaryState().orElseThrow().faciesClass());
    assertTrue(
        material
            .sedimentaryState()
            .orElseThrow()
            .sourceBodyIds()
            .contains(province.geometry().basementId()));
    assertTrue(
        material
            .sedimentaryState()
            .orElseThrow()
            .sourceBodyIds()
            .contains(province.proofIds().magmaLineageId()));
  }

  @Test
  void expandedSedimentaryClassesExposeDistinctFaciesAndDiagenesis() {
    MaterialQueryEngine query = Phase2World.create(4_104L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    for (Lithology lithology :
        List.of(Lithology.SILTSTONE, Lithology.LIMESTONE, Lithology.DOLOSTONE, Lithology.CHERT)) {
      PetrologicSample material =
          query.resolve(
              province,
              sample(
                  province,
                  new Point3(0.0, 0.0, 0.0),
                  province.geometry().basin().packageId(),
                  lithology,
                  new AgeKey(180.0, 0),
                  Overprint.NONE));
      var sedimentary = material.sedimentaryState().orElseThrow();
      String expectedFacies =
          switch (lithology) {
            case SILTSTONE -> "delta_front_to_offshore_transition";
            case LIMESTONE -> "carbonate_platform";
            case DOLOSTONE -> "dolomitized_carbonate_platform";
            case CHERT -> "marine_bedded_silica";
            default -> throw new AssertionError("unexpected fixture " + lithology);
          };
      assertEquals(expectedFacies, sedimentary.faciesClass());
      assertFalse(sedimentary.diagenesisClass().isBlank());
      assertTrue(sedimentary.sourceBodyIds().contains(province.geometry().basementId()));
    }
  }

  @Test
  void bandedIronFormationExposesSubtypeNeutralAncientRedoxState() {
    MaterialQueryEngine query = Phase2World.create(2_100L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample bif =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.BANDED_IRON_FORMATION,
                new AgeKey(2_100.0, 0),
                Overprint.NONE));

    var sedimentary = bif.sedimentaryState().orElseThrow();
    assertEquals("ancient_iron_silica_precipitation_basin", sedimentary.faciesClass());
    assertEquals("microcrystalline_banded", sedimentary.grainSizeClass());
    assertEquals("chemical_precipitate_redox_controlled", sedimentary.maturityClass());
    assertEquals("iron_oxide_carbonate_silica_recrystallization", sedimentary.diagenesisClass());
    assertTrue(sedimentary.sourceBodyIds().contains(province.geometry().basementId()));
    assertTrue(bif.resolvedComposition().elementMassPpm().get(ChemicalElement.FE) > 300_000L);
  }

  @Test
  void coalRetainsOrganicChemistryWhileLeavingRankToBurialHistory() {
    MaterialQueryEngine query = Phase2World.create(8_080L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample coal =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.COAL,
                new AgeKey(80.0, 0),
                Overprint.NONE));

    var sedimentary = coal.sedimentaryState().orElseThrow();
    assertEquals("buried_peat_mire", sedimentary.faciesClass());
    assertEquals("organic_bedded_with_clastic_partings", sedimentary.grainSizeClass());
    assertEquals("peat_derived_rank_unresolved", sedimentary.maturityClass());
    assertEquals("compaction_dewatering_and_burial_maturation", sedimentary.diagenesisClass());
    assertTrue(
        coal.primaryAssemblage().modesPpm().get("geological:constituent/coal_organic_matter")
            > 750_000L);
    assertTrue(coal.primaryComposition().elementMassPpm().get(ChemicalElement.C) > 400_000L);
    assertTrue(coal.primaryComposition().elementMassPpm().get(ChemicalElement.N) > 0L);
    assertTrue(coal.primarySolidSolutions().isEmpty());
  }

  @Test
  void evaporiteClassesExposePrecipitationSequenceAndPostDepositionalState() {
    MaterialQueryEngine query = Phase2World.create(6_171L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample sulfate =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.GYPSUM_ANHYDRITE_EVAPORITE,
                new AgeKey(160.0, 0),
                Overprint.NONE));
    PetrologicSample chloride =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.HALITE_POTASH_EVAPORITE,
                new AgeKey(159.0, 0),
                Overprint.NONE));

    assertEquals(
        "restricted_evaporite_margin", sulfate.sedimentaryState().orElseThrow().faciesClass());
    assertEquals(
        "gypsum_anhydrite_hydration_recrystallization",
        sulfate.sedimentaryState().orElseThrow().diagenesisClass());
    assertEquals(
        "restricted_evaporite_basin_center",
        chloride.sedimentaryState().orElseThrow().faciesClass());
    assertEquals(
        "late_stage_brine_precipitate", chloride.sedimentaryState().orElseThrow().maturityClass());
    assertEquals(
        "salt_recrystallization_dissolution_and_halokinesis",
        chloride.sedimentaryState().orElseThrow().diagenesisClass());
    assertTrue(
        chloride.resolvedComposition().elementMassPpm().get(ChemicalElement.CL)
            > sulfate.resolvedComposition().elementMassPpm().get(ChemicalElement.CL));
    assertEquals(
        MaterialAssemblage.SCALE,
        chloride.resolvedComposition().elementMassPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
  }

  @Test
  void peliticMetamorphicProductsRetainShaleProtolithAndProgressInGrade() {
    MaterialQueryEngine query = Phase2World.create(7_071L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample slate =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000601"),
                Lithology.SLATE_PHYLLITE,
                new AgeKey(420.0, 0),
                Overprint.NONE));
    PetrologicSample schist =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000602"),
                Lithology.MICA_SCHIST,
                new AgeKey(410.0, 0),
                Overprint.NONE));

    assertEquals("geological:rock/basin_shale", slate.metamorphism().protolithRockId());
    assertEquals("geological:rock/basin_shale", schist.metamorphism().protolithRockId());
    assertEquals(MetamorphicGrade.LOW, slate.metamorphism().grade());
    assertEquals(MetamorphicGrade.MEDIUM, schist.metamorphism().grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, slate.metamorphism().facies());
    assertEquals(MetamorphicFacies.AMPHIBOLITE, schist.metamorphism().facies());
    assertTrue(
        slate.metamorphism().maximumPeakTemperatureCelsius()
            < schist.metamorphism().maximumPeakTemperatureCelsius());
    assertTrue(
        slate.primaryAssemblage().modesPpm().get("geological:mineral/muscovite")
            > schist.primaryAssemblage().modesPpm().get("geological:mineral/muscovite"));
    assertTrue(slate.primaryAssemblage().modesPpm().containsKey("geological:mineral/graphite"));
    assertTrue(schist.primaryAssemblage().modesPpm().containsKey("geological:mineral/almandine"));
  }

  @Test
  void maficMetamorphicProductsRetainBasaltProtolithAndChangeAmphibolePhaseWithGrade() {
    MaterialQueryEngine query = Phase2World.create(7_272L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample greenschist =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000701"),
                Lithology.GREENSCHIST,
                new AgeKey(400.0, 0),
                Overprint.NONE));
    PetrologicSample amphibolite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000702"),
                Lithology.AMPHIBOLITE,
                new AgeKey(390.0, 0),
                Overprint.NONE));

    assertEquals("geological:rock/basaltic", greenschist.metamorphism().protolithRockId());
    assertEquals("geological:rock/basaltic", amphibolite.metamorphism().protolithRockId());
    assertEquals(MetamorphicGrade.LOW, greenschist.metamorphism().grade());
    assertEquals(MetamorphicGrade.MEDIUM, amphibolite.metamorphism().grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, greenschist.metamorphism().facies());
    assertEquals(MetamorphicFacies.AMPHIBOLITE, amphibolite.metamorphism().facies());
    assertTrue(
        greenschist.metamorphism().maximumPeakTemperatureCelsius()
            < amphibolite.metamorphism().maximumPeakTemperatureCelsius());
    assertTrue(
        greenschist.primarySolidSolutions().stream()
            .anyMatch(
                state ->
                    state.definitionId().equals("geological:solid_solution/calcic_amphibole")));
    assertFalse(
        greenschist.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/hornblende")));
    assertTrue(
        amphibolite.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/hornblende")));
    assertFalse(
        amphibolite.primarySolidSolutions().stream()
            .anyMatch(
                state ->
                    state.definitionId().equals("geological:solid_solution/calcic_amphibole")));
    assertTrue(
        amphibolite.primaryAssemblage().modesPpm().get("geological:mineral/anorthite")
            > greenschist.primaryAssemblage().modesPpm().get("geological:mineral/anorthite"));
  }

  @Test
  void granuliteRetainsBasaltProtolithWhileReplacingHydrousAmphiboleWithPyroxenes() {
    MaterialQueryEngine query = Phase2World.create(7_373L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample amphibolite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000702"),
                Lithology.AMPHIBOLITE,
                new AgeKey(390.0, 0),
                Overprint.NONE));
    PetrologicSample granulite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000703"),
                Lithology.GRANULITE,
                new AgeKey(380.0, 0),
                Overprint.NONE));

    assertEquals("geological:rock/basaltic", granulite.metamorphism().protolithRockId());
    assertEquals(MetamorphicGrade.HIGH, granulite.metamorphism().grade());
    assertEquals(MetamorphicFacies.GRANULITE, granulite.metamorphism().facies());
    assertTrue(
        amphibolite.metamorphism().maximumPeakTemperatureCelsius()
            < granulite.metamorphism().maximumPeakTemperatureCelsius());
    assertTrue(
        amphibolite.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/hornblende")));
    assertFalse(
        granulite.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/hornblende")));
    assertTrue(
        granulite.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/orthopyroxene")));
    assertTrue(
        granulite.primarySolidSolutions().stream()
            .anyMatch(
                state ->
                    state.definitionId().equals("geological:solid_solution/calcic_clinopyroxene")));
    assertEquals(
        0L, granulite.primaryComposition().elementMassPpm().getOrDefault(ChemicalElement.H, 0L));
  }

  @Test
  void quartziteAndMarbleQueriesPreserveTheirContrastingSedimentaryInheritance() {
    MaterialQueryEngine query = Phase2World.create(8_181L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample quartzite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000801"),
                Lithology.QUARTZITE,
                new AgeKey(370.0, 0),
                Overprint.NONE));
    PetrologicSample marble =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000802"),
                Lithology.MARBLE,
                new AgeKey(360.0, 0),
                Overprint.NONE));

    assertEquals("geological:rock/basin_sandstone", quartzite.metamorphism().protolithRockId());
    assertEquals("geological:rock/limestone", marble.metamorphism().protolithRockId());
    assertEquals(MetamorphicGrade.LOW, quartzite.metamorphism().grade());
    assertEquals(MetamorphicGrade.LOW, marble.metamorphism().grade());
    assertEquals(MetamorphicFacies.GREENSCHIST, quartzite.metamorphism().facies());
    assertEquals(MetamorphicFacies.GREENSCHIST, marble.metamorphism().facies());
    assertTrue(
        quartzite.primaryAssemblage().modesPpm().get("geological:mineral/quartz") >= 800_000L);
    assertTrue(
        marble.primaryAssemblage().modesPpm().get("geological:mineral/calcite")
                + marble.primaryAssemblage().modesPpm().get("geological:mineral/dolomite")
            >= 900_000L);
    assertTrue(
        quartzite.primaryComposition().elementMassPpm().get(ChemicalElement.SI)
            > marble.primaryComposition().elementMassPpm().get(ChemicalElement.SI));
    assertTrue(
        marble.primaryComposition().elementMassPpm().get(ChemicalElement.C)
            > quartzite.primaryComposition().elementMassPpm().get(ChemicalElement.C));
    assertTrue(
        quartzite.primarySolidSolutions().stream()
            .anyMatch(
                state -> state.definitionId().equals("geological:solid_solution/plagioclase")));
    assertTrue(marble.primarySolidSolutions().isEmpty());
  }

  @Test
  void serpentiniteQueryPreservesUltramaficInheritanceAndHydrationSignal() {
    MaterialQueryEngine query = Phase2World.create(8_182L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    PetrologicSample ultramafic =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000101"),
                Lithology.KOMATIITIC_ULTRAMAFIC,
                new AgeKey(2_700.0, 0),
                Overprint.NONE));
    PetrologicSample serpentinite =
        query.resolve(
            province,
            sample(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000901"),
                Lithology.SERPENTINITE,
                new AgeKey(350.0, 0),
                Overprint.NONE));

    assertEquals(
        "geological:rock/komatiitic_ultramafic", serpentinite.metamorphism().protolithRockId());
    assertEquals(MetamorphicGrade.LOW, serpentinite.metamorphism().grade());
    assertEquals(MetamorphicFacies.SUBGREENSCHIST, serpentinite.metamorphism().facies());
    assertEquals(MetamorphicPath.HYDROTHERMAL_HYDRATION, serpentinite.metamorphism().path());
    assertEquals(RockTexture.SERPENTINIZED_MESH, serpentinite.resolvedTexture());
    assertEquals(MaterialProcessClass.NONE, serpentinite.processClass());
    assertTrue(
        serpentinite.primaryComposition().elementMassPpm().get(ChemicalElement.H)
            > ultramafic.primaryComposition().elementMassPpm().getOrDefault(ChemicalElement.H, 0L));
    assertTrue(
        serpentinite.primaryComposition().density() < ultramafic.primaryComposition().density());
    assertEquals(serpentinite.primaryComposition(), serpentinite.resolvedComposition());
  }

  @Test
  void contactMetamorphismIsIsochemicalWhileHydrothermalAlterationCarriesTransfers() {
    MaterialQueryEngine query = Phase2World.create(99L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);

    GeologicalSample contact =
        sample(
            province,
            new Point3(0.0, 0.0, 0.0),
            province.geometry().basementId(),
            Lithology.GRANITIC_GNEISS,
            new AgeKey(1850.0, 0),
            Overprint.CONTACT_HORNFELS);
    PetrologicSample hornfels = query.resolve(province, contact);
    assertEquals(MetamorphicGrade.HIGH, hornfels.metamorphism().grade());
    assertEquals(MetamorphicFacies.HORNBLENDE_HORNFELS, hornfels.metamorphism().facies());
    assertEquals(MaterialProcessClass.ISOCHEMICAL_METAMORPHISM, hornfels.processClass());
    assertEquals(RockTexture.HORNFELSIC, hornfels.resolvedTexture());
    assertNotEquals(hornfels.rock().texture(), hornfels.resolvedTexture());
    assertTrue(hornfels.fluidState().isEmpty());
    assertTrue(hornfels.elementLedger().isIsochemical());
    assertEquals(
        province.geometry().aureoleId(),
        hornfels.materialProcessLedger().processId().orElseThrow());
    assertEquals(0L, hornfels.materialProcessLedger().exchangeMagnitudePpm());
    assertEquals(hornfels.primaryComposition(), hornfels.resolvedComposition());

    RiftArcGeometry.PlutonPulse stock = province.geometry().plutonPulses().getLast();
    GeologicalSample potassic =
        sample(
            province,
            stock.center(),
            stock.id(),
            stock.lithology(),
            stock.birthAge(),
            Overprint.POTASSIC_ALTERATION);
    PetrologicSample altered = query.resolve(province, potassic);
    assertEquals(MaterialProcessClass.HYDROTHERMAL_METASOMATISM, altered.processClass());
    assertEquals(FluidMedium.MAGMATIC_HYDROTHERMAL, altered.fluidState().orElseThrow().medium());
    assertEquals(SalinityClass.CONCENTRATED_BRINE, altered.fluidState().orElseThrow().salinity());
    assertEquals(3, altered.fluidState().orElseThrow().ligandCapacities().chloride());
    assertFalse(altered.elementLedger().isIsochemical());
    assertEquals(
        province.proofIds().porphyrySystemId(),
        altered.materialProcessLedger().processId().orElseThrow());
    assertFalse(altered.materialProcessLedger().eventIds().isEmpty());
    assertTrue(altered.materialProcessLedger().exchangeMagnitudePpm() > 0);
    assertTrue(
        altered.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.CU, 0L)
            > altered.primaryComposition().elementMassPpm().getOrDefault(ChemicalElement.CU, 0L));
    ChemicalElement[] elements = ChemicalElement.values();
    for (ChemicalElement element : elements) {
      assertEquals(
          altered.elementLedger().transferPpm().getOrDefault(element, 0L),
          altered.materialProcessLedger().netTransferPpm(element));
      assertEquals(
          altered.elementLedger().initialPpm().getOrDefault(element, 0L)
              + altered.elementLedger().transferPpm().getOrDefault(element, 0L),
          altered.elementLedger().resolvedPpm().getOrDefault(element, 0L));
    }
  }

  @Test
  void formedMineralSystemsExposeTypedSourceReservoirDebits() {
    MaterialQueryEngine query = Phase2World.create(8_675_309L);
    Province fertile =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);

    List<ElementReservoirLedger> ledgers = query.elementReservoirLedgers(fertile);

    assertEquals(3, ledgers.size());
    for (ElementReservoirLedger ledger : ledgers) {
      assertEquals(
          ledger.initialInventory(),
          ledger.transfers().stream().mapToLong(transfer -> transfer.amount()).sum());
      assertTrue(ledger.depositId().isPresent());
    }
    ElementReservoirLedger porphyry =
        ledgers.stream().filter(ledger -> ledger.element().equals("Cu")).findFirst().orElseThrow();
    assertEquals(fertile.proofIds().magmaLineageId(), porphyry.sourceReservoirId());
    assertEquals(fertile.proofIds().porphyryDepositId(), porphyry.depositId().orElseThrow());
    assertEquals(105_000L, porphyry.allocation("deposit"));
    assertEquals(850_000L, porphyry.allocation("retained_magma"));

    RiftArcGeometry.PlutonPulse stock = fertile.geometry().plutonPulses().getLast();
    GeologicalSample depositSample =
        new GeologicalSample(
            fertile.frame().toWorld(stock.center()),
            fertile.macroDomainId(),
            fertile.id(),
            stock.id(),
            stock.lithology(),
            stock.birthAge(),
            Overprint.POTASSIC_ALTERATION,
            false,
            List.of(porphyry.depositId().orElseThrow()));
    PetrologicSample mineralized = query.resolve(fertile, depositSample);
    assertTrue(
        mineralized.reservoirLedgers().stream()
            .anyMatch(ledger -> ledger.systemId().equals(porphyry.systemId())));

    Province barren =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    assertTrue(query.elementReservoirLedgers(barren).isEmpty());
  }

  @Test
  void publicMaterialQueriesAreReproducibleAcrossCacheEviction() {
    MaterialQueryEngine query = Phase2World.create(73_731L);
    Point3 point = new Point3(123.25, 64.5, -456.75);
    PetrologicSample expected = query.sample(point);

    query.sample(new Point3(80_000.25, -12.5, -95_000.75));
    query.clearCaches();

    assertEquals(expected, query.sample(point));
    assertEquals(expected, Phase2World.create(73_731L).sample(point));
  }

  @Test
  void petrologicColumnRunsExactlyMatchPointQueriesAtEveryBlockCenter() {
    MaterialQueryEngine query = Phase2World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Point2 contact =
        province
            .frame()
            .toWorld(
                new Point2(
                    province.geometry().porphyryCenter().x(),
                    province.geometry().porphyryCenter().z()));
    ColumnRequest request = new ColumnRequest(contact.x(), contact.z(), -64, 320);

    PetrologicColumnResult result = query.column(request);

    assertEquals(result.geology().runs().size(), result.materialEvaluations());
    assertTrue(result.materialEvaluations() < request.height());
    for (int y = request.minYInclusive(); y < request.maxYExclusive(); y++) {
      assertEquals(
          PetrologicState.from(query.sample(new Point3(request.x(), y + 0.5, request.z()))),
          result.stateAt(y),
          "petrologic interval crossed a transition at Y=" + y);
    }
    query.clearCaches();
    assertEquals(result, query.column(request));
    assertEquals(result, Phase2World.create(8_675_309L).column(request));
  }

  @Test
  void surfacePlacerCarriesSourceProvenanceAndTheClosedCoarseBudget() {
    MaterialQueryEngine query = Phase2World.create(8_675_309L);
    Province fertile =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Point2 placer = fertile.frame().toWorld(fertile.geometry().placerCenter());

    SurfacePetrologicSample surface = query.surface(placer);

    assertEquals(SurfaceMaterialKind.ALLUVIAL_PLACER, surface.context().kind());
    assertEquals(Lithology.ALLUVIAL_GRAVEL, surface.material().rock().lithology());
    assertEquals(fertile.proofIds().placerDepositId(), surface.context().depositId().orElseThrow());
    assertTrue(surface.context().sourceBodyIds().contains(fertile.proofIds().porphyryDepositId()));
    assertEquals("Au", surface.context().budgetElement().orElseThrow());
    assertEquals("phase0_fixed_units", surface.context().budgetUnit().orElseThrow());
    assertEquals(100_000L, surface.context().sourceInventoryFixedUnits());
    assertEquals(20_000L, surface.context().trappedInventoryFixedUnits());
    assertEquals(
        FluidMedium.METEORIC_WATER, surface.material().fluidState().orElseThrow().medium());
    assertEquals(
        fertile.proofIds().weatheringId(),
        surface.material().materialProcessLedger().processId().orElseThrow());
    assertFalse(surface.material().materialProcessLedger().eventIds().isEmpty());
    assertEquals(1, surface.material().reservoirLedgers().size());
    assertEquals(
        20_000L, surface.material().reservoirLedgers().getFirst().allocation("placer_trap"));
    assertTrue(
        surface
                .material()
                .resolvedComposition()
                .elementMassPpm()
                .getOrDefault(ChemicalElement.AU, 0L)
            > 0);

    query.clearCaches();
    assertEquals(surface, query.surface(placer));
  }

  @Test
  void barrenHydraulicTrapsCannotLeakAlluvialPlacerMaterial() {
    MaterialQueryEngine query = Phase2World.create(4_242L);
    Province barren =
        Phase1TestSupport.provinceWithGrammar(
            query.geology(), ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    Point2 rejectedTrap = barren.frame().toWorld(barren.geometry().placerCenter());

    SurfacePetrologicSample surface = query.surface(rejectedTrap);

    assertNotEquals(SurfaceMaterialKind.ALLUVIAL_PLACER, surface.context().kind());
    assertNotEquals(Lithology.ALLUVIAL_GRAVEL, surface.material().rock().lithology());
    assertTrue(surface.context().depositId().isEmpty());
    assertTrue(surface.context().budgetElement().isEmpty());
    assertTrue(surface.context().budgetUnit().isEmpty());
    assertEquals(0L, surface.context().sourceInventoryFixedUnits());
    assertEquals(0L, surface.context().trappedInventoryFixedUnits());
  }

  @Test
  void outcropAndRegolithRetainTheirBedrockSourceIdentity() {
    MaterialQueryEngine query = Phase2World.create(2_025L);
    SurfacePetrologicSample outcrop = null;
    SurfacePetrologicSample regolith = null;
    for (int z = -1_000; z <= 1_000 && (outcrop == null || regolith == null); z += 100) {
      for (int x = -1_000; x <= 1_000 && (outcrop == null || regolith == null); x += 100) {
        SurfacePetrologicSample candidate = query.surface(new Point2(x + 0.25, z - 0.25));
        if (candidate.context().kind() == SurfaceMaterialKind.BEDROCK_OUTCROP) {
          outcrop = candidate;
        } else if (candidate.context().kind() == SurfaceMaterialKind.IN_SITU_REGOLITH) {
          regolith = candidate;
        }
      }
    }

    SurfacePetrologicSample exposed = Objects.requireNonNull(outcrop, "no outcrop fixture found");
    SurfacePetrologicSample weathered =
        Objects.requireNonNull(regolith, "no regolith fixture found");
    for (SurfacePetrologicSample surface : List.of(exposed, weathered)) {
      assertEquals(surface.surface().bedrock().rockBodyId(), surface.context().materialBodyId());
      assertTrue(
          surface.context().sourceBodyIds().contains(surface.surface().bedrock().rockBodyId()));
      assertEquals(surface.surface().surfaceMaterial(), surface.material().rock().lithology());
      assertTrue(surface.context().depositId().isEmpty());
    }
    assertEquals(Overprint.WEATHERED_REGOLITH, weathered.material().geology().overprint());
  }

  @Test
  void midSlopeWeatheredMaterialFormsAYoungSourceLinkedColluvialMantle() {
    MaterialQueryEngine query = Phase2World.create(2_025L);
    SurfacePetrologicSample colluvium = null;
    Point2 fixture = null;
    for (int z = -1_500; z <= 1_500 && colluvium == null; z += 50) {
      for (int x = -1_500; x <= 1_500 && colluvium == null; x += 50) {
        Point2 point = new Point2(x + 0.25, z - 0.25);
        SurfacePetrologicSample candidate = query.surface(point);
        if (candidate.context().kind() == SurfaceMaterialKind.COLLUVIAL_MANTLE
            && candidate.context().sourceBodyIds().size() > 1) {
          colluvium = candidate;
          fixture = point;
        }
      }
    }

    SurfacePetrologicSample transported =
        Objects.requireNonNull(colluvium, "no colluvial mantle fixture found");
    Point2 point = Objects.requireNonNull(fixture, "no colluvial fixture point found");
    var phase1Surface = query.geology().surface(point);
    assertEquals(Lithology.SOIL_COLLUVIUM, transported.surface().surfaceMaterial());
    assertEquals(Lithology.SOIL_COLLUVIUM, transported.material().rock().lithology());
    assertEquals(Overprint.NONE, transported.surface().surfaceOverprint());
    assertNotEquals(Lithology.SOIL_COLLUVIUM, phase1Surface.surfaceMaterial());
    assertEquals(phase1Surface.fields(), transported.surface().fields());
    assertEquals(phase1Surface.bedrock(), transported.surface().bedrock());
    assertFalse(transported.surface().fields().outcrop());
    assertFalse(transported.surface().fields().drainage().channel());
    assertTrue(transported.surface().fields().slope() >= 0.10);
    assertTrue(transported.surface().fields().weatheringDepth() >= 4.0);
    assertTrue(transported.surface().fields().drainage().channelDistance() >= 32.0);
    assertEquals(new AgeKey(0.02, 0), transported.material().geology().formationAge());
    assertNotEquals(
        transported.surface().bedrock().rockBodyId(), transported.context().materialBodyId());
    ColluvialSourceMix sourceMix = transported.context().colluvialSourceMix().orElseThrow();
    assertEquals(sourceMix.sourceBodyIds(), transported.context().sourceBodyIds());
    assertTrue(sourceMix.sourceBodyIds().size() > 1);
    assertEquals(
        transported.surface().bedrock().rockBodyId(), sourceMix.localSource().sourceBodyId());
    assertEquals(
        transported.surface().bedrock().lithology(), sourceMix.localSource().sourceLithology());
    assertEquals(
        transported.surface().bedrock().overprint(), sourceMix.localSource().sourceOverprint());
    assertEquals(650_000L, sourceMix.sourceAssemblageFractionPpm());
    assertEquals(350_000L, sourceMix.weatheredMatrixFractionPpm());
    assertEquals(
        1.0,
        StrictMath.hypot(sourceMix.upslopeDirection().x(), sourceMix.upslopeDirection().z()),
        1.0e-12);
    double gradientX =
        (query.geology().surface(point.add(4.0, 0.0)).fields().elevation()
                - query.geology().surface(point.add(-4.0, 0.0)).fields().elevation())
            / 8.0;
    double gradientZ =
        (query.geology().surface(point.add(0.0, 4.0)).fields().elevation()
                - query.geology().surface(point.add(0.0, -4.0)).fields().elevation())
            / 8.0;
    double gradientLength = StrictMath.hypot(gradientX, gradientZ);
    assertEquals(
        new Point2(gradientX / gradientLength, gradientZ / gradientLength),
        sourceMix.upslopeDirection());
    assertEquals(
        List.of(0, 96, 192),
        sourceMix.sourceContributions().stream()
            .map(ColluvialSourceContribution::upslopeDistanceBlocks)
            .toList());
    assertEquals(
        List.of(350_000L, 200_000L, 100_000L),
        sourceMix.sourceContributions().stream()
            .map(ColluvialSourceContribution::assemblageFractionPpm)
            .toList());
    MaterialAssemblage genericMatrix =
        new BodyCompositionSampler(query.geology().atlas().identity())
            .sample(
                query.catalog().requireRock(Lithology.SOIL_COLLUVIUM),
                transported.context().materialBodyId());
    List<MaterialAssemblage.Share> shares = new ArrayList<>();
    shares.add(new MaterialAssemblage.Share(genericMatrix, sourceMix.weatheredMatrixFractionPpm()));
    List<SedimentGrainSize.Share> grainShares = new ArrayList<>();
    grainShares.add(
        new SedimentGrainSize.Share(
            query.catalog().requireRock(Lithology.SOIL_COLLUVIUM).sedimentYield(),
            sourceMix.weatheredMatrixFractionPpm()));
    for (ColluvialSourceContribution contribution : sourceMix.sourceContributions()) {
      Point2 expectedSourcePoint =
          point.add(
              sourceMix.upslopeDirection().x() * contribution.upslopeDistanceBlocks(),
              sourceMix.upslopeDirection().z() * contribution.upslopeDistanceBlocks());
      assertEquals(expectedSourcePoint, contribution.sourcePoint());
      Point2 sourcePoint = contribution.sourcePoint();
      GeologicalSample sourceGeology = query.geology().surface(sourcePoint).bedrock();
      Province sourceProvince = query.geology().atlas().provinceAt(sourcePoint);
      assertEquals(sourceProvince.id(), contribution.sourceProvinceId());
      assertEquals(sourceGeology.rockBodyId(), contribution.sourceBodyId());
      assertEquals(sourceGeology.lithology(), contribution.sourceLithology());
      assertEquals(sourceGeology.overprint(), contribution.sourceOverprint());
      shares.add(
          new MaterialAssemblage.Share(
              query.resolve(sourceProvince, sourceGeology).resolvedAssemblage(),
              contribution.assemblageFractionPpm()));
      grainShares.add(
          new SedimentGrainSize.Share(
              query.catalog().requireRock(contribution.sourceLithology()).sedimentYield(),
              contribution.assemblageFractionPpm()));
    }
    MaterialAssemblage expected = MaterialAssemblage.weightedBlend(shares);
    assertEquals(expected, transported.material().primaryAssemblage());
    assertEquals(expected, transported.material().resolvedAssemblage());
    assertNotEquals(genericMatrix, transported.material().primaryAssemblage());
    SedimentGrainSize expectedGrains = SedimentGrainSize.weightedBlend(grainShares);
    assertEquals(expectedGrains, sourceMix.textureState().grainSize());
    assertEquals(ColluvialTextureState.from(expectedGrains), sourceMix.textureState());
    assertEquals(SedimentSorting.UNSORTED_TO_POORLY_SORTED, sourceMix.textureState().sorting());
    assertEquals(ClastShape.ANGULAR_TO_SUBROUNDED, sourceMix.textureState().clastShape());
    ColluvialPhysicalState expectedPhysical =
        ColluvialPhysicalState.derive(
            sourceMix.textureState(),
            transported.material().rock().porosityDistribution(),
            transported.material().rock().permeabilityDistribution(),
            transported.material().rock().erodibilityDistribution());
    assertEquals(expectedPhysical, sourceMix.physicalState());
    assertEquals(expectedPhysical.porosityFraction(), transported.material().porosityFraction());
    assertEquals(expectedPhysical.permeabilityIndex(), transported.material().permeabilityIndex());
    assertEquals(expectedPhysical.erodibilityIndex(), transported.material().erodibilityIndex());
    assertEquals(
        query.catalog().composition(expected), transported.material().primaryComposition());
    assertTrue(transported.material().elementLedger().isIsochemical());
    assertTrue(transported.material().geology().depositIds().isEmpty());
    assertTrue(transported.material().reservoirLedgers().isEmpty());
    assertTrue(transported.context().depositId().isEmpty());
    assertTrue(transported.context().budgetElement().isEmpty());
    assertTrue(transported.context().budgetUnit().isEmpty());
    assertEquals(0L, transported.context().sourceInventoryFixedUnits());
    assertEquals(0L, transported.context().trappedInventoryFixedUnits());

    query.clearCaches();
    assertEquals(transported, query.surface(point));
    assertEquals(transported, Phase2World.create(2_025L).surface(point));
  }

  @Test
  void terrainDirectedColluviumResolvesEachCrossProvinceSourceWithItsOwner() {
    MaterialQueryEngine query = Phase2World.create(2_025L);
    Point2 point = new Point2(-2_199.75, -6_000.25);

    SurfacePetrologicSample transported = query.surface(point);

    assertEquals(SurfaceMaterialKind.COLLUVIAL_MANTLE, transported.context().kind());
    ColluvialSourceMix mix = transported.context().colluvialSourceMix().orElseThrow();
    assertEquals(
        2L,
        mix.sourceContributions().stream()
            .map(ColluvialSourceContribution::sourceProvinceId)
            .distinct()
            .count());
    for (ColluvialSourceContribution contribution : mix.sourceContributions()) {
      Province owner = query.geology().atlas().provinceAt(contribution.sourcePoint());
      assertEquals(owner.id(), contribution.sourceProvinceId());
      assertEquals(
          owner.id(), query.geology().surface(contribution.sourcePoint()).bedrock().provinceId());
    }
    assertNotEquals(
        mix.localSource().sourceProvinceId(),
        mix.sourceContributions().getLast().sourceProvinceId());
    assertEquals(transported, Phase2World.create(2_025L).surface(point));
  }

  @Test
  void colluvialSourceMixRequiresPositiveExactClosure() {
    Point2 sourcePoint = new Point2(10.0, 20.0);
    Point2 direction = new Point2(1.0, 0.0);
    StableId province = StableId.parse("00000000000000000000000000000a01");
    StableId source = StableId.parse("00000000000000000000000000000b01");
    ColluvialSourceContribution local =
        new ColluvialSourceContribution(
            sourcePoint, province, source, Lithology.GRANITIC_GNEISS, Overprint.NONE, 0, 650_000L);
    ColluvialTextureState texture =
        ColluvialTextureState.from(new SedimentGrainSize(300_000L, 300_000L, 400_000L));
    UnitIntervalDistribution distribution = new UnitIntervalDistribution(0.1, 0.4, 0.8);
    ColluvialPhysicalState physical =
        ColluvialPhysicalState.derive(texture, distribution, distribution, distribution);
    ColluvialTextureState otherTexture =
        ColluvialTextureState.from(new SedimentGrainSize(600_000L, 300_000L, 100_000L));
    ColluvialPhysicalState mismatchedPhysical =
        ColluvialPhysicalState.derive(otherTexture, distribution, distribution, distribution);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSourceMix(
                direction, List.of(local), 350_000L, texture, mismatchedPhysical));

    assertThrows(
        IllegalArgumentException.class,
        () -> new ColluvialSourceMix(direction, List.of(local), 349_999L, texture, physical));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSourceMix(
                direction,
                List.of(
                    new ColluvialSourceContribution(
                        sourcePoint.add(96.0, 0.0),
                        province,
                        source,
                        Lithology.GRANITIC_GNEISS,
                        Overprint.NONE,
                        96,
                        650_000L)),
                350_000L,
                texture,
                physical));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSourceMix(direction, List.of(local, local), 350_000L, texture, physical));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSourceMix(
                new Point2(0.5, 0.0), List.of(local), 350_000L, texture, physical));
    ColluvialSourceContribution misplaced =
        new ColluvialSourceContribution(
            sourcePoint.add(95.0, 0.0),
            province,
            source,
            Lithology.GRANITIC_GNEISS,
            Overprint.NONE,
            96,
            100_000L);
    ColluvialSourceContribution reducedLocal =
        new ColluvialSourceContribution(
            sourcePoint, province, source, Lithology.GRANITIC_GNEISS, Overprint.NONE, 0, 550_000L);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSourceMix(
                direction, List.of(reducedLocal, misplaced), 350_000L, texture, physical));
  }

  @Test
  void everyImplementedLithologyOverprintPairResolvesAndCloses() {
    MaterialQueryEngine query = Phase2World.create(1_337L);
    Province province = query.geology().atlas().provinceAt(new Point2(0.0, 0.0));
    assertEquals(
        Lithology.values().length * Overprint.values().length, query.resolvedRecipeCount());
    for (Lithology lithology : Lithology.values()) {
      for (Overprint overprint : Overprint.values()) {
        PetrologicSample material =
            query.resolve(
                province,
                sample(
                    province,
                    new Point3(0.0, 0.0, 0.0),
                    province.geometry().basementId(),
                    lithology,
                    new AgeKey(100.0, 0),
                    overprint));
        assertEquals(
            MaterialAssemblage.SCALE,
            material.resolvedAssemblage().modesPpm().values().stream()
                .mapToLong(Long::longValue)
                .sum(),
            lithology + "/" + overprint);
        assertEquals(
            MaterialAssemblage.SCALE,
            material.resolvedComposition().elementMassPpm().values().stream()
                .mapToLong(Long::longValue)
                .sum(),
            lithology + "/" + overprint);
      }
    }
  }

  private static GeologicalSample sample(
      Province province,
      Point3 localPoint,
      io.github.crunchybubbles.geological.determinism.StableId bodyId,
      Lithology lithology,
      AgeKey age,
      Overprint overprint) {
    Point3 worldPoint = province.frame().toWorld(localPoint);
    return new GeologicalSample(
        worldPoint,
        province.macroDomainId(),
        province.id(),
        bodyId,
        lithology,
        age,
        overprint,
        false,
        List.of());
  }

  private static SolidSolutionState solidSolution(
      List<SolidSolutionState> states, String definitionId) {
    return states.stream()
        .filter(state -> state.definitionId().equals(definitionId))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing solid solution " + definitionId));
  }
}
