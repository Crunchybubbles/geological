package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import java.util.List;
import java.util.Optional;

/** Bounded, read-only world-column projection of an alkaline complex or carrier pipe. */
public record OverworldCarbonatiteKimberliteColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    CarbonatiteKimberliteSystemState system,
    List<OverworldCarbonatiteKimberliteInterval> intervals) {
  public OverworldCarbonatiteKimberliteColumnPlan {
    if (provinceId == null || system == null || intervals == null) {
      throw new IllegalArgumentException("alkaline complex column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    int previousMax = minYInclusive;
    for (OverworldCarbonatiteKimberliteInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || interval.minYInclusive() < previousMax) {
        throw new IllegalArgumentException("alkaline complex intervals are inconsistent");
      }
      previousMax = interval.maxYExclusive();
    }
    if (system.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren alkaline columns cannot carry horizons");
    }
  }

  public boolean hasAlkalineComplex() {
    return system.status() == FormationStatus.FORMED && !intervals.isEmpty();
  }

  public Optional<OverworldCarbonatiteKimberliteInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "alkaline-complex column x="
        + blockX
        + " z="
        + blockZ
        + " status="
        + system.status()
        + " family="
        + system.family()
        + " host="
        + system.hostClass()
        + " source="
        + system.sourceClass()
        + " intervals="
        + intervals.size()
        + " source="
        + system.sourceBudgetFixedUnits()
        + " deposit="
        + system.depositAllocationFixedUnits();
  }
}
