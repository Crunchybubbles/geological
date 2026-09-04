package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SupergeneCopperState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Projects the Phase 3 supergene proof into bounded Overworld columns without changing Phase 2
 * material identity.
 *
 * <p>Province ownership is resolved independently for every world column, and each point is
 * transformed back into that province's local frame before horizon classification. Consequently,
 * shuffled access and adjacent chunks use the same source-gated profile at a seam.
 */
public final class OverworldSecondaryWeatheringPlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;

  private final OverworldRegolithPlanner regolith;

  private OverworldSecondaryWeatheringPlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  /** Creates a planner at the writable regolith/surface-clue boundary. */
  public static OverworldSecondaryWeatheringPlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldSecondaryWeatheringPlanner(
        Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Plans the source-gated weathering overlay for one world column. */
  public OverworldSecondaryWeatheringColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Province province =
        regolith.material().geology().atlas().provinceAt(new Point2(blockX + 0.5, blockZ + 0.5));
    if (!province.id().equals(base.terrainControls().provinceId())) {
      throw new IllegalStateException(
          "secondary weathering and terrain owners changed between stages");
    }
    SupergeneCopperState state = regolith.material().supergeneCopperState(province);
    List<OverworldSecondaryWeatheringInterval> intervals =
        state.status() == FormationStatus.FORMED
            ? classify(base, province, state, blockX, blockZ)
            : List.of();
    return new OverworldSecondaryWeatheringColumnPlan(
        blockX,
        blockZ,
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        province.id(),
        state.systemId(),
        state.status(),
        state.primaryDepositId(),
        state.weatheringProcessId(),
        state.sourceBudgetFixedUnits(),
        state.retainedHypogeneFixedUnits(),
        state.leachableCopperFixedUnits(),
        state.supergeneAllocationFixedUnits(),
        state.oxidizedAndDissolvedLossFixedUnits(),
        intervals);
  }

  /** Returns the horizon covering one block Y, if the column has one. */
  public Optional<OverworldSecondaryWeatheringInterval> at(long blockX, int blockY, long blockZ) {
    return plan(blockX, blockZ).at(blockY);
  }

  /** Plans exactly the authorized 16×16 target footprint in stable X-then-Z order. */
  public List<OverworldSecondaryWeatheringColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldSecondaryWeatheringColumnPlan> columns = new ArrayList<>(256);
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        columns.add(plan(blockX, blockZ));
      }
    }
    return List.copyOf(columns);
  }

  public OverworldRegolithPlanner regolith() {
    return regolith;
  }

  private static List<OverworldSecondaryWeatheringInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      SupergeneCopperState state,
      long blockX,
      long blockZ) {
    int profileMin =
        Math.max(
            base.minYInclusive(),
            (int)
                StrictMath.floor(
                    state.localCenter().y()
                        - state.profileThicknessBlocks() / 2.0
                        - PROFILE_PADDING_BLOCKS));
    int profileMax =
        Math.min(
            base.solidMaxYExclusive(),
            (int)
                    StrictMath.ceil(
                        state.localCenter().y()
                            + state.profileThicknessBlocks() / 2.0
                            + PROFILE_PADDING_BLOCKS)
                + 1);
    if (profileMax <= profileMin) {
      return List.of();
    }
    List<OverworldSecondaryWeatheringInterval> intervals = new ArrayList<>();
    SupergeneCopperState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      SupergeneCopperState.Horizon current =
          state.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(interval(intervalStart, blockY, previous, state));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(interval(intervalStart, profileMax, previous, state));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      SupergeneCopperState.Horizon first, SupergeneCopperState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }

  private static OverworldSecondaryWeatheringInterval interval(
      int minY, int maxY, SupergeneCopperState.Horizon horizon, SupergeneCopperState state) {
    return new OverworldSecondaryWeatheringInterval(
        minY,
        maxY,
        horizon.kind(),
        horizon.overprint(),
        state.systemId(),
        state.primaryDepositId(),
        state.weatheringProcessId(),
        horizon.bodyId(),
        state.sourceBudgetFixedUnits(),
        state.retainedHypogeneFixedUnits(),
        state.leachableCopperFixedUnits(),
        horizon.allocationFixedUnits());
  }
}
