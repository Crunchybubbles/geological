package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds deterministic Nether material-history and source-linked resource column plans. */
public final class NetherResourcePlanner {
  private final NetherThermalTerrainCompiler terrain;

  private NetherResourcePlanner(NetherThermalTerrainCompiler terrain) {
    this.terrain = Objects.requireNonNull(terrain, "Nether terrain compiler");
  }

  public static NetherResourcePlanner from(NetherThermalTerrainCompiler terrain) {
    return new NetherResourcePlanner(terrain);
  }

  public NetherResourceColumnPlan planColumn(long blockX, long blockZ) {
    return planColumn(terrain.planColumn(blockX, blockZ));
  }

  public NetherResourceChunkPlan plan(long chunkX, long chunkZ) {
    NetherThermalChunkPlan thermalChunk = terrain.plan(chunkX, chunkZ);
    List<NetherResourceColumnPlan> columns = new ArrayList<>(256);
    for (NetherThermalColumnPlan thermalColumn : thermalChunk.columns()) {
      columns.add(planColumn(thermalColumn));
    }
    return new NetherResourceChunkPlan(chunkX, chunkZ, thermalChunk.bounds(), columns);
  }

  public NetherMaterialHistoryState historyAt(long blockX, long blockZ) {
    return NetherMaterialHistoryState.from(
        terrain.provinceAt(blockX, blockZ), terrain.worldIdentity());
  }

  public NetherThermalTerrainCompiler terrain() {
    return terrain;
  }

  private NetherResourceColumnPlan planColumn(NetherThermalColumnPlan thermalColumn) {
    NetherThermalProvinceState province =
        terrain.provinceAt(thermalColumn.blockX(), thermalColumn.blockZ());
    NetherMaterialHistoryState history =
        NetherMaterialHistoryState.from(province, terrain.worldIdentity());
    NetherResourceSystemState resource =
        NetherResourceSystemState.proofFor(province, history, terrain.worldIdentity());
    List<NetherResourceInterval> intervals = classify(thermalColumn, resource);
    return new NetherResourceColumnPlan(
        thermalColumn.blockX(),
        thermalColumn.blockZ(),
        thermalColumn,
        history,
        resource,
        intervals);
  }

  private static List<NetherResourceInterval> classify(
      NetherThermalColumnPlan thermal, NetherResourceSystemState resource) {
    if (resource.status() != io.github.crunchybubbles.geological.mineral.FormationStatus.FORMED) {
      return List.of();
    }
    List<NetherResourceInterval> intervals = new ArrayList<>();
    NetherResourceSystemState.Horizon previous = null;
    int intervalStart = -64;
    for (int blockY = -64; blockY < 128; blockY++) {
      NetherResourceSystemState.Horizon current =
          thermal.isSolid(blockY) && !thermal.isLava(blockY)
              ? resource
                  .zoneAt(new Point3(thermal.blockX() + 0.5, blockY + 0.5, thermal.blockZ() + 0.5))
                  .orElse(null)
              : null;
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new NetherResourceInterval(intervalStart, blockY, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(new NetherResourceInterval(intervalStart, 128, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      NetherResourceSystemState.Horizon first, NetherResourceSystemState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
