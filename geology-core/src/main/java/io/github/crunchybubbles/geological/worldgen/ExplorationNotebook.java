package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.CanonicalCbor;
import io.github.crunchybubbles.geological.determinism.StableId;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable bounded notebook of player-visible exploration evidence. */
public record ExplorationNotebook(
    String modelVersion,
    String scientificDigest,
    String dimensionProfileId,
    List<ExplorationNotebookEntry> entries) {
  public static final String SCHEMA_ID = "geological:discovery-notebook:v1";
  public static final int MAX_ENTRIES = 256;
  public static final int MAP_CELL_SIZE_BLOCKS = 16;

  public ExplorationNotebook {
    if (modelVersion == null
        || modelVersion.isBlank()
        || scientificDigest == null
        || scientificDigest.isBlank()
        || dimensionProfileId == null
        || dimensionProfileId.isBlank()
        || entries == null
        || entries.size() > MAX_ENTRIES) {
      throw new IllegalArgumentException("exploration notebook values are invalid");
    }
    entries =
        List.copyOf(entries).stream()
            .sorted(java.util.Comparator.comparing(ExplorationNotebookEntry::entryId))
            .toList();
    if (entries.stream().map(ExplorationNotebookEntry::entryId).distinct().count()
        != entries.size()) {
      throw new IllegalArgumentException("notebook entry IDs must be unique");
    }
    if (entries.stream()
        .anyMatch(entry -> !dimensionProfileId.equals(entry.dimensionProfileId()))) {
      throw new IllegalArgumentException(
          "notebook entries must use the notebook dimension profile");
    }
  }

  public static ExplorationNotebook empty(
      String modelVersion, String scientificDigest, String dimensionProfileId) {
    return new ExplorationNotebook(modelVersion, scientificDigest, dimensionProfileId, List.of());
  }

  /** Adds or replaces evidence by stable entry ID, with a hard bound against unbounded saves. */
  public ExplorationNotebook record(ExplorationNotebookEntry entry) {
    Objects.requireNonNull(entry, "notebook entry");
    if (!dimensionProfileId.equals(entry.dimensionProfileId())) {
      throw new IllegalArgumentException("notebook entry dimension does not match");
    }
    List<ExplorationNotebookEntry> updated = new ArrayList<>(entries);
    int existing =
        updated.stream().map(ExplorationNotebookEntry::entryId).toList().indexOf(entry.entryId());
    if (existing >= 0) {
      updated.set(existing, entry);
    } else {
      if (updated.size() >= MAX_ENTRIES) {
        throw new IllegalStateException("discovery notebook is full");
      }
      updated.add(entry);
    }
    return new ExplorationNotebook(modelVersion, scientificDigest, dimensionProfileId, updated);
  }

  /** Atomically records a batch so a full notebook cannot be partially modified. */
  public ExplorationNotebook recordAll(List<ExplorationNotebookEntry> additions) {
    Objects.requireNonNull(additions, "notebook additions");
    List<ExplorationNotebookEntry> copied = List.copyOf(additions);
    Set<StableId> existing =
        new HashSet<>(entries.stream().map(ExplorationNotebookEntry::entryId).toList());
    long newCount =
        copied.stream()
            .map(ExplorationNotebookEntry::entryId)
            .filter(id -> !existing.contains(id))
            .distinct()
            .count();
    if (entries.size() + newCount > MAX_ENTRIES) {
      throw new IllegalStateException(
          "discovery notebook cannot exceed " + MAX_ENTRIES + " entries");
    }
    ExplorationNotebook result = this;
    for (ExplorationNotebookEntry addition : copied) {
      result = result.record(addition);
    }
    return result;
  }

  public ExplorationNotebook forget(StableId entryId) {
    Objects.requireNonNull(entryId, "entry ID");
    return entries.stream().anyMatch(entry -> entry.entryId().equals(entryId))
        ? new ExplorationNotebook(
            modelVersion,
            scientificDigest,
            dimensionProfileId,
            entries.stream().filter(entry -> !entry.entryId().equals(entryId)).toList())
        : this;
  }

  public Optional<ExplorationNotebookEntry> find(StableId entryId) {
    Objects.requireNonNull(entryId, "entry ID");
    return entries.stream().filter(entry -> entry.entryId().equals(entryId)).findFirst();
  }

  /** Derives a bounded map from retained markers; the map itself contains no natural geology. */
  public ExplorationMapSnapshot map(long centerX, long centerZ, int radiusBlocks) {
    if (radiusBlocks < 0 || radiusBlocks > ExplorationMapSnapshot.MAX_RADIUS_BLOCKS) {
      throw new IllegalArgumentException(
          "notebook map radius must be between 0 and " + ExplorationMapSnapshot.MAX_RADIUS_BLOCKS);
    }
    List<ExplorationMapMarker> markers =
        entries.stream()
            .filter(entry -> within(entry.blockX(), centerX, radiusBlocks))
            .filter(entry -> within(entry.blockZ(), centerZ, radiusBlocks))
            .map(
                entry ->
                    new ExplorationMapMarker(
                        entry.entryId(),
                        entry.evidenceKind(),
                        entry.blockX(),
                        entry.blockZ(),
                        Math.floorDiv(entry.blockX(), MAP_CELL_SIZE_BLOCKS),
                        Math.floorDiv(entry.blockZ(), MAP_CELL_SIZE_BLOCKS)))
            .toList();
    return new ExplorationMapSnapshot(
        centerX, centerZ, radiusBlocks, MAP_CELL_SIZE_BLOCKS, markers);
  }

  /** Canonical identity bytes cover only persisted player evidence and frozen model identity. */
  public byte[] canonicalBytes() {
    List<Object> canonicalEntries =
        entries.stream()
            .map(ExplorationNotebook::canonicalEntry)
            .map(value -> (Object) value)
            .toList();
    return CanonicalCbor.encodeTuple(
        SCHEMA_ID, modelVersion, scientificDigest, dimensionProfileId, canonicalEntries);
  }

  public String digest() {
    try {
      return "sha256:"
          + java.util.HexFormat.of()
              .formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalBytes()));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }

  public String summary() {
    return "notebook dimension=%s entries=%d/%d digest=%s"
        .formatted(dimensionProfileId, entries.size(), MAX_ENTRIES, digest());
  }

  private static List<Object> canonicalEntry(ExplorationNotebookEntry entry) {
    return List.of(
        entry.entryId().toString(),
        entry.evidenceKind().name(),
        entry.evidenceId().toString(),
        entry.dimensionProfileId(),
        entry.blockX(),
        entry.blockY(),
        entry.blockZ(),
        entry.evidenceSummary(),
        entry.note(),
        entry.provenanceBodyIds().stream().map(StableId::toString).toList(),
        entry.confidencePpm());
  }

  private static boolean within(long value, long center, int radius) {
    long radiusLong = radius;
    long lower = center < Long.MIN_VALUE + radiusLong ? Long.MIN_VALUE : center - radiusLong;
    long upper = center > Long.MAX_VALUE - radiusLong ? Long.MAX_VALUE : center + radiusLong;
    return value >= lower && value <= upper;
  }
}
