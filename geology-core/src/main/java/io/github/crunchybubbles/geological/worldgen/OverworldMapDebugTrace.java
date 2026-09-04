package io.github.crunchybubbles.geological.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable, read-only square map overlay assembled from deterministic column traces. */
public record OverworldMapDebugTrace(
    long centerX, long centerZ, int radius, List<OverworldColumnDebugTrace> columns) {
  public static final int MAX_RADIUS = 32;

  public OverworldMapDebugTrace {
    if (radius < 0 || radius > MAX_RADIUS || columns == null) {
      throw new IllegalArgumentException("map radius or columns are invalid");
    }
    long side = 2L * radius + 1L;
    long expected = side * side;
    if (columns.size() != expected) {
      throw new IllegalArgumentException("map must contain its complete square footprint");
    }
    columns = List.copyOf(columns);
    int index = 0;
    for (long blockX = centerX - radius; blockX <= centerX + radius; blockX++) {
      for (long blockZ = centerZ - radius; blockZ <= centerZ + radius; blockZ++) {
        OverworldColumnDebugTrace column = columns.get(index++);
        if (column.blockX() != blockX || column.blockZ() != blockZ) {
          throw new IllegalArgumentException("map columns must use stable X-then-Z order");
        }
      }
    }
  }

  /** Compact deterministic text suitable for a server command or log line. */
  public String summary() {
    return "map center=(%d,%d) radius=%d columns=%d lithology=%s clues=%s"
        .formatted(
            centerX,
            centerZ,
            radius,
            columns.size(),
            histogram(columns, column -> column.surfaceMaterial().lithology().name()),
            histogram(columns, column -> column.clueKind().name()));
  }

  private static String histogram(
      List<OverworldColumnDebugTrace> columns,
      Function<OverworldColumnDebugTrace, String> classifier) {
    Map<String, Long> counts =
        columns.stream()
            .map(classifier)
            .collect(
                Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    return Objects.toString(counts);
  }
}
