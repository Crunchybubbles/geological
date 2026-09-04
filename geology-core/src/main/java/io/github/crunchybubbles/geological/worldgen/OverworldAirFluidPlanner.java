package io.github.crunchybubbles.geological.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Derives explicit air and surface-water intervals from the bounded geological base-terrain plan.
 *
 * <p>This is deliberately a small presentation-independent policy. It does not infer groundwater,
 * aquifers, caves, or biome fluids; those belong to later logical stages. The default sea level is
 * the vanilla Overworld level, while the profile capability check prevents fictional dimensions
 * from accidentally receiving surface water.
 */
public final class OverworldAirFluidPlanner {
  public static final int DEFAULT_SEA_LEVEL = 63;

  private final OverworldBaseTerrainPlanner baseTerrain;
  private final int seaLevel;

  private OverworldAirFluidPlanner(OverworldBaseTerrainPlanner baseTerrain, int seaLevel) {
    this.baseTerrain = baseTerrain;
    this.seaLevel = seaLevel;
  }

  /** Creates the default Overworld surface-water policy at sea level 63. */
  public static OverworldAirFluidPlanner from(OverworldBaseTerrainPlanner baseTerrain) {
    return from(baseTerrain, DEFAULT_SEA_LEVEL);
  }

  /** Creates a bounded policy with an explicit sea level for deterministic fixtures. */
  public static OverworldAirFluidPlanner from(
      OverworldBaseTerrainPlanner baseTerrain, int seaLevel) {
    Objects.requireNonNull(baseTerrain, "base-terrain planner");
    WorldgenExecutionContext context = baseTerrain.context();
    if (!context
        .request()
        .profile()
        .fluidMedia()
        .contains(DimensionGeologyProfile.FluidMedium.SURFACE_WATER)) {
      throw new IllegalArgumentException("surface water is not allowed by the dimension profile");
    }
    ChunkBlockBounds bounds = context.targetBounds();
    if (seaLevel < bounds.minY() || seaLevel >= bounds.maxYExclusive()) {
      throw new IllegalArgumentException("sea level must lie inside the target vertical envelope");
    }
    return new OverworldAirFluidPlanner(baseTerrain, seaLevel);
  }

  /** Plans one column using the exact solid interval already derived by the base planner. */
  public OverworldAirFluidColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = baseTerrain.plan(blockX, blockZ);
    int surfaceWaterMax =
        Math.min(base.maxYExclusive(), Math.max(base.solidMaxYExclusive(), seaLevel + 1));
    return new OverworldAirFluidColumnPlan(
        base.blockX(),
        base.blockZ(),
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        surfaceWaterMax);
  }

  /** Plans exactly the target 16×16 footprint in the base planner's stable order. */
  public List<OverworldAirFluidColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = baseTerrain.context().targetBounds();
    List<OverworldAirFluidColumnPlan> columns = new ArrayList<>(256);
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        columns.add(plan(blockX, blockZ));
      }
    }
    return List.copyOf(columns);
  }

  public OverworldBaseTerrainPlanner baseTerrain() {
    return baseTerrain;
  }

  public int seaLevel() {
    return seaLevel;
  }
}
