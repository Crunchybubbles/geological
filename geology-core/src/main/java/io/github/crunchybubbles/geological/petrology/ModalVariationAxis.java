package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** One authored, mass-conserving covariance direction for body-scale mineral modes. */
public record ModalVariationAxis(String id, Map<String, Long> loadingsPpm) {
  private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");

  public ModalVariationAxis {
    if (id == null || !ID.matcher(id).matches() || loadingsPpm == null) {
      throw new IllegalArgumentException("modal variation axis identity must be complete");
    }
    TreeMap<String, Long> sorted = new TreeMap<>();
    loadingsPpm.forEach(
        (mineralId, loading) -> {
          if (mineralId == null
              || mineralId.isBlank()
              || loading == null
              || loading == 0
              || loading < -MineralAssemblage.SCALE
              || loading > MineralAssemblage.SCALE) {
            throw new IllegalArgumentException(
                "modal variation loadings must be named, non-zero, and bounded");
          }
          sorted.put(mineralId, loading);
        });
    if (sorted.size() < 2) {
      throw new IllegalArgumentException("modal variation axis must affect at least two minerals");
    }
    long sum = sorted.values().stream().mapToLong(Long::longValue).sum();
    if (sum != 0) {
      throw new IllegalArgumentException("modal variation axis loadings must sum to zero");
    }
    loadingsPpm = Collections.unmodifiableMap(sorted);
  }
}
