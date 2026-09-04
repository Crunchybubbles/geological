package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.CanonicalCbor;
import io.github.crunchybubbles.geological.determinism.StableId;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable exploration telemetry result; timings are intentionally outside this scientific report.
 */
public record ExplorationTelemetryReport(
    StableId reportId,
    long centerX,
    long centerZ,
    int radiusBlocks,
    int spacingBlocks,
    int cellsVisited,
    int observationCells,
    int handSampleCells,
    int sampledCells,
    int anomalyCells,
    int detectedAnomalyCells,
    int anomalousCells,
    int hypothesisCells,
    int nearestObservationDistanceBlocks,
    int nearestSampleDistanceBlocks,
    int nearestAnomalyDistanceBlocks,
    int travelBurdenBlocks,
    int travelBurdenPpm,
    int clueSufficiencyPpm,
    Map<String, Integer> observationKindCounts,
    Map<String, Integer> sampleKindCounts) {
  public ExplorationTelemetryReport {
    if (reportId == null
        || radiusBlocks < 0
        || radiusBlocks > ExplorationTelemetryRequest.MAX_RADIUS_BLOCKS
        || spacingBlocks < 1
        || spacingBlocks > ExplorationTelemetryRequest.MAX_SPACING_BLOCKS
        || cellsVisited < 1
        || cellsVisited > ExplorationTelemetryRequest.MAX_GRID_CELLS
        || observationCells < 0
        || handSampleCells < 0
        || sampledCells < 0
        || anomalyCells < 0
        || detectedAnomalyCells < 0
        || anomalousCells < 0
        || hypothesisCells < 0
        || observationCells > cellsVisited
        || handSampleCells > cellsVisited
        || sampledCells > cellsVisited
        || anomalyCells > cellsVisited
        || anomalyCells > sampledCells
        || detectedAnomalyCells > anomalyCells
        || anomalousCells > anomalyCells
        || hypothesisCells > cellsVisited
        || hypothesisCells > Math.min(observationCells, Math.min(sampledCells, anomalousCells))
        || nearestObservationDistanceBlocks < -1
        || nearestSampleDistanceBlocks < -1
        || nearestAnomalyDistanceBlocks < -1
        || travelBurdenBlocks < 0
        || travelBurdenPpm < 0
        || travelBurdenPpm > 1_000_000
        || clueSufficiencyPpm < 0
        || clueSufficiencyPpm > 1_000_000
        || observationKindCounts == null
        || sampleKindCounts == null) {
      throw new IllegalArgumentException("exploration telemetry report values are invalid");
    }
    observationKindCounts = canonicalCounts(observationKindCounts);
    sampleKindCounts = canonicalCounts(sampleKindCounts);
  }

  public byte[] canonicalBytes() {
    return CanonicalCbor.encodeTuple(
        "geological:exploration-telemetry:v1",
        reportId.toString(),
        centerX,
        centerZ,
        radiusBlocks,
        spacingBlocks,
        cellsVisited,
        observationCells,
        handSampleCells,
        sampledCells,
        anomalyCells,
        detectedAnomalyCells,
        anomalousCells,
        hypothesisCells,
        nearestObservationDistanceBlocks,
        nearestSampleDistanceBlocks,
        nearestAnomalyDistanceBlocks,
        travelBurdenBlocks,
        travelBurdenPpm,
        clueSufficiencyPpm,
        countsBytes(observationKindCounts),
        countsBytes(sampleKindCounts));
  }

  public String digest() {
    try {
      return "sha256:"
          + java.util.HexFormat.of()
              .formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalBytes()));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }

  public String summary() {
    return "exploration-telemetry id=%s center=(%d,%d) cells=%d observations=%d handSamples=%d samples=%d anomalies=%d detected=%d anomalous=%d hypothesis=%d travel=%d(%dppm) sufficiency=%d digest=%s"
        .formatted(
            reportId,
            centerX,
            centerZ,
            cellsVisited,
            observationCells,
            handSampleCells,
            sampledCells,
            anomalyCells,
            detectedAnomalyCells,
            anomalousCells,
            hypothesisCells,
            travelBurdenBlocks,
            travelBurdenPpm,
            clueSufficiencyPpm,
            digest());
  }

  private static Map<String, Integer> canonicalCounts(Map<String, Integer> counts) {
    TreeMap<String, Integer> sorted = new TreeMap<>();
    counts.forEach(
        (key, value) -> {
          if (key == null || key.isBlank() || value == null || value < 0) {
            throw new IllegalArgumentException("telemetry count values are invalid");
          }
          sorted.merge(key, value, Math::addExact);
        });
    return Collections.unmodifiableMap(sorted);
  }

  private static List<Object> countsBytes(Map<String, Integer> counts) {
    return counts.entrySet().stream()
        .map(entry -> List.of(entry.getKey(), entry.getValue()))
        .map(value -> (Object) value)
        .toList();
  }
}
