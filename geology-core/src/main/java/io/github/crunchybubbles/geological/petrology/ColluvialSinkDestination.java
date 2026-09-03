package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Optional;

/** Phase 1 surface/bedrock evidence at a representative colluvial sink position. */
public record ColluvialSinkDestination(
    ColluvialSinkState.SinkRole sinkRole,
    Optional<StableId> sourceBodyId,
    int upslopeDistanceBlocks,
    Point2 point,
    StableId receivingProvinceId,
    StableId receivingBedrockBodyId,
    Lithology receivingSurfaceMaterial,
    Overprint receivingSurfaceOverprint,
    Lithology receivingBedrockLithology,
    Overprint receivingBedrockOverprint) {
  public ColluvialSinkDestination {
    if (sinkRole == null
        || sinkRole == ColluvialSinkState.SinkRole.NONE
        || sourceBodyId == null
        || upslopeDistanceBlocks < 0
        || point == null
        || receivingProvinceId == null
        || receivingBedrockBodyId == null
        || receivingSurfaceMaterial == null
        || receivingSurfaceOverprint == null
        || receivingBedrockLithology == null
        || receivingBedrockOverprint == null) {
      throw new IllegalArgumentException("colluvial sink destination evidence is incomplete");
    }
  }
}
