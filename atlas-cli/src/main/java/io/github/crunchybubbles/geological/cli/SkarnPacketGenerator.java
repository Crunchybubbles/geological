package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SkarnSystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnPlanner;
import io.github.crunchybubbles.geological.worldgen.SkarnHostPolicy;
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

/** Writes a deterministic opt-in carbonate-contact skarn review artifact. */
final class SkarnPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  SkarnPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldSkarnPlanner discoveryPlanner = planner(0, 0, SkarnHostPolicy.fixture());
    OverworldSkarnColumnPlan sample = findFormedSample(discoveryPlanner);
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

    List<OverworldSkarnColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(
            planner(chunkX + offsetX, chunkZ + offsetZ, SkarnHostPolicy.fixture())
                .planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> commodityCounts = new TreeMap<>();
    Map<String, Integer> hostCounts = new TreeMap<>();
    Map<String, Integer> intrusionCounts = new TreeMap<>();
    Map<String, Integer> fluidCounts = new TreeMap<>();
    Map<String, Integer> permeabilityCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int formedProfiles = 0;
    int columnsWithIntervals = 0;
    int intervalCount = 0;
    long sourceTotal = 0;
    long releasedTotal = 0;
    long lossTotal = 0;
    long depositedTotal = 0;
    for (OverworldSkarnColumnPlan column : columns) {
      SkarnSystemState system = column.system();
      statusCounts.merge(system.status().name(), 1, Math::addExact);
      commodityCounts.merge(system.commodityClass().name(), 1, Math::addExact);
      hostCounts.merge(system.hostClass().name(), 1, Math::addExact);
      intrusionCounts.merge(system.intrusionClass().name(), 1, Math::addExact);
      fluidCounts.merge(system.fluidClass().name(), 1, Math::addExact);
      permeabilityCounts.merge(system.permeabilityClass().name(), 1, Math::addExact);
      preservationCounts.merge(system.preservationClass().name(), 1, Math::addExact);
      if (system.status() == FormationStatus.FORMED) {
        formedProfiles++;
      }
      if (column.hasSkarn()) {
        columnsWithIntervals++;
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldSkarnInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
      sourceTotal = Math.addExact(sourceTotal, system.sourceBudgetFixedUnits());
      releasedTotal = Math.addExact(releasedTotal, system.releasedFluidFixedUnits());
      lossTotal = Math.addExact(lossTotal, system.transportLossFixedUnits());
      depositedTotal = Math.addExact(depositedTotal, system.depositAllocationFixedUnits());
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    boolean defaultActualHostGate =
        planner(chunkX, chunkZ, SkarnHostPolicy.none())
            .plan(chunkX * 16L, chunkZ * 16L)
            .system()
            .failedGate()
            .map("reactive_carbonate_host"::equals)
            .orElse(false);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase7_skarn_carbonate_contact_fixture_projection_not_assay",
            "hostPolicy",
            SkarnHostPolicy.fixture().policyId(),
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
            "commodityCounts",
            commodityCounts,
            "hostCounts",
            hostCounts,
            "intrusionCounts",
            intrusionCounts,
            "fluidCounts",
            fluidCounts,
            "permeabilityCounts",
            permeabilityCounts,
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
            "defaultActualHostGate",
            defaultActualHostGate,
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("skarn.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static Map<String, Object> sampleProfile(SkarnSystemState system) {
    return JsonWriter.object(
        "status",
        system.status().name(),
        "commodityClass",
        system.commodityClass().name(),
        "hostClass",
        system.hostClass().name(),
        "intrusionClass",
        system.intrusionClass().name(),
        "fluidClass",
        system.fluidClass().name(),
        "permeabilityClass",
        system.permeabilityClass().name(),
        "preservationClass",
        system.preservationClass().name(),
        "formationAgeMa",
        system.formationAge().ageMa(),
        "intrusionId",
        system.intrusionId().toString(),
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
    OverworldSkarnPlanner left = planner(chunkX, chunkZ, SkarnHostPolicy.fixture());
    OverworldSkarnPlanner right = planner(chunkX + 1L, chunkZ, SkarnHostPolicy.fixture());
    OverworldSkarnPlanner lower = planner(chunkX, chunkZ, SkarnHostPolicy.fixture());
    OverworldSkarnPlanner upper = planner(chunkX, chunkZ + 1L, SkarnHostPolicy.fixture());
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

  private OverworldSkarnPlanner planner(long chunkX, long chunkZ, SkarnHostPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSkarnPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldSkarnColumnPlan findFormedSample(OverworldSkarnPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldSkarnColumnPlan column = planner.plan(blockX, blockZ);
        if (column.system().status() == FormationStatus.FORMED && column.hasSkarn()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed skarn sample found in the review window");
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
