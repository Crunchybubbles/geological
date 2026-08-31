package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.List;

/** Transient adaptive column plan and its compressed material runs. */
public record ColumnQueryResult(
    ColumnRequest request,
    StableId provinceId,
    List<SpatialCandidate> candidates,
    ColumnIntervalProof intervalProof,
    List<MaterialRun> runs,
    int pointEvaluations) {
  public ColumnQueryResult {
    if (request == null || provinceId == null || intervalProof == null) {
      throw new IllegalArgumentException("column result identity must be present");
    }
    candidates = List.copyOf(candidates);
    runs = List.copyOf(runs);
    if (intervalProof.splitYCoordinates().getFirst() != request.minYInclusive()
        || intervalProof.splitYCoordinates().getLast() != request.maxYExclusive()) {
      throw new IllegalArgumentException("column interval proof must cover the requested column");
    }
    if (runs.isEmpty()
        || runs.getFirst().minYInclusive() != request.minYInclusive()
        || runs.getLast().maxYExclusive() != request.maxYExclusive()) {
      throw new IllegalArgumentException("material runs must cover the requested column");
    }
    int expectedY = request.minYInclusive();
    for (MaterialRun run : runs) {
      if (run.minYInclusive() != expectedY) {
        throw new IllegalArgumentException("material runs must be contiguous and ordered");
      }
      expectedY = run.maxYExclusive();
    }
    if (pointEvaluations <= 0
        || pointEvaluations > request.height()
        || pointEvaluations != intervalProof.provenUniformIntervals()) {
      throw new IllegalArgumentException("point evaluation count is outside the column bounds");
    }
  }

  public MaterialState stateAt(int blockY) {
    if (blockY < request.minYInclusive() || blockY >= request.maxYExclusive()) {
      throw new IllegalArgumentException("Y is outside the requested column");
    }
    return runs.stream()
        .filter(run -> run.contains(blockY))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("validated column has a coverage gap"))
        .state();
  }

  public int skippedPointEvaluations() {
    return request.height() - pointEvaluations;
  }

  public ColumnPlanComplexity complexity() {
    return new ColumnPlanComplexity(
        candidates.size(),
        intervalProof.transitionElevations().size(),
        pointEvaluations,
        runs.size());
  }
}
