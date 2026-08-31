package io.github.crunchybubbles.geological.petrology;

/** Half-open integer Y interval sharing one coordinate-independent petrologic state. */
public record PetrologicRun(int minYInclusive, int maxYExclusive, PetrologicState state) {
  public PetrologicRun {
    if (maxYExclusive <= minYInclusive) {
      throw new IllegalArgumentException("petrologic run must be non-empty");
    }
    if (state == null) {
      throw new IllegalArgumentException("petrologic run state must be present");
    }
  }

  public boolean contains(int blockY) {
    return blockY >= minYInclusive && blockY < maxYExclusive;
  }
}
