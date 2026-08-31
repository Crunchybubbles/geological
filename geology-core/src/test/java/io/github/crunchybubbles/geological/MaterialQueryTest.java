package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MineralAssemblage;
import io.github.crunchybubbles.geological.petrology.PetrologicColumnResult;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.PetrologicState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MaterialQueryTest {
  @Test
  void phase2IdentityComposesFrozenPhase1ScienceWithMaterialContent() {
    assertEquals(
        "sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4",
        Phase1World.SCIENTIFIC_DIGEST);
    assertEquals(Phase1World.SCIENTIFIC_DIGEST, Phase2World.baseScientificSnapshot().digest());
    assertNotEquals(Phase1World.SCIENTIFIC_DIGEST, Phase2World.SCIENTIFIC_DIGEST);
    assertEquals(
        "sha256:d2283325312f05fd7274b30ea1b74af3715957dc7dd1e3c2f6e070a40a88afbd",
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
  void contactMetamorphismIsIsochemicalWhileHydrothermalAlterationCarriesTransfers() {
    MaterialQueryEngine query = Phase2World.create(99L);
    Province province =
        query
            .geology()
            .atlas()
            .provinceAt(new io.github.crunchybubbles.geological.model.Point2(0.0, 0.0));

    GeologicalSample contact =
        sample(
            province,
            new Point3(0.0, 0.0, 0.0),
            province.geometry().basementId(),
            Lithology.GRANITIC_GNEISS,
            new AgeKey(1850.0, 0),
            Overprint.CONTACT_HORNFELS);
    PetrologicSample hornfels = query.resolve(province, contact);
    assertEquals(MetamorphicFacies.HORNBLENDE_HORNFELS, hornfels.metamorphism().facies());
    assertEquals(MaterialProcessClass.ISOCHEMICAL_METAMORPHISM, hornfels.processClass());
    assertTrue(hornfels.elementLedger().isIsochemical());
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
    assertFalse(altered.elementLedger().isIsochemical());
    assertTrue(
        altered.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.CU, 0L)
            > altered.primaryComposition().elementMassPpm().getOrDefault(ChemicalElement.CU, 0L));
    ChemicalElement[] elements = ChemicalElement.values();
    for (ChemicalElement element : elements) {
      assertEquals(
          altered.elementLedger().initialPpm().getOrDefault(element, 0L)
              + altered.elementLedger().transferPpm().getOrDefault(element, 0L),
          altered.elementLedger().resolvedPpm().getOrDefault(element, 0L));
    }
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
            MineralAssemblage.SCALE,
            material.resolvedAssemblage().modesPpm().values().stream()
                .mapToLong(Long::longValue)
                .sum(),
            lithology + "/" + overprint);
        assertEquals(
            MineralAssemblage.SCALE,
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
}
