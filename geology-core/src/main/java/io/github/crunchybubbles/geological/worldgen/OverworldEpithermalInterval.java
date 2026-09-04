package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.EpithermalSystemState;

/** One contiguous block interval occupied by a shallow epithermal horizon. */
public record OverworldEpithermalInterval(
    int minYInclusive, int maxYExclusive, EpithermalSystemState.Horizon horizon) {
  public OverworldEpithermalInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("epithermal interval values are invalid");
    }
  }

  public String summary() {
    return "epithermal interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
