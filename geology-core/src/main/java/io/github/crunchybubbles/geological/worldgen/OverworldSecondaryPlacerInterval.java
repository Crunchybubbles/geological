package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.SecondaryPlacerState;

/** One contiguous world-column interval occupied by a projected secondary placer horizon. */
public record OverworldSecondaryPlacerInterval(
    int minYInclusive,
    int maxYExclusive,
    SecondaryPlacerState familyState,
    SecondaryPlacerState.Horizon horizon) {
  public OverworldSecondaryPlacerInterval {
    if (maxYExclusive <= minYInclusive || familyState == null || horizon == null) {
      throw new IllegalArgumentException("secondary placer interval values are invalid");
    }
    if (!familyState.horizons().contains(horizon)) {
      throw new IllegalArgumentException("secondary placer interval horizon is not in its profile");
    }
  }

  public String summary() {
    return "secondary-placer interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " family="
        + familyState.family()
        + " horizon="
        + horizon.kind()
        + " allocated="
        + horizon.allocationFixedUnits()
        + " source="
        + familyState.sourceBudgetFixedUnits()
        + " sources="
        + familyState.sourceBodyIds();
  }
}
