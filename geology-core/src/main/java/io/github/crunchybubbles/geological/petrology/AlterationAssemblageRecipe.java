package io.github.crunchybubbles.geological.petrology;

import java.util.EnumSet;
import java.util.List;

/** One protolith-family-conditioned target assemblage for an alteration response. */
public record AlterationAssemblageRecipe(
    List<GeneticFamily> protolithFamilies, MineralAssemblage targetAssemblage) {
  public AlterationAssemblageRecipe {
    if (protolithFamilies == null || targetAssemblage == null) {
      throw new IllegalArgumentException("alteration assemblage recipe must be complete");
    }
    if (protolithFamilies.isEmpty()) {
      throw new IllegalArgumentException("alteration recipe protolith families must be non-empty");
    }
    EnumSet<GeneticFamily> unique = EnumSet.copyOf(protolithFamilies);
    if (unique.size() != protolithFamilies.size()) {
      throw new IllegalArgumentException("alteration recipe protolith families must be unique");
    }
    protolithFamilies = unique.stream().toList();
  }

  public boolean appliesTo(GeneticFamily family) {
    return protolithFamilies.contains(family);
  }
}
