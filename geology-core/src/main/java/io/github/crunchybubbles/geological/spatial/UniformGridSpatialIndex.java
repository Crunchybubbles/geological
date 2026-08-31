package io.github.crunchybubbles.geological.spatial;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable finite 2-D grid index for one province's bounded 3-D candidates. */
public final class UniformGridSpatialIndex {
  private static final int MAX_CELLS_PER_CANDIDATE = 1024;
  private static final int MAX_QUERY_CELLS = 4096;
  private static final Comparator<SpatialCandidate> STABLE_ORDER =
      Comparator.comparing(SpatialCandidate::id).thenComparing(SpatialCandidate::kind);

  private final double cellSize;
  private final List<SpatialCandidate> allCandidates;
  private final Map<GridKey, List<SpatialCandidate>> buckets;

  public UniformGridSpatialIndex(double cellSize, List<SpatialCandidate> candidates) {
    if (!(cellSize > 0.0) || !Double.isFinite(cellSize)) {
      throw new IllegalArgumentException("index cell size must be positive and finite");
    }
    this.cellSize = cellSize;
    this.allCandidates = candidates.stream().sorted(STABLE_ORDER).toList();
    Map<GridKey, List<SpatialCandidate>> mutableBuckets = new HashMap<>();
    for (SpatialCandidate candidate : allCandidates) {
      Bounds2D bounds = candidate.bounds().horizontal();
      long minX = cell(bounds.minX());
      long minZ = cell(bounds.minZ());
      long maxX = cell(bounds.maxX());
      long maxZ = cell(bounds.maxZ());
      requireBoundedCellCount(minX, minZ, maxX, maxZ, MAX_CELLS_PER_CANDIDATE, "candidate");
      for (long x = minX; x <= maxX; x++) {
        for (long z = minZ; z <= maxZ; z++) {
          mutableBuckets
              .computeIfAbsent(new GridKey(x, z), ignored -> new ArrayList<>())
              .add(candidate);
        }
      }
    }
    Map<GridKey, List<SpatialCandidate>> frozen = new HashMap<>();
    mutableBuckets.forEach(
        (key, values) -> frozen.put(key, values.stream().sorted(STABLE_ORDER).toList()));
    this.buckets = Map.copyOf(frozen);
  }

  public List<SpatialCandidate> at(Point2 point) {
    return buckets.getOrDefault(new GridKey(cell(point.x()), cell(point.z())), List.of()).stream()
        .filter(candidate -> candidate.bounds().containsHorizontal(point))
        .toList();
  }

  public List<SpatialCandidate> intersecting(Bounds2D bounds) {
    long minX = cell(bounds.minX());
    long minZ = cell(bounds.minZ());
    long maxX = cell(bounds.maxX());
    long maxZ = cell(bounds.maxZ());
    requireBoundedCellCount(minX, minZ, maxX, maxZ, MAX_QUERY_CELLS, "query");
    Map<StableId, SpatialCandidate> unique = new TreeMap<>();
    for (long x = minX; x <= maxX; x++) {
      for (long z = minZ; z <= maxZ; z++) {
        for (SpatialCandidate candidate : buckets.getOrDefault(new GridKey(x, z), List.of())) {
          if (candidate.bounds().horizontal().intersects(bounds)) {
            unique.put(candidate.id(), candidate);
          }
        }
      }
    }
    return unique.values().stream().sorted(STABLE_ORDER).toList();
  }

  public List<SpatialCandidate> allCandidates() {
    return allCandidates;
  }

  private long cell(double coordinate) {
    double scaled = StrictMath.floor(coordinate / cellSize);
    if (scaled < Long.MIN_VALUE || scaled > Long.MAX_VALUE) {
      throw new IllegalArgumentException("coordinate is outside the spatial-index range");
    }
    return (long) scaled;
  }

  private static void requireBoundedCellCount(
      long minX, long minZ, long maxX, long maxZ, int cap, String subject) {
    long width = maxX - minX + 1;
    long height = maxZ - minZ + 1;
    if (width <= 0 || height <= 0 || width > cap / height) {
      throw new IllegalArgumentException(subject + " exceeds the bounded spatial-index cell cap");
    }
  }

  private record GridKey(long x, long z) {}
}
