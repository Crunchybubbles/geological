package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, platform-neutral debug projection for one dimension/chunk worldgen boundary.
 *
 * <p>The trace carries identity, owner provenance, bounded count summaries, and the capability
 * contract that a platform adapter must preserve. It contains no block states and never grants a
 * write permission.
 */
public record DimensionWorldgenTrace(
    String dimensionKey,
    String profileId,
    String modelVersion,
    String scientificDigest,
    long worldSeed,
    long chunkX,
    long chunkZ,
    StableId chunkId,
    String ownerKind,
    List<StableId> ownerIds,
    int columnsVisited,
    int solidColumns,
    int voidColumns,
    int fluidOrVoidColumns,
    int solidIntervalCount,
    int provenanceIntervalCount,
    int specialColumnCount,
    int protectedColumnCount,
    List<String> allowedProcessFamilies,
    List<String> forbiddenProcessFamilies,
    List<String> fluidMedia,
    String boundaryTerrainModel,
    boolean seamStable,
    boolean topologyValid) {
  public DimensionWorldgenTrace {
    requireText(dimensionKey, "dimension key");
    requireText(profileId, "profile ID");
    requireText(modelVersion, "model version");
    requireText(scientificDigest, "scientific digest");
    if (!scientificDigest.matches("sha256:[0-9a-f]{64}")) {
      throw new IllegalArgumentException("scientific digest must be sha256 hex");
    }
    Objects.requireNonNull(chunkId, "chunk ID");
    requireText(ownerKind, "owner kind");
    if (columnsVisited <= 0
        || solidColumns < 0
        || voidColumns < 0
        || solidColumns > columnsVisited
        || voidColumns > columnsVisited
        || solidColumns + voidColumns > columnsVisited
        || fluidOrVoidColumns < 0
        || fluidOrVoidColumns > columnsVisited
        || solidIntervalCount < 0
        || provenanceIntervalCount < 0
        || specialColumnCount < 0
        || specialColumnCount > columnsVisited
        || protectedColumnCount < 0
        || protectedColumnCount > columnsVisited) {
      throw new IllegalArgumentException("dimension worldgen trace counts are invalid");
    }
    ownerIds = sortedUnique(ownerIds, "owner IDs");
    allowedProcessFamilies = sortedUniqueText(allowedProcessFamilies, "allowed process families");
    forbiddenProcessFamilies =
        sortedUniqueText(forbiddenProcessFamilies, "forbidden process families");
    fluidMedia = sortedUniqueText(fluidMedia, "fluid media");
    requireText(boundaryTerrainModel, "boundary terrain model");
  }

  /** Compact deterministic text suitable for a server debug command or adapter log. */
  public String summary() {
    return "dimension="
        + dimensionKey
        + " profile="
        + profileId
        + " chunk=("
        + chunkX
        + ","
        + chunkZ
        + ") owners="
        + ownerIds.size()
        + " columns="
        + columnsVisited
        + " solid="
        + solidColumns
        + " void="
        + voidColumns
        + " special="
        + specialColumnCount
        + " protected="
        + protectedColumnCount
        + " seamStable="
        + seamStable
        + " topologyValid="
        + topologyValid;
  }

  private static List<StableId> sortedUnique(List<StableId> values, String label) {
    Objects.requireNonNull(values, label);
    List<StableId> sorted = new ArrayList<>(values);
    if (sorted.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(label + " cannot contain null");
    }
    sorted.sort(StableId::compareTo);
    if (sorted.size() != new HashSet<>(sorted).size()) {
      throw new IllegalArgumentException(label + " must be unique");
    }
    return List.copyOf(sorted);
  }

  private static List<String> sortedUniqueText(List<String> values, String label) {
    Objects.requireNonNull(values, label);
    List<String> sorted = new ArrayList<>(values);
    if (sorted.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(label + " cannot contain blank values");
    }
    sorted.sort(String::compareTo);
    if (sorted.size() != new HashSet<>(sorted).size()) {
      throw new IllegalArgumentException(label + " must be unique");
    }
    return List.copyOf(sorted);
  }

  private static void requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must be present");
    }
  }
}
