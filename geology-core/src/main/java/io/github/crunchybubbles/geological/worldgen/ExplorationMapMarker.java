package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;

/** A map marker derived from one persisted notebook entry. */
public record ExplorationMapMarker(
    StableId entryId,
    NotebookEvidenceKind evidenceKind,
    long blockX,
    long blockZ,
    long cellX,
    long cellZ) {
  public ExplorationMapMarker {
    if (entryId == null || evidenceKind == null) {
      throw new IllegalArgumentException("map marker identity is required");
    }
  }
}
