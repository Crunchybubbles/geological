package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.List;

/** Contiguous transient material log for one bounded vertical core. */
public record DrillCoreLog(
    StableId logId,
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    List<DrillCoreInterval> intervals,
    List<StableId> provenanceBodyIds,
    int materialEvaluations) {
  public DrillCoreLog {
    if (logId == null
        || maxYExclusive <= minYInclusive
        || intervals == null
        || intervals.isEmpty()
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || materialEvaluations <= 0
        || materialEvaluations < intervals.size()) {
      throw new IllegalArgumentException("drill-core log values are invalid");
    }
    intervals =
        List.copyOf(intervals).stream()
            .sorted(Comparator.comparingInt(DrillCoreInterval::minYInclusive))
            .toList();
    int expectedY = minYInclusive;
    for (DrillCoreInterval interval : intervals) {
      if (interval.minYInclusive() != expectedY || interval.maxYExclusive() > maxYExclusive) {
        throw new IllegalArgumentException("drill-core intervals must be contiguous");
      }
      expectedY = interval.maxYExclusive();
    }
    if (expectedY != maxYExclusive) {
      throw new IllegalArgumentException("drill-core intervals must cover the requested depth");
    }
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
  }

  public String summary() {
    return "drill-log id=%s at=(%d,%d) y=%d..%d intervals=%d evaluations=%d bodies=%d"
        .formatted(
            logId,
            blockX,
            blockZ,
            minYInclusive,
            maxYExclusive,
            intervals.size(),
            materialEvaluations,
            provenanceBodyIds.size());
  }
}
