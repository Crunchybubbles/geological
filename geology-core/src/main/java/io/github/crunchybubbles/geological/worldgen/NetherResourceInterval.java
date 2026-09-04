package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;

/** One contiguous block interval occupied by a dimension-native Nether resource horizon. */
public record NetherResourceInterval(
    int minYInclusive, int maxYExclusive, NetherResourceSystemState.Horizon horizon) {
  public NetherResourceInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException(
          "Nether resource intervals must be positive and identified");
    }
  }
}
