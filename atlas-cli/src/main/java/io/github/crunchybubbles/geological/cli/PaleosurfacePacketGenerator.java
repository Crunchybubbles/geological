package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.PaleosurfaceState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfaceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfaceInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfacePlanner;
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

/** Writes a deterministic Phase 6 structural paleosurface review artifact. */
final class PaleosurfacePacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  PaleosurfacePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldPaleosurfacePlanner discoveryPlanner = planner(0, 0);
    OverworldPaleosurfaceColumnPlan sample = findFormedSample(discoveryPlanner);
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
    List<OverworldPaleosurfaceColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }

    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> refinementStatusCounts = new TreeMap<>();
    Map<String, Integer> sourceBasisCounts = new TreeMap<>();
    Map<String, Integer> preservationCounts = new TreeMap<>();
    Map<String, Integer> failedGateCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int formedProfiles = 0;
    int intervalCount = 0;
    for (OverworldPaleosurfaceColumnPlan column : columns) {
      for (PaleosurfaceState profile : column.profiles()) {
        statusCounts.merge(profile.status().name(), 1, Math::addExact);
        refinementStatusCounts.merge(
            profile.refinementKind().name() + ":" + profile.status().name(), 1, Math::addExact);
        sourceBasisCounts.merge(profile.sourceBasis().name(), 1, Math::addExact);
        preservationCounts.merge(profile.preservationClass().name(), 1, Math::addExact);
        profile.failedGate().ifPresent(gate -> failedGateCounts.merge(gate, 1, Math::addExact));
        if (profile.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldPaleosurfaceInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase6_paleosurface_structural_refinement_not_ore_inventory",
            "inventoryKind",
            "structural_refinement_no_ore_inventory",
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
            "sampleProfiles",
            sampleProfiles(sample),
            "statusCounts",
            statusCounts,
            "refinementStatusCounts",
            refinementStatusCounts,
            "sourceBasisCounts",
            sourceBasisCounts,
            "preservationCounts",
            preservationCounts,
            "failedGateCounts",
            failedGateCounts,
            "formedProfiles",
            formedProfiles,
            "karstFormedProfiles",
            refinementStatusCounts.getOrDefault(
                PaleosurfaceState.RefinementKind.KARST_BAUXITE_POCKET.name() + ":FORMED", 0),
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("paleosurface.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static List<Map<String, Object>> sampleProfiles(OverworldPaleosurfaceColumnPlan column) {
    return column.profiles().stream()
        .map(
            profile ->
                JsonWriter.object(
                    "refinementKind",
                    profile.refinementKind().name(),
                    "status",
                    profile.status().name(),
                    "sourceBasis",
                    profile.sourceBasis().name(),
                    "parentBodyId",
                    profile.parentBodyId().toString(),
                    "sourceBodyIds",
                    profile.sourceBodyIds().stream().map(Object::toString).toList(),
                    "formationAgeMa",
                    profile.formationAge().ageMa(),
                    "preservationClass",
                    profile.preservationClass().name(),
                    "horizons",
                    profile.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
                    "failedGate",
                    profile.failedGate().orElse(null)))
        .toList();
  }

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldPaleosurfacePlanner left = planner(chunkX, chunkZ);
    OverworldPaleosurfacePlanner right = planner(chunkX + 1L, chunkZ);
    OverworldPaleosurfacePlanner lower = planner(chunkX, chunkZ);
    OverworldPaleosurfacePlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldPaleosurfacePlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldPaleosurfacePlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldPaleosurfaceColumnPlan findFormedSample(
      OverworldPaleosurfacePlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldPaleosurfaceColumnPlan column = planner.plan(blockX, blockZ);
        if (column.formedProfileCount() > 0 && !column.intervals().isEmpty()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed paleosurface sample found in the review window");
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
