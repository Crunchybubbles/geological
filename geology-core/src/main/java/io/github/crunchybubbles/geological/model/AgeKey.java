package io.github.crunchybubbles.geological.model;

/** Ma before present plus a deterministic tie-break ordinal; larger ages are older. */
public record AgeKey(double ageMa, int ordinal) implements Comparable<AgeKey> {
  public AgeKey {
    if (!Double.isFinite(ageMa) || ageMa < 0.0) {
      throw new IllegalArgumentException("age must be finite and non-negative");
    }
    if (ordinal < 0) {
      throw new IllegalArgumentException("ordinal must be non-negative");
    }
  }

  @Override
  public int compareTo(AgeKey other) {
    int ageComparison = Double.compare(other.ageMa, ageMa);
    return ageComparison != 0 ? ageComparison : Integer.compare(ordinal, other.ordinal);
  }

  public boolean youngerThan(AgeKey other) {
    return ageMa < other.ageMa || (ageMa == other.ageMa && ordinal > other.ordinal);
  }
}
