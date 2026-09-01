package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/** Authored bulk chemistry for organic matter, glass, and mineraloids without a crystal formula. */
public record NonCrystallineConstituentDefinition(
    String id,
    MaterialConstituentKind kind,
    Map<ChemicalElement, Long> elementMassPpm,
    double densityGramsPerCubicCentimeter,
    double weatheringResistance)
    implements MaterialConstituentDefinition {
  public NonCrystallineConstituentDefinition {
    if (id == null || id.isBlank() || kind == null || elementMassPpm == null) {
      throw new IllegalArgumentException("non-crystalline constituent must be complete");
    }
    if (kind == MaterialConstituentKind.MINERAL) {
      throw new IllegalArgumentException("non-crystalline constituent cannot use MINERAL kind");
    }
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    elementMassPpm.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount < 0) {
            throw new IllegalArgumentException(
                "non-crystalline element amounts must be non-negative");
          }
          if (amount > 0) {
            copied.put(element, amount);
          }
        });
    long sum = copied.values().stream().mapToLong(Long::longValue).sum();
    if (sum != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "non-crystalline element mass must close to "
              + MaterialAssemblage.SCALE
              + ", found "
              + sum);
    }
    elementMassPpm = Collections.unmodifiableMap(copied);
    requireRange(densityGramsPerCubicCentimeter, 0.1, 30.0, "constituent density");
    requireRange(weatheringResistance, 0.0, 1.0, "weathering resistance");
  }

  @Override
  public Map<ChemicalElement, Double> elementMassFractions() {
    Map<ChemicalElement, Double> fractions = new TreeMap<>();
    elementMassPpm.forEach(
        (element, amount) -> fractions.put(element, amount / (double) MaterialAssemblage.SCALE));
    return Collections.unmodifiableMap(fractions);
  }

  private static void requireRange(double value, double minimum, double maximum, String name) {
    if (!Double.isFinite(value) || value < minimum || value > maximum) {
      throw new IllegalArgumentException(name + " must lie in [" + minimum + ", " + maximum + "]");
    }
  }
}
