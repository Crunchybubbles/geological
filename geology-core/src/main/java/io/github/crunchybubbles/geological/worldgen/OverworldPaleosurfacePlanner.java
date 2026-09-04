package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.PaleosurfaceState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds deterministic, structural paleosurface/refined-regolith overlays for one column. */
public final class OverworldPaleosurfacePlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;

  private final OverworldRegolithPlanner regolith;

  private OverworldPaleosurfacePlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldPaleosurfacePlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldPaleosurfacePlanner(Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Plans all paleosurface refinement families for one world column. */
  public OverworldPaleosurfaceColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException("paleosurface and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    List<PaleosurfaceState> profiles =
        new ArrayList<>(PaleosurfaceState.RefinementKind.values().length);
    List<OverworldPaleosurfaceInterval> intervals = new ArrayList<>();
    for (PaleosurfaceState.RefinementKind kind : PaleosurfaceState.RefinementKind.values()) {
      PaleosurfaceState profile =
          PaleosurfaceState.proofFor(
              province, regolith.context().request().worldIdentity(), point, surface, parent, kind);
      profiles.add(profile);
      if (profile.status() == FormationStatus.FORMED) {
        intervals.addAll(classify(base, province, profile, blockX, blockZ));
      }
    }
    return new OverworldPaleosurfaceColumnPlan(
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

  public List<OverworldPaleosurfaceColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldPaleosurfaceColumnPlan> columns = new ArrayList<>(256);
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

  private static List<OverworldPaleosurfaceInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      PaleosurfaceState profile,
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
    List<OverworldPaleosurfaceInterval> intervals = new ArrayList<>();
    PaleosurfaceState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      PaleosurfaceState.Horizon current =
          profile.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldPaleosurfaceInterval(intervalStart, blockY, profile, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(
          new OverworldPaleosurfaceInterval(intervalStart, profileMax, profile, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      PaleosurfaceState.Horizon first, PaleosurfaceState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
