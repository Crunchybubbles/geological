package io.github.crunchybubbles.geological.worldgen;

/** Half-open vertical interval in one Nether column. */
public record NetherTerrainInterval(int minYInclusive, int maxYExclusive) {
  public NetherTerrainInterval {
    if (maxYExclusive <= minYInclusive) {
      throw new IllegalArgumentException("Nether terrain interval must have positive height");
    }
  }
}
