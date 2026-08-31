package io.github.crunchybubbles.geological.query;

public record TileKey(long originX, long originZ, int intervals, int spacing) {
  public TileKey {
    if (intervals <= 0 || intervals > 1024) {
      throw new IllegalArgumentException("intervals must lie in [1, 1024]");
    }
    if (spacing <= 0 || spacing > 4096) {
      throw new IllegalArgumentException("spacing must lie in [1, 4096]");
    }
    Math.addExact(originX, Math.multiplyExact((long) intervals, spacing));
    Math.addExact(originZ, Math.multiplyExact((long) intervals, spacing));
  }

  public int samplesPerSide() {
    return intervals + 1;
  }
}
