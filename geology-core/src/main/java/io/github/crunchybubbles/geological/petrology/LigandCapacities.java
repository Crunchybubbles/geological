package io.github.crunchybubbles.geological.petrology;

/** Independent ordinal 0..3 transport-capacity axes for common ligand families. */
public record LigandCapacities(int chloride, int reducedSulfur, int carbonate, int fluorineBoron) {
  public LigandCapacities {
    requireOrdinal(chloride, "chloride");
    requireOrdinal(reducedSulfur, "reduced sulfur");
    requireOrdinal(carbonate, "carbonate");
    requireOrdinal(fluorineBoron, "fluorine/boron");
  }

  private static void requireOrdinal(int value, String name) {
    if (value < 0 || value > 3) {
      throw new IllegalArgumentException(name + " ligand capacity must lie in [0, 3]");
    }
  }
}
