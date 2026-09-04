package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.List;

/**
 * Transient interval/censoring evidence for a field sample; it is not a laboratory assay or a
 * persisted anomaly object.
 */
public record GeochemicalAnomalyEstimate(
    StableId estimateId,
    StableId sampleId,
    long blockX,
    int blockY,
    long blockZ,
    ExplorationSampleKind sampleKind,
    List<GeochemicalIndicatorEstimate> indicators,
    List<StableId> provenanceBodyIds,
    int confidencePpm) {
  public GeochemicalAnomalyEstimate {
    if (estimateId == null
        || sampleId == null
        || sampleKind == null
        || indicators == null
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || confidencePpm < 0
        || confidencePpm > 1_000_000) {
      throw new IllegalArgumentException("geochemical anomaly estimate values are invalid");
    }
    indicators =
        List.copyOf(indicators).stream()
            .sorted(Comparator.comparing(GeochemicalIndicatorEstimate::element))
            .toList();
    if (indicators.stream().map(GeochemicalIndicatorEstimate::element).distinct().count()
        != indicators.size()) {
      throw new IllegalArgumentException("geochemical indicators must be unique");
    }
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
  }

  public boolean anyDetected() {
    return indicators.stream().anyMatch(GeochemicalIndicatorEstimate::detected);
  }

  public boolean anyAnomalous() {
    return indicators.stream().anyMatch(indicator -> indicator.anomalyScorePpm() > 0);
  }

  public String summary() {
    String values =
        indicators.stream()
            .map(GeochemicalIndicatorEstimate::summary)
            .collect(java.util.stream.Collectors.joining(", "));
    return "geochemical estimate id=%s sample=%s kind=%s at=(%d,%d,%d) detected=%s anomalous=%s indicators=[%s] confidence=%d bodies=%d"
        .formatted(
            estimateId,
            sampleId,
            sampleKind,
            blockX,
            blockY,
            blockZ,
            anyDetected(),
            anyAnomalous(),
            values,
            confidencePpm,
            provenanceBodyIds.size());
  }
}
