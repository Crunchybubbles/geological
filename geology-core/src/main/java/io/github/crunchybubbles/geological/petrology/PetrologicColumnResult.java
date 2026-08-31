package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import java.util.List;

/** Phase 1 interval proof projected into exact Phase 2 material runs. */
public record PetrologicColumnResult(
    ColumnQueryResult geology, List<PetrologicRun> runs, int materialEvaluations) {
  public PetrologicColumnResult {
    if (geology == null) {
      throw new IllegalArgumentException("geological column result is required");
    }
    runs = List.copyOf(runs);
    if (runs.isEmpty()
        || runs.getFirst().minYInclusive() != geology.request().minYInclusive()
        || runs.getLast().maxYExclusive() != geology.request().maxYExclusive()) {
      throw new IllegalArgumentException("petrologic runs must cover the requested column");
    }
    int expectedY = geology.request().minYInclusive();
    for (PetrologicRun run : runs) {
      if (run.minYInclusive() != expectedY) {
        throw new IllegalArgumentException("petrologic runs must be contiguous and ordered");
      }
      expectedY = run.maxYExclusive();
    }
    if (materialEvaluations <= 0 || materialEvaluations > geology.runs().size()) {
      throw new IllegalArgumentException("material evaluation count is outside the run bounds");
    }
  }

  public PetrologicState stateAt(int blockY) {
    if (blockY < geology.request().minYInclusive() || blockY >= geology.request().maxYExclusive()) {
      throw new IllegalArgumentException("Y is outside the requested column");
    }
    return runs.stream()
        .filter(run -> run.contains(blockY))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("validated petrologic column has a gap"))
        .state();
  }
}
