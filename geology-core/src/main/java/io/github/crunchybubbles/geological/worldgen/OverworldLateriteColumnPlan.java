package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LateriteProfileState;
import java.util.List;

/**
 * Bounded, read-only world-column projection of a bauxite or Ni-Co laterite profile.
 *
 * <p>The profile remains a source-budgeted overlay. It does not replace canonical Phase 2 runs,
 * claim per-block inventory, or authorize a chunk write.
 */
public record OverworldLateriteColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    LateriteProfileState profile,
    List<OverworldLateriteInterval> intervals) {
  public OverworldLateriteColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || profile == null
        || intervals == null) {
      throw new IllegalArgumentException("laterite column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    int previousEnd = minYInclusive;
    for (OverworldLateriteInterval interval : intervals) {
      if (interval.minYInclusive() < previousEnd
          || interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !profile.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("laterite intervals are inconsistent");
      }
      previousEnd = interval.maxYExclusive();
    }
    if (profile.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren laterite columns cannot carry horizons");
    }
  }

  public boolean hasLaterite() {
    return !intervals.isEmpty();
  }

  public boolean hasBauxite() {
    return profile.profileKind() == LateriteProfileState.ProfileKind.BAUXITE;
  }

  public boolean hasNiCoLaterite() {
    return profile.profileKind() == LateriteProfileState.ProfileKind.NI_CO_LATERITE;
  }

  public java.util.Optional<OverworldLateriteInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "laterite column x=%d z=%d status=%s kind=%s intervals=%d source=%d residual=%d"
        .formatted(
            blockX,
            blockZ,
            profile.status(),
            profile.profileKind(),
            intervals.size(),
            profile.totalSourceFixedUnits(),
            profile.totalResidualAllocationFixedUnits());
  }
}
