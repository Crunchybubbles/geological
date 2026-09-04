package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.LayeredIntrusionSystemState;

/** One contiguous block interval occupied by a layered-intrusion horizon. */
public record OverworldLayeredIntrusionInterval(
    int minYInclusive, int maxYExclusive, LayeredIntrusionSystemState.Horizon horizon) {
  public OverworldLayeredIntrusionInterval {
    if (minYInclusive >= maxYExclusive || horizon == null) {
      throw new IllegalArgumentException("layered intrusion interval values are invalid");
    }
  }

  public String summary() {
    return "layered-intrusion interval="
        + horizon.kind()
        + " y="
        + minYInclusive
        + ".."
        + maxYExclusive;
  }
}
