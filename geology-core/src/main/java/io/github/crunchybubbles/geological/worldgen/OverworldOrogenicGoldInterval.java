package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.OrogenicGoldSystemState;

/** One contiguous block interval occupied by an orogenic-gold horizon. */
public record OverworldOrogenicGoldInterval(
    int minYInclusive, int maxYExclusive, OrogenicGoldSystemState.Horizon horizon) {
  public OverworldOrogenicGoldInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("orogenic-gold interval values are invalid");
    }
  }

  public String summary() {
    return "orogenic-gold interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
