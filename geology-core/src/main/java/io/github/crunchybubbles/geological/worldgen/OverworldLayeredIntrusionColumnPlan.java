package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LayeredIntrusionSystemState;
import java.util.List;
import java.util.Optional;

/** Bounded, read-only world-column projection of a layered-intrusion system. */
public record OverworldLayeredIntrusionColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    LayeredIntrusionSystemState system,
    List<OverworldLayeredIntrusionInterval> intervals) {
  public OverworldLayeredIntrusionColumnPlan {
    if (provinceId == null || system == null || intervals == null) {
      throw new IllegalArgumentException("layered intrusion column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    int previousMax = minYInclusive;
    for (OverworldLayeredIntrusionInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || interval.minYInclusive() < previousMax) {
        throw new IllegalArgumentException("layered intrusion intervals are inconsistent");
      }
      previousMax = interval.maxYExclusive();
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren layered intrusion columns cannot carry horizons");
    }
  }

  public boolean hasLayeredIntrusion() {
    return system.status() == FormationStatus.FORMED && !intervals.isEmpty();
  }

  public Optional<OverworldLayeredIntrusionInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "layered-intrusion column x="
        + blockX
        + " z="
        + blockZ
        + " status="
        + system.status()
        + " family="
        + system.family()
        + " host="
        + system.hostClass()
        + " saturation="
        + system.saturationClass()
        + " intervals="
        + intervals.size()
        + " source="
        + system.sourceBudgetFixedUnits()
        + " deposit="
        + system.depositAllocationFixedUnits();
  }
}
