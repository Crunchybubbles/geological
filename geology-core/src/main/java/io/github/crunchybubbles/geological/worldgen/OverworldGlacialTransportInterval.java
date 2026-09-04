package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.GlacialTransportState;

/** One contiguous block interval occupied by a glacial transport horizon. */
public record OverworldGlacialTransportInterval(
    int minYInclusive, int maxYExclusive, GlacialTransportState.Horizon horizon) {
  public OverworldGlacialTransportInterval {
    if (maxYExclusive <= minYInclusive || horizon == null) {
      throw new IllegalArgumentException("glacial interval values are invalid");
    }
  }

  public String summary() {
    return "glacial interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
