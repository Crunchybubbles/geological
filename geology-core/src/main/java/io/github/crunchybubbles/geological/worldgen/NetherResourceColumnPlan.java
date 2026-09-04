package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import java.util.HashSet;
import java.util.List;

/** Chunk-local Nether material history and resource projection for one block column. */
public record NetherResourceColumnPlan(
    long blockX,
    long blockZ,
    NetherThermalColumnPlan thermal,
    NetherMaterialHistoryState history,
    NetherResourceSystemState resource,
    List<NetherResourceInterval> intervals) {
  public NetherResourceColumnPlan {
    if (thermal == null || history == null || resource == null || intervals == null) {
      throw new IllegalArgumentException("Nether resource column stages are required");
    }
    if (thermal.blockX() != blockX || thermal.blockZ() != blockZ) {
      throw new IllegalArgumentException("Nether resource column coordinates changed");
    }
    if (!thermal.provinceId().equals(history.provinceId())
        || !thermal.provinceId().equals(resource.provinceId())) {
      throw new IllegalArgumentException("Nether resource and terrain province owners diverged");
    }
    intervals = List.copyOf(intervals);
    HashSet<StableId> horizonIds =
        new HashSet<>(
            resource.horizons().stream().map(NetherResourceSystemState.Horizon::bodyId).toList());
    int previousEnd = Integer.MIN_VALUE;
    for (NetherResourceInterval interval : intervals) {
      if (interval == null
          || interval.minYInclusive() < -64
          || interval.maxYExclusive() > 128
          || interval.minYInclusive() < previousEnd
          || !horizonIds.contains(interval.horizon().bodyId())) {
        throw new IllegalArgumentException(
            "Nether resource intervals are out of bounds or unordered");
      }
      for (int y = interval.minYInclusive(); y < interval.maxYExclusive(); y++) {
        if (!thermal.isSolid(y) || thermal.isLava(y)) {
          throw new IllegalArgumentException(
              "Nether resource intervals must occupy solid non-lava blocks");
        }
      }
      previousEnd = interval.maxYExclusive();
    }
    if (resource.status() == FormationStatus.BARREN_SYSTEM && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren Nether resources cannot carry intervals");
    }
  }

  public boolean hasResource() {
    return !intervals.isEmpty();
  }

  public boolean isResource(int blockY) {
    return intervals.stream()
        .anyMatch(
            interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive());
  }
}
