package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.MacroDomain;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceAdjacency;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.FixedPointLedger;
import io.github.crunchybubbles.geological.mineral.GateEvidence;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.AtlasTile;
import io.github.crunchybubbles.geological.query.ColumnPlanBudget;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialRun;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.PointQueryTrace;
import io.github.crunchybubbles.geological.query.TileKey;
import io.github.crunchybubbles.geological.query.TileQueryEngine;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import io.github.crunchybubbles.geological.trace.ProvenanceStep;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

final class ReviewPacketGenerator {
  private static final int MAP_SIZE = 320;
  private static final double MAP_SPAN = 1800.0;

  private final long seed;
  private final GeologyQueryEngine query;

  ReviewPacketGenerator(long seed) {
    this.seed = seed;
    this.query = Phase1World.create(seed);
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    Province province = referenceProvince();
    MacroDomain macro = query.atlas().macroDomainAt(province.site());
    List<MineralSystemDecision> decisions = query.mineralDecisions(province);

    List<String> artifacts = new ArrayList<>();
    artifacts.addAll(renderMaps(outputDirectory, province));
    artifacts.add(renderCrossSection(outputDirectory, province));
    artifacts.addAll(renderGrammarMap(outputDirectory));
    artifacts.add(writeRegistrySnapshot(outputDirectory));
    artifacts.add(writeStratigraphicTrace(outputDirectory, province));
    artifacts.add(writeColumnRuns(outputDirectory, province));
    artifacts.add(writeDeformationTrace(outputDirectory, province));
    Path tracesDirectory = outputDirectory.resolve("traces");
    Files.createDirectories(tracesDirectory);
    for (MineralSystemDecision decision : decisions) {
      String fileName = traceFileName(decision);
      JsonWriter.write(tracesDirectory.resolve(fileName), decisionJson(decision));
      artifacts.add("traces/" + fileName);
    }

    List<Map<String, Object>> tileDigests = tileDigests(province);
    JsonWriter.write(outputDirectory.resolve("tile-digests.json"), tileDigests);
    artifacts.add("tile-digests.json");
    JsonWriter.write(
        outputDirectory.resolve("atlas-summary.json"),
        summaryJson(province, macro, decisions, tileDigests, artifacts));
    artifacts.add("atlas-summary.json");
    return outputDirectory;
  }

  private Province referenceProvince() {
    Point2 origin = new Point2(0.0, 0.0);
    return query
        .atlas()
        .provincesIntersecting(new Bounds2D(-8192.0, -8192.0, 8192.0, 8192.0))
        .stream()
        .filter(province -> province.grammar() == ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC)
        .min(
            Comparator.comparingDouble(
                    (Province province) -> province.site().squaredDistance(origin))
                .thenComparing(Province::id))
        .orElseThrow(() -> new IllegalStateException("review area contains no fertile province"));
  }

  private List<String> renderGrammarMap(Path outputDirectory) throws IOException {
    int size = 320;
    double span = 16_384.0;
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    Map<ProvinceGrammar, Integer> counts = new EnumMap<>(ProvinceGrammar.class);
    for (int pixelZ = 0; pixelZ < size; pixelZ++) {
      double worldZ = -span / 2.0 + (pixelZ + 0.5) * span / size;
      for (int pixelX = 0; pixelX < size; pixelX++) {
        double worldX = -span / 2.0 + (pixelX + 0.5) * span / size;
        ProvinceGrammar grammar = query.atlas().provinceAt(new Point2(worldX, worldZ)).grammar();
        counts.merge(grammar, 1, Integer::sum);
        Color color =
            switch (grammar) {
              case EXHUMED_FERTILE_RIFT_TO_ARC -> new Color(62, 142, 93);
              case BURIED_FERTILE_RIFT_TO_ARC -> new Color(80, 105, 161);
              case BARREN_DRY_RIFT_TO_ARC -> new Color(157, 104, 71);
            };
        image.setRGB(pixelX, pixelZ, color.getRGB());
      }
    }
    String fileName = "province-grammar-map.png";
    writePng(outputDirectory.resolve(fileName), image);
    String metadataName = "province-grammar-map.json";
    JsonWriter.write(
        outputDirectory.resolve(metadataName),
        JsonWriter.object(
            "centerX",
            0.0,
            "centerZ",
            0.0,
            "spanBlocks",
            span,
            "pixelsPerSide",
            size,
            "legendRgb",
            JsonWriter.object(
                ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC.name(), List.of(62, 142, 93),
                ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC.name(), List.of(80, 105, 161),
                ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC.name(), List.of(157, 104, 71)),
            "sampledPixelCounts",
            JsonWriter.object(
                ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC.name(),
                counts.getOrDefault(ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC, 0),
                ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC.name(),
                counts.getOrDefault(ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC, 0),
                ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC.name(),
                counts.getOrDefault(ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC, 0))));
    return List.of(fileName, metadataName);
  }

  private String writeColumnRuns(Path outputDirectory, Province province) throws IOException {
    Point3 contactLocal =
        province.geometry().pushForward(province.geometry().porphyryCenter(), new AgeKey(92.0, 0));
    Point3 contactWorld = province.frame().toWorld(contactLocal);
    ColumnQueryResult contact =
        query.column(new ColumnRequest(contactWorld.x(), contactWorld.z(), -64, 320));
    ColumnQueryResult background = mostAdaptiveColumn(province);
    String fileName = "column-runs.json";
    JsonWriter.write(
        outputDirectory.resolve(fileName),
        JsonWriter.object(
            "sampling",
            "integer block centers over half-open Y intervals",
            "columns",
            List.of(
                columnJson("porphyry-contact", contact), columnJson("background", background))));
    return fileName;
  }

  private String writeRegistrySnapshot(Path outputDirectory) throws IOException {
    String fileName = "registry-snapshot.json";
    Files.writeString(
        outputDirectory.resolve(fileName),
        Phase1World.scientificSnapshot().canonicalJson() + System.lineSeparator(),
        StandardCharsets.UTF_8);
    return fileName;
  }

  private String writeStratigraphicTrace(Path outputDirectory, Province province)
      throws IOException {
    Point2 center = province.geometry().stratigraphicPackage().center();
    Point2 margin =
        new Point2(
            center.x() + province.geometry().stratigraphicPackage().radiusU() * 0.92, center.z());
    String fileName = "stratigraphic-trace.json";
    JsonWriter.write(
        outputDirectory.resolve(fileName),
        JsonWriter.object(
            "unconformityId",
            province.geometry().unconformity().id().toString(),
            "unconformityAgeMa",
            province.geometry().unconformity().age().ageMa(),
            "weatheringThicknessBlocks",
            province.geometry().unconformity().weatheringThickness(),
            "packageId",
            province.geometry().stratigraphicPackage().id().toString(),
            "packageAgeMa",
            province.geometry().stratigraphicPackage().birthAge().ageMa(),
            "center",
            stratigraphicColumnJson(province, center),
            "onlapMargin",
            stratigraphicColumnJson(province, margin)));
    return fileName;
  }

  private Map<String, Object> stratigraphicColumnJson(Province province, Point2 local) {
    List<Double> formationBoundaries =
        province.geometry().stratigraphicPackage().formationBoundaries(local);
    return JsonWriter.object(
        "localX",
        local.x(),
        "localZ",
        local.z(),
        "unconformityElevation",
        province.geometry().unconformity().elevation(local),
        "formationBoundaryY",
        formationBoundaries,
        "presentBoundaryY",
        formationBoundaries.stream()
            .map(
                y ->
                    province
                        .geometry()
                        .pushForward(
                            new Point3(local.x(), y, local.z()),
                            province.geometry().stratigraphicPackage().birthAge())
                        .y())
            .toList());
  }

  private ColumnQueryResult mostAdaptiveColumn(Province province) {
    ColumnQueryResult best = null;
    for (int u = -900; u <= 900; u += 300) {
      for (int v = -900; v <= 900; v += 300) {
        Point2 world = province.frame().toWorld(new Point2(u, v));
        ColumnQueryResult candidate =
            query.column(new ColumnRequest(world.x(), world.z(), -64, 320));
        if (candidate.provinceId().equals(province.id())
            && (best == null
                || candidate.skippedPointEvaluations() > best.skippedPointEvaluations())) {
          best = candidate;
        }
      }
    }
    if (best == null) {
      throw new IllegalStateException("could not find an adaptive reference column");
    }
    return best;
  }

  private Map<String, Object> columnJson(String label, ColumnQueryResult result) {
    return JsonWriter.object(
        "label", label,
        "x", result.request().x(),
        "z", result.request().z(),
        "minYInclusive", result.request().minYInclusive(),
        "maxYExclusive", result.request().maxYExclusive(),
        "provinceId", result.provinceId().toString(),
        "pointEvaluations", result.pointEvaluations(),
        "skippedPointEvaluations", result.skippedPointEvaluations(),
        "complexity",
            JsonWriter.object(
                "candidates", result.complexity().candidates(),
                "transitions", result.complexity().transitions(),
                "pointEvaluations", result.complexity().pointEvaluations(),
                "materialRuns", result.complexity().materialRuns(),
                "phase1ReviewBudgetViolations",
                    result.complexity().violations(ColumnPlanBudget.PHASE1_REVIEW)),
        "intervalProof",
            JsonWriter.object(
                "method", result.intervalProof().method(),
                "transitionElevations", result.intervalProof().transitionElevations(),
                "splitYCoordinates", result.intervalProof().splitYCoordinates(),
                "provenUniformIntervals", result.intervalProof().provenUniformIntervals()),
        "candidates", result.candidates().stream().map(this::candidateJson).toList(),
        "runs", result.runs().stream().map(this::runJson).toList());
  }

  private Map<String, Object> runJson(MaterialRun run) {
    return JsonWriter.object(
        "minYInclusive", run.minYInclusive(),
        "maxYExclusive", run.maxYExclusive(),
        "rockBodyId", run.state().rockBodyId().toString(),
        "lithology", run.state().lithology().name(),
        "formationAgeMa", run.state().formationAge().ageMa(),
        "overprint", run.state().overprint().name(),
        "faultDamageZone", run.state().faultDamageZone(),
        "depositIds", run.state().depositIds().stream().map(Object::toString).toList());
  }

  private String writeDeformationTrace(Path outputDirectory, Province province) throws IOException {
    Point3 presentLocal =
        province.geometry().pushForward(province.geometry().vmsCenter(), new AgeKey(241.0, 0));
    PointQueryTrace trace = query.trace(province.frame().toWorld(presentLocal));
    String fileName = "deformation-trace.json";
    JsonWriter.write(
        outputDirectory.resolve(fileName),
        JsonWriter.object(
            "provinceGrammar", trace.provinceGrammar().name(),
            "selectedRockBodyId", trace.sample().rockBodyId().toString(),
            "selectedLithology", trace.sample().lithology().name(),
            "bodyAgeMa", trace.sample().formationAge().ageMa(),
            "presentWorldPoint", pointJson(trace.sample().point()),
            "presentLocalPoint", pointJson(trace.presentLocalPoint()),
            "afterFaultPullback", pointJson(trace.afterFaultPullback()),
            "formationPoint", pointJson(trace.formationPoint()),
            "reconstructedPresentPoint", pointJson(trace.reconstructedPresentPoint()),
            "roundTripResidualBlocks", trace.roundTripResidual(),
            "requiredMaximumResidualBlocks", 1.0 / 256.0,
            "candidates", trace.candidates().stream().map(this::candidateJson).toList()));
    return fileName;
  }

  private Map<String, Object> candidateJson(SpatialCandidate candidate) {
    return JsonWriter.object(
        "id", candidate.id().toString(),
        "kind", candidate.kind().name(),
        "birthAgeMa", candidate.birthAge().ageMa(),
        "affectsColumnState", candidate.affectsColumnState(),
        "bounds",
            JsonWriter.object(
                "minX", candidate.bounds().minX(),
                "minY", candidate.bounds().minY(),
                "minZ", candidate.bounds().minZ(),
                "maxX", candidate.bounds().maxX(),
                "maxY", candidate.bounds().maxY(),
                "maxZ", candidate.bounds().maxZ()));
  }

  private List<String> renderMaps(Path outputDirectory, Province province) throws IOException {
    BufferedImage geology = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
    BufferedImage terrain = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
    BufferedImage weathering = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
    BufferedImage systems = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
    double minimumX = province.site().x() - MAP_SPAN / 2.0;
    double minimumZ = province.site().z() - MAP_SPAN / 2.0;

    for (int pixelZ = 0; pixelZ < MAP_SIZE; pixelZ++) {
      double worldZ = minimumZ + (pixelZ + 0.5) * MAP_SPAN / MAP_SIZE;
      for (int pixelX = 0; pixelX < MAP_SIZE; pixelX++) {
        double worldX = minimumX + (pixelX + 0.5) * MAP_SPAN / MAP_SIZE;
        SurfaceSample sample = query.surface(new Point2(worldX, worldZ));
        geology.setRGB(pixelX, pixelZ, geologyColor(sample).getRGB());
        terrain.setRGB(pixelX, pixelZ, terrainColor(sample).getRGB());
        weathering.setRGB(pixelX, pixelZ, weatheringColor(sample).getRGB());
        systems.setRGB(pixelX, pixelZ, systemColor(sample).getRGB());
      }
    }

    writePng(outputDirectory.resolve("bedrock-geology.png"), geology);
    writePng(outputDirectory.resolve("terrain-drainage.png"), terrain);
    writePng(outputDirectory.resolve("weathering-outcrop.png"), weathering);
    writePng(outputDirectory.resolve("mineral-systems.png"), systems);

    BufferedImage overview =
        new BufferedImage(MAP_SIZE * 2, MAP_SIZE * 2, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = overview.createGraphics();
    try {
      graphics.drawImage(geology, 0, 0, null);
      graphics.drawImage(terrain, MAP_SIZE, 0, null);
      graphics.drawImage(weathering, 0, MAP_SIZE, null);
      graphics.drawImage(systems, MAP_SIZE, MAP_SIZE, null);
    } finally {
      graphics.dispose();
    }
    writePng(outputDirectory.resolve("overview.png"), overview);
    return List.of(
        "bedrock-geology.png",
        "terrain-drainage.png",
        "weathering-outcrop.png",
        "mineral-systems.png",
        "overview.png");
  }

  private String renderCrossSection(Path outputDirectory, Province province) throws IOException {
    int width = 512;
    int panelHeight = 190;
    int separator = 4;
    BufferedImage image =
        new BufferedImage(width, panelHeight * 2 + separator, BufferedImage.TYPE_INT_RGB);
    Arrays.fill(
        ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData(),
        new Color(24, 28, 35).getRGB());
    double[] sectionV = {
      province.geometry().vmsCenter().z(), province.geometry().porphyryCenter().z()
    };
    double minimumY = -100.0;
    double maximumY = 230.0;
    double halfWidth = 900.0;
    for (int panel = 0; panel < sectionV.length; panel++) {
      for (int pixelX = 0; pixelX < width; pixelX++) {
        double localU = -halfWidth + (pixelX + 0.5) * 2.0 * halfWidth / width;
        Point2 worldHorizontal = province.frame().toWorld(new Point2(localU, sectionV[panel]));
        SurfaceSample surface = query.surface(worldHorizontal);
        for (int pixelY = 0; pixelY < panelHeight; pixelY++) {
          double worldY = maximumY - (pixelY + 0.5) * (maximumY - minimumY) / panelHeight;
          Color color;
          if (worldY > surface.fields().elevation()) {
            color = worldY <= 63.0 ? new Color(38, 91, 145) : new Color(184, 218, 235);
          } else {
            GeologicalSample sample =
                query.sample(
                    province, new Point3(worldHorizontal.x(), worldY, worldHorizontal.z()));
            color = applyOverprint(lithologyColor(sample.lithology()), sample.overprint());
            if (sample.faultDamageZone()) {
              color = blend(color, new Color(45, 39, 48), 0.45);
            }
          }
          image.setRGB(pixelX, panel * (panelHeight + separator) + pixelY, color.getRGB());
        }
      }
    }
    String fileName = "cross-section-rift-to-arc.png";
    writePng(outputDirectory.resolve(fileName), image);
    return fileName;
  }

  private List<Map<String, Object>> tileDigests(Province province) {
    TileQueryEngine tiles = new TileQueryEngine(query);
    long originX = (long) StrictMath.floor(province.site().x()) - 512;
    long originZ = (long) StrictMath.floor(province.site().z()) - 512;
    List<Map<String, Object>> result = new ArrayList<>();
    for (int tileZ = 0; tileZ < 2; tileZ++) {
      for (int tileX = 0; tileX < 2; tileX++) {
        TileKey key = new TileKey(originX + tileX * 512L, originZ + tileZ * 512L, 32, 16);
        AtlasTile tile = tiles.query(key);
        result.add(
            JsonWriter.object(
                "originX", key.originX(),
                "originZ", key.originZ(),
                "intervals", key.intervals(),
                "spacing", key.spacing(),
                "sha256", tile.digest()));
      }
    }
    return result;
  }

  private Map<String, Object> summaryJson(
      Province province,
      MacroDomain macro,
      List<MineralSystemDecision> decisions,
      List<Map<String, Object>> tileDigests,
      List<String> artifacts) {
    return JsonWriter.object(
        "project",
        "Geological",
        "phase",
        "Phase 1 authored-registry, property-validation, and chunk-measurement increment",
        "worldSeed",
        seed,
        "modelVersion",
        Phase1World.MODEL_VERSION,
        "scientificDigest",
        Phase1World.SCIENTIFIC_DIGEST,
        "dimensionProfile",
        query.atlas().profile().id(),
        "macroDomain",
        JsonWriter.object(
            "id", macro.id().toString(),
            "homeCell",
                cellJson(macro.homeCell().level(), macro.homeCell().x(), macro.homeCell().z()),
            "crustClass", macro.crustClass().name(),
            "basementAgeMa", macro.basementAgeMa(),
            "adjacentIds", macro.adjacentDomainIds().stream().map(Object::toString).toList()),
        "province",
        JsonWriter.object(
            "id", province.id().toString(),
            "homeCell",
                cellJson(
                    province.homeCell().level(), province.homeCell().x(), province.homeCell().z()),
            "site", pointJson(province.site()),
            "grammar", province.grammar().name(),
            "adjacency", province.adjacency().stream().map(this::adjacencyJson).toList()),
        "chronicle",
        province.chronicle().events().stream().map(this::eventJson).toList(),
        "mineralSystems",
        decisions.stream().map(this::decisionSummaryJson).toList(),
        "tileDigests",
        tileDigests,
        "artifacts",
        artifacts.stream().sorted().toList());
  }

  private Map<String, Object> decisionJson(MineralSystemDecision decision) {
    return JsonWriter.object(
        "candidateId", decision.candidateId().toString(),
        "modelId", decision.modelId(),
        "status", decision.status().name(),
        "deposit",
            decision.deposit() == null
                ? null
                : JsonWriter.object(
                    "id", decision.deposit().id().toString(),
                    "systemId", decision.deposit().mineralSystemId().toString(),
                    "type", decision.deposit().type().name(),
                    "formationAgeMa", decision.deposit().formationAge().ageMa(),
                    "center", pointJson(decision.deposit().center()),
                    "bounds",
                        JsonWriter.object(
                            "minX", decision.deposit().bounds().minX(),
                            "minZ", decision.deposit().bounds().minZ(),
                            "maxX", decision.deposit().bounds().maxX(),
                            "maxZ", decision.deposit().bounds().maxZ()),
                    "sourceIds",
                        decision.deposit().sourceIds().stream().map(Object::toString).toList(),
                    "intensityProxy", decision.deposit().intensityProxy()),
        "gates", decision.gates().stream().map(this::gateJson).toList(),
        "provenance", decision.provenance().stream().map(this::provenanceJson).toList(),
        "ledger", decision.ledger() == null ? null : ledgerJson(decision.ledger()));
  }

  private Map<String, Object> decisionSummaryJson(MineralSystemDecision decision) {
    return JsonWriter.object(
        "candidateId", decision.candidateId().toString(),
        "modelId", decision.modelId(),
        "status", decision.status().name(),
        "depositId", decision.deposit() == null ? null : decision.deposit().id().toString(),
        "traceFile", "traces/" + traceFileName(decision));
  }

  private Map<String, Object> gateJson(GateEvidence gate) {
    return JsonWriter.object(
        "gate", gate.gate(),
        "status", gate.status().name(),
        "explanation", gate.explanation(),
        "upstreamObjectIds", gate.upstreamObjectIds().stream().map(Object::toString).toList());
  }

  private Map<String, Object> provenanceJson(ProvenanceStep step) {
    return JsonWriter.object(
        "order", step.order(),
        "process", step.process(),
        "explanation", step.explanation(),
        "inputIds", step.inputIds().stream().map(Object::toString).toList(),
        "outputIds", step.outputIds().stream().map(Object::toString).toList());
  }

  private static Map<String, Object> ledgerJson(FixedPointLedger ledger) {
    return JsonWriter.object(
        "element", ledger.element(),
        "unit", ledger.unit(),
        "sourceAmount", ledger.sourceAmount(),
        "allocations", ledger.allocations());
  }

  private Map<String, Object> eventJson(GeologicalEvent event) {
    return JsonWriter.object(
        "id", event.id().toString(),
        "type", event.type().name(),
        "ageMa", event.age().ageMa(),
        "ordinal", event.age().ordinal(),
        "description", event.description(),
        "inputs", event.inputs().stream().map(Object::toString).toList(),
        "outputs", event.outputs().stream().map(Object::toString).toList());
  }

  private Map<String, Object> adjacencyJson(ProvinceAdjacency adjacency) {
    return JsonWriter.object(
        "neighborId", adjacency.neighborId().toString(),
        "boundaryType", adjacency.boundaryType().name());
  }

  private static Map<String, Object> cellJson(String level, long x, long z) {
    return JsonWriter.object("level", level, "x", x, "z", z);
  }

  private static Map<String, Object> pointJson(Point2 point) {
    return JsonWriter.object("x", point.x(), "z", point.z());
  }

  private static Map<String, Object> pointJson(Point3 point) {
    return JsonWriter.object("x", point.x(), "y", point.y(), "z", point.z());
  }

  private static String traceFileName(MineralSystemDecision decision) {
    String model = decision.modelId().substring(decision.modelId().indexOf(':') + 1);
    return decision.status().name().toLowerCase(Locale.ROOT).replace('_', '-')
        + "-"
        + model
        + "-"
        + decision.candidateId().toString().substring(0, 8)
        + ".json";
  }

  private static Color geologyColor(SurfaceSample sample) {
    Color base = lithologyColor(sample.bedrock().lithology());
    Color result = applyOverprint(base, sample.bedrock().overprint());
    return sample.bedrock().faultDamageZone() ? blend(result, Color.BLACK, 0.52) : result;
  }

  private static Color terrainColor(SurfaceSample sample) {
    if (sample.fields().drainage().channel()) {
      int blue = (int) (145 + 90 * sample.fields().drainage().flowAccumulation());
      return new Color(28, 92, StrictMath.min(255, blue));
    }
    double normalized = clamp((sample.fields().elevation() + 30.0) / 260.0, 0.0, 1.0);
    int red = (int) (45 + normalized * 180);
    int green = (int) (68 + normalized * 150);
    int blue = (int) (42 + normalized * 155);
    return new Color(red, green, blue);
  }

  private static Color weatheringColor(SurfaceSample sample) {
    if (sample.fields().outcrop()) {
      return lithologyColor(sample.bedrock().lithology());
    }
    double depth = clamp(sample.fields().weatheringDepth() / 12.0, 0.0, 1.0);
    return blend(new Color(183, 147, 88), new Color(91, 58, 31), depth);
  }

  private static Color systemColor(SurfaceSample sample) {
    if (sample.fields().drainage().sourceLinkedPlacer()) {
      return new Color(255, 201, 38);
    }
    if (sample.bedrock().lithology() == Lithology.VMS_MASSIVE_SULFIDE) {
      return new Color(41, 219, 214);
    }
    return switch (sample.bedrock().overprint()) {
      case POTASSIC_ALTERATION -> new Color(219, 71, 155);
      case PHYLLIC_ALTERATION -> new Color(244, 149, 200);
      case PROPYLITIC_ALTERATION -> new Color(119, 194, 92);
      case CONTACT_HORNFELS -> new Color(139, 75, 143);
      default -> blend(lithologyColor(sample.bedrock().lithology()), new Color(64, 69, 72), 0.62);
    };
  }

  private static Color lithologyColor(Lithology lithology) {
    return switch (lithology) {
      case GRANITIC_GNEISS -> new Color(154, 142, 137);
      case BASAL_CONGLOMERATE -> new Color(139, 93, 61);
      case MARINE_VOLCANICLASTIC -> new Color(67, 118, 105);
      case BASIN_SHALE -> new Color(75, 79, 88);
      case BASIN_SANDSTONE -> new Color(205, 174, 111);
      case SILTSTONE -> new Color(152, 133, 111);
      case LIMESTONE -> new Color(194, 195, 177);
      case DOLOSTONE -> new Color(156, 157, 142);
      case CHERT -> new Color(125, 115, 104);
      case KOMATIITIC_ULTRAMAFIC -> new Color(63, 85, 63);
      case BASALTIC -> new Color(62, 72, 78);
      case GABBROIC -> new Color(78, 91, 87);
      case DIORITE_PULSE -> new Color(112, 119, 120);
      case GRANODIORITE_PULSE -> new Color(175, 164, 151);
      case FELSIC_STOCK -> new Color(224, 184, 175);
      case VMS_MASSIVE_SULFIDE -> new Color(72, 91, 91);
      case ALLUVIAL_GRAVEL -> new Color(154, 126, 76);
    };
  }

  private static Color applyOverprint(Color base, Overprint overprint) {
    return switch (overprint) {
      case NONE -> base;
      case WEATHERED_UNCONFORMITY -> blend(base, new Color(171, 116, 67), 0.52);
      case CONTACT_HORNFELS -> blend(base, new Color(91, 39, 102), 0.48);
      case POTASSIC_ALTERATION -> blend(base, new Color(230, 48, 158), 0.55);
      case PHYLLIC_ALTERATION -> blend(base, new Color(238, 143, 201), 0.50);
      case PROPYLITIC_ALTERATION -> blend(base, new Color(83, 173, 76), 0.48);
      case CHLORITIC_FOOTWALL -> blend(base, new Color(27, 123, 94), 0.58);
      case OXIDIZED_GOSSAN -> blend(base, new Color(196, 79, 29), 0.60);
      case WEATHERED_REGOLITH -> blend(base, new Color(116, 83, 48), 0.45);
    };
  }

  private static Color blend(Color first, Color second, double amount) {
    double bounded = clamp(amount, 0.0, 1.0);
    int red = (int) StrictMath.rint(first.getRed() * (1.0 - bounded) + second.getRed() * bounded);
    int green =
        (int) StrictMath.rint(first.getGreen() * (1.0 - bounded) + second.getGreen() * bounded);
    int blue =
        (int) StrictMath.rint(first.getBlue() * (1.0 - bounded) + second.getBlue() * bounded);
    return new Color(red, green, blue);
  }

  private static double clamp(double value, double minimum, double maximum) {
    return StrictMath.max(minimum, StrictMath.min(maximum, value));
  }

  private static void writePng(Path path, BufferedImage image) throws IOException {
    if (!ImageIO.write(image, "png", path.toFile())) {
      throw new IOException("No PNG writer is available");
    }
  }
}
