package io.github.crunchybubbles.geological.query;

/** Half-open integer Y interval sharing one exact material state. */
public record MaterialRun(int minYInclusive, int maxYExclusive, MaterialState state) {
  public MaterialRun {
    if (maxYExclusive <= minYInclusive) {
      throw new IllegalArgumentException("material run must be non-empty");
    }
    if (state == null) {
      throw new IllegalArgumentException("material run state must be present");
    }
  }

  public boolean contains(int blockY) {
    return blockY >= minYInclusive && blockY < maxYExclusive;
  }
}
