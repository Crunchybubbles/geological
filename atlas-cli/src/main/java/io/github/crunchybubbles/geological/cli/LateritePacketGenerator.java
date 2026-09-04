package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LateriteProfileState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldLateriteColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldLateriteInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldLateritePlanner;
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

/** Writes a deterministic Phase 6 source-budgeted bauxite/Ni-Co laterite review artifact. */
final class LateritePacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private final long seed;

  LateritePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldLateritePlanner discoveryPlanner = planner(0, 0);
    OverworldLateriteColumnPlan sample = findFormedBauxite(discoveryPlanner);
    Province province =
        discoveryPlanner
            .regolith()
            .material()
            .geology()
            .atlas()
            .provinceAt(new Point2(sample.blockX() + 0.5, sample.blockZ() + 0.5));
    Point3 worldCenter = province.frame().toWorld(sample.profile().localCenter());
    long centerX = (long) StrictMath.floor(worldCenter.x());
    long centerZ = (long) StrictMath.floor(worldCenter.z());
    long chunkX = Math.floorDiv(centerX, 16L);
    long chunkZ = Math.floorDiv(centerZ, 16L);

    List<OverworldLateriteColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> profileKindCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    Map<String, Map<String, Long>> budgetTotals = new TreeMap<>();
    int columnsWithIntervals = 0;
    int formedProfiles = 0;
    int formedNiCoProfiles = 0;
    int intervalCount = 0;
    for (OverworldLateriteColumnPlan column : columns) {
      statusCounts.merge(column.profile().status().name(), 1, Math::addExact);
      profileKindCounts.merge(column.profile().profileKind().name(), 1, Math::addExact);
      if (column.hasLaterite()) {
        columnsWithIntervals++;
      }
      if (column.profile().status() == FormationStatus.FORMED) {
        formedProfiles++;
        if (column.profile().profileKind() == LateriteProfileState.ProfileKind.NI_CO_LATERITE) {
          formedNiCoProfiles++;
        }
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldLateriteInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
      for (LateriteProfileState.CommodityBudget budget : column.profile().commodityBudgets()) {
        Map<String, Long> totals =
            budgetTotals.computeIfAbsent(budget.commodity().name(), ignored -> new TreeMap<>());
        totals.merge("sourceFixedUnits", budget.sourceFixedUnits(), Math::addExact);
        totals.merge(
            "residualAllocationFixedUnits", budget.residualAllocationFixedUnits(), Math::addExact);
        totals.merge("dissolvedLossFixedUnits", budget.dissolvedLossFixedUnits(), Math::addExact);
      }
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase6_laterite_source_budgeted_projection_not_voxel_inventory",
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
            "sampleCenter",
            JsonWriter.object("x", centerX, "z", centerZ),
            "sampleChunk",
            JsonWriter.object("chunkX", chunkX, "chunkZ", chunkZ),
            "sampleProfileKind",
            sample.profile().profileKind().name(),
            "sampleParentBodyId",
            sample.profile().parentBodyId().toString(),
            "sampleSourceBasis",
            sample.profile().sourceBasis().name(),
            "sampleBudgets",
            sampleBudgets(sample.profile()),
            "statusCounts",
            statusCounts,
            "profileKindCounts",
            profileKindCounts,
            "columnsWithIntervals",
            columnsWithIntervals,
            "formedProfiles",
            formedProfiles,
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "budgetTotalsByCommodity",
            budgetTotals,
            "niCoFormedProfiles",
            formedNiCoProfiles,
            "budgetClosed",
            columns.stream().allMatch(LateritePacketGenerator::budgetClosed),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("laterite.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static List<Map<String, Object>> sampleBudgets(LateriteProfileState profile) {
    return profile.commodityBudgets().stream()
        .map(
            budget ->
                JsonWriter.object(
                    "commodity",
                    budget.commodity().name(),
                    "sourceFixedUnits",
                    budget.sourceFixedUnits(),
                    "residualAllocationFixedUnits",
                    budget.residualAllocationFixedUnits(),
                    "dissolvedLossFixedUnits",
                    budget.dissolvedLossFixedUnits()))
        .toList();
  }

  private static boolean budgetClosed(OverworldLateriteColumnPlan column) {
    LateriteProfileState profile = column.profile();
    return profile.totalSourceFixedUnits()
            == profile.totalResidualAllocationFixedUnits() + profile.totalDissolvedLossFixedUnits()
        && profile.commodityBudgets().stream()
            .allMatch(
                budget ->
                    budget.residualAllocationFixedUnits()
                        == profile.horizons().stream()
                            .mapToLong(horizon -> horizon.allocationFixedUnits(budget.commodity()))
                            .sum());
  }

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldLateritePlanner left = planner(chunkX, chunkZ);
    OverworldLateritePlanner right = planner(chunkX + 1L, chunkZ);
    OverworldLateritePlanner lower = planner(chunkX, chunkZ);
    OverworldLateritePlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldLateritePlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldLateritePlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldLateriteColumnPlan findFormedBauxite(OverworldLateritePlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldLateriteColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.profile().status() == FormationStatus.FORMED
            && candidate.profile().profileKind() == LateriteProfileState.ProfileKind.BAUXITE
            && candidate.hasLaterite()) {
          return candidate;
        }
      }
    }
    throw new IllegalStateException("no formed bauxite profile found in the review window");
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
