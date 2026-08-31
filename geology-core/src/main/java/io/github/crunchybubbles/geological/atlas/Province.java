package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Chronicle;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Comparator;
import java.util.List;

public record Province(
    StableId id,
    CellKey homeCell,
    StableId macroDomainId,
    Point2 site,
    double cellSize,
    LocalFrame frame,
    ProvinceGrammar grammar,
    Chronicle chronicle,
    RiftArcGeometry geometry,
    ProvinceProofIds proofIds,
    List<ProvinceAdjacency> adjacency) {
  public Province {
    if (id == null
        || homeCell == null
        || macroDomainId == null
        || site == null
        || frame == null
        || grammar == null
        || chronicle == null
        || geometry == null
        || proofIds == null) {
      throw new IllegalArgumentException("province descriptor must be complete");
    }
    if (!(cellSize > 0.0) || !Double.isFinite(cellSize)) {
      throw new IllegalArgumentException("cellSize must be positive and finite");
    }
    adjacency =
        adjacency.stream().sorted(Comparator.comparing(ProvinceAdjacency::neighborId)).toList();
  }
}
