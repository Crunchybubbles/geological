package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.SkarnSystemState;

/** One contiguous block interval occupied by a skarn calc-silicate horizon. */
public record OverworldSkarnInterval(
    int minYInclusive, int maxYExclusive, SkarnSystemState.Horizon horizon) {
  public OverworldSkarnInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("skarn interval values are invalid");
    }
  }

  public String summary() {
    return "skarn interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
