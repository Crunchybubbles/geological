package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Coarse hand-sample identification derived from a sampled material state.
 *
 * <p>Only major visible constituent modes are reported. Sub-visible constituents remain behind the
 * {@link #assayRequired()} flag so a hand sample cannot reveal hidden assay truth for free.
 */
public record HandSampleIdentification(
    StableId sampleId,
    long blockX,
    int blockY,
    long blockZ,
    String samplingContext,
    MaterialState material,
    String rockDefinitionId,
    RockTexture texture,
    Overprint overprint,
    Map<String, Long> visibleConstituentsPpm,
    boolean assayRequired,
    List<StableId> provenanceBodyIds,
    int confidencePpm) {
  public HandSampleIdentification {
    if (sampleId == null
        || samplingContext == null
        || samplingContext.isBlank()
        || material == null
        || rockDefinitionId == null
        || rockDefinitionId.isBlank()
        || texture == null
        || overprint == null
        || visibleConstituentsPpm == null
        || visibleConstituentsPpm.isEmpty()
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || confidencePpm < 0
        || confidencePpm > 1_000_000) {
      throw new IllegalArgumentException("hand-sample identification values are invalid");
    }
    TreeMap<String, Long> visible = new TreeMap<>();
    visibleConstituentsPpm.forEach(
        (id, amount) -> {
          if (id == null || id.isBlank() || amount == null || amount <= 0 || amount > 1_000_000) {
            throw new IllegalArgumentException("visible hand-sample modes are invalid");
          }
          visible.put(id, amount);
        });
    visibleConstituentsPpm = Collections.unmodifiableMap(visible);
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
    if (!provenanceBodyIds.contains(material.rockBodyId())) {
      throw new IllegalArgumentException("hand-sample provenance must include its material body");
    }
  }

  /** Compact deterministic text suitable for a player-facing sample readout. */
  public String summary() {
    return "hand-sample id=%s at=(%d,%d,%d) context=%s lithology=%s rock=%s texture=%s overprint=%s visible=%s assayRequired=%s confidence=%d bodies=%d"
        .formatted(
            sampleId,
            blockX,
            blockY,
            blockZ,
            samplingContext,
            material.lithology(),
            rockDefinitionId,
            texture,
            overprint,
            visibleConstituentsPpm,
            assayRequired,
            confidencePpm,
            provenanceBodyIds.size());
  }
}
