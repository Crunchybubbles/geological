package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.SedimentaryResourceSystemState;

/** One contiguous block interval occupied by a sedimentary-resource horizon. */
public record OverworldSedimentaryResourceInterval(
    int minYInclusive, int maxYExclusive, SedimentaryResourceSystemState.Horizon horizon) {
  public OverworldSedimentaryResourceInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("sedimentary resource interval values are invalid");
    }
  }

  public String summary() {
    return "sedimentary resource interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
