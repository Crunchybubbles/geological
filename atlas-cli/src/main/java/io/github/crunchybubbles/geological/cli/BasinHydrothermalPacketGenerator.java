package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.BasinHydrothermalSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.BasinHydrothermalHostPolicy;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldBasinHydrothermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldBasinHydrothermalInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldBasinHydrothermalPlanner;
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

/** Writes a deterministic MVT/SEDEX/sediment-hosted-copper basin review artifact. */
final class BasinHydrothermalPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  BasinHydrothermalPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldBasinHydrothermalPlanner defaultDiscovery =
        planner(0, 0, BasinHydrothermalHostPolicy.none());
    OverworldBasinHydrothermalColumnPlan defaultSample = findFormedSample(defaultDiscovery);
    OverworldBasinHydrothermalPlanner fixtureDiscovery =
        planner(0, 0, BasinHydrothermalHostPolicy.fixture());
    OverworldBasinHydrothermalColumnPlan fixtureSample = findFormedSample(fixtureDiscovery);
    Province province =
        defaultDiscovery
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(new Point2(defaultSample.blockX() + 0.5, defaultSample.blockZ() + 0.5));
    long defaultChunkX = Math.floorDiv(defaultSample.blockX(), 16L);
    long defaultChunkZ = Math.floorDiv(defaultSample.blockZ(), 16L);
    long fixtureChunkX = Math.floorDiv(fixtureSample.blockX(), 16L);
    long fixtureChunkZ = Math.floorDiv(fixtureSample.blockZ(), 16L);

    List<Probe> probes =
        List.of(
            new Probe(
                "actual_bedrock_only",
                collect(defaultChunkX, defaultChunkZ, BasinHydrothermalHostPolicy.none())),
            new Probe(
                BasinHydrothermalHostPolicy.fixture().policyId(),
                collect(fixtureChunkX, fixtureChunkZ, BasinHydrothermalHostPolicy.fixture())));
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> basinSettingCounts = new TreeMap<>();
    Map<String, Integer> salinityCounts = new TreeMap<>();
    Map<String, Integer> redoxCounts = new TreeMap<>();
    Map<String, Integer> fluidSourceCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Integer> policyCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0;
    long releasedTotal = 0;
    long lossTotal = 0;
    long depositedTotal = 0;
    for (Probe probe : probes) {
      policyCounts.merge(probe.policyId(), probe.columns().size(), Math::addExact);
      for (OverworldBasinHydrothermalColumnPlan column : probe.columns()) {
        BasinHydrothermalSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        basinSettingCounts.merge(system.basinSetting().name(), 1, Math::addExact);
        salinityCounts.merge(system.salinityClass().name(), 1, Math::addExact);
        redoxCounts.merge(system.redoxClass().name(), 1, Math::addExact);
        fluidSourceCounts.merge(system.fluidSourceClass().name(), 1, Math::addExact);
        hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasBasinHydrothermal()) {
          columnsWithIntervals++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldBasinHydrothermalInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedFluidFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
      }
    }
    boolean seamStable =
        seamStable(defaultChunkX, defaultChunkZ, BasinHydrothermalHostPolicy.none())
            && seamStable(fixtureChunkX, fixtureChunkZ, BasinHydrothermalHostPolicy.fixture());
    BasinHydrothermalSystemState defaultNegative =
        planner(0, 0, BasinHydrothermalHostPolicy.none()).plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_basin_hydrothermal_redox_proxy_projection_not_assay",
            "worldSeed",
            seed,
            "authorizedStage",
            WorldgenStage.REGOLITH_SURFACE_CLUES.id(),
            "chunksVisited",
            8,
            "columnsVisited",
            probes.stream().mapToInt(probe -> probe.columns().size()).sum(),
            "formedProvinceId",
            province.id().toString(),
            "defaultSampleColumn",
            JsonWriter.object("x", defaultSample.blockX(), "z", defaultSample.blockZ()),
            "defaultSampleChunk",
            JsonWriter.object("chunkX", defaultChunkX, "chunkZ", defaultChunkZ),
            "fixtureSampleColumn",
            JsonWriter.object("x", fixtureSample.blockX(), "z", fixtureSample.blockZ()),
            "fixtureSampleChunk",
            JsonWriter.object("chunkX", fixtureChunkX, "chunkZ", fixtureChunkZ),
            "sampleProfiles",
            List.of(
                sampleProfile("actual_bedrock_only", defaultSample.system()),
                sampleProfile(
                    BasinHydrothermalHostPolicy.fixture().policyId(), fixtureSample.system())),
            "policyCounts",
            policyCounts,
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "basinSettingCounts",
            basinSettingCounts,
            "salinityCounts",
            salinityCounts,
            "redoxCounts",
            redoxCounts,
            "fluidSourceCounts",
            fluidSourceCounts,
            "hostCounts",
            hostCounts,
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
            "releasedFluidFixedUnits",
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
                "failedGate",
                defaultNegative.failedGate().orElse(null)),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("basin-hydrothermal.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldBasinHydrothermalColumnPlan> collect(
      long chunkX, long chunkZ, BasinHydrothermalHostPolicy policy) {
    List<OverworldBasinHydrothermalColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ, policy).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(
      String policy, BasinHydrothermalSystemState system) {
    return JsonWriter.object(
        "policy",
        policy,
        "status",
        system.status().name(),
        "family",
        system.family().name(),
        "basinSetting",
        system.basinSetting().name(),
        "salinityClass",
        system.salinityClass().name(),
        "redoxClass",
        system.redoxClass().name(),
        "fluidSourceClass",
        system.fluidSourceClass().name(),
        "hostClass",
        system.hostClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "basinId",
        system.basinId().toString(),
        "fluidSourceId",
        system.fluidSourceId().toString(),
        "structureId",
        system.structureId().toString(),
        "hostBodyId",
        system.hostBodyId().toString(),
        "sourceBodyIds",
        system.sourceBodyIds().stream().map(Object::toString).toList(),
        "sourceBudgetFixedUnits",
        system.sourceBudgetFixedUnits(),
        "releasedFluidFixedUnits",
        system.releasedFluidFixedUnits(),
        "transportLossFixedUnits",
        system.transportLossFixedUnits(),
        "depositAllocationFixedUnits",
        system.depositAllocationFixedUnits(),
        "horizons",
        system.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "failedGate",
        system.failedGate().orElse(null));
  }

  private boolean seamStable(long chunkX, long chunkZ, BasinHydrothermalHostPolicy policy) {
    OverworldBasinHydrothermalPlanner left = planner(chunkX, chunkZ, policy);
    OverworldBasinHydrothermalPlanner right = planner(chunkX + 1L, chunkZ, policy);
    OverworldBasinHydrothermalPlanner lower = planner(chunkX, chunkZ, policy);
    OverworldBasinHydrothermalPlanner upper = planner(chunkX, chunkZ + 1L, policy);
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

  private OverworldBasinHydrothermalPlanner planner(
      long chunkX, long chunkZ, BasinHydrothermalHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldBasinHydrothermalPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldBasinHydrothermalColumnPlan findFormedSample(
      OverworldBasinHydrothermalPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldBasinHydrothermalColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED && column.hasBasinHydrothermal()) {
          return column;
        }
      }
    }
    throw new IllegalStateException(
        "no formed basin hydrothermal sample found in the review window");
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

  private record Probe(String policyId, List<OverworldBasinHydrothermalColumnPlan> columns) {}
}
