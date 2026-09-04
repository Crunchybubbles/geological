package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.OrogenicGoldSystemState;
import java.util.List;

/** Bounded, read-only world-column projection of one orogenic-gold system. */
public record OverworldOrogenicGoldColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    OrogenicGoldSystemState system,
    List<OverworldOrogenicGoldInterval> intervals) {
  public OverworldOrogenicGoldColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || system == null
        || intervals == null) {
      throw new IllegalArgumentException("orogenic-gold column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldOrogenicGoldInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !system.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("orogenic-gold intervals are inconsistent");
      }
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren orogenic-gold columns cannot carry horizons");
    }
  }

  public boolean hasOrogenicGold() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldOrogenicGoldInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "orogenic-gold column x=%d z=%d status=%s depth=%s intervals=%d source=%d deposit=%d"
        .formatted(
            blockX,
            blockZ,
            system.status(),
            system.depthClass(),
            intervals.size(),
            system.sourceBudgetFixedUnits(),
            system.depositAllocationFixedUnits());
  }
}
