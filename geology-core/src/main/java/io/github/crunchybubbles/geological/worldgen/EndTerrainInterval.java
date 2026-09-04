package io.github.crunchybubbles.geological.worldgen;

/** Half-open Y interval in a bounded End island body. */
public record EndTerrainInterval(int minYInclusive, int maxYExclusive) {
  public EndTerrainInterval {
    if (maxYExclusive <= minYInclusive) {
      throw new IllegalArgumentException("End terrain intervals must have positive height");
    }
  }
}
