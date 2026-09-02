package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;
import java.util.Optional;

/** Provenance and coarse fixed-point budget attached to a resolved present-surface parcel. */
public record SurfaceMaterialContext(
    SurfaceMaterialKind kind,
    StableId materialBodyId,
    List<StableId> sourceBodyIds,
    Optional<ColluvialSourceMix> colluvialSourceMix,
    Optional<StableId> depositId,
    Optional<String> budgetElement,
    Optional<String> budgetUnit,
    long sourceInventoryFixedUnits,
    long trappedInventoryFixedUnits) {
  public SurfaceMaterialContext {
    if (kind == null
        || materialBodyId == null
        || colluvialSourceMix == null
        || depositId == null
        || budgetElement == null
        || budgetUnit == null) {
      throw new IllegalArgumentException("surface material context identity must be complete");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()) {
      throw new IllegalArgumentException("surface material must name a source body");
    }
    if (sourceInventoryFixedUnits < 0
        || trappedInventoryFixedUnits < 0
        || trappedInventoryFixedUnits > sourceInventoryFixedUnits) {
      throw new IllegalArgumentException("surface material inventory values are invalid");
    }
    if ((kind == SurfaceMaterialKind.ALLUVIAL_PLACER) != depositId.isPresent()) {
      throw new IllegalArgumentException("only alluvial placer material may carry a deposit ID");
    }
    if ((kind == SurfaceMaterialKind.COLLUVIAL_MANTLE) != colluvialSourceMix.isPresent()) {
      throw new IllegalArgumentException(
          "colluvial source mixture is required exactly for colluvial material");
    }
    if (budgetElement.isPresent() != budgetUnit.isPresent()
        || (kind == SurfaceMaterialKind.ALLUVIAL_PLACER) != budgetElement.isPresent()) {
      throw new IllegalArgumentException("placer budget element and unit must be explicit");
    }
    budgetElement.ifPresent(
        element -> {
          if (element.isBlank()) {
            throw new IllegalArgumentException("budget element must be non-blank");
          }
        });
    budgetUnit.ifPresent(
        unit -> {
          if (unit.isBlank()) {
            throw new IllegalArgumentException("budget unit must be non-blank");
          }
        });
    if (kind == SurfaceMaterialKind.ALLUVIAL_PLACER
        && (sourceInventoryFixedUnits == 0 || trappedInventoryFixedUnits == 0)) {
      throw new IllegalArgumentException(
          "placer material requires a positive source and trap budget");
    }
    if (kind != SurfaceMaterialKind.ALLUVIAL_PLACER
        && (sourceInventoryFixedUnits != 0 || trappedInventoryFixedUnits != 0)) {
      throw new IllegalArgumentException(
          "non-placer surface material cannot carry a placer budget");
    }
    if (kind == SurfaceMaterialKind.COLLUVIAL_MANTLE && sourceBodyIds.contains(materialBodyId)) {
      throw new IllegalArgumentException(
          "transported colluvial material must differ from its source body");
    }
    if (colluvialSourceMix.isPresent()
        && !sourceBodyIds.equals(List.of(colluvialSourceMix.orElseThrow().sourceBodyId()))) {
      throw new IllegalArgumentException(
          "colluvial mixture source must be the context's sole source body");
    }
    if ((kind == SurfaceMaterialKind.BEDROCK_OUTCROP
            || kind == SurfaceMaterialKind.IN_SITU_REGOLITH)
        && !sourceBodyIds.contains(materialBodyId)) {
      throw new IllegalArgumentException(
          "in-place surface material must retain its source body identity");
    }
  }
}
