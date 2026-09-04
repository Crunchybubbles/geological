package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.UraniumSystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldUraniumColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldUraniumInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldUraniumPlanner;
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

/** Writes a deterministic unconformity and sandstone roll-front uranium review artifact. */
final class UraniumPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  UraniumPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldUraniumPlanner discovery = planner(0, 0);
    OverworldUraniumColumnPlan unconformitySample =
        findFormedSample(discovery, UraniumSystemState.DepositFamily.UNCONFORMITY_RELATED);
    OverworldUraniumColumnPlan rollFrontSample =
        findFormedSample(discovery, UraniumSystemState.DepositFamily.SANDSTONE_ROLL_FRONT);
    Province province =
        discovery
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(
                new Point2(unconformitySample.blockX() + 0.5, unconformitySample.blockZ() + 0.5));
    long unconformityChunkX = Math.floorDiv(unconformitySample.blockX(), 16L);
    long unconformityChunkZ = Math.floorDiv(unconformitySample.blockZ(), 16L);
    long rollFrontChunkX = Math.floorDiv(rollFrontSample.blockX(), 16L);
    long rollFrontChunkZ = Math.floorDiv(rollFrontSample.blockZ(), 16L);
    List<Probe> probes =
        List.of(
            new Probe("unconformity_related", collect(unconformityChunkX, unconformityChunkZ)),
            new Probe("sandstone_roll_front", collect(rollFrontChunkX, rollFrontChunkZ)));
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> sourceCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> fluidCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> salinityCounts = new TreeMap<>();
    Map<String, Integer> redoxCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0L;
    long releasedTotal = 0L;
    long lossTotal = 0L;
    long depositedTotal = 0L;
    for (Probe probe : probes) {
      for (OverworldUraniumColumnPlan column : probe.columns()) {
        UraniumSystemState system = column.system();
        statusCounts.merge(system.status().name(), 1, Math::addExact);
        familyCounts.merge(system.family().name(), 1, Math::addExact);
        sourceCounts.merge(system.sourceClass().name(), 1, Math::addExact);
        hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
        fluidCounts.merge(system.fluidSourceClass().name(), 1, Math::addExact);
        pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
        trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
        preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
        salinityCounts.merge(system.fluidSalinity().name(), 1, Math::addExact);
        redoxCounts.merge(system.hostRedox().name(), 1, Math::addExact);
        if (system.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        if (column.hasUranium()) {
          columnsWithIntervals++;
        }
        intervalCount = Math.addExact(intervalCount, column.intervals().size());
        for (OverworldUraniumInterval interval : column.intervals()) {
          horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
        }
        sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
        releasedTotal = Math.addExact(releasedTotal, system.releasedFluidFixedUnits());
        lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
        depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
      }
    }
    boolean seamStable =
        seamStable(unconformityChunkX, unconformityChunkZ)
            && seamStable(rollFrontChunkX, rollFrontChunkZ);
    UraniumSystemState negative = planner(0, 0).plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_uranium_redox_groundwater_proxy_projection_not_assay",
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
            "unconformitySampleColumn",
            JsonWriter.object("x", unconformitySample.blockX(), "z", unconformitySample.blockZ()),
            "unconformitySampleChunk",
            JsonWriter.object("chunkX", unconformityChunkX, "chunkZ", unconformityChunkZ),
            "rollFrontSampleColumn",
            JsonWriter.object("x", rollFrontSample.blockX(), "z", rollFrontSample.blockZ()),
            "rollFrontSampleChunk",
            JsonWriter.object("chunkX", rollFrontChunkX, "chunkZ", rollFrontChunkZ),
            "sampleProfiles",
            List.of(
                sampleProfile("unconformity_related", unconformitySample.system()),
                sampleProfile("sandstone_roll_front", rollFrontSample.system())),
            "statusCounts",
            statusCounts,
            "familyCounts",
            familyCounts,
            "sourceCounts",
            sourceCounts,
            "hostCounts",
            hostCounts,
            "fluidSourceCounts",
            fluidCounts,
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
                negative.status().name(),
                "failedGate",
                negative.failedGate().orElse(null)),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("uranium.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private List<OverworldUraniumColumnPlan> collect(long chunkX, long chunkZ) {
    List<OverworldUraniumColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }
    return List.copyOf(columns);
  }

  private static Map<String, Object> sampleProfile(String label, UraniumSystemState system) {
    return JsonWriter.object(
        "sample",
        label,
        "status",
        system.status().name(),
        "family",
        system.family().name(),
        "sourceClass",
        system.sourceClass().name(),
        "hostClass",
        system.hostClass().name(),
        "fluidSourceClass",
        system.fluidSourceClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "fluidSalinity",
        system.fluidSalinity().name(),
        "hostRedox",
        system.hostRedox().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "basementSourceId",
        system.basementSourceId().toString(),
        "fluidSourceId",
        system.fluidSourceId().toString(),
        "structureId",
        system.structureId().toString(),
        "hostBodyId",
        system.hostBodyId().toString(),
        "unconformityId",
        system.unconformityId().toString(),
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

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldUraniumPlanner left = planner(chunkX, chunkZ);
    OverworldUraniumPlanner right = planner(chunkX + 1L, chunkZ);
    OverworldUraniumPlanner lower = planner(chunkX, chunkZ);
    OverworldUraniumPlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldUraniumPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldUraniumPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldUraniumColumnPlan findFormedSample(
      OverworldUraniumPlanner planner, UraniumSystemState.DepositFamily family) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 16L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 16L) {
        OverworldUraniumColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED
            && column.system().family() == family
            && column.hasUranium()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed uranium " + family + " sample found");
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

  private record Probe(String label, List<OverworldUraniumColumnPlan> columns) {}
}
