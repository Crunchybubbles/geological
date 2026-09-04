package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GeothermalSystemState;
import java.util.List;

/** Bounded, read-only world-column projection of geothermal heat and reservoir state. */
public record OverworldGeothermalColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    GeothermalSystemState system,
    List<OverworldGeothermalInterval> intervals) {
  public OverworldGeothermalColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("geothermal column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldGeothermalInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("geothermal intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren geothermal columns cannot carry horizons");
    }
  }

  public boolean hasGeothermalReservoir() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldGeothermalInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "geothermal column x=%d z=%d status=%s family=%s intervals=%d source=%d reservoir=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.family(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.reservoirAllocationFixedUnits());
  }
}
