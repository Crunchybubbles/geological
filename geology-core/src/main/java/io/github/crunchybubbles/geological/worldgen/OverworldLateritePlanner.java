package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LateriteProfileState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a bounded bauxite/Ni-Co laterite overlay from the existing surface and parent material.
 *
 * <p>Every column re-resolves province ownership and the parent bedrock, so no neighbor-loaded
 * state or mutable regional depletion map can affect a result.
 */
public final class OverworldLateritePlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;

  private final OverworldRegolithPlanner regolith;

  private OverworldLateritePlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldLateritePlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldLateritePlanner(Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Plans one world column without replacing the canonical base/regolith material. */
  public OverworldLateriteColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException("laterite and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    LateriteProfileState profile =
        LateriteProfileState.proofFor(
            province, regolith.context().request().worldIdentity(), point, surface, parent);
    List<OverworldLateriteInterval> intervals =
        profile.status() == FormationStatus.FORMED
            ? classify(base, province, profile, blockX, blockZ)
            : List.of();
    return new OverworldLateriteColumnPlan(
        blockX,
        blockZ,
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        surface.surface().fields().elevation(),
        province.id(),
        profile,
        intervals);
  }

  public Optional<OverworldLateriteInterval> at(long blockX, int blockY, long blockZ) {
    return plan(blockX, blockZ).at(blockY);
  }

  /** Plans exactly the authorized target chunk in stable X-then-Z order. */
  public List<OverworldLateriteColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldLateriteColumnPlan> columns = new ArrayList<>(256);
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

  private static List<OverworldLateriteInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      LateriteProfileState profile,
      long blockX,
      long blockZ) {
    int profileMin =
        Math.max(
            base.minYInclusive(),
            (int)
                StrictMath.floor(
                    profile.localCenter().y()
                        - profile.profileThicknessBlocks() / 2.0
                        - PROFILE_PADDING_BLOCKS));
    int profileMax =
        Math.min(
            base.solidMaxYExclusive(),
            (int)
                    StrictMath.ceil(
                        profile.localCenter().y()
                            + profile.profileThicknessBlocks() / 2.0
                            + PROFILE_PADDING_BLOCKS)
                + 1);
    if (profileMax <= profileMin) {
      return List.of();
    }
    List<OverworldLateriteInterval> intervals = new ArrayList<>();
    LateriteProfileState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      LateriteProfileState.Horizon current =
          profile.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldLateriteInterval(intervalStart, blockY, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(new OverworldLateriteInterval(intervalStart, profileMax, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      LateriteProfileState.Horizon first, LateriteProfileState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
