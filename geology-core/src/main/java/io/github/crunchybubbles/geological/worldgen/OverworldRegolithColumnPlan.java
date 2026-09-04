package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.List;

/** Bounded top-of-solid regolith/clue projection for one authorized Overworld column. */
public record OverworldRegolithColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    int regolithMinYInclusive,
    MaterialState surfaceMaterial,
    SurfaceClueKind clueKind,
    double weatheringDepth,
    double slope,
    double flowAccumulation,
    double channelDistance,
    StableId materialBodyId,
    List<StableId> sourceBodyIds,
    List<StableId> depositIds) {
  public OverworldRegolithColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || regolithMinYInclusive < minYInclusive
        || regolithMinYInclusive > solidMaxYExclusive
        || surfaceMaterial == null
        || clueKind == null
        || !Double.isFinite(weatheringDepth)
        || weatheringDepth < 0.0
        || !Double.isFinite(slope)
        || slope < 0.0
        || !Double.isFinite(flowAccumulation)
        || flowAccumulation < 0.0
        || flowAccumulation > 1.0
        || !Double.isFinite(channelDistance)
        || channelDistance < 0.0
        || materialBodyId == null
        || sourceBodyIds == null
        || depositIds == null) {
      throw new IllegalArgumentException("regolith column plan values are invalid");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    depositIds = List.copyOf(depositIds).stream().sorted().toList();
    if (!materialBodyId.equals(surfaceMaterial.rockBodyId())) {
      throw new IllegalArgumentException("regolith material body must match surface material");
    }
    if (!surfaceMaterial.depositIds().equals(depositIds)) {
      throw new IllegalArgumentException("regolith deposit provenance must match surface material");
    }
    if (sourceBodyIds.isEmpty()) {
      throw new IllegalArgumentException("regolith plan must retain at least one source body");
    }
    boolean transported =
        clueKind == SurfaceClueKind.COLLUVIAL_MANTLE || clueKind == SurfaceClueKind.ALLUVIAL_PLACER;
    if (transported == sourceBodyIds.contains(materialBodyId)) {
      throw new IllegalArgumentException(
          "surface source and material body relationship is inconsistent");
    }
    if (clueKind == SurfaceClueKind.BEDROCK_OUTCROP
        && regolithMinYInclusive != solidMaxYExclusive) {
      throw new IllegalArgumentException("bedrock outcrops cannot carry a regolith interval");
    }
  }

  public boolean hasRegolith() {
    return regolithMinYInclusive < solidMaxYExclusive;
  }

  public int regolithDepth() {
    return solidMaxYExclusive - regolithMinYInclusive;
  }
}
