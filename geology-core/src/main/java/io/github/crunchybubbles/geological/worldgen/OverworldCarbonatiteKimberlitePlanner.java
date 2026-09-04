package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds chunk-local carbonatite, peralkaline, and kimberlite overlays. */
public final class OverworldCarbonatiteKimberlitePlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;
  private final OverworldRegolithPlanner regolith;
  private final CarbonatiteKimberliteHostPolicy hostPolicy;

  private OverworldCarbonatiteKimberlitePlanner(
      OverworldRegolithPlanner regolith, CarbonatiteKimberliteHostPolicy hostPolicy) {
    this.regolith = regolith;
    this.hostPolicy = hostPolicy;
  }

  /** Uses actual resolved bedrock only; no alkaline complex or carrier pipe is inferred. */
  public static OverworldCarbonatiteKimberlitePlanner from(OverworldRegolithPlanner regolith) {
    return from(regolith, CarbonatiteKimberliteHostPolicy.none());
  }

  public static OverworldCarbonatiteKimberlitePlanner from(
      OverworldRegolithPlanner regolith, CarbonatiteKimberliteHostPolicy hostPolicy) {
    return new OverworldCarbonatiteKimberlitePlanner(
        Objects.requireNonNull(regolith, "regolith planner"),
        Objects.requireNonNull(hostPolicy, "carbonatite/kimberlite host policy"));
  }

  public OverworldCarbonatiteKimberliteColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException("alkaline complex and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    CarbonatiteKimberliteHostPolicy.HostEvidence host =
        hostPolicy.resolve(
            province, regolith.context().request().worldIdentity(), point, surface, parent);
    CarbonatiteKimberliteSystemState system =
        CarbonatiteKimberliteSystemState.proofFor(
            province, regolith.context().request().worldIdentity(), point, surface, parent, host);
    List<OverworldCarbonatiteKimberliteInterval> intervals =
        system.status() == FormationStatus.FORMED
            ? classify(base, province, system, blockX, blockZ)
            : List.of();
    return new OverworldCarbonatiteKimberliteColumnPlan(
        blockX,
        blockZ,
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        surface.surface().fields().elevation(),
        province.id(),
        system,
        intervals);
  }

  public List<OverworldCarbonatiteKimberliteColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldCarbonatiteKimberliteColumnPlan> columns = new ArrayList<>(256);
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

  public CarbonatiteKimberliteHostPolicy hostPolicy() {
    return hostPolicy;
  }

  private static List<OverworldCarbonatiteKimberliteInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      CarbonatiteKimberliteSystemState system,
      long blockX,
      long blockZ) {
    int profileMin =
        Math.max(
            base.minYInclusive(),
            (int)
                StrictMath.floor(
                    system.localCenter().y()
                        - system.verticalExtentBlocks() / 2.0
                        - PROFILE_PADDING_BLOCKS));
    int profileMax =
        Math.min(
            base.solidMaxYExclusive(),
            (int)
                    StrictMath.ceil(
                        system.localCenter().y()
                            + system.verticalExtentBlocks() / 2.0
                            + PROFILE_PADDING_BLOCKS)
                + 1);
    if (profileMax <= profileMin) {
      return List.of();
    }
    List<OverworldCarbonatiteKimberliteInterval> intervals = new ArrayList<>();
    CarbonatiteKimberliteSystemState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      CarbonatiteKimberliteSystemState.Horizon current =
          system.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldCarbonatiteKimberliteInterval(intervalStart, blockY, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(
          new OverworldCarbonatiteKimberliteInterval(intervalStart, profileMax, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      CarbonatiteKimberliteSystemState.Horizon first,
      CarbonatiteKimberliteSystemState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
