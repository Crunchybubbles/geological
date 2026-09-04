package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read-only vertical cross-section made from contiguous transient drill-core logs. */
public record OverworldVerticalSectionTrace(
    StableId sectionId,
    Axis axis,
    long originX,
    long originZ,
    int length,
    int depthBlocks,
    List<DrillCoreLog> columns,
    List<StableId> provenanceBodyIds,
    int materialEvaluations) {
  public static final int MAX_LENGTH = 64;

  public OverworldVerticalSectionTrace {
    if (sectionId == null
        || axis == null
        || length < 1
        || length > MAX_LENGTH
        || depthBlocks < 1
        || depthBlocks > OverworldDrillCorePlanner.MAX_CORE_DEPTH_BLOCKS
        || columns == null
        || columns.size() != length
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || materialEvaluations <= 0) {
      throw new IllegalArgumentException("vertical section values are invalid");
    }
    columns =
        List.copyOf(columns).stream()
            .sorted(
                Comparator.comparingLong(DrillCoreLog::blockX)
                    .thenComparingLong(DrillCoreLog::blockZ))
            .toList();
    for (int index = 0; index < length; index++) {
      long expectedX = axis == Axis.X ? originX + index : originX;
      long expectedZ = axis == Axis.Z ? originZ + index : originZ;
      DrillCoreLog column = columns.get(index);
      if (column.blockX() != expectedX
          || column.blockZ() != expectedZ
          || column.maxYExclusive() - column.minYInclusive() > depthBlocks) {
        throw new IllegalArgumentException(
            "vertical section columns must be contiguous and bounded");
      }
    }
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
  }

  /** Compact deterministic text suitable for a server command or review trace. */
  public String summary() {
    return "vertical-section id=%s axis=%s origin=(%d,%d) length=%d depth=%d columns=%d evaluations=%d lithology=%s bodies=%d"
        .formatted(
            sectionId,
            axis,
            originX,
            originZ,
            length,
            depthBlocks,
            columns.size(),
            materialEvaluations,
            histogram(
                columns.stream()
                    .flatMap(column -> column.intervals().stream())
                    .map(interval -> interval.material().lithology().name())
                    .toList()),
            provenanceBodyIds.size());
  }

  private static String histogram(List<String> values) {
    Map<String, Long> counts =
        values.stream()
            .collect(
                Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    return counts.toString();
  }
}
