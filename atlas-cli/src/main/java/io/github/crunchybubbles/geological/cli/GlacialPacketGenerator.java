package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GlacialTransportState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.GlacialHistoryPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportPlanner;
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

/** Writes a deterministic opt-in glacial transport prototype review artifact. */
final class GlacialPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  GlacialPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldGlacialTransportPlanner discoveryPlanner =
        planner(0, 0, GlacialHistoryPolicy.fixture());
    OverworldGlacialTransportColumnPlan sample = findFormedSample(discoveryPlanner);
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
    List<OverworldGlacialTransportColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(
            planner(chunkX + offsetX, chunkZ + offsetZ, GlacialHistoryPolicy.fixture())
                .planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> transportKindCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int formedProfiles = 0;
    int intervalCount = 0;
    long sourceTotal = 0;
    long releasedTotal = 0;
    long lossTotal = 0;
    long depositedTotal = 0;
    for (OverworldGlacialTransportColumnPlan column : columns) {
      GlacialTransportState profile = column.profile();
      statusCounts.merge(profile.status().name(), 1, Math::addExact);
      transportKindCounts.merge(profile.transportKind().name(), 1, Math::addExact);
      if (profile.status() == FormationStatus.FORMED) {
        formedProfiles++;
      }
      sourceTotal = Math.addExact(sourceTotal, profile.sourceInventoryFixedUnits());
      releasedTotal = Math.addExact(releasedTotal, profile.releasedInventoryFixedUnits());
      lossTotal = Math.addExact(lossTotal, profile.transportLossFixedUnits());
      depositedTotal = Math.addExact(depositedTotal, profile.depositAllocationFixedUnits());
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldGlacialTransportInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase6_glacial_transport_opt_in_source_budgeted_prototype",
            "historyPolicy",
            "deterministic-fixture",
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
            sampleProfile(sample.profile()),
            "statusCounts",
            statusCounts,
            "transportKindCounts",
            transportKindCounts,
            "formedProfiles",
            formedProfiles,
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "sourceInventoryFixedUnits",
            sourceTotal,
            "releasedInventoryFixedUnits",
            releasedTotal,
            "transportLossFixedUnits",
            lossTotal,
            "depositAllocationFixedUnits",
            depositedTotal,
            "budgetClosed",
            releasedTotal == lossTotal + depositedTotal && releasedTotal <= sourceTotal,
            "defaultNoIceGate",
            defaultNoIceGate(chunkX, chunkZ),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("glacial.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static Map<String, Object> sampleProfile(GlacialTransportState profile) {
    return JsonWriter.object(
        "status",
        profile.status().name(),
        "transportKind",
        profile.transportKind().name(),
        "sourceBasis",
        profile.sourceBasis().name(),
        "iceClass",
        profile.iceClass().name(),
        "formationAgeMa",
        profile.formationAge().ageMa(),
        "sourceBodyIds",
        profile.sourceBodyIds().stream().map(Object::toString).toList(),
        "sourceInventoryFixedUnits",
        profile.sourceInventoryFixedUnits(),
        "releasedInventoryFixedUnits",
        profile.releasedInventoryFixedUnits(),
        "transportLossFixedUnits",
        profile.transportLossFixedUnits(),
        "depositAllocationFixedUnits",
        profile.depositAllocationFixedUnits(),
        "horizons",
        profile.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "failedGate",
        profile.failedGate().orElse(null));
  }

  private boolean defaultNoIceGate(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, GlacialHistoryPolicy.none())
        .plan(chunkX * 16L, chunkZ * 16L)
        .profile()
        .failedGate()
        .map("ice_history"::equals)
        .orElse(false);
  }

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldGlacialTransportPlanner left = planner(chunkX, chunkZ, GlacialHistoryPolicy.fixture());
    OverworldGlacialTransportPlanner right =
        planner(chunkX + 1L, chunkZ, GlacialHistoryPolicy.fixture());
    OverworldGlacialTransportPlanner lower =
        planner(chunkX, chunkZ, GlacialHistoryPolicy.fixture());
    OverworldGlacialTransportPlanner upper =
        planner(chunkX, chunkZ + 1L, GlacialHistoryPolicy.fixture());
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

  private OverworldGlacialTransportPlanner planner(
      long chunkX, long chunkZ, GlacialHistoryPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldGlacialTransportPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldGlacialTransportColumnPlan findFormedSample(
      OverworldGlacialTransportPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldGlacialTransportColumnPlan column = planner.plan(blockX, blockZ);
        if (column.profile().status() == FormationStatus.FORMED && column.hasGlacialTransport()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed glacial sample found in the review window");
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
