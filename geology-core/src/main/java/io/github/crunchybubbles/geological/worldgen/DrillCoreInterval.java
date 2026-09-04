package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** One contiguous, bounded interval in a transient drill-core log. */
public record DrillCoreInterval(
    StableId intervalId,
    int minYInclusive,
    int maxYExclusive,
    MaterialState material,
    String rockDefinitionId,
    RockTexture texture,
    Map<String, Long> visibleConstituentsPpm,
    Map<ChemicalElement, Long> indicatorSignalsPpm,
    List<StableId> provenanceBodyIds,
    int confidencePpm) {
  public DrillCoreInterval {
    if (intervalId == null
        || maxYExclusive <= minYInclusive
        || material == null
        || rockDefinitionId == null
        || rockDefinitionId.isBlank()
        || texture == null
        || visibleConstituentsPpm == null
        || visibleConstituentsPpm.isEmpty()
        || indicatorSignalsPpm == null
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || confidencePpm < 0
        || confidencePpm > 1_000_000) {
      throw new IllegalArgumentException("drill-core interval values are invalid");
    }
    visibleConstituentsPpm = boundedModes(visibleConstituentsPpm, "visible constituents");
    indicatorSignalsPpm = boundedIndicators(indicatorSignalsPpm);
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
    if (!provenanceBodyIds.contains(material.rockBodyId())) {
      throw new IllegalArgumentException(
          "drill interval provenance must include its material body");
    }
  }

  public String summary() {
    return "interval id=%s y=%d..%d lithology=%s rock=%s texture=%s visible=%s indicators=%s confidence=%d bodies=%d"
        .formatted(
            intervalId,
            minYInclusive,
            maxYExclusive,
            material.lithology(),
            rockDefinitionId,
            texture,
            visibleConstituentsPpm,
            indicatorSignalsPpm,
            confidencePpm,
            provenanceBodyIds.size());
  }

  private static Map<String, Long> boundedModes(Map<String, Long> source, String name) {
    TreeMap<String, Long> sorted = new TreeMap<>();
    source.forEach(
        (id, amount) -> {
          if (id == null || id.isBlank() || amount == null || amount <= 0 || amount > 1_000_000) {
            throw new IllegalArgumentException(name + " must contain positive bounded values");
          }
          sorted.put(id, amount);
        });
    if (sorted.values().stream().mapToLong(Long::longValue).sum() > 1_000_000L) {
      throw new IllegalArgumentException(name + " cannot exceed the normalized sample scale");
    }
    return Collections.unmodifiableMap(sorted);
  }

  private static Map<ChemicalElement, Long> boundedIndicators(Map<ChemicalElement, Long> source) {
    TreeMap<ChemicalElement, Long> sorted = new TreeMap<>();
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount <= 0 || amount > 1_000_000) {
            throw new IllegalArgumentException("indicator signals must be positive bounded values");
          }
          sorted.put(element, amount);
        });
    return Collections.unmodifiableMap(sorted);
  }
}
