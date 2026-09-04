package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SecondaryPlacerState;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerPlanner;
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

/** Writes a deterministic Phase 6 source-budgeted secondary-placer review artifact. */
final class SecondaryPlacerPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  SecondaryPlacerPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldSecondaryPlacerPlanner discoveryPlanner = planner(0, 0);
    OverworldSecondaryPlacerColumnPlan sample = findFormedSample(discoveryPlanner);
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
    List<OverworldSecondaryPlacerColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }

    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> familyStatusCounts = new TreeMap<>();
    Map<String, Integer> sourceBasisCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Map<String, Long>> budgetTotals = new TreeMap<>();
    int formedProfiles = 0;
    int intervalCount = 0;
    for (OverworldSecondaryPlacerColumnPlan column : columns) {
      for (SecondaryPlacerState profile : column.profiles()) {
        statusCounts.merge(profile.status().name(), 1, Math::addExact);
        familyStatusCounts.merge(
            profile.family().name() + ":" + profile.status().name(), 1, Math::addExact);
        sourceBasisCounts.merge(profile.sourceBasis().name(), 1, Math::addExact);
        if (profile.status() == FormationStatus.FORMED) {
          formedProfiles++;
        }
        Map<String, Long> totals =
            budgetTotals.computeIfAbsent(profile.family().name(), ignored -> new TreeMap<>());
        totals.merge("sourceFixedUnits", profile.sourceBudgetFixedUnits(), Math::addExact);
        totals.merge(
            "releasedBudgetFixedUnits", profile.releasedBudgetFixedUnits(), Math::addExact);
        totals.merge("transportLossFixedUnits", profile.transportLossFixedUnits(), Math::addExact);
        totals.merge(
            "depositAllocationFixedUnits", profile.depositAllocationFixedUnits(), Math::addExact);
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldSecondaryPlacerInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase6_secondary_placer_source_budgeted_projection_not_voxel_inventory",
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
            "familyStatusCounts",
            familyStatusCounts,
            "sourceBasisCounts",
            sourceBasisCounts,
            "formedProfiles",
            formedProfiles,
            "diamondFormedProfiles",
            formedCount(familyStatusCounts, SecondaryPlacerState.PlacerFamily.DIAMOND),
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "budgetTotalsByFamily",
            budgetTotals,
            "budgetClosed",
            columns.stream()
                .flatMap(column -> column.profiles().stream())
                .allMatch(SecondaryPlacerPacketGenerator::budgetClosed),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("secondary-placers.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static List<Map<String, Object>> sampleProfiles(
      OverworldSecondaryPlacerColumnPlan column) {
    return column.profiles().stream()
        .map(
            profile ->
                JsonWriter.object(
                    "family",
                    profile.family().name(),
                    "status",
                    profile.status().name(),
                    "sourceBasis",
                    profile.sourceBasis().name(),
                    "sourceBodyIds",
                    profile.sourceBodyIds().stream().map(Object::toString).toList(),
                    "sourceBudgetFixedUnits",
                    profile.sourceBudgetFixedUnits(),
                    "releasedBudgetFixedUnits",
                    profile.releasedBudgetFixedUnits(),
                    "transportLossFixedUnits",
                    profile.transportLossFixedUnits(),
                    "depositAllocationFixedUnits",
                    profile.depositAllocationFixedUnits(),
                    "failedGate",
                    profile.failedGate().orElse(null)))
        .toList();
  }

  private static int formedCount(
      Map<String, Integer> familyStatusCounts, SecondaryPlacerState.PlacerFamily family) {
    return familyStatusCounts.getOrDefault(family.name() + ":FORMED", 0);
  }

  private static boolean budgetClosed(SecondaryPlacerState profile) {
    return profile.sourceBudgetFixedUnits() >= profile.releasedBudgetFixedUnits()
        && profile.releasedBudgetFixedUnits()
            == profile.transportLossFixedUnits() + profile.depositAllocationFixedUnits()
        && profile.depositAllocationFixedUnits() == profile.totalProfileAllocationFixedUnits();
  }

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldSecondaryPlacerPlanner left = planner(chunkX, chunkZ);
    OverworldSecondaryPlacerPlanner right = planner(chunkX + 1L, chunkZ);
    OverworldSecondaryPlacerPlanner lower = planner(chunkX, chunkZ);
    OverworldSecondaryPlacerPlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldSecondaryPlacerPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSecondaryPlacerPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldSecondaryPlacerColumnPlan findFormedSample(
      OverworldSecondaryPlacerPlanner planner) {
    var atlas = planner.regolith().material().geology().atlas();
    for (long cellX = -12; cellX <= 12; cellX++) {
      for (long cellZ = -12; cellZ <= 12; cellZ++) {
        Province candidate = atlas.province(new CellKey("province", cellX, cellZ));
        if (candidate.grammar() != ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC) {
          continue;
        }
        Point2 trap = candidate.frame().toWorld(candidate.geometry().placerCenter());
        OverworldSecondaryPlacerColumnPlan column =
            planner.plan((long) StrictMath.floor(trap.x()), (long) StrictMath.floor(trap.z()));
        if (column.intervals().stream()
            .anyMatch(
                interval ->
                    interval.familyState().family() != SecondaryPlacerState.PlacerFamily.DIAMOND)) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no formed secondary placer sample found in review window");
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
