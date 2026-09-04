package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;

/** One contiguous block interval occupied by a carbonatite, alkaline, or kimberlite horizon. */
public record OverworldCarbonatiteKimberliteInterval(
    int minYInclusive, int maxYExclusive, CarbonatiteKimberliteSystemState.Horizon horizon) {
  public OverworldCarbonatiteKimberliteInterval {
    if (minYInclusive >= maxYExclusive || horizon == null) {
      throw new IllegalArgumentException("alkaline complex interval values are invalid");
    }
  }

  public String summary() {
    return "alkaline-complex interval="
        + horizon.kind()
        + " y="
        + minYInclusive
        + ".."
        + maxYExclusive;
  }
}
