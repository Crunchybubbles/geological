package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GreisenSystemState;
import java.util.List;

/** Bounded, read-only world-column projection of one greisen system. */
public record OverworldGreisenColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    GreisenSystemState system,
    List<OverworldGreisenInterval> intervals) {
  public OverworldGreisenColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("greisen column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldGreisenInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("greisen intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren greisen columns cannot carry horizons");
    }
  }

  public boolean hasGreisen() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldGreisenInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "greisen column x=%d z=%d status=%s parent=%s intervals=%d source=%d deposit=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.parentClass(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.depositAllocationFixedUnits());
  }
}
