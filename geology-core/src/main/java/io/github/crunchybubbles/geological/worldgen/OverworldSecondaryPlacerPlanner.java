package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SecondaryPlacerState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds chunk-local secondary placer overlays from existing source and drainage state. */
public final class OverworldSecondaryPlacerPlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;
  private final OverworldRegolithPlanner regolith;

  private OverworldSecondaryPlacerPlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldSecondaryPlacerPlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldSecondaryPlacerPlanner(
        Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Plans all three secondary placer families for one world column. */
  public OverworldSecondaryPlacerColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException("secondary placer and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    Point3 sourceWorld = province.frame().toWorld(province.geometry().porphyryCenter());
    PetrologicSample sourceParent =
        regolith
            .material()
            .resolve(province, regolith.material().geology().sample(province, sourceWorld));
    io.github.crunchybubbles.geological.mineral.PlacerSystemState transportProof =
        regolith.material().placerSystemState(province);
    List<SecondaryPlacerState> profiles = new ArrayList<>(3);
    List<OverworldSecondaryPlacerInterval> intervals = new ArrayList<>();
    for (SecondaryPlacerState.PlacerFamily family : SecondaryPlacerState.PlacerFamily.values()) {
      SecondaryPlacerState state =
          SecondaryPlacerState.proofFor(
              province,
              regolith.context().request().worldIdentity(),
              point,
              surface,
              parent,
              sourceParent,
              transportProof,
              family);
      profiles.add(state);
      if (state.status() == FormationStatus.FORMED) {
        intervals.addAll(classify(base, province, state, blockX, blockZ));
      }
    }
    return new OverworldSecondaryPlacerColumnPlan(
        blockX,
        blockZ,
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        surface.surface().fields().elevation(),
        province.id(),
        profiles,
        intervals);
  }

  public java.util.List<OverworldSecondaryPlacerColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldSecondaryPlacerColumnPlan> columns = new ArrayList<>(256);
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

  private static List<OverworldSecondaryPlacerInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      SecondaryPlacerState state,
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
    List<OverworldSecondaryPlacerInterval> intervals = new ArrayList<>();
    SecondaryPlacerState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      SecondaryPlacerState.Horizon current =
          state.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldSecondaryPlacerInterval(intervalStart, blockY, state, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(
          new OverworldSecondaryPlacerInterval(intervalStart, profileMax, state, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      SecondaryPlacerState.Horizon first, SecondaryPlacerState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
