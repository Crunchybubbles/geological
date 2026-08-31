package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/** Validated ideal endmember and the physical properties needed by Phase 2 queries. */
public record MineralDefinition(
    String id,
    Map<ChemicalElement, Integer> formula,
    double densityGramsPerCubicCentimeter,
    double hardnessMohs,
    double weatheringResistance) {
  public MineralDefinition {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("mineral ID must be present");
    }
    EnumMap<ChemicalElement, Integer> copied = new EnumMap<>(ChemicalElement.class);
    formula.forEach(
        (element, count) -> {
          if (element == null || count == null || count <= 0) {
            throw new IllegalArgumentException("mineral formula counts must be positive");
          }
          copied.put(element, count);
        });
    if (copied.isEmpty()) {
      throw new IllegalArgumentException("mineral formula must contain at least one element");
    }
    formula = Collections.unmodifiableMap(copied);
    requireRange(densityGramsPerCubicCentimeter, 0.1, 30.0, "mineral density");
    requireRange(hardnessMohs, 1.0, 10.0, "Mohs hardness");
    requireRange(weatheringResistance, 0.0, 1.0, "weathering resistance");
  }

  public Map<ChemicalElement, Double> elementMassFractions() {
    double formulaMass =
        formula.entrySet().stream()
            .mapToDouble(entry -> entry.getKey().atomicWeight() * entry.getValue())
            .sum();
    Map<ChemicalElement, Double> fractions = new TreeMap<>();
    formula.forEach(
        (element, count) -> fractions.put(element, element.atomicWeight() * count / formulaMass));
    return Collections.unmodifiableMap(fractions);
  }

  private static void requireRange(double value, double minimum, double maximum, String name) {
    if (!Double.isFinite(value) || value < minimum || value > maximum) {
      throw new IllegalArgumentException(name + " must lie in [" + minimum + ", " + maximum + "]");
    }
  }
}
