package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.List;

public record MacroDomain(
    StableId id,
    CellKey homeCell,
    Point2 site,
    CrustClass crustClass,
    double basementAgeMa,
    List<StableId> adjacentDomainIds) {
  public MacroDomain {
    if (id == null || homeCell == null || site == null || crustClass == null) {
      throw new IllegalArgumentException("macro-domain identity and context are required");
    }
    if (!Double.isFinite(basementAgeMa) || basementAgeMa <= 0.0) {
      throw new IllegalArgumentException("basement age must be positive and finite");
    }
    adjacentDomainIds = List.copyOf(adjacentDomainIds);
  }
}
