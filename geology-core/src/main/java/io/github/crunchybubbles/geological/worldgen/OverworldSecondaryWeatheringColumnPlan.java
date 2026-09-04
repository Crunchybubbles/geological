package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import java.util.List;

/**
 * Bounded, read-only world-column projection of a source-gated secondary-weathering profile.
 *
 * <p>The plan is an overlay contract. It does not replace the canonical Phase 2 material runs or
 * authorize a chunk write; each interval retains the primary source, weathering process, horizon
 * body, and fixed-point profile budgets needed by a later writer to make an explicit policy choice.
 */
public record OverworldSecondaryWeatheringColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    StableId provinceId,
    StableId systemId,
    FormationStatus status,
    StableId primaryDepositId,
    StableId weatheringProcessId,
    long sourceBudgetFixedUnits,
    long retainedHypogeneFixedUnits,
    long leachableCopperFixedUnits,
    long supergeneAllocationFixedUnits,
    long oxidizedAndDissolvedLossFixedUnits,
    List<OverworldSecondaryWeatheringInterval> intervals) {
  public OverworldSecondaryWeatheringColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || provinceId == null
        || systemId == null
        || status == null
        || primaryDepositId == null
        || weatheringProcessId == null
        || sourceBudgetFixedUnits < 0L
        || retainedHypogeneFixedUnits < 0L
        || leachableCopperFixedUnits < 0L
        || supergeneAllocationFixedUnits < 0L
        || oxidizedAndDissolvedLossFixedUnits < 0L
        || leachableCopperFixedUnits > sourceBudgetFixedUnits
        || supergeneAllocationFixedUnits > leachableCopperFixedUnits
        || Math.addExact(retainedHypogeneFixedUnits, leachableCopperFixedUnits)
            != sourceBudgetFixedUnits
        || Math.addExact(supergeneAllocationFixedUnits, oxidizedAndDissolvedLossFixedUnits)
            != leachableCopperFixedUnits
        || intervals == null) {
      throw new IllegalArgumentException("secondary-weathering column plan values are invalid");
    }
    intervals = List.copyOf(intervals);
    int previousEnd = minYInclusive;
    long intervalAllocation = 0L;
    for (OverworldSecondaryWeatheringInterval interval : intervals) {
      if (interval.minYInclusive() < previousEnd
          || interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || !interval.systemId().equals(systemId)
          || !interval.primaryDepositId().equals(primaryDepositId)
          || !interval.weatheringProcessId().equals(weatheringProcessId)
          || interval.sourceBudgetFixedUnits() != sourceBudgetFixedUnits
          || interval.retainedHypogeneFixedUnits() != retainedHypogeneFixedUnits
          || interval.leachableCopperFixedUnits() != leachableCopperFixedUnits) {
        throw new IllegalArgumentException("secondary-weathering intervals are inconsistent");
      }
      previousEnd = interval.maxYExclusive();
      intervalAllocation = Math.addExact(intervalAllocation, interval.allocatedCopperFixedUnits());
    }
    if (intervalAllocation > leachableCopperFixedUnits) {
      throw new IllegalArgumentException("secondary-weathering column over-allocates leachable Cu");
    }
    if (status != FormationStatus.FORMED
        && (sourceBudgetFixedUnits != 0L
            || retainedHypogeneFixedUnits != 0L
            || leachableCopperFixedUnits != 0L
            || supergeneAllocationFixedUnits != 0L
            || oxidizedAndDissolvedLossFixedUnits != 0L
            || !intervals.isEmpty())) {
      throw new IllegalArgumentException("barren secondary-weathering columns cannot carry budget");
    }
    if (status == FormationStatus.FORMED && intervals.isEmpty()) {
      // A formed profile may miss a column because its radial footprint is finite; the global
      // source budget remains valid even when this particular column has no horizon interval.
      if (sourceBudgetFixedUnits <= 0L || leachableCopperFixedUnits <= 0L) {
        throw new IllegalArgumentException(
            "formed secondary-weathering columns require source budget");
      }
    }
  }

  public boolean hasSecondaryWeathering() {
    return !intervals.isEmpty();
  }

  public boolean hasEnrichedSulfide() {
    return intervals.stream()
        .anyMatch(
            interval ->
                interval.horizonKind()
                    == io.github.crunchybubbles.geological.mineral.SupergeneCopperState.HorizonKind
                        .SUPERGENE_SULFIDE);
  }

  /** Returns the interval containing a block Y, if this column carries one. */
  public java.util.Optional<OverworldSecondaryWeatheringInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .findFirst();
  }

  public String summary() {
    return "secondary-weathering column x=%d z=%d status=%s intervals=%d source=%d leachable=%d supergene=%d"
        .formatted(
            blockX,
            blockZ,
            status,
            intervals.size(),
            sourceBudgetFixedUnits,
            leachableCopperFixedUnits,
            supergeneAllocationFixedUnits);
  }
}
