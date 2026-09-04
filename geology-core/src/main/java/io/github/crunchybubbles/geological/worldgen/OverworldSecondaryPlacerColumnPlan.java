package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SecondaryPlacerState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Bounded, read-only world-column projection of cassiterite, heavy-mineral, and diamond placers.
 *
 * <p>Profiles retain source, transport, trap, and fixed-point ledger evidence. They do not replace
 * the canonical Phase 2 material runs or authorize per-block inventory.
 */
public record OverworldSecondaryPlacerColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    List<SecondaryPlacerState> profiles,
    List<OverworldSecondaryPlacerInterval> intervals) {
  public OverworldSecondaryPlacerColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || profiles == null
        || intervals == null) {
      throw new IllegalArgumentException("secondary placer column plan values are invalid");
    }
    profiles = List.copyOf(profiles);
    if (profiles.stream().anyMatch(profile -> profile == null)) {
      throw new IllegalArgumentException("secondary placer profiles cannot be null");
    }
    if (profiles.stream().map(SecondaryPlacerState::family).distinct().count() != profiles.size()) {
      throw new IllegalArgumentException("secondary placer profiles must have unique families");
    }
    intervals =
        List.copyOf(intervals).stream()
            .sorted(
                Comparator.comparingInt(OverworldSecondaryPlacerInterval::minYInclusive)
                    .thenComparing(interval -> interval.familyState().family()))
            .toList();
    for (OverworldSecondaryPlacerInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !profiles.contains(interval.familyState())) {
        throw new IllegalArgumentException("secondary placer intervals are inconsistent");
      }
    }
    for (SecondaryPlacerState profile : profiles) {
      long allocation =
          profile.horizons().stream()
              .mapToLong(SecondaryPlacerState.Horizon::allocationFixedUnits)
              .sum();
      if (allocation != profile.depositAllocationFixedUnits()) {
        throw new IllegalArgumentException("secondary placer profile allocation is not closed");
      }
      if (profile.status() != FormationStatus.FORMED
          && intervals.stream().anyMatch(interval -> interval.familyState().equals(profile))) {
        throw new IllegalArgumentException(
            "barren secondary placer profiles cannot carry intervals");
      }
    }
  }

  public boolean hasPlacers() {
    return !intervals.isEmpty();
  }

  public boolean hasFamily(SecondaryPlacerState.PlacerFamily family) {
    if (family == null) {
      throw new IllegalArgumentException("placer family is required");
    }
    return intervals.stream().anyMatch(interval -> interval.familyState().family() == family);
  }

  public Optional<OverworldSecondaryPlacerInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    long formed =
        profiles.stream().filter(profile -> profile.status() == FormationStatus.FORMED).count();
    return "secondary-placer column x=%d z=%d formed=%d intervals=%d"
        .formatted(blockX, blockZ, formed, intervals.size());
  }
}
