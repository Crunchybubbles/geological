package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.UraniumSystemState;

/** One contiguous block interval occupied by a uranium horizon. */
public record OverworldUraniumInterval(
    int minYInclusive, int maxYExclusive, UraniumSystemState.Horizon horizon) {
  public OverworldUraniumInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("uranium interval values are invalid");
    }
  }

  public String summary() {
    return "uranium interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
