package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runs a bounded, deterministic exploration survey over the existing Overworld query projection.
 *
 * <p>The report measures evidence coverage and travel burden only. It never treats telemetry as an
 * oracle for hidden deposit truth and never persists the surveyed material state.
 */
public final class OverworldExplorationTelemetry {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private static final int FULL_SCALE = 1_000_000;

  private OverworldExplorationTelemetry() {}

  /** Runs a survey against a supplied immutable regolith planner. */
  public static ExplorationTelemetryReport run(
      OverworldRegolithPlanner regolith, ExplorationTelemetryRequest request) {
    if (regolith == null || request == null) {
      throw new IllegalArgumentException("regolith planner and telemetry request are required");
    }
    OverworldExplorationObservationPlanner observations =
        OverworldExplorationObservationPlanner.from(regolith);
    OverworldHandSamplePlanner handSamples = OverworldHandSamplePlanner.from(regolith);
    OverworldSedimentSampler sediment = OverworldSedimentSampler.from(regolith);
    OverworldGeochemicalAnomalyPlanner anomalies =
        OverworldGeochemicalAnomalyPlanner.from(sediment);
    Map<String, Integer> observationKinds = new TreeMap<>();
    Map<String, Integer> sampleKinds = new TreeMap<>();
    int observationCells = 0;
    int handSampleCells = 0;
    int sampledCells = 0;
    int anomalyCells = 0;
    int detectedAnomalyCells = 0;
    int anomalousCells = 0;
    int hypothesisCells = 0;
    int nearestObservation = Integer.MAX_VALUE;
    int nearestSample = Integer.MAX_VALUE;
    int nearestAnomaly = Integer.MAX_VALUE;
    for (int offsetX = -request.gridSteps(); offsetX <= request.gridSteps(); offsetX++) {
      long blockX = Math.addExact(request.centerX(), (long) offsetX * request.spacingBlocks());
      for (int offsetZ = -request.gridSteps(); offsetZ <= request.gridSteps(); offsetZ++) {
        long blockZ = Math.addExact(request.centerZ(), (long) offsetZ * request.spacingBlocks());
        int distance = distanceFromCenter(offsetX, offsetZ, request.spacingBlocks());
        List<OverworldExplorationObservation> cellObservations = observations.plan(blockX, blockZ);
        if (!cellObservations.isEmpty()) {
          observationCells++;
          cellObservations.forEach(
              observation -> observationKinds.merge(observation.kind().name(), 1, Math::addExact));
          nearestObservation = Math.min(nearestObservation, distance);
        }
        try {
          handSamples.identifySurface(blockX, blockZ);
          handSampleCells++;
        } catch (IllegalArgumentException ignored) {
          // Empty or fluid columns are not hand-sample settings.
        }
        boolean sampled = false;
        boolean hasAnomaly = false;
        boolean hasDetectedAnomaly = false;
        boolean hasAnomalous = false;
        for (ExplorationSampleKind kind : ExplorationSampleKind.values()) {
          try {
            OverworldSedimentSample sample = sediment.sample(kind, blockX, blockZ);
            sampled = true;
            sampleKinds.merge(kind.name(), 1, Math::addExact);
            GeochemicalAnomalyEstimate estimate = anomalies.estimate(sample);
            hasAnomaly = true;
            hasDetectedAnomaly |= estimate.anyDetected();
            hasAnomalous |= estimate.anyAnomalous();
          } catch (IllegalArgumentException ignored) {
            // The sampling method is not valid at this surface setting.
          }
        }
        if (sampled) {
          sampledCells++;
          nearestSample = Math.min(nearestSample, distance);
        }
        if (hasAnomaly) {
          anomalyCells++;
          detectedAnomalyCells += hasDetectedAnomaly ? 1 : 0;
          anomalousCells += hasAnomalous ? 1 : 0;
          if (hasAnomalous) {
            nearestAnomaly = Math.min(nearestAnomaly, distance);
          }
        }
        if (!cellObservations.isEmpty() && sampled && hasAnomalous) {
          hypothesisCells++;
        }
      }
    }
    int effectiveRadius = request.effectiveRadiusBlocks();
    int travel =
        Math.max(
            distanceOrPenalty(nearestObservation, effectiveRadius),
            Math.max(
                distanceOrPenalty(nearestSample, effectiveRadius),
                distanceOrPenalty(nearestAnomaly, effectiveRadius)));
    int travelPpm = ratioPpm(travel, Math.max(1, effectiveRadius));
    int categoryScore =
        (observationCells > 0 ? 333_333 : 0)
            + (sampledCells > 0 ? 333_333 : 0)
            + (anomalousCells > 0 ? 333_334 : 0);
    int sufficiency = Math.min(FULL_SCALE, categoryScore + (hypothesisCells > 0 ? 250_000 : 0));
    StableId reportId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "telemetry",
                CellKey.containing(
                    "block", request.centerX(), request.centerZ(), request.spacingBlocks()),
                (((long) request.radiusBlocks()) << 32) ^ request.spacingBlocks())
            .stableId();
    return new ExplorationTelemetryReport(
        reportId,
        request.centerX(),
        request.centerZ(),
        request.radiusBlocks(),
        request.spacingBlocks(),
        request.cells(),
        observationCells,
        handSampleCells,
        sampledCells,
        anomalyCells,
        detectedAnomalyCells,
        anomalousCells,
        hypothesisCells,
        missingDistance(nearestObservation),
        missingDistance(nearestSample),
        missingDistance(nearestAnomaly),
        travel,
        travelPpm,
        sufficiency,
        observationKinds,
        sampleKinds);
  }

  /** Convenience fixture used by the standalone review artifact. */
  public static ExplorationTelemetryReport run(
      long worldSeed, ExplorationTelemetryRequest request) {
    long chunkX = Math.floorDiv(request.centerX(), 16L);
    long chunkZ = Math.floorDiv(request.centerZ(), 16L);
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                worldSeed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return run(OverworldRegolithPlanner.from(context), request);
  }

  private static int distanceFromCenter(int offsetX, int offsetZ, int spacingBlocks) {
    return (int)
        StrictMath.ceil(
            StrictMath.hypot((long) offsetX * spacingBlocks, (long) offsetZ * spacingBlocks));
  }

  private static int distanceOrPenalty(int distance, int effectiveRadius) {
    return distance == Integer.MAX_VALUE ? effectiveRadius : distance;
  }

  private static int missingDistance(int distance) {
    return distance == Integer.MAX_VALUE ? -1 : distance;
  }

  private static int ratioPpm(int numerator, int denominator) {
    return Math.min(FULL_SCALE, (int) Math.round(numerator * (double) FULL_SCALE / denominator));
  }
}
