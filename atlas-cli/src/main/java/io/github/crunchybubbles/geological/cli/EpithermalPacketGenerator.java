package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.EpithermalSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldEpithermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldEpithermalInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldEpithermalPlanner;
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

/** Writes a deterministic shallow magmatic-hydrothermal epithermal review artifact. */
final class EpithermalPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  EpithermalPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldEpithermalPlanner discoveryPlanner = planner(0, 0);
    OverworldEpithermalColumnPlan sample = findFormedSample(discoveryPlanner);
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

    List<OverworldEpithermalColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> sulfidationCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> fluidPathCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
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
    for (OverworldEpithermalColumnPlan column : columns) {
      EpithermalSystemState system = column.system();
      statusCounts.merge(system.status().name(), 1, Math::addExact);
      sulfidationCounts.merge(system.sulfidationClass().name(), 1, Math::addExact);
      hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
      fluidPathCounts.merge(system.fluidPathClass().name(), 1, Math::addExact);
      pathwayCounts.merge(system.pathwayClass().name(), 1, Math::addExact);
      trapCounts.merge(system.trapClass().name(), 1, Math::addExact);
      preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
      if (system.status() == FormationStatus.FORMED) {
        formedProfiles++;
      }
      if (column.hasEpithermal()) {
        columnsWithIntervals++;
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldEpithermalInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
      sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
      releasedTotal = Math.addExact(releasedTotal, system.releasedFluidFixedUnits());
      lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
      depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_epithermal_shallow_hydrothermal_projection_not_assay",
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
            "statusCounts",
            statusCounts,
            "sulfidationCounts",
            sulfidationCounts,
            "hostCounts",
            hostCounts,
            "fluidPathCounts",
            fluidPathCounts,
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
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("epithermal.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static Map<String, Object> sampleProfile(EpithermalSystemState system) {
    return JsonWriter.object(
        "status",
        system.status().name(),
        "sulfidationClass",
        system.sulfidationClass().name(),
        "hostClass",
        system.hostClass().name(),
        "fluidPathClass",
        system.fluidPathClass().name(),
        "pathwayClass",
        system.pathwayClass().name(),
        "trapClass",
        system.trapClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "sourceIntrusionId",
        system.sourceIntrusionId().toString(),
        "fluidSystemId",
        system.fluidSystemId().toString(),
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
    OverworldEpithermalPlanner left = planner(chunkX, chunkZ);
    OverworldEpithermalPlanner right = planner(chunkX + 1L, chunkZ);
    OverworldEpithermalPlanner lower = planner(chunkX, chunkZ);
    OverworldEpithermalPlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldEpithermalPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldEpithermalPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldEpithermalColumnPlan findFormedSample(
      OverworldEpithermalPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldEpithermalColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED && column.hasEpithermal()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed epithermal sample found in the review window");
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
