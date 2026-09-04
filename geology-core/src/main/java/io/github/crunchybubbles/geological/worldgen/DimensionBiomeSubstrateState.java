package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only biome/substrate controls at a dimension boundary.
 *
 * <p>This state intentionally contains normalized signals and provenance rather than biome IDs,
 * block states, or write permissions. A platform adapter may consume it while choosing a biome or
 * surface presentation, but it cannot use the state to infer deep material or authorize a chunk
 * mutation.
 */
public record DimensionBiomeSubstrateState(
    String dimensionKey,
    String adapterId,
    Optional<StableId> ownerId,
    String substrateId,
    double primarySignal,
    double secondarySignal,
    Set<String> semanticTags,
    boolean surfaceWaterEligible,
    boolean voidMedium) {
  public DimensionBiomeSubstrateState {
    requireText(dimensionKey, "dimension key");
    requireText(adapterId, "biome/substrate adapter ID");
    if (ownerId == null) {
      throw new IllegalArgumentException("biome/substrate owner is required as an Optional");
    }
    requireText(substrateId, "substrate ID");
    requireUnitInterval(primarySignal, "primary signal");
    requireUnitInterval(secondarySignal, "secondary signal");
    if (semanticTags == null) {
      throw new IllegalArgumentException("biome/substrate semantic tags are required");
    }
    TreeSet<String> canonicalTags = new TreeSet<>();
    for (String tag : semanticTags) {
      requireText(tag, "biome/substrate semantic tag");
      canonicalTags.add(tag);
    }
    semanticTags = Collections.unmodifiableSet(canonicalTags);
    if (voidMedium && surfaceWaterEligible) {
      throw new IllegalArgumentException("void medium cannot be surface-water eligible");
    }
    if (!voidMedium && ownerId.isEmpty()) {
      throw new IllegalArgumentException("non-void biome/substrate state needs an owner ID");
    }
  }

  private static void requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must be present");
    }
  }

  private static void requireUnitInterval(double value, String label) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(label + " must be finite in [0,1]");
    }
  }
}
