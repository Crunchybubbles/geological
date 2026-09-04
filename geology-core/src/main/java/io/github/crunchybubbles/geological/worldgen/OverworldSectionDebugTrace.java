package io.github.crunchybubbles.geological.worldgen;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable, read-only linear cross-section overlay assembled from column traces. */
public record OverworldSectionDebugTrace(
    Axis axis, long originX, long originZ, int length, List<OverworldColumnDebugTrace> columns) {
  public static final int MAX_LENGTH = 256;

  public OverworldSectionDebugTrace {
    if (axis == null || length < 1 || length > MAX_LENGTH || columns == null) {
      throw new IllegalArgumentException("section axis, length, or columns are invalid");
    }
    if (columns.size() != length) {
      throw new IllegalArgumentException("section must contain its complete linear footprint");
    }
    columns = List.copyOf(columns);
    for (int index = 0; index < length; index++) {
      long expectedX = axis == Axis.X ? originX + index : originX;
      long expectedZ = axis == Axis.Z ? originZ + index : originZ;
      OverworldColumnDebugTrace column = columns.get(index);
      if (column.blockX() != expectedX || column.blockZ() != expectedZ) {
        throw new IllegalArgumentException("section columns must be contiguous in axis order");
      }
    }
  }

  /** Compact deterministic text suitable for a server command or log line. */
  public String summary() {
    return "section axis=%s origin=(%d,%d) length=%d lithology=%s clues=%s"
        .formatted(
            axis,
            originX,
            originZ,
            length,
            histogram(column -> column.surfaceMaterial().lithology().name()),
            histogram(column -> column.clueKind().name()));
  }

  private String histogram(Function<OverworldColumnDebugTrace, String> classifier) {
    Map<String, Long> counts =
        columns.stream()
            .map(classifier)
            .collect(
                Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    return counts.toString();
  }

  public enum Axis {
    X,
    Z
  }
}
