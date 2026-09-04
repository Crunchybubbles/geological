package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GeothermalSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.GeothermalHostPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldGeothermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGeothermalInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldGeothermalPlanner;
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

/** Writes a deterministic geothermal heat/fluid/reservoir review artifact. */
final class GeothermalPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  GeothermalPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    GeothermalHostPolicy fixture = GeothermalHostPolicy.fixture();
    OverworldGeothermalPlanner fixtureDiscovery = planner(0, 0, fixture);
    List<OverworldGeothermalColumnPlan> samples = new ArrayList<>();
    for (GeothermalSystemState.GeothermalType type :
        GeothermalSystemState.GeothermalType.values()) {
      if (type != GeothermalSystemState.GeothermalType.NONE) {
        samples.add(findFormedSample(fixtureDiscovery, type));
      }
    }
    OverworldGeothermalPlanner actualDiscovery = planner(0, 0, GeothermalHostPolicy.none());
    OverworldGeothermalColumnPlan actualNegative = actualDiscovery.plan(-5_000L, -300L);
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
    List<Probe> probes = new ArrayList<>();
    probes.add(
        new Probe(
            "actual_evidence_only",
            collect(actualChunkX, actualChunkZ, GeothermalHostPolicy.none())));
    for (OverworldGeothermalColumnPlan sample : samples) {
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
    Map<String, Integer> heatCounts = new TreeMap<>();
    Map<String, Integer> fluidCounts = new TreeMap<>();
    Map<String, Integer> reservoirCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Integer> policyCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithReservoir = 0;
    int intervalCount = 0;
    int columnsVisited = 0;
    long sourceTotal = 0L;
    long releasedTotal = 0L;
    long lossTotal = 0L;
    long allocationTotal = 0L;
    for (Probe probe : probes) {
      policyCounts.merge(probe.policyId(), probe.columns().size(), Math::addExact);
      columnsVisited = Math.addExact(columnsVisited, probe.columns().size());
      for (OverworldGeothermalColumnPlan column : probe.columns()) {
        GeothermalSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        heatCounts.merge(system.heatClass().name(), 1, Math::addExact);
        fluidCounts.merge(system.fluidClass().name(), 1, Math::addExact);
        reservoirCounts.merge(system.reservoirClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasGeothermalReservoir()) {
          columnsWithReservoir++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldGeothermalInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedHeatFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        allocationTotal = Math.addExact(allocationTotal, system.reservoirAllocationFixedUnits());
      }
    }
    boolean seamStable = true;
    for (Probe probe : probes) {
      OverworldGeothermalColumnPlan first = probe.columns().get(0);
      seamStable &=
          seamStable(
              Math.floorDiv(first.blockX(), 16L),
              Math.floorDiv(first.blockZ(), 16L),
              probe.policyId().equals(fixture.policyId()) ? fixture : GeothermalHostPolicy.none());
    }
    GeothermalSystemState defaultNegative = actualDiscovery.plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_geothermal_heat_fluid_reservoir_proxy_not_assay",
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
            samples.stream().map(GeothermalPacketGenerator::sampleProfile).toList(),
            "policyCounts",
            policyCounts,
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "heatCounts",
            heatCounts,
            "fluidCounts",
            fluidCounts,
            "reservoirCounts",
            reservoirCounts,
            "pathwayCounts",
            pathwayCounts,
            "trapCounts",
            trapCounts,
            "preservationCounts",
            preservationCounts,
            "columnsWithReservoir",
            columnsWithReservoir,
            "formedProfiles",
            formedProfiles,
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "sourceBudgetFixedUnits",
            sourceTotal,
            "releasedHeatFixedUnits",
            releasedTotal,
            "transportLossFixedUnits",
            lossTotal,
            "reservoirAllocationFixedUnits",
            allocationTotal,
            "budgetClosed",
            releasedTotal == lossTotal + allocationTotal && releasedTotal <= sourceTotal,
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
    Path reportPath = outputDirectory.resolve("geothermal.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldGeothermalColumnPlan> collect(
      long chunkX, long chunkZ, GeothermalHostPolicy policy) {
    List<OverworldGeothermalColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ, policy).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(OverworldGeothermalColumnPlan column) {
    GeothermalSystemState system = column.system();
    return JsonWriter.object(
        "sample",
        system.family().name().toLowerCase(java.util.Locale.ROOT),
        "status",
        system.status().name(),
        "family",
        system.family().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "heatClass",
        system.heatClass().name(),
        "fluidClass",
        system.fluidClass().name(),
        "reservoirClass",
        system.reservoirClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "heatSourceId",
        system.heatSourceId().toString(),
        "fluidSourceId",
        system.fluidSourceId().toString(),
        "structureId",
        system.structureId().toString(),
        "reservoirBodyId",
        system.reservoirBodyId().toString(),
        "sourceBodyIds",
        system.sourceBodyIds().stream().map(Object::toString).toList(),
        "sourceBudgetFixedUnits",
        system.sourceBudgetFixedUnits(),
        "releasedHeatFixedUnits",
        system.releasedHeatFixedUnits(),
        "transportLossFixedUnits",
        system.transportLossFixedUnits(),
        "reservoirAllocationFixedUnits",
        system.reservoirAllocationFixedUnits(),
        "horizons",
        system.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "failedGate",
        system.failedGate().orElse(null));
  }

  private boolean seamStable(long chunkX, long chunkZ, GeothermalHostPolicy policy) {
    OverworldGeothermalPlanner left = planner(chunkX, chunkZ, policy);
    OverworldGeothermalPlanner right = planner(chunkX + 1L, chunkZ, policy);
    OverworldGeothermalPlanner lower = planner(chunkX, chunkZ, policy);
    OverworldGeothermalPlanner upper = planner(chunkX, chunkZ + 1L, policy);
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

  private OverworldGeothermalPlanner planner(
      long chunkX, long chunkZ, GeothermalHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldGeothermalPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldGeothermalColumnPlan findFormedSample(
      OverworldGeothermalPlanner planner, GeothermalSystemState.GeothermalType type) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldGeothermalColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED
            && column.system().family() == type
            && column.hasGeothermalReservoir()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed geothermal sample found for " + type);
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

  private record Probe(String policyId, List<OverworldGeothermalColumnPlan> columns) {}
}
