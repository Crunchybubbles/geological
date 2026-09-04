package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GlacialTransportState;
import java.util.List;

/** Bounded, read-only world-column projection of the opt-in glacial transport prototype. */
public record OverworldGlacialTransportColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    GlacialTransportState profile,
    List<OverworldGlacialTransportInterval> intervals) {
  public OverworldGlacialTransportColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || profile == null
        || intervals == null) {
      throw new IllegalArgumentException("glacial column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    for (OverworldGlacialTransportInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !profile.horizons().contains(interval.horizon())) {
        throw new IllegalArgumentException("glacial intervals are inconsistent");
      }
    }
    if (profile.status() != FormationStatus.FORMED && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren glacial columns cannot carry horizons");
    }
  }

  public boolean hasGlacialTransport() {
    return !intervals.isEmpty();
  }

  public java.util.Optional<OverworldGlacialTransportInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "glacial column x=%d z=%d status=%s kind=%s intervals=%d source=%d released=%d deposited=%d"
        .formatted(
            blockX,
            blockZ,
            profile.status(),
            profile.transportKind(),
            intervals.size(),
            profile.sourceInventoryFixedUnits(),
            profile.releasedInventoryFixedUnits(),
            profile.depositAllocationFixedUnits());
  }
}
