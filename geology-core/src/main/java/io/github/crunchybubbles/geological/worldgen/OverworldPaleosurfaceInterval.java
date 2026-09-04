package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.PaleosurfaceState;

/** One contiguous block interval occupied by a paleosurface or regolith horizon. */
public record OverworldPaleosurfaceInterval(
    int minYInclusive,
    int maxYExclusive,
    PaleosurfaceState profile,
    PaleosurfaceState.Horizon horizon) {
  public OverworldPaleosurfaceInterval {
    if (maxYExclusive <= minYInclusive || profile == null || horizon == null) {
      throw new IllegalArgumentException("paleosurface interval values are invalid");
    }
    if (!profile.horizons().contains(horizon)) {
      throw new IllegalArgumentException("paleosurface interval horizon is not in its profile");
    }
  }

  public String summary() {
    return "paleosurface interval="
        + minYInclusive
        + ".."
        + maxYExclusive
        + " kind="
        + profile.refinementKind()
        + " horizon="
        + horizon.kind()
        + " body="
        + horizon.bodyId();
  }
}
