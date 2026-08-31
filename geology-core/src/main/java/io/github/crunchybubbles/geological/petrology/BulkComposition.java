package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Formula- and density-derived whole-rock element composition on a one-million-part basis. */
public record BulkComposition(Map<ChemicalElement, Long> elementMassPpm, double density) {
  public BulkComposition {
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    elementMassPpm.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount < 0) {
            throw new IllegalArgumentException("bulk element amounts must be non-negative");
          }
          if (amount > 0) {
            copied.put(element, amount);
          }
        });
    long sum = copied.values().stream().mapToLong(Long::longValue).sum();
    if (sum != MineralAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "bulk element composition must close to " + MineralAssemblage.SCALE + ", found " + sum);
    }
    if (!Double.isFinite(density) || density <= 0.0) {
      throw new IllegalArgumentException("bulk density must be positive and finite");
    }
    elementMassPpm = Collections.unmodifiableMap(copied);
  }
}
