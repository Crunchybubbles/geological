package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.GreisenSystemState;

/** One contiguous block interval occupied by a greisen alteration horizon. */
public record OverworldGreisenInterval(
    int minYInclusive, int maxYExclusive, GreisenSystemState.Horizon horizon) {
  public OverworldGreisenInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("greisen interval values are invalid");
    }
  }

  public String summary() {
    return "greisen interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
