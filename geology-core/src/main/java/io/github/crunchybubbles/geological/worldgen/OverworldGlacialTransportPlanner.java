package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GlacialTransportState;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds chunk-local glacial transport overlays from an explicit ice-history policy. */
public final class OverworldGlacialTransportPlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;

  private final OverworldRegolithPlanner regolith;
  private final GlacialHistoryPolicy historyPolicy;

  private OverworldGlacialTransportPlanner(
      OverworldRegolithPlanner regolith, GlacialHistoryPolicy historyPolicy) {
    this.regolith = regolith;
    this.historyPolicy = historyPolicy;
  }

  /** Uses the safe no-ice default until a climate compiler supplies event-local history. */
  public static OverworldGlacialTransportPlanner from(OverworldRegolithPlanner regolith) {
    return from(regolith, GlacialHistoryPolicy.none());
  }

  public static OverworldGlacialTransportPlanner from(
      OverworldRegolithPlanner regolith, GlacialHistoryPolicy historyPolicy) {
    return new OverworldGlacialTransportPlanner(
        Objects.requireNonNull(regolith, "regolith planner"),
        Objects.requireNonNull(historyPolicy, "glacial history policy"));
  }

  public OverworldGlacialTransportColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException(
          "glacial transport and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    GlacialTransportState.History history =
        historyPolicy.history(province, regolith.context().request().worldIdentity(), cell);
    GlacialTransportState profile =
        GlacialTransportState.proofFor(
            province,
            regolith.context().request().worldIdentity(),
            point,
            surface,
            parent,
            history);
    List<OverworldGlacialTransportInterval> intervals =
        profile.status() == FormationStatus.FORMED
            ? classify(base, province, profile, blockX, blockZ)
            : List.of();
    return new OverworldGlacialTransportColumnPlan(
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

  public java.util.List<OverworldGlacialTransportColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldGlacialTransportColumnPlan> columns = new ArrayList<>(256);
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

  public GlacialHistoryPolicy historyPolicy() {
    return historyPolicy;
  }

  private static List<OverworldGlacialTransportInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      GlacialTransportState profile,
      long blockX,
      long blockZ) {
    int profileMin =
        Math.max(
            base.minYInclusive(),
            (int)
                StrictMath.floor(
                    profile.localCenterY()
                        - profile.profileThicknessBlocks() / 2.0
                        - PROFILE_PADDING_BLOCKS));
    int profileMax =
        Math.min(
            base.solidMaxYExclusive(),
            (int)
                    StrictMath.ceil(
                        profile.localCenterY()
                            + profile.profileThicknessBlocks() / 2.0
                            + PROFILE_PADDING_BLOCKS)
                + 1);
    if (profileMax <= profileMin) {
      return List.of();
    }
    List<OverworldGlacialTransportInterval> intervals = new ArrayList<>();
    GlacialTransportState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      GlacialTransportState.Horizon current =
          profile.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldGlacialTransportInterval(intervalStart, blockY, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(new OverworldGlacialTransportInterval(intervalStart, profileMax, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      GlacialTransportState.Horizon first, GlacialTransportState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
