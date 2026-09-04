package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.OrogenicGoldSystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldOrogenicGoldColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldOrogenicGoldInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldOrogenicGoldPlanner;
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

/** Writes a deterministic metamorphic-fluid orogenic-gold review artifact. */
final class OrogenicGoldPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  OrogenicGoldPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldOrogenicGoldPlanner discoveryPlanner = planner(0, 0);
    OverworldOrogenicGoldColumnPlan sample = findFormedSample(discoveryPlanner);
    Province province =
        discoveryPlanner
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(new Point2(sample.blockX() + 0.5, sample.blockZ() + 0.5));
    long centerX = sample.blockX();
    long centerZ = sample.blockZ();
    long chunkX = Math.floorDiv(centerX, 16L);
    long chunkZ = Math.floorDiv(centerZ, 16L);

    List<OverworldOrogenicGoldColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> depthCounts = new TreeMap<>();
    Map<String, Integer> fluidSourceCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> structureCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0;
    long releasedTotal = 0;
    long lossTotal = 0;
    long depositedTotal = 0;
    for (OverworldOrogenicGoldColumnPlan column : columns) {
      OrogenicGoldSystemState system = column.system();
      statusCounts.merge(system.status().name(), 1, Math::addExact);
      depthCounts.merge(system.depthClass().name(), 1, Math::addExact);
      fluidSourceCounts.merge(system.fluidSourceClass().name(), 1, Math::addExact);
      hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
      structureCounts.merge(system.structureClass().name(), 1, Math::addExact);
      trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
      preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
      if (system.status() == FormationStatus.FORMED) {
        formedProfiles++;
      }
      if (column.hasOrogenicGold()) {
        columnsWithIntervals++;
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldOrogenicGoldInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
      sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
      releasedTotal = Math.addExact(releasedTotal, system.releasedFluidFixedUnits());
      lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
      depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    OrogenicGoldSystemState negativeProof = planner(0, 0).plan(10_000L, 10_000L).system();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_orogenic_gold_metamorphic_fluid_proxy_projection_not_assay",
            "worldSeed",
            seed,
            "authorizedStage",
            WorldgenStage.REGOLITH_SURFACE_CLUES.id(),
            "chunksVisited",
            4,
            "columnsVisited",
            columns.size(),
            "formedProvinceId",
            province.id().toString(),
            "sampleColumn",
            JsonWriter.object("x", centerX, "z", centerZ),
            "sampleChunk",
            JsonWriter.object("chunkX", chunkX, "chunkZ", chunkZ),
            "sampleProfile",
            sampleProfile(sample.system()),
            "negativeProof",
            JsonWriter.object(
                "status",
                negativeProof.status().name(),
                "failedGate",
                negativeProof.failedGate().orElse(null)),
            "statusCounts",
            statusCounts,
            "depthCounts",
            depthCounts,
            "fluidSourceCounts",
            fluidSourceCounts,
            "hostCounts",
            hostCounts,
            "structureCounts",
            structureCounts,
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
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("orogenic-gold.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static Map<String, Object> sampleProfile(OrogenicGoldSystemState system) {
    return JsonWriter.object(
        "status",
        system.status().name(),
        "depthClass",
        system.depthClass().name(),
        "fluidSourceClass",
        system.fluidSourceClass().name(),
        "hostClass",
        system.hostClass().name(),
        "structureClass",
        system.structureClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "metamorphicDriverId",
        system.metamorphicDriverId().toString(),
        "fluidSystemId",
        system.fluidSystemId().toString(),
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

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldOrogenicGoldPlanner left = planner(chunkX, chunkZ);
    OverworldOrogenicGoldPlanner right = planner(chunkX + 1L, chunkZ);
    OverworldOrogenicGoldPlanner lower = planner(chunkX, chunkZ);
    OverworldOrogenicGoldPlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldOrogenicGoldPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldOrogenicGoldPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldOrogenicGoldColumnPlan findFormedSample(
      OverworldOrogenicGoldPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldOrogenicGoldColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED && column.hasOrogenicGold()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed orogenic-gold sample found in the review window");
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
}
