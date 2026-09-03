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
import io.github.crunchybubbles.geological.petrology.AlterationContribution;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ColluvialAbsoluteMassBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialCohesionState;
import io.github.crunchybubbles.geological.petrology.ColluvialGrainDispersionState;
import io.github.crunchybubbles.geological.petrology.ColluvialGrainSourceShare;
import io.github.crunchybubbles.geological.petrology.ColluvialHorizonState;
import io.github.crunchybubbles.geological.petrology.ColluvialHydraulicState;
import io.github.crunchybubbles.geological.petrology.ColluvialMassScale;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialProductionState;
import io.github.crunchybubbles.geological.petrology.ColluvialRoutePolicy;
import io.github.crunchybubbles.geological.petrology.ColluvialSedimentBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkAllocation;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkDestination;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkState;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceCapacityLedger;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceClaim;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceClaimLedger;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceContribution;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceGrainShare;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceMix;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceUsage;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportPolicy;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcess;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessMix;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessStageMix;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessUsage;
import io.github.crunchybubbles.geological.petrology.ElementReservoirLedger;
import io.github.crunchybubbles.geological.petrology.MagmaLineageState;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MaterialProcessLedger;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.MetamorphicEventTiming;
import io.github.crunchybubbles.geological.petrology.ModalVariationAxis;
import io.github.crunchybubbles.geological.petrology.NonCrystallineConstituentDefinition;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.PrimaryMetamorphicDefinition;
import io.github.crunchybubbles.geological.petrology.ProcessFluidState;
import io.github.crunchybubbles.geological.petrology.RegionalMetamorphicState;
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
  private static final ColluvialMassScale REVIEW_MASS_SCALE =
      new ColluvialMassScale("kg", 2_500.0, 1.0);
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
    List<SurfacePetrologicSample> colluvialFixtures =
        findSurfaceMaterials(province, SurfaceMaterialKind.COLLUVIAL_MANTLE, 2);
    SurfacePetrologicSample colluvium = colluvialFixtures.getFirst();
    ColluvialSourceClaimLedger sourceClaimLedger =
        query.colluvialSourceClaimLedger(
            colluvialFixtures.stream().map(sample -> sample.surface().fields().point()).toList());
    Map<StableId, Long> reviewSourceCapacities = new TreeMap<>();
    for (ColluvialSourceClaimLedger.SourceAggregate aggregate :
        sourceClaimLedger.sourceAggregates()) {
      reviewSourceCapacities.put(aggregate.sourceBodyId(), aggregate.mobilizedFixedUnits() / 2L);
    }
    ColluvialSourceCapacityLedger sourceCapacityLedger =
        sourceClaimLedger.reconcileSourceCapacity(reviewSourceCapacities);

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
            "granitic-gneiss-partial-melting",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000705"),
                Lithology.GRANITIC_GNEISS,
                new AgeKey(430.0, 0),
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
            "limestone-contact-decarbonation",
            resolve(
                province,
                new Point3(0.0, 0.0, 0.0),
                StableId.parse("00000000000000000000000000000704"),
                Lithology.LIMESTONE,
                new AgeKey(1_850.0, 1),
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
                Phase2World.SCIENTIFIC_DIGEST,
                "phase1Geology",
                JsonWriter.object(
                    "worldSeed",
                    query.geology().atlas().identity().worldSeed(),
                    "modelVersion",
                    query.geology().atlas().identity().modelVersion(),
                    "scientificDigest",
                    query.geology().atlas().identity().scientificDigest(),
                    "dimensionProfile",
                    query.geology().atlas().identity().dimensionProfileId()),
                "phase2Material",
                JsonWriter.object(
                    "worldSeed",
                    query.materialIdentity().worldSeed(),
                    "modelVersion",
                    query.materialIdentity().modelVersion(),
                    "scientificDigest",
                    query.materialIdentity().scientificDigest(),
                    "dimensionProfile",
                    query.materialIdentity().dimensionProfileId())),
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
            "colluvialSourceClaimLedger",
            colluvialSourceClaimLedgerJson(sourceClaimLedger),
            "colluvialSourceCapacityLedger",
            colluvialSourceCapacityLedgerJson(sourceCapacityLedger),
            "surfacePlacerContext",
            surfaceContextJson(placer),
            "surfaceColluviumContext",
            surfaceContextJson(colluvium)));
    return output;
  }

  private SurfacePetrologicSample findSurfaceMaterial(Province province, SurfaceMaterialKind kind) {
    return findSurfaceMaterials(province, kind, 1).getFirst();
  }

  private List<SurfacePetrologicSample> findSurfaceMaterials(
      Province province, SurfaceMaterialKind kind, int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("surface-material fixture count must be positive");
    }
    List<SurfacePetrologicSample> fixtures = new ArrayList<>();
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
          if (fixtures.stream()
              .noneMatch(
                  fixture ->
                      fixture
                          .surface()
                          .fields()
                          .point()
                          .equals(candidate.surface().fields().point()))) {
            fixtures.add(candidate);
          }
          if (fixtures.size() >= count) {
            return List.copyOf(fixtures);
          }
        }
      }
    }
    throw new IllegalStateException(
        "reference province contains fewer than " + count + " " + kind + " fixtures");
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
        "initialUpslopeDirection",
        pointJson(mix.initialUpslopeDirection()),
        "maximumRouteDeflectionDegrees",
        mix.routePolicy().maximumDeflectionDegrees(),
        "routePolicy",
        colluvialRoutePolicyJson(mix.routePolicy()),
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
            "sortingDominanceIndex",
            mix.textureState().sortingDominanceIndex(),
            "support",
            mix.textureState().support().name(),
            "clastShape",
            mix.textureState().clastShape().name()),
        "dispersionState",
        colluvialGrainDispersionStateJson(mix.textureState().dispersionState()),
        "physicalState",
        colluvialPhysicalStateJson(mix.physicalState()),
        "cohesionState",
        colluvialCohesionStateJson(mix.physicalState().cohesionState()),
        "hydraulicState",
        colluvialHydraulicStateJson(mix.physicalState().hydraulicState()),
        "horizonState",
        colluvialHorizonStateJson(mix.horizonState()),
        "sedimentBudget",
        colluvialSedimentBudgetJson(mix.sedimentBudget()),
        "sinkDestinations",
        mix.sinkDestinations().stream()
            .map(MaterialReviewPacketGenerator::colluvialSinkDestinationJson)
            .toList(),
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

  private static Map<String, Object> colluvialRoutePolicyJson(ColluvialRoutePolicy policy) {
    return JsonWriter.object(
        "minimumSlope",
        policy.minimumSlope(),
        "minimumWeatheringDepth",
        policy.minimumWeatheringDepth(),
        "minimumChannelDistance",
        policy.minimumChannelDistance(),
        "gradientStepBlocks",
        policy.gradientStepBlocks(),
        "roughnessStencilRadiusBlocks",
        policy.roughnessStencilRadiusBlocks(),
        "pathReachLengthBlocks",
        policy.pathReachLengthBlocks(),
        "nearSourceDistanceBlocks",
        policy.nearSourceDistanceBlocks(),
        "farSourceDistanceBlocks",
        policy.farSourceDistanceBlocks(),
        "maximumDeflectionDegrees",
        policy.maximumDeflectionDegrees(),
        "weatheredMatrixCapacityFixedUnits",
        policy.weatheredMatrixCapacityFixedUnits(),
        "localSourceCapacityFixedUnits",
        policy.localSourceCapacityFixedUnits(),
        "nearSourceCapacityFixedUnits",
        policy.nearSourceCapacityFixedUnits(),
        "farSourceCapacityFixedUnits",
        policy.farSourceCapacityFixedUnits());
  }

  private static Map<String, Object> colluvialSinkDestinationJson(
      ColluvialSinkDestination destination) {
    return JsonWriter.object(
        "sinkRole",
        destination.sinkRole().name(),
        "sourceBodyId",
        destination.sourceBodyId().map(Object::toString).orElse(null),
        "upslopeDistanceBlocks",
        destination.upslopeDistanceBlocks(),
        "point",
        pointJson(destination.point()),
        "receivingProvinceId",
        destination.receivingProvinceId().toString(),
        "receivingBedrockBodyId",
        destination.receivingBedrockBodyId().toString(),
        "receivingSurfaceMaterial",
        destination.receivingSurfaceMaterial().name(),
        "receivingSurfaceOverprint",
        destination.receivingSurfaceOverprint().name(),
        "receivingBedrockLithology",
        destination.receivingBedrockLithology().name(),
        "receivingBedrockOverprint",
        destination.receivingBedrockOverprint().name(),
        "fixedUnits",
        destination.fixedUnits(),
        "unit",
        ColluvialSedimentBudget.NORMALIZED_MASS_UNIT);
  }

  private static Map<String, Object> colluvialSourceClaimLedgerJson(
      ColluvialSourceClaimLedger ledger) {
    return JsonWriter.object(
        "parcelCount",
        ledger.parcelCount(),
        "hasCrossParcelReuse",
        ledger.hasCrossParcelReuse(),
        "claimedCapacityFixedUnits",
        ledger.claimedCapacityFixedUnits(),
        "mobilizedFixedUnits",
        ledger.mobilizedFixedUnits(),
        "retainedFixedUnits",
        ledger.retainedFixedUnits(),
        "transportLossFixedUnits",
        ledger.transportLossFixedUnits(),
        "bypassedFixedUnits",
        ledger.bypassedFixedUnits(),
        "depositedFixedUnits",
        ledger.depositedFixedUnits(),
        "claimedCapacityGrainMass",
        colluvialGrainMassFixedJson(ledger.claimedCapacityGrainMass()),
        "mobilizedGrainMass",
        colluvialGrainMassFixedJson(ledger.mobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(ledger.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(ledger.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(ledger.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(ledger.depositedGrainMass()),
        "claims",
        ledger.claims().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceClaimJson)
            .toList(),
        "sourceAggregates",
        ledger.sourceAggregates().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceAggregateJson)
            .toList());
  }

  private static Map<String, Object> colluvialSourceCapacityLedgerJson(
      ColluvialSourceCapacityLedger ledger) {
    return JsonWriter.object(
        "sourceCapacityFixedUnits",
        ledger.sourceCapacityFixedUnits(),
        "claimedCapacityFixedUnits",
        ledger.claimedCapacityFixedUnits(),
        "requestedMobilizedFixedUnits",
        ledger.requestedMobilizedFixedUnits(),
        "allocatedMobilizedFixedUnits",
        ledger.allocatedMobilizedFixedUnits(),
        "unallocatedMobilizedFixedUnits",
        ledger.unallocatedMobilizedFixedUnits(),
        "retainedFixedUnits",
        ledger.retainedFixedUnits(),
        "transportLossFixedUnits",
        ledger.transportLossFixedUnits(),
        "bypassedFixedUnits",
        ledger.bypassedFixedUnits(),
        "depositedFixedUnits",
        ledger.depositedFixedUnits(),
        "claimedCapacityGrainMass",
        colluvialGrainMassFixedJson(ledger.claimedCapacityGrainMass()),
        "requestedMobilizedGrainMass",
        colluvialGrainMassFixedJson(ledger.requestedMobilizedGrainMass()),
        "allocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(ledger.allocatedMobilizedGrainMass()),
        "unallocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(ledger.unallocatedMobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(ledger.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(ledger.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(ledger.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(ledger.depositedGrainMass()),
        "remainingSourceCapacityFixedUnits",
        ledger.remainingSourceCapacityFixedUnits(),
        "hasDepletion",
        ledger.hasDepletion(),
        "claims",
        ledger.claims().stream()
            .map(MaterialReviewPacketGenerator::colluvialReconciledClaimJson)
            .toList(),
        "sourceAggregates",
        ledger.sourceAggregates().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceCapacityAggregateJson)
            .toList());
  }

  private static Map<String, Object> colluvialReconciledClaimJson(
      ColluvialSourceCapacityLedger.ReconciledClaim claim) {
    return JsonWriter.object(
        "parcelPoint",
        pointJson(claim.parcelPoint()),
        "parcelBodyId",
        claim.parcelBodyId().toString(),
        "sourceBodyId",
        claim.sourceBodyId().toString(),
        "upslopeDistanceBlocks",
        claim.upslopeDistanceBlocks(),
        "claimedCapacityFixedUnits",
        claim.claimedCapacityFixedUnits(),
        "requestedMobilizedFixedUnits",
        claim.requestedMobilizedFixedUnits(),
        "allocatedMobilizedFixedUnits",
        claim.allocatedMobilizedFixedUnits(),
        "unallocatedMobilizedFixedUnits",
        claim.unallocatedMobilizedFixedUnits(),
        "retainedFixedUnits",
        claim.retainedFixedUnits(),
        "transportLossFixedUnits",
        claim.transportLossFixedUnits(),
        "bypassedFixedUnits",
        claim.bypassedFixedUnits(),
        "depositedFixedUnits",
        claim.depositedFixedUnits(),
        "claimedCapacityGrainMass",
        colluvialGrainMassFixedJson(claim.claimedCapacityGrainMass()),
        "requestedMobilizedGrainMass",
        colluvialGrainMassFixedJson(claim.requestedMobilizedGrainMass()),
        "allocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(claim.allocatedMobilizedGrainMass()),
        "unallocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(claim.unallocatedMobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(claim.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(claim.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(claim.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(claim.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialSourceCapacityAggregateJson(
      ColluvialSourceCapacityLedger.SourceCapacityAggregate aggregate) {
    return JsonWriter.object(
        "sourceBodyId",
        aggregate.sourceBodyId().toString(),
        "sourceCapacityFixedUnits",
        aggregate.sourceCapacityFixedUnits(),
        "claimCount",
        aggregate.claimCount(),
        "claimedCapacityFixedUnits",
        aggregate.claimedCapacityFixedUnits(),
        "requestedMobilizedFixedUnits",
        aggregate.requestedMobilizedFixedUnits(),
        "allocatedMobilizedFixedUnits",
        aggregate.allocatedMobilizedFixedUnits(),
        "unallocatedMobilizedFixedUnits",
        aggregate.unallocatedMobilizedFixedUnits(),
        "retainedFixedUnits",
        aggregate.retainedFixedUnits(),
        "transportLossFixedUnits",
        aggregate.transportLossFixedUnits(),
        "bypassedFixedUnits",
        aggregate.bypassedFixedUnits(),
        "depositedFixedUnits",
        aggregate.depositedFixedUnits(),
        "claimedCapacityGrainMass",
        colluvialGrainMassFixedJson(aggregate.claimedCapacityGrainMass()),
        "requestedMobilizedGrainMass",
        colluvialGrainMassFixedJson(aggregate.requestedMobilizedGrainMass()),
        "allocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(aggregate.allocatedMobilizedGrainMass()),
        "unallocatedMobilizedGrainMass",
        colluvialGrainMassFixedJson(aggregate.unallocatedMobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(aggregate.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(aggregate.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(aggregate.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(aggregate.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialSourceClaimJson(ColluvialSourceClaim claim) {
    return JsonWriter.object(
        "parcelPoint",
        pointJson(claim.parcelPoint()),
        "parcelBodyId",
        claim.parcelBodyId().toString(),
        "sourceBodyId",
        claim.sourceBodyId().toString(),
        "upslopeDistanceBlocks",
        claim.upslopeDistanceBlocks(),
        "claimedCapacityFixedUnits",
        claim.claimedCapacityFixedUnits(),
        "mobilizedFixedUnits",
        claim.mobilizedFixedUnits(),
        "retainedFixedUnits",
        claim.retainedFixedUnits(),
        "transportLossFixedUnits",
        claim.transportLossFixedUnits(),
        "bypassedFixedUnits",
        claim.bypassedFixedUnits(),
        "depositedFixedUnits",
        claim.depositedFixedUnits(),
        "capacityGrainMass",
        colluvialGrainMassFixedJson(claim.capacityGrainMass()),
        "mobilizedGrainMass",
        colluvialGrainMassFixedJson(claim.mobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(claim.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(claim.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(claim.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(claim.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialSourceAggregateJson(
      ColluvialSourceClaimLedger.SourceAggregate aggregate) {
    return JsonWriter.object(
        "sourceBodyId",
        aggregate.sourceBodyId().toString(),
        "parcelCount",
        aggregate.parcelCount(),
        "trancheCount",
        aggregate.trancheCount(),
        "claimedCapacityFixedUnits",
        aggregate.claimedCapacityFixedUnits(),
        "mobilizedFixedUnits",
        aggregate.mobilizedFixedUnits(),
        "retainedFixedUnits",
        aggregate.retainedFixedUnits(),
        "transportLossFixedUnits",
        aggregate.transportLossFixedUnits(),
        "bypassedFixedUnits",
        aggregate.bypassedFixedUnits(),
        "depositedFixedUnits",
        aggregate.depositedFixedUnits(),
        "claimedCapacityGrainMass",
        colluvialGrainMassFixedJson(aggregate.claimedCapacityGrainMass()),
        "mobilizedGrainMass",
        colluvialGrainMassFixedJson(aggregate.mobilizedGrainMass()),
        "retainedGrainMass",
        colluvialGrainMassFixedJson(aggregate.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialGrainMassFixedJson(aggregate.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialGrainMassFixedJson(aggregate.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialGrainMassFixedJson(aggregate.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialGrainMassFixedJson(
      ColluvialSedimentBudget.GrainMass grainMass) {
    return JsonWriter.object(
        "gravelAndCoarserFixedUnits",
        grainMass.gravelAndCoarserFixedUnits(),
        "sandFixedUnits",
        grainMass.sandFixedUnits(),
        "finesFixedUnits",
        grainMass.finesFixedUnits(),
        "totalFixedUnits",
        grainMass.totalFixedUnits());
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

  private static Map<String, Object> colluvialCohesionStateJson(
      ColluvialCohesionState cohesionState) {
    return JsonWriter.object(
        "cohesionClass",
        cohesionState.cohesionClass().name(),
        "finesFraction",
        cohesionState.finesFraction(),
        "cohesionIndex",
        cohesionState.cohesionIndex(),
        "cohesionAdjustedErodibilityIndex",
        cohesionState.cohesionAdjustedErodibilityIndex());
  }

  private static Map<String, Object> colluvialHydraulicStateJson(
      ColluvialHydraulicState hydraulicState) {
    return JsonWriter.object(
        "hydraulicClass",
        hydraulicState.hydraulicClass().name(),
        "waterStorageIndex",
        hydraulicState.waterStorageIndex(),
        "infiltrationIndex",
        hydraulicState.infiltrationIndex(),
        "drainageIndex",
        hydraulicState.drainageIndex(),
        "runoffPartitionIndex",
        hydraulicState.runoffPartitionIndex());
  }

  private static Map<String, Object> colluvialGrainDispersionStateJson(
      ColluvialGrainDispersionState dispersionState) {
    return JsonWriter.object(
        "dispersionClass",
        dispersionState.dispersionClass().name(),
        "coarseSpreadIndex",
        dispersionState.coarseSpreadIndex(),
        "sandSpreadIndex",
        dispersionState.sandSpreadIndex(),
        "finesSpreadIndex",
        dispersionState.finesSpreadIndex(),
        "weightedSpreadIndex",
        dispersionState.weightedSpreadIndex());
  }

  private static Map<String, Object> colluvialHorizonStateJson(ColluvialHorizonState horizonState) {
    return JsonWriter.object(
        "profileClass",
        horizonState.profileClass().name(),
        "weatheringIndex",
        horizonState.weatheringIndex(),
        "weatheredMatrixFractionPpm",
        horizonState.weatheredMatrixFractionPpm(),
        "transportedSourceFractionPpm",
        horizonState.transportedSourceFractionPpm());
  }

  private static Map<String, Object> colluvialSedimentBudgetJson(ColluvialSedimentBudget budget) {
    return JsonWriter.object(
        "unit",
        budget.unit(),
        "grainTransportModel",
        budget.grainTransportModel().name(),
        "transportPolicy",
        colluvialTransportPolicyJson(budget.transportPolicy()),
        "absoluteMassCalibration",
        colluvialAbsoluteMassBudgetJson(budget.absoluteMass(REVIEW_MASS_SCALE)),
        "depositionSlope",
        budget.depositionSlope(),
        "sourceCapacityFixedUnits",
        budget.sourceCapacityFixedUnits(),
        "mobilizedInventoryFixedUnits",
        budget.mobilizedInventoryFixedUnits(),
        "retainedInventoryFixedUnits",
        budget.retainedInventoryFixedUnits(),
        "transportLossFixedUnits",
        budget.transportLossFixedUnits(),
        "bypassedInventoryFixedUnits",
        budget.bypassedInventoryFixedUnits(),
        "depositedInventoryFixedUnits",
        budget.depositedInventoryFixedUnits(),
        "capacityGrainMassFixedUnits",
        colluvialGrainMassJson(budget.capacityGrainMass()),
        "mobilizedGrainMassFixedUnits",
        colluvialGrainMassJson(budget.mobilizedGrainMass()),
        "retainedGrainMassFixedUnits",
        colluvialGrainMassJson(budget.retainedGrainMass()),
        "transportLossGrainMassFixedUnits",
        colluvialGrainMassJson(budget.transportLossGrainMass()),
        "bypassedGrainMassFixedUnits",
        colluvialGrainMassJson(budget.bypassedGrainMass()),
        "depositedGrainMassFixedUnits",
        colluvialGrainMassJson(budget.depositedGrainMass()),
        "depositedGrainSizePpm",
        sedimentGrainSizeJson(budget.depositedGrainSize()),
        "weatheredMatrixBalance",
        colluvialSedimentInputBalanceJson(
            budget.weatheredMatrixBalance(), budget.weatheredMatrixFractionPpm()),
        "sourceBalances",
        budget.sourceBalances().stream()
            .map(source -> colluvialSedimentSourceBalanceJson(budget, source))
            .toList(),
        "sourceUsages",
        budget.sourceUsages().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceUsageJson)
            .toList(),
        "sourceGrainShares",
        budget.sourceGrainShares().stream()
            .map(MaterialReviewPacketGenerator::colluvialSourceGrainShareJson)
            .toList(),
        "grainSourceShares",
        budget.grainSourceShares().stream()
            .map(MaterialReviewPacketGenerator::colluvialGrainSourceShareJson)
            .toList(),
        "transportProcessMix",
        colluvialTransportProcessMixJson(budget.transportProcessMix()),
        "transportProcessUsages",
        budget.transportProcessUsages().stream()
            .map(MaterialReviewPacketGenerator::colluvialTransportProcessUsageJson)
            .toList(),
        "transportProcessStageMix",
        colluvialTransportProcessStageMixJson(budget.transportProcessStageMix()));
  }

  private static Map<String, Object> colluvialAbsoluteMassBudgetJson(
      ColluvialAbsoluteMassBudget budget) {
    return JsonWriter.object(
        "calibrationKind",
        "caller_supplied_review_proof_scale",
        "massUnit",
        budget.massUnit(),
        "normalizedCapacityMass",
        budget.scale().normalizedCapacityMass(),
        "durationYears",
        budget.scale().durationYears(),
        "capacityMass",
        budget.capacityMass(),
        "mobilizedMass",
        budget.mobilizedMass(),
        "retainedMass",
        budget.retainedMass(),
        "transportLossMass",
        budget.transportLossMass(),
        "bypassedMass",
        budget.bypassedMass(),
        "depositedMass",
        budget.depositedMass(),
        "capacityRate",
        budget.capacityRate(),
        "mobilizedRate",
        budget.mobilizedRate(),
        "depositedRate",
        budget.depositedRate(),
        "capacityGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.capacityGrainMass()),
        "mobilizedGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.mobilizedGrainMass()),
        "retainedGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.retainedGrainMass()),
        "transportLossGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.transportLossGrainMass()),
        "bypassedGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.bypassedGrainMass()),
        "depositedGrainMass",
        colluvialAbsoluteGrainMassJson(budget, budget.depositedGrainMass()),
        "inputBalances",
        budget.inputBalances().stream()
            .map(
                input ->
                    JsonWriter.object(
                        "sourceBodyId",
                        input.sourceBodyId().map(StableId::toString).orElse(null),
                        "upslopeDistanceBlocks",
                        input.upslopeDistanceBlocks(),
                        "capacityFixedUnits",
                        input.capacityFixedUnits(),
                        "mobilizedFixedUnits",
                        input.mobilizedFixedUnits(),
                        "retainedFixedUnits",
                        input.retainedFixedUnits(),
                        "transportLossFixedUnits",
                        input.transportLossFixedUnits(),
                        "bypassedFixedUnits",
                        input.bypassedFixedUnits(),
                        "depositedFixedUnits",
                        input.depositedFixedUnits(),
                        "capacityMass",
                        budget.scale().mass(input.capacityFixedUnits()),
                        "mobilizedMass",
                        budget.scale().mass(input.mobilizedFixedUnits()),
                        "depositedMass",
                        budget.scale().mass(input.depositedFixedUnits()),
                        "capacityGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.capacityGrainMass()),
                        "mobilizedGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.mobilizedGrainMass()),
                        "retainedGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.retainedGrainMass()),
                        "transportLossGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.transportLossGrainMass()),
                        "bypassedGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.bypassedGrainMass()),
                        "depositedGrainMass",
                        colluvialAbsoluteGrainMassJson(budget, input.depositedGrainMass())))
            .toList());
  }

  private static Map<String, Object> colluvialAbsoluteGrainMassJson(
      ColluvialAbsoluteMassBudget budget, ColluvialSedimentBudget.GrainMass grainMass) {
    return JsonWriter.object(
        "gravelAndCoarserFixedUnits",
        grainMass.gravelAndCoarserFixedUnits(),
        "sandFixedUnits",
        grainMass.sandFixedUnits(),
        "finesFixedUnits",
        grainMass.finesFixedUnits(),
        "totalFixedUnits",
        grainMass.totalFixedUnits(),
        "gravelAndCoarserMass",
        budget.scale().mass(grainMass.gravelAndCoarserFixedUnits()),
        "sandMass",
        budget.scale().mass(grainMass.sandFixedUnits()),
        "finesMass",
        budget.scale().mass(grainMass.finesFixedUnits()),
        "totalMass",
        budget.scale().mass(grainMass.totalFixedUnits()));
  }

  private static Map<String, Object> colluvialTransportPolicyJson(ColluvialTransportPolicy policy) {
    return JsonWriter.object(
        "weatheringDepthReference",
        policy.weatheringDepthReference(),
        "slopeMobilityReference",
        policy.slopeMobilityReference(),
        "minimumSlopeMobility",
        policy.minimumSlopeMobility(),
        "minimumRunoffMobilityResponse",
        policy.minimumRunoffMobilityResponse(),
        "minimumTransportSlopeResponse",
        policy.minimumTransportSlopeResponse(),
        "minimumTransportRoughnessResponse",
        policy.minimumTransportRoughnessResponse(),
        "minimumTransportPathResponse",
        policy.minimumTransportPathResponse(),
        "minimumTransportRouteGradeResponse",
        policy.minimumTransportRouteGradeResponse(),
        "minimumTransportRunoffResponse",
        policy.minimumTransportRunoffResponse(),
        "gravelAndCoarserReferenceEFoldingDistanceBlocks",
        policy.gravelAndCoarserReferenceEFoldingDistanceBlocks(),
        "sandReferenceEFoldingDistanceBlocks",
        policy.sandReferenceEFoldingDistanceBlocks(),
        "finesReferenceEFoldingDistanceBlocks",
        policy.finesReferenceEFoldingDistanceBlocks(),
        "maximumBypassFraction",
        policy.maximumBypassFraction(),
        "hillslopeCreepResponse",
        policy.hillslopeCreepResponse(),
        "sheetwashResponse",
        policy.sheetwashResponse(),
        "dryRavelResponse",
        policy.dryRavelResponse());
  }

  private static Map<String, Object> colluvialSedimentSourceBalanceJson(
      ColluvialSedimentBudget budget, ColluvialSedimentBudget.SourceBalance source) {
    return JsonWriter.object(
        "sourceBodyId",
        source.sourceBodyId().toString(),
        "upslopeDistanceBlocks",
        source.upslopeDistanceBlocks(),
        "massBalance",
        colluvialSedimentInputBalanceJson(
            source.balance(),
            budget.sourceFractionPpm(source.sourceBodyId(), source.upslopeDistanceBlocks())));
  }

  private static Map<String, Object> colluvialSourceUsageJson(ColluvialSourceUsage usage) {
    return JsonWriter.object(
        "sourceBodyId",
        usage.sourceBodyId().toString(),
        "trancheCount",
        usage.trancheCount(),
        "claimedCapacityFixedUnits",
        usage.claimedCapacityFixedUnits(),
        "mobilizedFixedUnits",
        usage.mobilizedFixedUnits(),
        "retainedFixedUnits",
        usage.retainedFixedUnits(),
        "transportLossFixedUnits",
        usage.transportLossFixedUnits(),
        "bypassedFixedUnits",
        usage.bypassedFixedUnits(),
        "depositedFixedUnits",
        usage.depositedFixedUnits());
  }

  private static Map<String, Object> colluvialSourceGrainShareJson(
      ColluvialSourceGrainShare grainShare) {
    return JsonWriter.object(
        "sourceBodyId",
        grainShare.sourceBodyId().toString(),
        "upslopeDistanceBlocks",
        grainShare.upslopeDistanceBlocks(),
        "depositedFixedUnits",
        grainShare.depositedFixedUnits(),
        "depositedGrainMassFixedUnits",
        colluvialGrainMassJson(grainShare.depositedGrainMass()),
        "depositedGrainSizePpm",
        sedimentGrainSizeJson(grainShare.depositedGrainSize()));
  }

  private static Map<String, Object> colluvialGrainSourceShareJson(
      ColluvialGrainSourceShare grainShare) {
    return JsonWriter.object(
        "sourceRole",
        grainShare.sourceRole().name(),
        "sourceBodyId",
        grainShare.sourceBodyId().map(StableId::toString).orElse(null),
        "upslopeDistanceBlocks",
        grainShare.upslopeDistanceBlocks(),
        "depositedFixedUnits",
        grainShare.depositedFixedUnits(),
        "depositedGrainMassFixedUnits",
        colluvialGrainMassJson(grainShare.depositedGrainMass()),
        "gravelAndCoarserFractionPpm",
        grainShare.gravelAndCoarserFractionPpm(),
        "sandFractionPpm",
        grainShare.sandFractionPpm(),
        "finesFractionPpm",
        grainShare.finesFractionPpm());
  }

  private static Map<String, Object> colluvialTransportProcessMixJson(
      ColluvialTransportProcessMix processMix) {
    return JsonWriter.object(
        "dominantProcess",
        processMix.dominantProcess().name(),
        "hillslopeCreepFractionPpm",
        processMix.hillslopeCreepFractionPpm(),
        "sheetwashFractionPpm",
        processMix.sheetwashFractionPpm(),
        "dryRavelFractionPpm",
        processMix.dryRavelFractionPpm());
  }

  private static Map<String, Object> colluvialTransportProcessUsageJson(
      ColluvialTransportProcessUsage usage) {
    return JsonWriter.object(
        "processClass",
        usage.processClass().name(),
        "trancheCount",
        usage.trancheCount(),
        "capacityFixedUnits",
        usage.capacityFixedUnits(),
        "mobilizedFixedUnits",
        usage.mobilizedFixedUnits(),
        "retainedFixedUnits",
        usage.retainedFixedUnits(),
        "transportLossFixedUnits",
        usage.transportLossFixedUnits(),
        "bypassedFixedUnits",
        usage.bypassedFixedUnits(),
        "depositedFixedUnits",
        usage.depositedFixedUnits(),
        "capacityGrainMassFixedUnits",
        colluvialGrainMassJson(usage.capacityGrainMass()),
        "mobilizedGrainMassFixedUnits",
        colluvialGrainMassJson(usage.mobilizedGrainMass()),
        "retainedGrainMassFixedUnits",
        colluvialGrainMassJson(usage.retainedGrainMass()),
        "transportLossGrainMassFixedUnits",
        colluvialGrainMassJson(usage.transportLossGrainMass()),
        "bypassedGrainMassFixedUnits",
        colluvialGrainMassJson(usage.bypassedGrainMass()),
        "depositedGrainMassFixedUnits",
        colluvialGrainMassJson(usage.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialTransportProcessStageMixJson(
      ColluvialTransportProcessStageMix stageMix) {
    return JsonWriter.object(
        "capacity",
        colluvialTransportProcessMixJson(stageMix.capacity()),
        "mobilized",
        colluvialTransportProcessMixJson(stageMix.mobilized()),
        "arrived",
        colluvialTransportProcessMixJson(stageMix.arrived()),
        "deposited",
        colluvialTransportProcessMixJson(stageMix.deposited()));
  }

  private static Map<String, Object> colluvialSedimentInputBalanceJson(
      ColluvialSedimentBudget.InputBalance balance, long normalizedDepositFractionPpm) {
    return JsonWriter.object(
        "capacityFixedUnits",
        balance.input().capacityFixedUnits(),
        "weatheringDepth",
        balance.input().weatheringDepth(),
        "slope",
        balance.input().slope(),
        "erodibilityIndex",
        balance.input().erodibilityIndex(),
        "terrainRoughnessIndex",
        balance.input().terrainRoughnessIndex(),
        "runoffIndex",
        balance.input().runoffIndex(),
        "transportProcess",
        colluvialTransportProcessJson(balance.transportProcess()),
        "productionState",
        colluvialProductionStateJson(balance.productionState()),
        "sinkState",
        colluvialSinkStateJson(balance.sinkState()),
        "sinkAllocation",
        colluvialSinkAllocationJson(balance.sinkAllocation()),
        "terrainPath",
        colluvialTerrainPathJson(balance.input().terrainPath()),
        "transportPathResponse",
        balance.transportPathResponse(),
        "transportDistanceScale",
        balance.transportDistanceScale(),
        "transportEFoldingDistanceBlocks",
        colluvialGrainTransportLengthsJson(balance.grainTransportLengths()),
        "sedimentYieldPpm",
        sedimentGrainSizeJson(balance.input().sedimentYield()),
        "mobilizedFixedUnits",
        balance.mobilizedFixedUnits(),
        "retainedFixedUnits",
        balance.retainedFixedUnits(),
        "transportLossFixedUnits",
        balance.transportLossFixedUnits(),
        "bypassedFixedUnits",
        balance.bypassedFixedUnits(),
        "depositedFixedUnits",
        balance.depositedFixedUnits(),
        "normalizedDepositFractionPpm",
        normalizedDepositFractionPpm,
        "capacityGrainMassFixedUnits",
        colluvialGrainMassJson(balance.capacityGrainMass()),
        "mobilizedGrainMassFixedUnits",
        colluvialGrainMassJson(balance.mobilizedGrainMass()),
        "retainedGrainMassFixedUnits",
        colluvialGrainMassJson(balance.retainedGrainMass()),
        "transportLossGrainMassFixedUnits",
        colluvialGrainMassJson(balance.transportLossGrainMass()),
        "bypassedGrainMassFixedUnits",
        colluvialGrainMassJson(balance.bypassedGrainMass()),
        "depositedGrainMassFixedUnits",
        colluvialGrainMassJson(balance.depositedGrainMass()));
  }

  private static Map<String, Object> colluvialTransportProcessJson(
      ColluvialTransportProcess process) {
    return JsonWriter.object(
        "processClass",
        process.processClass().name(),
        "hillslopeCreepScore",
        process.hillslopeCreepScore(),
        "sheetwashScore",
        process.sheetwashScore(),
        "dryRavelScore",
        process.dryRavelScore(),
        "selectedScore",
        process.selectedScore(),
        "selectionMargin",
        process.selectionMargin());
  }

  private static Map<String, Object> colluvialProductionStateJson(
      ColluvialProductionState productionState) {
    return JsonWriter.object(
        "weatheringAvailability",
        productionState.weatheringAvailability(),
        "erodibilityResponse",
        productionState.erodibilityResponse(),
        "slopeMobilityResponse",
        productionState.slopeMobilityResponse(),
        "runoffMobilityResponse",
        productionState.runoffMobilityResponse(),
        "mobilizationPotential",
        productionState.mobilizationPotential(),
        "processResponse",
        productionState.processResponse(),
        "mobilizedFraction",
        productionState.mobilizedFraction(),
        "retainedFraction",
        productionState.retainedFraction(),
        "transportArrivalFraction",
        productionState.transportArrivalFraction(),
        "depositionFraction",
        productionState.depositionFraction(),
        "netDepositionFraction",
        productionState.netDepositionFraction());
  }

  private static Map<String, Object> colluvialSinkStateJson(ColluvialSinkState sinkState) {
    return JsonWriter.object(
        "transportLossSink",
        sinkState.transportLossSink().name(),
        "bypassSink",
        sinkState.bypassSink().name(),
        "transportLossFraction",
        sinkState.transportLossFraction(),
        "bypassFraction",
        sinkState.bypassFraction());
  }

  private static Map<String, Object> colluvialSinkAllocationJson(
      ColluvialSinkAllocation allocation) {
    return JsonWriter.object(
        "transportLossDistanceBlocks",
        allocation.transportLossDistanceBlocks(),
        "transportLossPoint",
        pointJson(allocation.transportLossPoint()),
        "bypassDistanceBlocks",
        allocation.bypassDistanceBlocks(),
        "bypassPoint",
        pointJson(allocation.bypassPoint()));
  }

  private static Map<String, Object> colluvialTerrainPathJson(
      ColluvialSedimentBudget.TerrainPath path) {
    return JsonWriter.object(
        "reachLengthBlocks",
        path.reachLengthBlocks(),
        "distanceBlocks",
        path.distanceBlocks(),
        "reachCount",
        path.reachCount(),
        "straightLineDistanceBlocks",
        path.straightLineDistanceBlocks(),
        "routedDistanceBlocks",
        path.routedDistanceBlocks(),
        "routeDirectnessIndex",
        path.routeDirectnessIndex(),
        "netUpslopeReliefBlocks",
        path.netUpslopeReliefBlocks(),
        "routeGradeIndex",
        path.routeGradeIndex(),
        "maximumDeflectionFromInitialDegrees",
        path.maximumDeflectionFromInitialDegrees(),
        "reachDecisions",
        path.reaches().stream()
            .map(
                reach ->
                    JsonWriter.object(
                        "upslopeDistanceBlocks",
                        reach.upslopeDistanceBlocks(),
                        "startPoint",
                        pointJson(reach.startPoint()),
                        "endPoint",
                        pointJson(reach.endPoint()),
                        "rawUpslopeDirection",
                        pointJson(reach.rawUpslopeDirection()),
                        "routedUpslopeDirection",
                        pointJson(reach.routedUpslopeDirection()),
                        "flatTerrainFallback",
                        reach.flatTerrainFallback(),
                        "deflectionClipped",
                        reach.deflectionClipped()))
            .toList(),
        "routeSamples",
        path.samples().stream()
            .map(
                sample ->
                    JsonWriter.object(
                        "upslopeDistanceBlocks",
                        sample.upslopeDistanceBlocks(),
                        "point",
                        pointJson(sample.point()),
                        "elevation",
                        sample.elevation()))
            .toList(),
        "cumulativeDownslopeReliefBlocks",
        path.cumulativeDownslopeReliefBlocks(),
        "cumulativeBarrierReliefBlocks",
        path.cumulativeBarrierReliefBlocks(),
        "descendingReachFraction",
        path.descendingReachFraction(),
        "downslopeContinuityIndex",
        path.downslopeContinuityIndex());
  }

  private static Map<String, Object> colluvialGrainTransportLengthsJson(
      ColluvialSedimentBudget.GrainTransportLengths lengths) {
    return JsonWriter.object(
        "gravelAndCoarser",
        lengths.gravelAndCoarserBlocks(),
        "sand",
        lengths.sandBlocks(),
        "fines",
        lengths.finesBlocks());
  }

  private static Map<String, Object> colluvialGrainMassJson(
      ColluvialSedimentBudget.GrainMass grainMass) {
    return JsonWriter.object(
        "gravelAndCoarser",
        grainMass.gravelAndCoarserFixedUnits(),
        "sand",
        grainMass.sandFixedUnits(),
        "fines",
        grainMass.finesFixedUnits(),
        "total",
        grainMass.totalFixedUnits());
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
            sample.metamorphism().eventIds().stream().map(Object::toString).toList(),
            "eventAges",
            sample.metamorphism().eventAges().stream()
                .map(age -> JsonWriter.object("ageMa", age.ageMa(), "ordinal", age.ordinal()))
                .toList(),
            "eventTimeline",
            sample.metamorphism().eventTimeline().stream()
                .map(this::metamorphicEventTimingJson)
                .toList(),
            "regionalState",
            sample
                .metamorphism()
                .regionalState()
                .map(this::regionalMetamorphicStateJson)
                .orElse(null),
            "processState",
            JsonWriter.object(
                "burialCurveClass",
                sample.metamorphism().processState().burialCurveClass().name(),
                "strainClass",
                sample.metamorphism().processState().strainClass().name(),
                "fluidAvailabilityClass",
                sample.metamorphism().processState().fluidAvailabilityClass().name(),
                "reactionProgressPpm",
                sample.metamorphism().processState().reactionProgressPpm(),
                "massTransferPpm",
                sample.metamorphism().processState().massTransferPpm(),
                "retrogressionPotentialPpm",
                sample.metamorphism().processState().retrogressionPotentialPpm(),
                "reactionState",
                JsonWriter.object(
                    "reactionMechanism",
                    sample.metamorphism().processState().reactionState().reactionMechanism().name(),
                    "retrogressionClass",
                    sample
                        .metamorphism()
                        .processState()
                        .reactionState()
                        .retrogressionClass()
                        .name(),
                    "dehydrationPpm",
                    sample.metamorphism().processState().reactionState().dehydrationPpm(),
                    "decarbonationPpm",
                    sample.metamorphism().processState().reactionState().decarbonationPpm(),
                    "partialMeltingPpm",
                    sample.metamorphism().processState().reactionState().partialMeltingPpm(),
                    "fluidContributions",
                    sample
                        .metamorphism()
                        .processState()
                        .reactionState()
                        .fluidContributions()
                        .stream()
                        .map(
                            contribution ->
                                JsonWriter.object(
                                    "fluidSpecies", contribution.fluidSpecies().name(),
                                    "direction", contribution.direction().name(),
                                    "amountPpm", contribution.amountPpm()))
                        .toList(),
                    "serpentinizationBalance",
                    JsonWriter.object(
                        "rockReactantPpm",
                        sample
                            .metamorphism()
                            .processState()
                            .reactionState()
                            .serpentinizationBalance()
                            .rockReactantPpm(),
                        "fluidInputPpm",
                        sample
                            .metamorphism()
                            .processState()
                            .reactionState()
                            .serpentinizationBalance()
                            .fluidInputPpm(),
                        "serpentineProductPpm",
                        sample
                            .metamorphism()
                            .processState()
                            .reactionState()
                            .serpentinizationBalance()
                            .serpentineProductPpm(),
                        "residualRockPpm",
                        sample
                            .metamorphism()
                            .processState()
                            .reactionState()
                            .serpentinizationBalance()
                            .residualRockPpm(),
                        "residualFluidPpm",
                        sample
                            .metamorphism()
                            .processState()
                            .reactionState()
                            .serpentinizationBalance()
                            .residualFluidPpm())))),
        "materialProcess",
        processJson(sample.materialProcessLedger()),
        "alterationContribution",
        alterationContributionJson(sample.alterationContribution()),
        "fluidState",
        sample.fluidState().map(this::fluidStateJson).orElse(null),
        "reservoirSystemIds",
        sample.reservoirLedgers().stream().map(ledger -> ledger.systemId().toString()).toList());
  }

  private Map<String, Object> regionalMetamorphicStateJson(RegionalMetamorphicState state) {
    return JsonWriter.object(
        "driverEventId",
        state.driverEventId().toString(),
        "eventAge",
        JsonWriter.object("ageMa", state.eventAge().ageMa(), "ordinal", state.eventAge().ordinal()),
        "grade",
        state.grade().name(),
        "facies",
        state.facies().name(),
        "path",
        state.path().name(),
        "peakTemperatureCelsius",
        state.peakTemperatureCelsius(),
        "peakPressureMpa",
        state.peakPressureMpa(),
        "strainClass",
        state.strainClass().name(),
        "intensityPpm",
        state.intensityPpm());
  }

  private Map<String, Object> metamorphicEventTimingJson(MetamorphicEventTiming timing) {
    return JsonWriter.object(
        "eventId",
        timing.eventId().toString(),
        "ageMa",
        timing.age().ageMa(),
        "ordinal",
        timing.age().ordinal());
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
        state.residualFluidPotential(),
        "differentiationState",
        JsonWriter.object(
            "tectonicSetting",
            state.differentiationState().tectonicSetting().name(),
            "sourceReservoirIds",
            state.differentiationState().sourceReservoirIds().stream()
                .map(Object::toString)
                .toList(),
            "meltingMechanism",
            state.differentiationState().meltingMechanism().name(),
            "sourceLithologyClass",
            state.differentiationState().sourceLithologyClass().name(),
            "meltFractionClass",
            state.differentiationState().meltFractionClass().name(),
            "sulfurSaturationHistory",
            state.differentiationState().sulfurSaturationHistory().name(),
            "crustalAssimilationClass",
            state.differentiationState().crustalAssimilationClass().name(),
            "differentiationPath",
            state.differentiationState().differentiationPath().name(),
            "cumulativeCrystalFractionPpm",
            state.differentiationState().cumulativeCrystalFractionPpm(),
            "residualMeltFractionPpm",
            state.differentiationState().residualMeltFractionPpm(),
            "residualFluidPotential",
            state.differentiationState().residualFluidPotential().name(),
            "fertilityTags",
            state.differentiationState().fertilityTags()));
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
        state.sourceBodyIds().stream().map(Object::toString).toList(),
        "basinState",
        JsonWriter.object(
            "basinType",
            state.basinState().basinType(),
            "depositionalEnvironment",
            state.basinState().depositionalEnvironment(),
            "waterBodyConnectivity",
            state.basinState().waterBodyConnectivity(),
            "waterDepthClass",
            state.basinState().waterDepthClass(),
            "accommodationTrend",
            state.basinState().accommodationTrend(),
            "salinityClass",
            state.basinState().salinityClass().name(),
            "redoxClass",
            state.basinState().redoxClass().name(),
            "clasticDilutionPpm",
            state.basinState().clasticDilutionPpm(),
            "carbonateProductivityPpm",
            state.basinState().carbonateProductivityPpm(),
            "sourceCatchmentIds",
            state.basinState().sourceCatchmentIds().stream().map(Object::toString).toList()),
        "inputBudget",
        JsonWriter.object(
            "clasticPpm",
            state.inputBudget().clasticPpm(),
            "volcanicPpm",
            state.inputBudget().volcanicPpm(),
            "carbonatePpm",
            state.inputBudget().carbonatePpm(),
            "organicPpm",
            state.inputBudget().organicPpm(),
            "chemicalPrecipitatePpm",
            state.inputBudget().chemicalPrecipitatePpm(),
            "evaporiticBrinePpm",
            state.inputBudget().evaporiticBrinePpm()),
        "reservoirContributions",
        state.reservoirContributions().stream()
            .map(
                contribution ->
                    JsonWriter.object(
                        "kind",
                        contribution.kind().name(),
                        "fractionPpm",
                        contribution.fractionPpm(),
                        "sourceBodyIds",
                        contribution.sourceBodyIds().stream().map(Object::toString).toList()))
            .toList(),
        "diagenesisState",
        JsonWriter.object(
            "compactionClass",
            state.diagenesisState().compactionClass().name(),
            "cementationClass",
            state.diagenesisState().cementationClass().name(),
            "dissolutionClass",
            state.diagenesisState().dissolutionClass().name(),
            "dolomitizationClass",
            state.diagenesisState().dolomitizationClass().name(),
            "organicMaturityClass",
            state.diagenesisState().organicMaturityClass().name(),
            "fluidSalinity",
            state.diagenesisState().fluidSalinity().name(),
            "retainedPorosityPpm",
            state.diagenesisState().retainedPorosityPpm()));
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

  private Map<String, Object> alterationContributionJson(AlterationContribution contribution) {
    return JsonWriter.object(
        "processId",
        contribution.processId().map(Object::toString).orElse(null),
        "processClass",
        contribution.processClass().name(),
        "eventIds",
        contribution.eventIds().stream().map(Object::toString).toList(),
        "eventAges",
        contribution.eventAges().stream()
            .map(age -> JsonWriter.object("ageMa", age.ageMa(), "ordinal", age.ordinal()))
            .toList(),
        "reactionProgressPpm",
        contribution.reactionProgressPpm(),
        "replacementPpm",
        contribution.replacementPpm(),
        "mineralModeDeltaPpm",
        contribution.mineralModeDeltaPpm(),
        "additionsPpm",
        elementMap(contribution.additionsPpm()),
        "removalsPpm",
        elementMap(contribution.removalsPpm()),
        "responseTexture",
        contribution.responseTexture().map(Enum::name).orElse(null),
        "fluidState",
        contribution.fluidState().map(this::fluidStateJson).orElse(null),
        "porosityMultiplier",
        contribution.porosityMultiplier(),
        "erodibilityDelta",
        contribution.erodibilityDelta());
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
        transfer.amount(),
        "processId",
        transfer.processId().map(Object::toString).orElse(null),
        "ageMa",
        transfer.age().map(AgeKey::ageMa).orElse(null),
        "ageOrdinal",
        transfer.age().map(AgeKey::ordinal).orElse(null),
        "confidencePpm",
        transfer.confidencePpm());
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
