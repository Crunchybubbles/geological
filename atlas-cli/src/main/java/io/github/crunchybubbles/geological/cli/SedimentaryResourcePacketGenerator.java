package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SedimentaryResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentaryResourceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentaryResourceInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentaryResourcePlanner;
import io.github.crunchybubbles.geological.worldgen.SedimentaryResourceHostPolicy;
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

/** Writes a deterministic sedimentary-resource and brine review artifact. */
final class SedimentaryResourcePacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  SedimentaryResourcePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    SedimentaryResourceHostPolicy fixture = SedimentaryResourceHostPolicy.fixture();
    OverworldSedimentaryResourcePlanner fixtureDiscovery = planner(0, 0, fixture);
    List<Probe> probes = new ArrayList<>();
    List<OverworldSedimentaryResourceColumnPlan> samples = new ArrayList<>();
    for (SedimentaryResourceSystemState.ResourceFamily family :
        SedimentaryResourceSystemState.ResourceFamily.values()) {
      if (family == SedimentaryResourceSystemState.ResourceFamily.NONE) {
        continue;
      }
      samples.add(findFormedSample(fixtureDiscovery, family));
    }
    OverworldSedimentaryResourcePlanner actualDiscovery =
        planner(0, 0, SedimentaryResourceHostPolicy.none());
    OverworldSedimentaryResourceColumnPlan actualNegative = actualDiscovery.plan(-5_000L, -300L);
    Province province =
        fixtureDiscovery
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(
                new io.github.crunchybubbles.geological.model.Point2(
                    samples.get(0).blockX() + 0.5, samples.get(0).blockZ() + 0.5));
    long actualChunkX = Math.floorDiv(actualNegative.blockX(), 16L);
    long actualChunkZ = Math.floorDiv(actualNegative.blockZ(), 16L);
    probes.add(
        new Probe(
            "actual_bedrock_only",
            collect(actualChunkX, actualChunkZ, SedimentaryResourceHostPolicy.none())));
    for (OverworldSedimentaryResourceColumnPlan sample : samples) {
      probes.add(
          new Probe(
              fixture.policyId(),
              collect(
                  Math.floorDiv(sample.blockX(), 16L),
                  Math.floorDiv(sample.blockZ(), 16L),
                  fixture)));
    }

    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> basinSettingCounts = new TreeMap<>();
    Map<String, Integer> sourceCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> salinityCounts = new TreeMap<>();
    Map<String, Integer> redoxCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Integer> policyCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0L;
    long releasedTotal = 0L;
    long lossTotal = 0L;
    long depositedTotal = 0L;
    int columnsVisited = 0;
    for (Probe probe : probes) {
      policyCounts.merge(probe.policyId(), probe.columns().size(), Math::addExact);
      columnsVisited = Math.addExact(columnsVisited, probe.columns().size());
      for (OverworldSedimentaryResourceColumnPlan column : probe.columns()) {
        SedimentaryResourceSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        basinSettingCounts.merge(system.basinSetting().name(), 1, Math::addExact);
        sourceCounts.merge(system.sourceClass().name(), 1, Math::addExact);
        hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        salinityCounts.merge(system.salinityClass().name(), 1, Math::addExact);
        redoxCounts.merge(system.redoxClass().name(), 1, Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasSedimentaryResource()) {
          columnsWithIntervals++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldSedimentaryResourceInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedResourceFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
      }
    }
    List<Boolean> seamChecks = new ArrayList<>();
    for (Probe probe : probes) {
      OverworldSedimentaryResourceColumnPlan first = probe.columns().get(0);
      seamChecks.add(
          seamStable(
              Math.floorDiv(first.blockX(), 16L),
              Math.floorDiv(first.blockZ(), 16L),
              probe.policyId().equals(fixture.policyId())
                  ? fixture
                  : SedimentaryResourceHostPolicy.none()));
    }
    boolean seamStable = seamChecks.stream().allMatch(Boolean::booleanValue);
    SedimentaryResourceSystemState defaultNegative =
        actualDiscovery.plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_phosphorite_manganese_coal_brine_gas_proxy_projection_not_assay",
            "worldSeed",
            seed,
            "authorizedStage",
            WorldgenStage.REGOLITH_SURFACE_CLUES.id(),
            "chunksVisited",
            probes.size() * 4,
            "columnsVisited",
            columnsVisited,
            "formedProvinceId",
            province.id().toString(),
            "actualNegativeColumn",
            JsonWriter.object("x", actualNegative.blockX(), "z", actualNegative.blockZ()),
            "sampleProfiles",
            samples.stream().map(sample -> sampleProfile(sample.system())).toList(),
            "policyCounts",
            policyCounts,
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "basinSettingCounts",
            basinSettingCounts,
            "sourceCounts",
            sourceCounts,
            "hostCounts",
            hostCounts,
            "pathwayCounts",
            pathwayCounts,
            "trapCounts",
            trapCounts,
            "preservationCounts",
            preservationCounts,
            "salinityCounts",
            salinityCounts,
            "redoxCounts",
            redoxCounts,
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
            "releasedResourceFixedUnits",
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
    Path reportPath = outputDirectory.resolve("sedimentary-resources.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldSedimentaryResourceColumnPlan> collect(
      long chunkX, long chunkZ, SedimentaryResourceHostPolicy policy) {
    List<OverworldSedimentaryResourceColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ, policy).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(SedimentaryResourceSystemState system) {
    return JsonWriter.object(
        "sample",
        system.family().name().toLowerCase(java.util.Locale.ROOT),
        "status",
        system.status().name(),
        "family",
        system.family().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "faciesClass",
        system.faciesClass(),
        "salinityClass",
        system.salinityClass().name(),
        "redoxClass",
        system.redoxClass().name(),
        "basinSetting",
        system.basinSetting().name(),
        "sourceClass",
        system.sourceClass().name(),
        "hostClass",
        system.hostClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "basinId",
        system.basinId().toString(),
        "sourceBodyId",
        system.sourceBodyId().toString(),
        "structureId",
        system.structureId().toString(),
        "hostBodyId",
        system.hostBodyId().toString(),
        "sourceBodyIds",
        system.sourceBodyIds().stream().map(Object::toString).toList(),
        "sourceBudgetFixedUnits",
        system.sourceBudgetFixedUnits(),
        "releasedResourceFixedUnits",
        system.releasedResourceFixedUnits(),
        "transportLossFixedUnits",
        system.transportLossFixedUnits(),
        "depositAllocationFixedUnits",
        system.depositAllocationFixedUnits(),
        "horizons",
        system.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "failedGate",
        system.failedGate().orElse(null));
  }

  private boolean seamStable(long chunkX, long chunkZ, SedimentaryResourceHostPolicy policy) {
    OverworldSedimentaryResourcePlanner left = planner(chunkX, chunkZ, policy);
    OverworldSedimentaryResourcePlanner right = planner(chunkX + 1L, chunkZ, policy);
    OverworldSedimentaryResourcePlanner lower = planner(chunkX, chunkZ, policy);
    OverworldSedimentaryResourcePlanner upper = planner(chunkX, chunkZ + 1L, policy);
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

  private OverworldSedimentaryResourcePlanner planner(
      long chunkX, long chunkZ, SedimentaryResourceHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSedimentaryResourcePlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldSedimentaryResourceColumnPlan findFormedSample(
      OverworldSedimentaryResourcePlanner planner,
      SedimentaryResourceSystemState.ResourceFamily family) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldSedimentaryResourceColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED
            && column.system().family() == family
            && column.hasSedimentaryResource()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed sedimentary resource sample found for " + family);
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

  private record Probe(String policyId, List<OverworldSedimentaryResourceColumnPlan> columns) {}
}
