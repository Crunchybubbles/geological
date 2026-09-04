package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.text.Normalizer;
import java.util.List;

/**
 * Bounded player-authored notebook evidence.
 *
 * <p>This record deliberately retains a presentation summary and provenance references only. It
 * does not retain a material state, an assay vector, or an unobserved deposit answer.
 */
public record ExplorationNotebookEntry(
    StableId entryId,
    NotebookEvidenceKind evidenceKind,
    StableId evidenceId,
    String dimensionProfileId,
    long blockX,
    int blockY,
    long blockZ,
    String evidenceSummary,
    String note,
    List<StableId> provenanceBodyIds,
    int confidencePpm) {
  public static final int MAX_SUMMARY_LENGTH = 512;
  public static final int MAX_NOTE_LENGTH = 256;

  public ExplorationNotebookEntry {
    if (entryId == null
        || evidenceKind == null
        || evidenceId == null
        || dimensionProfileId == null
        || dimensionProfileId.isBlank()
        || evidenceSummary == null
        || evidenceSummary.isBlank()
        || note == null
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || confidencePpm < 0
        || confidencePpm > 1_000_000) {
      throw new IllegalArgumentException("exploration notebook entry values are invalid");
    }
    dimensionProfileId = normalize(dimensionProfileId, "dimension profile ID", 128);
    evidenceSummary = normalize(evidenceSummary, "evidence summary", MAX_SUMMARY_LENGTH);
    note = normalize(note, "notebook note", MAX_NOTE_LENGTH);
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().distinct().sorted().toList();
    if (provenanceBodyIds.isEmpty()) {
      throw new IllegalArgumentException("notebook provenance must not be empty");
    }
  }

  /** Converts a transient surface observation into safe, player-visible persisted evidence. */
  public static ExplorationNotebookEntry fromObservation(
      OverworldExplorationObservation observation, String note, String dimensionProfileId) {
    if (observation == null) {
      throw new IllegalArgumentException("observation is required");
    }
    return new ExplorationNotebookEntry(
        observation.observationId(),
        NotebookEvidenceKind.OBSERVATION,
        observation.observationId(),
        dimensionProfileId,
        observation.blockX(),
        observation.blockY(),
        observation.blockZ(),
        observation.summary(),
        note,
        observation.provenanceBodyIds(),
        observation.confidencePpm());
  }

  /** Compact deterministic text suitable for a notebook or map command. */
  public String summary() {
    String suffix = note.isBlank() ? "" : " note=\"" + note + "\"";
    return "notebook-entry id=%s kind=%s evidence=%s at=(%d,%d,%d) confidence=%d bodies=%d%s"
        .formatted(
            entryId,
            evidenceKind,
            evidenceId,
            blockX,
            blockY,
            blockZ,
            confidencePpm,
            provenanceBodyIds.size(),
            suffix);
  }

  private static String normalize(String value, String label, int maxLength) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(label + " exceeds " + maxLength + " characters");
    }
    return normalized;
  }
}
