package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Bounded map projection of persisted notebook markers; no natural geology is stored here. */
public record ExplorationMapSnapshot(
    long centerX,
    long centerZ,
    int radiusBlocks,
    int cellSizeBlocks,
    List<ExplorationMapMarker> markers) {
  public static final int MAX_RADIUS_BLOCKS = 512;
  public static final int MAX_CELL_SIZE_BLOCKS = 64;

  public ExplorationMapSnapshot {
    if (radiusBlocks < 0
        || radiusBlocks > MAX_RADIUS_BLOCKS
        || cellSizeBlocks < 1
        || cellSizeBlocks > MAX_CELL_SIZE_BLOCKS
        || markers == null) {
      throw new IllegalArgumentException("exploration map values are invalid");
    }
    markers =
        List.copyOf(markers).stream()
            .sorted(
                Comparator.comparingLong(ExplorationMapMarker::cellX)
                    .thenComparingLong(ExplorationMapMarker::cellZ)
                    .thenComparing(ExplorationMapMarker::evidenceKind)
                    .thenComparing(ExplorationMapMarker::entryId))
            .toList();
    for (ExplorationMapMarker marker : markers) {
      if (marker.cellX() != Math.floorDiv(marker.blockX(), cellSizeBlocks)
          || marker.cellZ() != Math.floorDiv(marker.blockZ(), cellSizeBlocks)
          || !within(marker.blockX(), centerX, radiusBlocks)
          || !within(marker.blockZ(), centerZ, radiusBlocks)) {
        throw new IllegalArgumentException("map markers must be bounded and cell-indexed");
      }
    }
    if (markers.stream().map(ExplorationMapMarker::entryId).distinct().count() != markers.size()) {
      throw new IllegalArgumentException("map markers must be unique");
    }
  }

  /** Compact deterministic text suitable for a player-facing map readout. */
  public String summary() {
    Map<String, Long> kinds =
        markers.stream()
            .collect(
                Collectors.groupingBy(
                    marker -> marker.evidenceKind().name(), TreeMap::new, Collectors.counting()));
    return "notebook-map center=(%d,%d) radius=%d cell=%d markers=%d kinds=%s"
        .formatted(centerX, centerZ, radiusBlocks, cellSizeBlocks, markers.size(), kinds);
  }

  /** Returns the marker IDs in canonical display order. */
  public List<StableId> markerIds() {
    return markers.stream().map(ExplorationMapMarker::entryId).toList();
  }

  private static boolean within(long value, long center, int radius) {
    long radiusLong = radius;
    long lower = center < Long.MIN_VALUE + radiusLong ? Long.MIN_VALUE : center - radiusLong;
    long upper = center > Long.MAX_VALUE - radiusLong ? Long.MAX_VALUE : center + radiusLong;
    return value >= lower && value <= upper;
  }
}
