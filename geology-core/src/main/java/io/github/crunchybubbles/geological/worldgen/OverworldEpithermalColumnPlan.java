package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.EpithermalSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import java.util.List;

/** Bounded, read-only world-column projection of one epithermal system. */
public record OverworldEpithermalColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    EpithermalSystemState system,
    List<OverworldEpithermalInterval> intervals) {
  public OverworldEpithermalColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("epithermal column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldEpithermalInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("epithermal intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren epithermal columns cannot carry horizons");
    }
  }

  public boolean hasEpithermal() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldEpithermalInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "epithermal column x=%d z=%d status=%s sulfidation=%s intervals=%d source=%d deposit=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.sulfidationClass(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.depositAllocationFixedUnits());
  }
}
