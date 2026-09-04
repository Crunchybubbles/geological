package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.LateriteProfileState;

/** One contiguous block interval occupied by a classified laterite horizon. */
public record OverworldLateriteInterval(
    int minYInclusive, int maxYExclusive, LateriteProfileState.Horizon horizon) {
  public OverworldLateriteInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("laterite interval values are invalid");
    }
  }

  public String summary() {
    return "laterite interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " allocations="
        + horizon.allocationFixedUnits()
        + " body="
        + horizon.bodyId();
  }
}
