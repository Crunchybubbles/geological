package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;
import java.util.Optional;

/** Complete 3-D membership and provenance projection for one End X/Z column. */
public record EndFragmentColumnPlan(
    long blockX,
    long blockZ,
    Optional<EndParentBodyState> body,
    int baseY,
    int surfaceY,
    List<EndTerrainInterval> solidIntervals,
    List<EndTerrainInterval> regolithIntervals,
    List<EndTerrainInterval> impactMeltIntervals) {
  public EndFragmentColumnPlan {
    if (body == null
        || solidIntervals == null
        || regolithIntervals == null
        || impactMeltIntervals == null) {
      throw new IllegalArgumentException("End fragment column stages are required");
    }
    body = body.map(value -> value);
    solidIntervals = List.copyOf(solidIntervals);
    regolithIntervals = List.copyOf(regolithIntervals);
    impactMeltIntervals = List.copyOf(impactMeltIntervals);
    validateIntervals(solidIntervals, "solid");
    validateIntervals(regolithIntervals, "regolith");
    validateIntervals(impactMeltIntervals, "impact melt");
    if (solidIntervals.stream()
            .anyMatch(interval -> interval.minYInclusive() < -64 || interval.maxYExclusive() > 320)
        || regolithIntervals.stream()
            .anyMatch(interval -> interval.minYInclusive() < -64 || interval.maxYExclusive() > 320)
        || impactMeltIntervals.stream()
            .anyMatch(
                interval -> interval.minYInclusive() < -64 || interval.maxYExclusive() > 320)) {
      throw new IllegalArgumentException("End fragment intervals exceed the vertical envelope");
    }
    for (EndTerrainInterval interval : regolithIntervals) {
      requireInside(interval, solidIntervals, "regolith");
    }
    for (EndTerrainInterval interval : impactMeltIntervals) {
      requireInside(interval, solidIntervals, "impact melt");
    }
    if (body.isEmpty()) {
      if (!solidIntervals.isEmpty()
          || !regolithIntervals.isEmpty()
          || !impactMeltIntervals.isEmpty()) {
        throw new IllegalArgumentException("void End columns cannot carry terrain intervals");
      }
    } else if (solidIntervals.isEmpty() || baseY < -64 || surfaceY < baseY || surfaceY >= 320) {
      throw new IllegalArgumentException("End island columns need bounded solid terrain");
    }
  }

  public boolean isVoid() {
    return body.isEmpty();
  }

  public Optional<StableId> parentBodyId() {
    return body.map(EndParentBodyState::parentBodyId);
  }

  public boolean isSolid(int blockY) {
    return contains(solidIntervals, blockY);
  }

  public boolean isRegolith(int blockY) {
    return contains(regolithIntervals, blockY);
  }

  public boolean isImpactMelt(int blockY) {
    return contains(impactMeltIntervals, blockY);
  }

  private static boolean contains(List<EndTerrainInterval> intervals, int blockY) {
    return intervals.stream()
        .anyMatch(
            interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive());
  }

  private static void validateIntervals(List<EndTerrainInterval> intervals, String label) {
    int previousEnd = Integer.MIN_VALUE;
    for (EndTerrainInterval interval : intervals) {
      if (interval == null || interval.minYInclusive() < previousEnd) {
        throw new IllegalArgumentException(
            "End " + label + " intervals must be sorted and disjoint");
      }
      previousEnd = interval.maxYExclusive();
    }
  }

  private static void requireInside(
      EndTerrainInterval interval, List<EndTerrainInterval> containers, String label) {
    boolean inside =
        containers.stream()
            .anyMatch(
                container ->
                    interval.minYInclusive() >= container.minYInclusive()
                        && interval.maxYExclusive() <= container.maxYExclusive());
    if (!inside) {
      throw new IllegalArgumentException("End " + label + " interval must be inside solid terrain");
    }
  }
}
