package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SedimentaryResourceSystemState;
import java.util.List;

/** Bounded, read-only world-column projection of a sedimentary resource system. */
public record OverworldSedimentaryResourceColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    SedimentaryResourceSystemState system,
    List<OverworldSedimentaryResourceInterval> intervals) {
  public OverworldSedimentaryResourceColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("sedimentary resource column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldSedimentaryResourceInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("sedimentary resource intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException(
          "barren sedimentary resource columns cannot carry horizons");
    }
  }

  public boolean hasSedimentaryResource() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldSedimentaryResourceInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "sedimentary resource column x=%d z=%d status=%s family=%s intervals=%d source=%d deposit=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.family(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.depositAllocationFixedUnits());
  }
}
