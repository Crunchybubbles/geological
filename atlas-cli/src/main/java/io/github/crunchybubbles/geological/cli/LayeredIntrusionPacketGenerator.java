package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LayeredIntrusionSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.LayeredIntrusionHostPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldLayeredIntrusionColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldLayeredIntrusionInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldLayeredIntrusionPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Writes a deterministic stratiform chromite and magmatic Ni-Cu-PGE review artifact. */
final class LayeredIntrusionPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  LayeredIntrusionPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    LayeredIntrusionHostPolicy fixture = LayeredIntrusionHostPolicy.fixture();
    OverworldLayeredIntrusionPlanner fixtureDiscovery = planner(0, 0, fixture);
    OverworldLayeredIntrusionColumnPlan chromiteSample =
        findFormedSample(
            fixtureDiscovery, LayeredIntrusionSystemState.DepositFamily.STRATIFORM_CHROMITE);
    OverworldLayeredIntrusionColumnPlan sulfideSample =
        findFormedSample(
            fixtureDiscovery, LayeredIntrusionSystemState.DepositFamily.NI_CU_PGE_SULFIDE);
    OverworldLayeredIntrusionColumnPlan reefSample =
        findFormedSample(
            fixtureDiscovery, LayeredIntrusionSystemState.DepositFamily.LAYERED_PGE_REEF);
    OverworldLayeredIntrusionPlanner actualDiscovery =
        planner(0, 0, LayeredIntrusionHostPolicy.none());
    OverworldLayeredIntrusionColumnPlan actualNegative = actualDiscovery.plan(-5_000L, -300L);
    Province province =
        fixtureDiscovery
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(
                new io.github.crunchybubbles.geological.model.Point2(
                    chromiteSample.blockX() + 0.5, chromiteSample.blockZ() + 0.5));

    long actualChunkX = Math.floorDiv(actualNegative.blockX(), 16L);
    long actualChunkZ = Math.floorDiv(actualNegative.blockZ(), 16L);
    long chromiteChunkX = Math.floorDiv(chromiteSample.blockX(), 16L);
    long chromiteChunkZ = Math.floorDiv(chromiteSample.blockZ(), 16L);
    long sulfideChunkX = Math.floorDiv(sulfideSample.blockX(), 16L);
    long sulfideChunkZ = Math.floorDiv(sulfideSample.blockZ(), 16L);
    long reefChunkX = Math.floorDiv(reefSample.blockX(), 16L);
    long reefChunkZ = Math.floorDiv(reefSample.blockZ(), 16L);

    List<Probe> probes =
        List.of(
            new Probe(
                "actual_bedrock_only",
                collect(actualChunkX, actualChunkZ, LayeredIntrusionHostPolicy.none())),
            new Probe(fixture.policyId(), collect(chromiteChunkX, chromiteChunkZ, fixture)),
            new Probe(fixture.policyId(), collect(sulfideChunkX, sulfideChunkZ, fixture)),
            new Probe(fixture.policyId(), collect(reefChunkX, reefChunkZ, fixture)));
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> magmaSettingCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> saturationCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Integer> policyCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0L;
    long releasedTotal = 0L;
    long lossTotal = 0L;
    long depositedTotal = 0L;
    for (Probe probe : probes) {
      policyCounts.merge(probe.policyId(), probe.columns().size(), Math::addExact);
      for (OverworldLayeredIntrusionColumnPlan column : probe.columns()) {
        LayeredIntrusionSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        magmaSettingCounts.merge(system.magmaSetting().name(), 1, Math::addExact);
        hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
        saturationCounts.merge(system.saturationClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasLayeredIntrusion()) {
          columnsWithIntervals++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldLayeredIntrusionInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedMeltFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
      }
    }
    boolean seamStable =
        seamStable(actualChunkX, actualChunkZ, LayeredIntrusionHostPolicy.none())
            && seamStable(chromiteChunkX, chromiteChunkZ, fixture)
            && seamStable(sulfideChunkX, sulfideChunkZ, fixture)
            && seamStable(reefChunkX, reefChunkZ, fixture);
    LayeredIntrusionSystemState defaultNegative = actualDiscovery.plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_layered_intrusion_chromite_ni_cu_pge_proxy_projection_not_assay",
            "worldSeed",
            seed,
            "authorizedStage",
            WorldgenStage.REGOLITH_SURFACE_CLUES.id(),
            "chunksVisited",
            probes.size() * 4,
            "columnsVisited",
            probes.stream().mapToInt(probe -> probe.columns().size()).sum(),
            "formedProvinceId",
            province.id().toString(),
            "actualNegativeColumn",
            JsonWriter.object("x", actualNegative.blockX(), "z", actualNegative.blockZ()),
            "chromiteSampleColumn",
            JsonWriter.object("x", chromiteSample.blockX(), "z", chromiteSample.blockZ()),
            "sulfideSampleColumn",
            JsonWriter.object("x", sulfideSample.blockX(), "z", sulfideSample.blockZ()),
            "reefSampleColumn",
            JsonWriter.object("x", reefSample.blockX(), "z", reefSample.blockZ()),
            "sampleProfiles",
            List.of(
                sampleProfile("stratiform_chromite", chromiteSample.system()),
                sampleProfile("ni_cu_pge_sulfide", sulfideSample.system()),
                sampleProfile("layered_pge_reef", reefSample.system())),
            "policyCounts",
            policyCounts,
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "magmaSettingCounts",
            magmaSettingCounts,
            "hostCounts",
            hostCounts,
            "saturationCounts",
            saturationCounts,
            "pathwayCounts",
            pathwayCounts,
            "trapCounts",
            trapCounts,
            "preservationCounts",
            preservationCounts,
            "columnsWithIntervals",
            columnsWithIntervals,
            "formedProfiles",
            formedProfiles,
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "sourceBudgetFixedUnits",
            sourceTotal,
            "releasedMeltFixedUnits",
            releasedTotal,
            "transportLossFixedUnits",
            lossTotal,
            "depositAllocationFixedUnits",
            depositedTotal,
            "budgetClosed",
            releasedTotal == lossTotal + depositedTotal && releasedTotal <= sourceTotal,
            "defaultNegativeProof",
            JsonWriter.object(
                "status",
                defaultNegative.status().name(),
                "family",
                defaultNegative.family().name(),
                "failedGate",
                defaultNegative.failedGate().orElse(null)),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("layered-intrusion.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldLayeredIntrusionColumnPlan> collect(
      long chunkX, long chunkZ, LayeredIntrusionHostPolicy policy) {
    List<OverworldLayeredIntrusionColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ, policy).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(
      String sample, LayeredIntrusionSystemState system) {
    return JsonWriter.object(
        "sample",
        sample,
        "status",
        system.status().name(),
        "family",
        system.family().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "magmaSetting",
        system.magmaSetting().name(),
        "hostClass",
        system.hostClass().name(),
        "saturationClass",
        system.saturationClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "intrusionId",
        system.intrusionId().toString(),
        "magmaSourceId",
        system.magmaSourceId().toString(),
        "structureId",
        system.structureId().toString(),
        "hostBodyId",
        system.hostBodyId().toString(),
        "sourceBodyIds",
        system.sourceBodyIds().stream().map(Object::toString).toList(),
        "sourceBudgetFixedUnits",
        system.sourceBudgetFixedUnits(),
        "releasedMeltFixedUnits",
        system.releasedMeltFixedUnits(),
        "transportLossFixedUnits",
        system.transportLossFixedUnits(),
        "depositAllocationFixedUnits",
        system.depositAllocationFixedUnits(),
        "horizons",
        system.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "failedGate",
        system.failedGate().orElse(null));
  }

  private boolean seamStable(long chunkX, long chunkZ, LayeredIntrusionHostPolicy policy) {
    OverworldLayeredIntrusionPlanner left = planner(chunkX, chunkZ, policy);
    OverworldLayeredIntrusionPlanner right = planner(chunkX + 1L, chunkZ, policy);
    OverworldLayeredIntrusionPlanner lower = planner(chunkX, chunkZ, policy);
    OverworldLayeredIntrusionPlanner upper = planner(chunkX, chunkZ + 1L, policy);
    long originX = chunkX * 16L;
    long originZ = chunkZ * 16L;
    for (int offset = 0; offset < 16; offset++) {
      if (!left.plan(originX + 16L, originZ + offset)
          .equals(right.plan(originX + 16L, originZ + offset))) {
        return false;
      }
      if (!lower
          .plan(originX + offset, originZ + 16L)
          .equals(upper.plan(originX + offset, originZ + 16L))) {
        return false;
      }
    }
    return true;
  }

  private OverworldLayeredIntrusionPlanner planner(
      long chunkX, long chunkZ, LayeredIntrusionHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldLayeredIntrusionPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldLayeredIntrusionColumnPlan findFormedSample(
      OverworldLayeredIntrusionPlanner planner, LayeredIntrusionSystemState.DepositFamily family) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldLayeredIntrusionColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED
            && column.system().family() == family
            && column.hasLayeredIntrusion()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed layered intrusion " + family + " sample found");
  }

  private static String digest(String value) {
    try {
      return "sha256:"
          + java.util.HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }

  private record Probe(String policyId, List<OverworldLayeredIntrusionColumnPlan> columns) {}
}
