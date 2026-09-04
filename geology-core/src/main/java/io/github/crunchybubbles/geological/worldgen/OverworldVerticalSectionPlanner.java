package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Composes a bounded vertical section from surface-collared drill logs. */
public final class OverworldVerticalSectionPlanner {
  private final OverworldRegolithPlanner regolith;
  private final OverworldDrillCorePlanner drill;

  private OverworldVerticalSectionPlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
    this.drill = OverworldDrillCorePlanner.from(regolith);
  }

  public static OverworldVerticalSectionPlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldVerticalSectionPlanner(
        Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Builds a contiguous X- or Z-oriented section with a per-column surface collar. */
  public OverworldVerticalSectionTrace section(
      Axis axis, long originX, long originZ, int length, int depthBlocks) {
    Objects.requireNonNull(axis, "section axis");
    if (length < 1 || length > OverworldVerticalSectionTrace.MAX_LENGTH) {
      throw new IllegalArgumentException(
          "vertical section length must be between 1 and "
              + OverworldVerticalSectionTrace.MAX_LENGTH);
    }
    if (depthBlocks < 1 || depthBlocks > OverworldDrillCorePlanner.MAX_CORE_DEPTH_BLOCKS) {
      throw new IllegalArgumentException(
          "vertical section depth must be between 1 and "
              + OverworldDrillCorePlanner.MAX_CORE_DEPTH_BLOCKS);
    }
    List<DrillCoreLog> columns = new ArrayList<>(length);
    List<StableId> provenance = new ArrayList<>();
    int evaluations = 0;
    for (int index = 0; index < length; index++) {
      long blockX = axis == Axis.X ? originX + index : originX;
      long blockZ = axis == Axis.Z ? originZ + index : originZ;
      DrillCoreLog column = drill.logSurface(blockX, blockZ, depthBlocks);
      columns.add(column);
      provenance.addAll(column.provenanceBodyIds());
      evaluations = Math.addExact(evaluations, column.materialEvaluations());
    }
    StableId sectionId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "vertical-section:" + axis.name().toLowerCase(java.util.Locale.ROOT),
                CellKey.containing("block", originX, originZ, 1),
                (((long) length) << 32) ^ depthBlocks)
            .stableId();
    return new OverworldVerticalSectionTrace(
        sectionId,
        axis,
        originX,
        originZ,
        length,
        depthBlocks,
        columns,
        provenance.stream().distinct().sorted().toList(),
        evaluations);
  }
}
