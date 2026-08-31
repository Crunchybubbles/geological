package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ElementReservoirLedger;
import io.github.crunchybubbles.geological.petrology.MaterialProcessLedger;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.ReservoirTransfer;
import io.github.crunchybubbles.geological.petrology.RockDefinition;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
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
    samples.add(sampleJson("surface-placer", placer.material()));

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
                "rockCount",
                query.catalog().rocks().size(),
                "overprintCount",
                query.catalog().alterations().size(),
                "rocks",
                query.catalog().rocks().stream().map(this::rockJson).toList()),
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
            JsonWriter.object(
                "kind",
                placer.context().kind().name(),
                "materialBodyId",
                placer.context().materialBodyId().toString(),
                "sourceBodyIds",
                placer.context().sourceBodyIds().stream().map(Object::toString).toList(),
                "depositId",
                placer.context().depositId().map(Object::toString).orElse(null),
                "budgetElement",
                placer.context().budgetElement().orElse(null),
                "budgetUnit",
                placer.context().budgetUnit().orElse(null),
                "sourceInventoryFixedUnits",
                placer.context().sourceInventoryFixedUnits(),
                "trappedInventoryFixedUnits",
                placer.context().trappedInventoryFixedUnits())));
    return output;
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
        "modalSpreadFraction",
        rock.modalSpreadFraction(),
        "centralModesPpm",
        rock.primaryAssemblage().modesPpm());
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
        "primaryModesPpm",
        sample.primaryAssemblage().modesPpm(),
        "resolvedModesPpm",
        sample.resolvedAssemblage().modesPpm(),
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
        "metamorphism",
        JsonWriter.object(
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
        "reservoirSystemIds",
        sample.reservoirLedgers().stream().map(ledger -> ledger.systemId().toString()).toList());
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

  private static Map<String, Object> pointJson(Point2 point) {
    return JsonWriter.object("x", point.x(), "z", point.z());
  }
}
