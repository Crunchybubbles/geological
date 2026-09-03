package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceContribution;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceMix;
import io.github.crunchybubbles.geological.petrology.ElementReservoirLedger;
import io.github.crunchybubbles.geological.petrology.MagmaLineageState;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MaterialProcessLedger;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.ModalVariationAxis;
import io.github.crunchybubbles.geological.petrology.NonCrystallineConstituentDefinition;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.PrimaryMetamorphicDefinition;
import io.github.crunchybubbles.geological.petrology.ProcessFluidState;
import io.github.crunchybubbles.geological.petrology.ReservoirTransfer;
import io.github.crunchybubbles.geological.petrology.RockDefinition;
import io.github.crunchybubbles.geological.petrology.SedimentGrainSize;
import io.github.crunchybubbles.geological.petrology.SedimentaryState;
import io.github.crunchybubbles.geological.petrology.SolidSolutionDefinition;
import io.github.crunchybubbles.geological.petrology.SolidSolutionState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Generates deterministic, human-reviewable Phase 2 material and reservoir state. */
final class MaterialReviewPacketGenerator {
  private final long seed;
  private final MaterialQueryEngine query;

  MaterialReviewPacketGenerator(long seed) {
    this.seed = seed;
    this.query = Phase2World.create(seed);
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    Province province = referenceProvince();
    SurfacePetrologicSample placer =
        query.surface(province.frame().toWorld(province.geometry().placerCenter()));
    SurfacePetrologicSample colluvium =
        findSurfaceMaterial(province, SurfaceMaterialKind.COLLUVIAL_MANTLE);

    List<Map<String, Object>> samples = new ArrayList<>();
    List<RiftArcGeometry.PlutonPulse> pulses = province.geometry().plutonPulses();
    for (int index = 0; index < pulses.size(); index++) {
      RiftArcGeometry.PlutonPulse pulse = pulses.get(index);
      samples.add(
          sampleJson(
              "pluton-pulse-" + index,
              resolve(
                  province,
                  pulse.center(),
                  pulse.id(),
                  pulse.lithology(),
                  pulse.birthAge(),
                  Overprint.NONE)));
    }
    samples.add(
        sampleJson(
            "komatiitic-ultramafic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000101"),
                Lithology.KOMATIITIC_ULTRAMAFIC,
                new AgeKey(2_700.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "basaltic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000102"),
                Lithology.BASALTIC,
                new AgeKey(250.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "gabbroic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000103"),
                Lithology.GABBROIC,
                new AgeKey(250.0, 1),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "andesitic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000104"),
                Lithology.ANDESITIC,
                new AgeKey(245.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "rhyolitic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000105"),
                Lithology.RHYOLITIC,
                new AgeKey(240.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "alkaline-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000106"),
                Lithology.ALKALINE,
                new AgeKey(235.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "carbonatitic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000107"),
                Lithology.CARBONATITIC,
                new AgeKey(230.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "kimberlitic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000108"),
                Lithology.KIMBERLITIC,
                new AgeKey(225.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "pyroclastic-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000109"),
                Lithology.PYROCLASTIC,
                new AgeKey(220.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "siltstone-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000201"),
                Lithology.SILTSTONE,
                new AgeKey(180.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "limestone-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000202"),
                Lithology.LIMESTONE,
                new AgeKey(180.0, 1),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "dolostone-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000203"),
                Lithology.DOLOSTONE,
                new AgeKey(170.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "chert-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000204"),
                Lithology.CHERT,
                new AgeKey(165.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "banded-iron-formation-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000401"),
                Lithology.BANDED_IRON_FORMATION,
                new AgeKey(2_100.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "gypsum-anhydrite-evaporite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000301"),
                Lithology.GYPSUM_ANHYDRITE_EVAPORITE,
                new AgeKey(160.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "halite-potash-evaporite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000302"),
                Lithology.HALITE_POTASH_EVAPORITE,
                new AgeKey(159.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "coal-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000501"),
                Lithology.COAL,
                new AgeKey(80.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "slate-phyllite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000601"),
                Lithology.SLATE_PHYLLITE,
                new AgeKey(420.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "mica-schist-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000602"),
                Lithology.MICA_SCHIST,
                new AgeKey(410.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "greenschist-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000701"),
                Lithology.GREENSCHIST,
                new AgeKey(400.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "amphibolite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000702"),
                Lithology.AMPHIBOLITE,
                new AgeKey(390.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "granulite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000703"),
                Lithology.GRANULITE,
                new AgeKey(380.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "quartzite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000801"),
                Lithology.QUARTZITE,
                new AgeKey(370.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "marble-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000802"),
                Lithology.MARBLE,
                new AgeKey(360.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "serpentinite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000901"),
                Lithology.SERPENTINITE,
                new AgeKey(350.0, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "laterite-bauxite-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a01"),
                Lithology.LATERITE_BAUXITE,
                new AgeKey(1.5, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "soil-colluvium-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a02"),
                Lithology.SOIL_COLLUVIUM,
                new AgeKey(0.02, 0),
                Overprint.NONE)));
    samples.add(
        sampleJson(
            "glacial-till-catalog",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000a03"),
                Lithology.GLACIAL_TILL,
                new AgeKey(0.015, 0),
                Overprint.NONE)));
    RiftArcGeometry.PlutonPulse stock = pulses.getLast();
    samples.add(
        sampleJson(
            "felsic-stock-potassic",
            resolve(
                province,
                stock.center(),
                stock.id(),
                stock.lithology(),
                stock.birthAge(),
                Overprint.POTASSIC_ALTERATION)));
    samples.add(
        sampleJson(
            "basement-contact-hornfels",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basementId(),
                Lithology.GRANITIC_GNEISS,
                new AgeKey(1_850.0, 0),
                Overprint.CONTACT_HORNFELS)));
    samples.add(
        sampleJson(
            "basin-sandstone-phyllic",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                province.geometry().basin().packageId(),
                Lithology.BASIN_SANDSTONE,
                province.geometry().basin().birthAge(),
                Overprint.PHYLLIC_ALTERATION)));
    samples.add(
        sampleJson(
            "vms-massive-sulfide-gossan",
            resolve(
                province,
                province.geometry().vmsCenter(),
                province.proofIds().vmsDepositId(),
                Lithology.VMS_MASSIVE_SULFIDE,
                new AgeKey(241.0, 0),
                Overprint.OXIDIZED_GOSSAN)));
    samples.add(sampleJson("surface-placer", placer.material()));
    samples.add(sampleJson("surface-colluvial-mantle", colluvium.material()));

    Path output = outputDirectory.resolve("phase2-material-review.json");
    JsonWriter.write(
        output,
        JsonWriter.object(
            "project",
            "Geological",
            "phase",
            "Phase 2 petrologic material-state review",
            "worldSeed",
            seed,
            "identity",
            JsonWriter.object(
                "modelVersion",
                Phase2World.MODEL_VERSION,
                "dimensionProfile",
                query.geology().atlas().profile().id(),
                "baseScientificDigest",
                Phase1World.SCIENTIFIC_DIGEST,
                "materialCatalogDigest",
                query.catalog().digest(),
                "compositeScientificDigest",
                Phase2World.SCIENTIFIC_DIGEST),
            "catalog",
            JsonWriter.object(
                "mineralCount",
                query.catalog().minerals().size(),
                "nonCrystallineConstituentCount",
                query.catalog().nonCrystallineConstituents().size(),
                "constituentCount",
                query.catalog().constituents().size(),
                "solidSolutionCount",
                query.catalog().solidSolutions().size(),
                "rockCount",
                query.catalog().rocks().size(),
                "overprintCount",
                query.catalog().alterations().size(),
                "rocks",
                query.catalog().rocks().stream().map(this::rockJson).toList(),
                "nonCrystallineConstituents",
                query.catalog().nonCrystallineConstituents().stream()
                    .map(MaterialReviewPacketGenerator::nonCrystallineConstituentJson)
                    .toList(),
                "solidSolutions",
                query.catalog().solidSolutions().stream()
                    .map(MaterialReviewPacketGenerator::solidSolutionDefinitionJson)
                    .toList(),
                "overprints",
                query.catalog().alterations().stream().map(this::alterationJson).toList()),
            "referenceProvince",
            JsonWriter.object(
                "id",
                province.id().toString(),
                "grammar",
                province.grammar().name(),
                "site",
                pointJson(province.site())),
            "representativeSamples",
            samples,
            "elementReservoirLedgers",
            query.elementReservoirLedgers(province).stream().map(this::reservoirJson).toList(),
            "surfacePlacerContext",
            surfaceContextJson(placer),
            "surfaceColluviumContext",
            surfaceContextJson(colluvium)));
    return output;
  }

  private SurfacePetrologicSample findSurfaceMaterial(Province province, SurfaceMaterialKind kind) {
    double extent = province.cellSize() * 0.45;
    double step = province.cellSize() / 40.0;
    for (double z = -extent; z <= extent; z += step) {
      for (double x = -extent; x <= extent; x += step) {
        SurfacePetrologicSample candidate =
            query.surface(province.frame().toWorld(new Point2(x + 0.25, z - 0.25)));
        if (candidate.context().kind() == kind
            && (kind != SurfaceMaterialKind.COLLUVIAL_MANTLE
                || candidate.context().sourceBodyIds().size() > 1)
            && candidate.surface().bedrock().provinceId().equals(province.id())) {
          return candidate;
        }
      }
    }
    throw new IllegalStateException("reference province contains no " + kind + " fixture");
  }

  private static Map<String, Object> surfaceContextJson(SurfacePetrologicSample surface) {
    return JsonWriter.object(
        "kind",
        surface.context().kind().name(),
        "point",
        pointJson(surface.surface().fields().point()),
        "slope",
        surface.surface().fields().slope(),
        "weatheringDepth",
        surface.surface().fields().weatheringDepth(),
        "channelDistance",
        surface.surface().fields().drainage().channelDistance(),
        "channel",
        surface.surface().fields().drainage().channel(),
        "outcrop",
        surface.surface().fields().outcrop(),
        "formationAgeMa",
        surface.material().geology().formationAge().ageMa(),
        "formationAgeOrdinal",
        surface.material().geology().formationAge().ordinal(),
        "underlyingBedrockBodyId",
        surface.surface().bedrock().rockBodyId().toString(),
        "materialBodyId",
        surface.context().materialBodyId().toString(),
        "sourceBodyIds",
        surface.context().sourceBodyIds().stream().map(Object::toString).toList(),
        "colluvialSourceMix",
        surface
            .context()
            .colluvialSourceMix()
            .map(MaterialReviewPacketGenerator::colluvialSourceMixJson)
            .orElse(null),
        "depositId",
        surface.context().depositId().map(Object::toString).orElse(null),
        "budgetElement",
        surface.context().budgetElement().orElse(null),
        "budgetUnit",
        surface.context().budgetUnit().orElse(null),
        "sourceInventoryFixedUnits",
        surface.context().sourceInventoryFixedUnits(),
        "trappedInventoryFixedUnits",
        surface.context().trappedInventoryFixedUnits());
  }

  private static Map<String, Object> colluvialSourceMixJson(ColluvialSourceMix mix) {
    return JsonWriter.object(
        "upslopeDirection",
        pointJson(mix.upslopeDirection()),
        "sourceAssemblageFractionPpm",
        mix.sourceAssemblageFractionPpm(),
        "weatheredMatrixFractionPpm",
        mix.weatheredMatrixFractionPpm(),
        "textureState",
        JsonWriter.object(
            "grainSizePpm",
            sedimentGrainSizeJson(mix.textureState().grainSize()),
            "sorting",
            mix.textureState().sorting().name(),
            "support",
            mix.textureState().support().name(),
            "clastShape",
            mix.textureState().clastShape().name()),
        "physicalState",
        colluvialPhysicalStateJson(mix.physicalState()),
        "sourceContributions",
        mix.sourceContributions().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceContributionJson)
            .toList());
  }

  private static Map<String, Object> colluvialSourceContributionJson(
      ColluvialSourceContribution contribution) {
    return JsonWriter.object(
        "sourcePoint",
        pointJson(contribution.sourcePoint()),
        "sourceProvinceId",
        contribution.sourceProvinceId().toString(),
        "upslopeDistanceBlocks",
        contribution.upslopeDistanceBlocks(),
        "sourceBodyId",
        contribution.sourceBodyId().toString(),
        "sourceLithology",
        contribution.sourceLithology().name(),
        "sourceOverprint",
        contribution.sourceOverprint().name(),
        "assemblageFractionPpm",
        contribution.assemblageFractionPpm());
  }

  private static Map<String, Object> colluvialPhysicalStateJson(
      ColluvialPhysicalState physicalState) {
    return JsonWriter.object(
        "porosityQuantile",
        physicalState.porosityQuantile(),
        "permeabilityQuantile",
        physicalState.permeabilityQuantile(),
        "erodibilityQuantile",
        physicalState.erodibilityQuantile(),
        "porosityFraction",
        physicalState.porosityFraction(),
        "permeabilityIndex",
        physicalState.permeabilityIndex(),
        "erodibilityIndex",
        physicalState.erodibilityIndex());
  }

  private Province referenceProvince() {
    Point2 origin = new Point2(0.0, 0.0);
    return query
        .geology()
        .atlas()
        .provincesIntersecting(
            new io.github.crunchybubbles.geological.model.Bounds2D(
                -8192.0, -8192.0, 8192.0, 8192.0))
        .stream()
        .filter(province -> province.grammar() == ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC)
        .min(
            Comparator.comparingDouble(
                    (Province province) -> province.site().squaredDistance(origin))
                .thenComparing(Province::id))
        .orElseThrow(() -> new IllegalStateException("review area contains no fertile province"));
  }

  private PetrologicSample resolve(
      Province province,
      Point3 localPoint,
      io.github.crunchybubbles.geological.determinism.StableId bodyId,
      Lithology lithology,
      AgeKey age,
      Overprint overprint) {
    Point3 worldPoint = province.frame().toWorld(localPoint);
    GeologicalSample geological =
        new GeologicalSample(
            worldPoint,
            province.macroDomainId(),
            province.id(),
            bodyId,
            lithology,
            age,
            overprint,
            false,
            List.of());
    return query.resolve(province, geological);
  }

  private Map<String, Object> rockJson(RockDefinition rock) {
    return JsonWriter.object(
        "id",
        rock.id(),
        "lithology",
        rock.lithology().name(),
        "geneticFamily",
        rock.geneticFamily().name(),
        "texture",
        rock.texture().name(),
        "primaryMetamorphism",
        rock.primaryMetamorphism()
            .map(MaterialReviewPacketGenerator::primaryMetamorphismJson)
            .orElse(null),
        "modalSpreadFraction",
        rock.modalSpreadFraction(),
        "modalVariationAxes",
        rock.modalVariationAxes().stream().map(this::modalVariationAxisJson).toList(),
        "sedimentYieldPpm",
        sedimentGrainSizeJson(rock.sedimentYield()),
        "porosityDistribution",
        distributionJson(rock.porosityDistribution()),
        "permeabilityDistribution",
        distributionJson(rock.permeabilityDistribution()),
        "erodibilityDistribution",
        distributionJson(rock.erodibilityDistribution()),
        "centralModesPpm",
        rock.primaryAssemblage().modesPpm());
  }

  private Map<String, Object> modalVariationAxisJson(ModalVariationAxis axis) {
    return JsonWriter.object("id", axis.id(), "loadingsPpm", axis.loadingsPpm());
  }

  private static Map<String, Object> sedimentGrainSizeJson(SedimentGrainSize grainSize) {
    return JsonWriter.object(
        "gravelAndCoarser",
        grainSize.gravelAndCoarserPpm(),
        "sand",
        grainSize.sandPpm(),
        "fines",
        grainSize.finesPpm());
  }

  private static Map<String, Object> primaryMetamorphismJson(
      PrimaryMetamorphicDefinition metamorphism) {
    return JsonWriter.object(
        "protolithRockId",
        metamorphism.protolithRockId(),
        "grade",
        metamorphism.grade().name(),
        "facies",
        metamorphism.facies().name(),
        "path",
        metamorphism.path().name(),
        "temperatureC",
        List.of(metamorphism.minimumTemperatureCelsius(), metamorphism.maximumTemperatureCelsius()),
        "pressureMpa",
        List.of(metamorphism.minimumPressureMpa(), metamorphism.maximumPressureMpa()));
  }

  private static Map<String, Object> distributionJson(UnitIntervalDistribution distribution) {
    return JsonWriter.object(
        "minimum",
        distribution.minimum(),
        "mode",
        distribution.mode(),
        "maximum",
        distribution.maximum());
  }

  private static Map<String, Object> solidSolutionDefinitionJson(
      SolidSolutionDefinition definition) {
    return JsonWriter.object(
        "id",
        definition.id(),
        "mixingModel",
        definition.mixingModel().name(),
        "endmemberIds",
        definition.endmemberIds());
  }

  private static Map<String, Object> nonCrystallineConstituentJson(
      NonCrystallineConstituentDefinition definition) {
    return JsonWriter.object(
        "id",
        definition.id(),
        "kind",
        definition.kind().name(),
        "elementMassPpm",
        elementMap(definition.elementMassPpm()),
        "density",
        definition.densityGramsPerCubicCentimeter(),
        "weatheringResistance",
        definition.weatheringResistance());
  }

  private Map<String, Object> alterationJson(AlterationDefinition alteration) {
    return JsonWriter.object(
        "overprint",
        alteration.overprint().name(),
        "processClass",
        alteration.processClass().name(),
        "fluidState",
        alteration.fluidState().map(this::fluidStateJson).orElse(null),
        "responseTexture",
        alteration.responseTexture().map(Enum::name).orElse(null),
        "replacementPpm",
        alteration.replacementPpm(),
        "targetRecipes",
        alteration.targetRecipes().stream()
            .map(
                recipe ->
                    JsonWriter.object(
                        "protolithFamilies",
                        recipe.protolithFamilies().stream().map(Enum::name).toList(),
                        "targetModesPpm",
                        recipe.targetAssemblage().modesPpm()))
            .toList());
  }

  private Map<String, Object> sampleJson(String label, PetrologicSample sample) {
    return JsonWriter.object(
        "label",
        label,
        "rockBodyId",
        sample.geology().rockBodyId().toString(),
        "lithology",
        sample.geology().lithology().name(),
        "overprint",
        sample.geology().overprint().name(),
        "rockDefinitionId",
        sample.rock().id(),
        "geneticFamily",
        sample.rock().geneticFamily().name(),
        "primaryTexture",
        sample.rock().texture().name(),
        "resolvedTexture",
        sample.resolvedTexture().name(),
        "primaryModesPpm",
        sample.primaryAssemblage().modesPpm(),
        "resolvedModesPpm",
        sample.resolvedAssemblage().modesPpm(),
        "primarySolidSolutions",
        sample.primarySolidSolutions().stream().map(this::solidSolutionStateJson).toList(),
        "resolvedSolidSolutions",
        sample.resolvedSolidSolutions().stream().map(this::solidSolutionStateJson).toList(),
        "primaryElementsPpm",
        elementMap(sample.primaryComposition().elementMassPpm()),
        "resolvedElementsPpm",
        elementMap(sample.resolvedComposition().elementMassPpm()),
        "density",
        sample.resolvedComposition().density(),
        "porosityFraction",
        sample.porosityFraction(),
        "permeabilityIndex",
        sample.permeabilityIndex(),
        "erodibilityIndex",
        sample.erodibilityIndex(),
        "magmaLineage",
        sample.magmaLineage().map(this::magmaLineageJson).orElse(null),
        "mantleCargo",
        sample.mantleCargo().map(this::mantleCargoJson).orElse(null),
        "sedimentaryState",
        sample.sedimentaryState().map(this::sedimentaryStateJson).orElse(null),
        "metamorphism",
        JsonWriter.object(
            "protolithRockId",
            sample.metamorphism().protolithRockId(),
            "grade",
            sample.metamorphism().grade().name(),
            "facies",
            sample.metamorphism().facies().name(),
            "path",
            sample.metamorphism().path().name(),
            "temperatureC",
            List.of(
                sample.metamorphism().minimumPeakTemperatureCelsius(),
                sample.metamorphism().maximumPeakTemperatureCelsius()),
            "pressureMpa",
            List.of(
                sample.metamorphism().minimumPeakPressureMpa(),
                sample.metamorphism().maximumPeakPressureMpa()),
            "eventIds",
            sample.metamorphism().eventIds().stream().map(Object::toString).toList()),
        "materialProcess",
        processJson(sample.materialProcessLedger()),
        "fluidState",
        sample.fluidState().map(this::fluidStateJson).orElse(null),
        "reservoirSystemIds",
        sample.reservoirLedgers().stream().map(ledger -> ledger.systemId().toString()).toList());
  }

  private Map<String, Object> magmaLineageJson(MagmaLineageState state) {
    return JsonWriter.object(
        "systemId",
        state.systemId().toString(),
        "pulseId",
        state.pulseId().toString(),
        "pulseOrder",
        state.pulseOrder(),
        "differentiationProgress",
        state.differentiationProgress(),
        "sourceReservoirClass",
        state.sourceReservoirClass(),
        "waterClass",
        state.waterClass(),
        "oxidationClass",
        state.oxidationClass(),
        "residualFluidPotential",
        state.residualFluidPotential());
  }

  private Map<String, Object> mantleCargoJson(MantleCargoState state) {
    return JsonWriter.object(
        "carrierBodyId",
        state.carrierBodyId().toString(),
        "sourceReservoirId",
        state.sourceReservoirId().map(Object::toString).orElse(null),
        "status",
        state.status().name(),
        "diamondMineralId",
        state.diamondMineralId(),
        "diamondGradePpbByMass",
        state.diamondGradePpbByMass(),
        "candidateIndicatorMineralIds",
        state.candidateIndicatorMineralIds());
  }

  private Map<String, Object> sedimentaryStateJson(SedimentaryState state) {
    return JsonWriter.object(
        "faciesClass",
        state.faciesClass(),
        "grainSizeClass",
        state.grainSizeClass(),
        "maturityClass",
        state.maturityClass(),
        "diagenesisClass",
        state.diagenesisClass(),
        "sourceBodyIds",
        state.sourceBodyIds().stream().map(Object::toString).toList());
  }

  private Map<String, Object> solidSolutionStateJson(SolidSolutionState state) {
    return JsonWriter.object(
        "definitionId",
        state.definitionId(),
        "mixingModel",
        state.mixingModel().name(),
        "phaseModePpm",
        state.phaseModePpm(),
        "endmemberVolumeFractionsPpm",
        state.endmemberVolumeFractionsPpm(),
        "endmemberMoleFractionsPpm",
        state.endmemberMoleFractionsPpm(),
        "idealFormulaAtoms",
        formulaMap(state.idealFormulaAtoms()),
        "bulkElementsPpm",
        elementMap(state.bulkComposition().elementMassPpm()),
        "density",
        state.bulkComposition().density(),
        "hardnessMohs",
        state.hardnessMohs(),
        "weatheringResistance",
        state.weatheringResistance());
  }

  private Map<String, Object> processJson(MaterialProcessLedger process) {
    return JsonWriter.object(
        "processId",
        process.processId().map(Object::toString).orElse(null),
        "processClass",
        process.processClass().name(),
        "eventIds",
        process.eventIds().stream().map(Object::toString).toList(),
        "additionsPpm",
        elementMap(process.additionsPpm()),
        "removalsPpm",
        elementMap(process.removalsPpm()),
        "normalizedExchangeMagnitudePpm",
        process.exchangeMagnitudePpm());
  }

  private Map<String, Object> fluidStateJson(ProcessFluidState state) {
    return JsonWriter.object(
        "medium",
        state.medium().name(),
        "redox",
        state.redox().name(),
        "acidity",
        state.acidity().name(),
        "salinity",
        state.salinity().name(),
        "sulfurState",
        state.sulfurState().name(),
        "integratedFluxClass",
        state.integratedFluxClass(),
        "ligandCapacities",
        JsonWriter.object(
            "chloride",
            state.ligandCapacities().chloride(),
            "reducedSulfur",
            state.ligandCapacities().reducedSulfur(),
            "carbonate",
            state.ligandCapacities().carbonate(),
            "fluorineBoron",
            state.ligandCapacities().fluorineBoron()));
  }

  private Map<String, Object> reservoirJson(ElementReservoirLedger ledger) {
    return JsonWriter.object(
        "systemId",
        ledger.systemId().toString(),
        "sourceReservoirId",
        ledger.sourceReservoirId().toString(),
        "depositId",
        ledger.depositId().map(Object::toString).orElse(null),
        "element",
        ledger.element(),
        "unit",
        ledger.unit(),
        "initialInventory",
        ledger.initialInventory(),
        "transfers",
        ledger.transfers().stream().map(this::reservoirTransferJson).toList());
  }

  private Map<String, Object> reservoirTransferJson(ReservoirTransfer transfer) {
    return JsonWriter.object(
        "role",
        transfer.role(),
        "sinkKind",
        transfer.sinkKind().name(),
        "sinkId",
        transfer.sinkId().map(Object::toString).orElse(null),
        "amount",
        transfer.amount());
  }

  private static Map<String, Long> elementMap(Map<ChemicalElement, Long> source) {
    TreeMap<String, Long> result = new TreeMap<>();
    source.forEach((element, amount) -> result.put(element.symbol(), amount));
    return result;
  }

  private static Map<String, Double> formulaMap(Map<ChemicalElement, Double> source) {
    TreeMap<String, Double> result = new TreeMap<>();
    source.forEach((element, value) -> result.put(element.symbol(), value));
    return result;
  }

  private static Map<String, Object> pointJson(Point2 point) {
    return JsonWriter.object("x", point.x(), "z", point.z());
  }
}
