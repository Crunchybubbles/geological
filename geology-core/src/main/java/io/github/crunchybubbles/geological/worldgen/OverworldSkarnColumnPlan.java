package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SkarnSystemState;
import java.util.List;

/** Bounded, read-only world-column projection of one skarn system. */
public record OverworldSkarnColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    SkarnSystemState system,
    List<OverworldSkarnInterval> intervals) {
  public OverworldSkarnColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("skarn column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldSkarnInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("skarn intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren skarn columns cannot carry horizons");
    }
  }

  public boolean hasSkarn() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldSkarnInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "skarn column x=%d z=%d status=%s host=%s commodity=%s intervals=%d source=%d deposit=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.hostClass(),
            system.commodityClass(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.depositAllocationFixedUnits());
  }
}
