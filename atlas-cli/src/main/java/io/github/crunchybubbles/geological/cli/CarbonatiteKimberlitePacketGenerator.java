package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.worldgen.CarbonatiteKimberliteHostPolicy;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldCarbonatiteKimberliteColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldCarbonatiteKimberliteInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldCarbonatiteKimberlitePlanner;
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

/** Writes a deterministic carbonatite/peralkaline REE and kimberlite/diamond review artifact. */
final class CarbonatiteKimberlitePacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  CarbonatiteKimberlitePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    CarbonatiteKimberliteHostPolicy fixture = CarbonatiteKimberliteHostPolicy.fixture();
    OverworldCarbonatiteKimberlitePlanner fixtureDiscovery = planner(0, 0, fixture);
    OverworldCarbonatiteKimberliteColumnPlan carbonatiteSample =
        findFormedSample(
            fixtureDiscovery, CarbonatiteKimberliteSystemState.DepositFamily.CARBONATITE_REE);
    OverworldCarbonatiteKimberliteColumnPlan peralkalineSample =
        findFormedSample(
            fixtureDiscovery, CarbonatiteKimberliteSystemState.DepositFamily.PERALKALINE_REE);
    OverworldCarbonatiteKimberliteColumnPlan kimberliteSample =
        findFormedSample(
            fixtureDiscovery, CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND);
    OverworldCarbonatiteKimberlitePlanner actualDiscovery =
        planner(0, 0, CarbonatiteKimberliteHostPolicy.none());
    OverworldCarbonatiteKimberliteColumnPlan actualNegative = actualDiscovery.plan(-5_000L, -300L);
    Province province =
        fixtureDiscovery
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(
                new io.github.crunchybubbles.geological.model.Point2(
                    carbonatiteSample.blockX() + 0.5, carbonatiteSample.blockZ() + 0.5));

    long actualChunkX = Math.floorDiv(actualNegative.blockX(), 16L);
    long actualChunkZ = Math.floorDiv(actualNegative.blockZ(), 16L);
    long carbonatiteChunkX = Math.floorDiv(carbonatiteSample.blockX(), 16L);
    long carbonatiteChunkZ = Math.floorDiv(carbonatiteSample.blockZ(), 16L);
    long peralkalineChunkX = Math.floorDiv(peralkalineSample.blockX(), 16L);
    long peralkalineChunkZ = Math.floorDiv(peralkalineSample.blockZ(), 16L);
    long kimberliteChunkX = Math.floorDiv(kimberliteSample.blockX(), 16L);
    long kimberliteChunkZ = Math.floorDiv(kimberliteSample.blockZ(), 16L);

    List<Probe> probes =
        List.of(
            new Probe(
                "actual_bedrock_only",
                collect(actualChunkX, actualChunkZ, CarbonatiteKimberliteHostPolicy.none())),
            new Probe(fixture.policyId(), collect(carbonatiteChunkX, carbonatiteChunkZ, fixture)),
            new Probe(fixture.policyId(), collect(peralkalineChunkX, peralkalineChunkZ, fixture)),
            new Probe(fixture.policyId(), collect(kimberliteChunkX, kimberliteChunkZ, fixture)));
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> settingCounts = new TreeMap<>();
    Map<String, Integer> sourceCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> cargoCounts = new TreeMap<>();
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
      for (OverworldCarbonatiteKimberliteColumnPlan column : probe.columns()) {
        CarbonatiteKimberliteSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        settingCounts.merge(system.setting().name(), 1, Math::addExact);
        sourceCounts.merge(system.sourceClass().name(), 1, Math::addExact);
        hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        cargoCounts.merge(
            system.mantleCargo().map(MantleCargoState::status).map(Enum::name).orElse("NONE"),
            1,
            Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasAlkalineComplex()) {
          columnsWithIntervals++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldCarbonatiteKimberliteInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedBudgetFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
      }
    }
    boolean seamStable =
        seamStable(actualChunkX, actualChunkZ, CarbonatiteKimberliteHostPolicy.none())
            && seamStable(carbonatiteChunkX, carbonatiteChunkZ, fixture)
            && seamStable(peralkalineChunkX, peralkalineChunkZ, fixture)
            && seamStable(kimberliteChunkX, kimberliteChunkZ, fixture);
    CarbonatiteKimberliteSystemState defaultNegative =
        actualDiscovery.plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_carbonatite_peralkaline_ree_kimberlite_diamond_proxy_projection_not_assay",
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
            "carbonatiteSampleColumn",
            JsonWriter.object("x", carbonatiteSample.blockX(), "z", carbonatiteSample.blockZ()),
            "peralkalineSampleColumn",
            JsonWriter.object("x", peralkalineSample.blockX(), "z", peralkalineSample.blockZ()),
            "kimberliteSampleColumn",
            JsonWriter.object("x", kimberliteSample.blockX(), "z", kimberliteSample.blockZ()),
            "sampleProfiles",
            List.of(
                sampleProfile("carbonatite_ree", carbonatiteSample.system()),
                sampleProfile("peralkaline_ree", peralkalineSample.system()),
                sampleProfile("kimberlite_diamond", kimberliteSample.system())),
            "policyCounts",
            policyCounts,
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "settingCounts",
            settingCounts,
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
            "cargoCounts",
            cargoCounts,
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
            "releasedBudgetFixedUnits",
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
    Path reportPath = outputDirectory.resolve("carbonatite-kimberlite.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldCarbonatiteKimberliteColumnPlan> collect(
      long chunkX, long chunkZ, CarbonatiteKimberliteHostPolicy policy) {
    List<OverworldCarbonatiteKimberliteColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ, policy).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(
      String sample, CarbonatiteKimberliteSystemState system) {
    Map<String, Object> profile =
        new LinkedHashMap<>(
            JsonWriter.object(
                "sample",
                sample,
                "status",
                system.status().name(),
                "family",
                system.family().name(),
                "formationAgeMa",
                system.formationAge().ageMa(),
                "setting",
                system.setting().name(),
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
                "intrusionId",
                system.intrusionId().toString(),
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
                "releasedBudgetFixedUnits",
                system.releasedBudgetFixedUnits(),
                "transportLossFixedUnits",
                system.transportLossFixedUnits(),
                "depositAllocationFixedUnits",
                system.depositAllocationFixedUnits(),
                "horizons",
                system.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
                "failedGate",
                system.failedGate().orElse(null)));
    system
        .mantleCargo()
        .ifPresent(
            cargo -> {
              profile.put("mantleCargoStatus", cargo.status().name());
              profile.put("mantleCargoCarrierBodyId", cargo.carrierBodyId().toString());
              profile.put(
                  "mantleCargoSourceReservoirId",
                  cargo.sourceReservoirId().map(Object::toString).orElse(null));
              profile.put("diamondMineralId", cargo.diamondMineralId());
              profile.put("diamondGradePpbByMass", cargo.diamondGradePpbByMass());
              profile.put("indicatorMineralIds", cargo.candidateIndicatorMineralIds());
            });
    return profile;
  }

  private boolean seamStable(long chunkX, long chunkZ, CarbonatiteKimberliteHostPolicy policy) {
    OverworldCarbonatiteKimberlitePlanner left = planner(chunkX, chunkZ, policy);
    OverworldCarbonatiteKimberlitePlanner right = planner(chunkX + 1L, chunkZ, policy);
    OverworldCarbonatiteKimberlitePlanner lower = planner(chunkX, chunkZ, policy);
    OverworldCarbonatiteKimberlitePlanner upper = planner(chunkX, chunkZ + 1L, policy);
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

  private OverworldCarbonatiteKimberlitePlanner planner(
      long chunkX, long chunkZ, CarbonatiteKimberliteHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldCarbonatiteKimberlitePlanner.from(
        OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldCarbonatiteKimberliteColumnPlan findFormedSample(
      OverworldCarbonatiteKimberlitePlanner planner,
      CarbonatiteKimberliteSystemState.DepositFamily family) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldCarbonatiteKimberliteColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED
            && column.system().family() == family
            && column.hasAlkalineComplex()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed alkaline complex " + family + " sample found");
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

  private record Probe(String policyId, List<OverworldCarbonatiteKimberliteColumnPlan> columns) {}
}
