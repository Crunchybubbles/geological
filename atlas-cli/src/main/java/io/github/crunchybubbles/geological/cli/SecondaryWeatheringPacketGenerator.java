package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SupergeneCopperState;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringPlanner;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Writes a deterministic Phase 6 source-budgeted secondary-weathering review artifact. */
final class SecondaryWeatheringPacketGenerator {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  private final long seed;

  SecondaryWeatheringPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldSecondaryWeatheringPlanner discoveryPlanner = planner(0, 0);
    Province province = findFormedProvince(discoveryPlanner);
    SupergeneCopperState state =
        discoveryPlanner.regolith().material().supergeneCopperState(province);
    Point3 worldCenter = province.frame().toWorld(state.localCenter());
    long centerX = (long) StrictMath.floor(worldCenter.x());
    long centerZ = (long) StrictMath.floor(worldCenter.z());
    long chunkX = Math.floorDiv(centerX, 16L);
    long chunkZ = Math.floorDiv(centerZ, 16L);

    List<OverworldSecondaryWeatheringColumnPlan> columns = new ArrayList<>();
    for (long offsetX = 0; offsetX < 2; offsetX++) {
      for (long offsetZ = 0; offsetZ < 2; offsetZ++) {
        columns.addAll(planner(chunkX + offsetX, chunkZ + offsetZ).planTargetChunk());
      }
    }
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    int columnsWithIntervals = 0;
    int columnsWithEnrichedSulfide = 0;
    int intervalCount = 0;
    for (OverworldSecondaryWeatheringColumnPlan column : columns) {
      statusCounts.merge(column.status().name(), 1, Math::addExact);
      if (column.hasSecondaryWeathering()) {
        columnsWithIntervals++;
      }
      if (column.hasEnrichedSulfide()) {
        columnsWithEnrichedSulfide++;
      }
      intervalCount = Math.addExact(intervalCount, column.intervals().size());
      for (OverworldSecondaryWeatheringInterval interval : column.intervals()) {
        horizonCounts.merge(interval.horizonKind().name(), 1, Math::addExact);
      }
    }
    boolean seamStable = seamStable(chunkX, chunkZ);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase6_secondary_weathering_source_budgeted_projection_not_voxel_inventory",
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
            "statusCounts",
            statusCounts,
            "columnsWithIntervals",
            columnsWithIntervals,
            "columnsWithEnrichedSulfide",
            columnsWithEnrichedSulfide,
            "intervalCount",
            intervalCount,
            "horizonCounts",
            horizonCounts,
            "sourceBudgetFixedUnits",
            state.sourceBudgetFixedUnits(),
            "retainedHypogeneFixedUnits",
            state.retainedHypogeneFixedUnits(),
            "leachableCopperFixedUnits",
            state.leachableCopperFixedUnits(),
            "supergeneAllocationFixedUnits",
            state.supergeneAllocationFixedUnits(),
            "oxidizedAndDissolvedLossFixedUnits",
            state.oxidizedAndDissolvedLossFixedUnits(),
            "budgetClosed",
            state.retainedHypogeneFixedUnits() + state.leachableCopperFixedUnits()
                    == state.sourceBudgetFixedUnits()
                && state.supergeneAllocationFixedUnits()
                        + state.oxidizedAndDissolvedLossFixedUnits()
                    == state.leachableCopperFixedUnits(),
            "seamStable",
            seamStable,
            "systemId",
            state.systemId().toString(),
            "primaryDepositId",
            state.primaryDepositId().toString(),
            "weatheringProcessId",
            state.weatheringProcessId().toString());
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new java.util.LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("secondary-weathering.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private boolean seamStable(long chunkX, long chunkZ) {
    OverworldSecondaryWeatheringPlanner left = planner(chunkX, chunkZ);
    OverworldSecondaryWeatheringPlanner right = planner(chunkX + 1L, chunkZ);
    OverworldSecondaryWeatheringPlanner lower = planner(chunkX, chunkZ);
    OverworldSecondaryWeatheringPlanner upper = planner(chunkX, chunkZ + 1L);
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

  private OverworldSecondaryWeatheringPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                seed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSecondaryWeatheringPlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static Province findFormedProvince(OverworldSecondaryWeatheringPlanner planner) {
    var atlas = planner.regolith().material().geology().atlas();
    for (long cellX = -2; cellX <= 2; cellX++) {
      for (long cellZ = -2; cellZ <= 2; cellZ++) {
        Province candidate = atlas.province(new CellKey("province", cellX, cellZ));
        SupergeneCopperState state = planner.regolith().material().supergeneCopperState(candidate);
        if (state.status() != FormationStatus.FORMED) {
          continue;
        }
        Point3 worldCenter = candidate.frame().toWorld(state.localCenter());
        Province owner = atlas.provinceAt(new Point2(worldCenter.x(), worldCenter.z()));
        if (owner.id().equals(candidate.id())) {
          return candidate;
        }
      }
    }
    throw new IllegalStateException("no formed supergene province found in the review window");
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
