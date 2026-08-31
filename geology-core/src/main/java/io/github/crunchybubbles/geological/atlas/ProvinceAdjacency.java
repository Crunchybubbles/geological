package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;

public record ProvinceAdjacency(StableId neighborId, BoundaryType boundaryType) {
  public ProvinceAdjacency {
    if (neighborId == null || boundaryType == null) {
      throw new IllegalArgumentException("adjacency values are required");
    }
  }
}
