package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** One authored, mass-conserving covariance direction for body-scale constituent modes. */
public record ModalVariationAxis(String id, Map<String, Long> loadingsPpm) {
  private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");

  public ModalVariationAxis {
    if (id == null || !ID.matcher(id).matches() || loadingsPpm == null) {
      throw new IllegalArgumentException("modal variation axis identity must be complete");
    }
    TreeMap<String, Long> sorted = new TreeMap<>();
    loadingsPpm.forEach(
        (constituentId, loading) -> {
          if (constituentId == null
              || constituentId.isBlank()
              || loading == null
              || loading == 0
              || loading < -MaterialAssemblage.SCALE
              || loading > MaterialAssemblage.SCALE) {
            throw new IllegalArgumentException(
                "modal variation loadings must be named, non-zero, and bounded");
          }
          sorted.put(constituentId, loading);
        });
    if (sorted.size() < 2) {
      throw new IllegalArgumentException(
          "modal variation axis must affect at least two constituents");
    }
    long sum = sorted.values().stream().mapToLong(Long::longValue).sum();
    if (sum != 0) {
      throw new IllegalArgumentException("modal variation axis loadings must sum to zero");
    }
    loadingsPpm = Collections.unmodifiableMap(sorted);
  }
}
