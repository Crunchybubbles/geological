package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.BasinHydrothermalSystemState;

/** One contiguous block interval occupied by a basin-hydrothermal horizon. */
public record OverworldBasinHydrothermalInterval(
    int minYInclusive, int maxYExclusive, BasinHydrothermalSystemState.Horizon horizon) {
  public OverworldBasinHydrothermalInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("basin hydrothermal interval values are invalid");
    }
  }

  public String summary() {
    return "basin hydrothermal interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
