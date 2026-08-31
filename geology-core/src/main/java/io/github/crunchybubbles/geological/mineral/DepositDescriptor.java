package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.List;

public record DepositDescriptor(
    StableId id,
    StableId mineralSystemId,
    DepositType type,
    Point3 center,
    Bounds2D bounds,
    AgeKey formationAge,
    List<StableId> sourceIds,
    double intensityProxy) {
  public DepositDescriptor {
    if (id == null
        || mineralSystemId == null
        || type == null
        || center == null
        || bounds == null
        || formationAge == null) {
      throw new IllegalArgumentException("deposit descriptor must be complete");
    }
    sourceIds = List.copyOf(sourceIds);
    if (sourceIds.isEmpty()) {
      throw new IllegalArgumentException("deposit must name at least one source");
    }
    if (!Double.isFinite(intensityProxy) || intensityProxy < 0.0 || intensityProxy > 1.0) {
      throw new IllegalArgumentException("intensity proxy must lie in [0, 1]");
    }
  }
}
