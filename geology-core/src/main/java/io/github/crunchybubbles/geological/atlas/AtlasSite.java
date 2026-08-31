package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;

/** Immutable identity and location of a jittered atlas ownership site. */
public record AtlasSite(StableId id, Point2 point) {
  public AtlasSite {
    if (id == null || point == null) {
      throw new IllegalArgumentException("atlas site fields must be present");
    }
  }
}
