package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.List;

/**
 * Immutable, transient evidence a player could record at one Overworld location.
 *
 * <p>The observation intentionally contains only exposed material and causal body references. It is
 * not a persisted geological answer and does not expose an unobserved deposit or assay.
 */
public record OverworldExplorationObservation(
    StableId observationId,
    long blockX,
    int blockY,
    long blockZ,
    ExplorationObservationKind kind,
    MaterialState material,
    MaterialState adjacentMaterial,
    List<StableId> provenanceBodyIds,
    int confidencePpm,
    int observationScaleBlocks) {
  public OverworldExplorationObservation {
    if (observationId == null
        || kind == null
        || material == null
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || confidencePpm < 0
        || confidencePpm > 1_000_000
        || observationScaleBlocks < 1) {
      throw new IllegalArgumentException("exploration observation values are invalid");
    }
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
    if (!provenanceBodyIds.contains(material.rockBodyId())) {
      throw new IllegalArgumentException("observation provenance must include its material body");
    }
    if (kind == ExplorationObservationKind.CONTACT) {
      if (adjacentMaterial == null
          || adjacentMaterial.equals(material)
          || !provenanceBodyIds.contains(adjacentMaterial.rockBodyId())) {
        throw new IllegalArgumentException("contact observations require a distinct adjacent body");
      }
    } else if (adjacentMaterial != null) {
      throw new IllegalArgumentException("only contact observations may have adjacent material");
    }
  }

  /** Compact deterministic text suitable for a notebook/debug command. */
  public String summary() {
    return "observation id=%s kind=%s at=(%d,%d,%d) material=%s adjacent=%s confidence=%d scale=%d bodies=%d"
        .formatted(
            observationId,
            kind,
            blockX,
            blockY,
            blockZ,
            material.lithology(),
            adjacentMaterial == null ? "none" : adjacentMaterial.lithology(),
            confidencePpm,
            observationScaleBlocks,
            provenanceBodyIds.size());
  }
}
