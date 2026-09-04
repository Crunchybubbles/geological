package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.GeothermalSystemState;

/** One contiguous block interval occupied by a geothermal reservoir horizon. */
public record OverworldGeothermalInterval(
    int minYInclusive, int maxYExclusive, GeothermalSystemState.Horizon horizon) {
  public OverworldGeothermalInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("geothermal interval values are invalid");
    }
  }

  public String summary() {
    return "geothermal interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
