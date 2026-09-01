package io.github.crunchybubbles.geological.petrology;

import java.util.Map;

/** Shared composition contract for crystalline and non-crystalline rock constituents. */
public interface MaterialConstituentDefinition {
  String id();

  MaterialConstituentKind kind();

  Map<ChemicalElement, Double> elementMassFractions();

  double densityGramsPerCubicCentimeter();

  double weatheringResistance();
}
