package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.petrology.ChemicalElement;

/** One censored, interval-valued indicator estimate derived from a field sample. */
public record GeochemicalIndicatorEstimate(
    ChemicalElement element,
    long reportedSignalPpm,
    long detectionLimitPpm,
    long lowerBoundPpm,
    long upperBoundPpm,
    int anomalyScorePpm,
    boolean detected,
    boolean censored) {
  public GeochemicalIndicatorEstimate {
    if (element == null
        || reportedSignalPpm < 1
        || reportedSignalPpm > 1_000_000
        || detectionLimitPpm < 1
        || detectionLimitPpm > 1_000_000
        || lowerBoundPpm < 0
        || upperBoundPpm < lowerBoundPpm
        || upperBoundPpm > 1_000_000
        || reportedSignalPpm < lowerBoundPpm
        || reportedSignalPpm > upperBoundPpm
        || anomalyScorePpm < 0
        || anomalyScorePpm > 1_000_000
        || detected != (reportedSignalPpm >= detectionLimitPpm)
        || censored == detected) {
      throw new IllegalArgumentException("geochemical indicator estimate is invalid");
    }
  }

  public String summary() {
    return "%s signal=%d limit=%d interval=[%d,%d] anomaly=%d detected=%s censored=%s"
        .formatted(
            element,
            reportedSignalPpm,
            detectionLimitPpm,
            lowerBoundPpm,
            upperBoundPpm,
            anomalyScorePpm,
            detected,
            censored);
  }
}
