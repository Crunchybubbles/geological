package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.BasinHydrothermalSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import java.util.List;

/** Bounded, read-only world-column projection of a basin-hydrothermal system. */
public record OverworldBasinHydrothermalColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    BasinHydrothermalSystemState system,
    List<OverworldBasinHydrothermalInterval> intervals) {
  public OverworldBasinHydrothermalColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("basin hydrothermal column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldBasinHydrothermalInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("basin hydrothermal intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren basin hydrothermal columns cannot carry horizons");
    }
  }

  public boolean hasBasinHydrothermal() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldBasinHydrothermalInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "basin hydrothermal column x=%d z=%d status=%s family=%s intervals=%d source=%d deposit=%d"
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
