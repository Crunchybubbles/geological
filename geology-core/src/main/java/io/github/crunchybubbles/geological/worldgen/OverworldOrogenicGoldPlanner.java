package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.OrogenicGoldSystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds chunk-local orogenic-gold overlays from regional strain and metamorphic-fluid evidence.
 */
public final class OverworldOrogenicGoldPlanner {
  private static final int PROFILE_PADDING_BLOCKS = 2;
  private final OverworldRegolithPlanner regolith;

  private OverworldOrogenicGoldPlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldOrogenicGoldPlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldOrogenicGoldPlanner(Objects.requireNonNull(regolith, "regolith planner"));
  }

  public OverworldOrogenicGoldColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = regolith.material().geology().atlas().provinceAt(point);
    SurfacePetrologicSample surface = regolith.material().surface(point);
    if (!province.id().equals(base.terrainControls().provinceId())
        || !province.id().equals(surface.surface().bedrock().provinceId())) {
      throw new IllegalStateException("orogenic-gold and terrain owners changed between stages");
    }
    PetrologicSample parent = regolith.material().resolve(province, surface.surface().bedrock());
    OrogenicGoldSystemState system =
        OrogenicGoldSystemState.proofFor(
            province, regolith.context().request().worldIdentity(), point, surface, parent);
    List<OverworldOrogenicGoldInterval> intervals =
        system.status() == FormationStatus.FORMED
            ? classify(base, province, system, blockX, blockZ)
            : List.of();
    return new OverworldOrogenicGoldColumnPlan(
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

  public List<OverworldOrogenicGoldColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldOrogenicGoldColumnPlan> columns = new ArrayList<>(256);
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

  private static List<OverworldOrogenicGoldInterval> classify(
      OverworldBaseTerrainColumnPlan base,
      Province province,
      OrogenicGoldSystemState system,
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
    List<OverworldOrogenicGoldInterval> intervals = new ArrayList<>();
    OrogenicGoldSystemState.Horizon previous = null;
    int intervalStart = profileMin;
    for (int blockY = profileMin; blockY < profileMax; blockY++) {
      Point3 worldPoint = new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
      OrogenicGoldSystemState.Horizon current =
          system.zoneAt(province.frame().toLocal(worldPoint)).orElse(null);
      if (sameHorizon(previous, current)) {
        continue;
      }
      if (previous != null) {
        intervals.add(new OverworldOrogenicGoldInterval(intervalStart, blockY, previous));
      }
      previous = current;
      intervalStart = blockY;
    }
    if (previous != null) {
      intervals.add(new OverworldOrogenicGoldInterval(intervalStart, profileMax, previous));
    }
    return List.copyOf(intervals);
  }

  private static boolean sameHorizon(
      OrogenicGoldSystemState.Horizon first, OrogenicGoldSystemState.Horizon second) {
    return first == second
        || (first != null
            && second != null
            && first.kind() == second.kind()
            && first.bodyId().equals(second.bodyId()));
  }
}
