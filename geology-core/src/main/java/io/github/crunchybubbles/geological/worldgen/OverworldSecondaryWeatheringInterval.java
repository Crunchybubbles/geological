package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.SupergeneCopperState;
import io.github.crunchybubbles.geological.model.Overprint;

/**
 * One contiguous, world-column interval occupied by a projected secondary-weathering horizon.
 *
 * <p>The allocation is the authored profile allocation, not a per-block inventory. It is carried
 * here so a writer or review tool cannot mistake a horizon with no source budget for a generated
 * resource.
 */
public record OverworldSecondaryWeatheringInterval(
    int minYInclusive,
    int maxYExclusive,
    SupergeneCopperState.HorizonKind horizonKind,
    Overprint overprint,
    StableId systemId,
    StableId primaryDepositId,
    StableId weatheringProcessId,
    StableId horizonBodyId,
    long sourceBudgetFixedUnits,
    long retainedHypogeneFixedUnits,
    long leachableCopperFixedUnits,
    long allocatedCopperFixedUnits) {
  public OverworldSecondaryWeatheringInterval {
    if (maxYExclusive <= minYInclusive
        || horizonKind == null
        || overprint == null
        || systemId == null
        || primaryDepositId == null
        || weatheringProcessId == null
        || horizonBodyId == null
        || sourceBudgetFixedUnits < 0L
        || retainedHypogeneFixedUnits < 0L
        || leachableCopperFixedUnits < 0L
        || allocatedCopperFixedUnits < 0L
        || leachableCopperFixedUnits > sourceBudgetFixedUnits
        || allocatedCopperFixedUnits > leachableCopperFixedUnits
        || (horizonKind == SupergeneCopperState.HorizonKind.LEACHED_CAP
            && allocatedCopperFixedUnits != 0L)
        || Math.addExact(retainedHypogeneFixedUnits, leachableCopperFixedUnits)
            != sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("secondary-weathering interval values are invalid");
    }
  }

  /** Compact deterministic text suitable for a review packet or server command. */
  public String summary() {
    return "secondary-weathering interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizonKind
        + " overprint="
        + overprint
        + " allocated="
        + allocatedCopperFixedUnits
        + " source="
        + sourceBudgetFixedUnits
        + " body="
        + horizonBodyId;
  }
}
