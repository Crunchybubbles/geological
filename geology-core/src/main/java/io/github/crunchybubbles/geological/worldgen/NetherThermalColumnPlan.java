package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** One deterministic three-dimensional Nether column with roof, floor, and lava boundaries. */
public record NetherThermalColumnPlan(
    long blockX,
    long blockZ,
    StableId provinceId,
    NetherThermalProvinceState.NetherProvinceKind provinceKind,
    int floorY,
    int roofY,
    int lavaLevelY,
    List<NetherTerrainInterval> solidIntervals,
    List<NetherTerrainInterval> lavaIntervals,
    boolean hangingBridge) {
  public NetherThermalColumnPlan {
    if (provinceId == null
        || provinceKind == null
        || solidIntervals == null
        || lavaIntervals == null) {
      throw new IllegalArgumentException("Nether thermal column identities are required");
    }
    if (floorY < -64
        || roofY > 127
        || roofY <= floorY + 8
        || lavaLevelY < -64
        || lavaLevelY > 127) {
      throw new IllegalArgumentException("Nether thermal column boundaries are out of bounds");
    }
    solidIntervals = List.copyOf(solidIntervals);
    lavaIntervals = List.copyOf(lavaIntervals);
    validateIntervals(solidIntervals, "solid");
    validateIntervals(lavaIntervals, "lava");
    if (solidIntervals.stream()
            .anyMatch(interval -> interval.minYInclusive() < -64 || interval.maxYExclusive() > 128)
        || lavaIntervals.stream()
            .anyMatch(
                interval -> interval.minYInclusive() < -64 || interval.maxYExclusive() > 128)) {
      throw new IllegalArgumentException("Nether thermal intervals exceed the vertical envelope");
    }
    for (NetherTerrainInterval lava : lavaIntervals) {
      for (NetherTerrainInterval solid : solidIntervals) {
        if (overlaps(lava, solid)) {
          throw new IllegalArgumentException("Nether lava cannot overlap solid terrain");
        }
      }
    }
  }

  public boolean hasLava() {
    return !lavaIntervals.isEmpty();
  }

  public boolean isSolid(int y) {
    return solidIntervals.stream()
        .anyMatch(interval -> y >= interval.minYInclusive() && y < interval.maxYExclusive());
  }

  public boolean isLava(int y) {
    return lavaIntervals.stream()
        .anyMatch(interval -> y >= interval.minYInclusive() && y < interval.maxYExclusive());
  }

  private static void validateIntervals(List<NetherTerrainInterval> intervals, String label) {
    int previousEnd = Integer.MIN_VALUE;
    for (NetherTerrainInterval interval : intervals) {
      if (interval == null || interval.minYInclusive() < previousEnd) {
        throw new IllegalArgumentException(
            "Nether " + label + " intervals must be sorted and disjoint");
      }
      previousEnd = interval.maxYExclusive();
    }
  }

  private static boolean overlaps(NetherTerrainInterval first, NetherTerrainInterval second) {
    return first.minYInclusive() < second.maxYExclusive()
        && second.minYInclusive() < first.maxYExclusive();
  }
}
