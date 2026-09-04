package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.LateriteProfileState;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldLateriteColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldLateritePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldLateritePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void formedBauxiteRetainsParentAndCommodityLedger() {
    OverworldLateritePlanner planner = planner(-251, 43);
    OverworldLateriteColumnPlan plan = findFormedBauxite(planner);
    LateriteProfileState profile = plan.profile();

    assertEquals(FormationStatus.FORMED, profile.status());
    assertEquals(LateriteProfileState.ProfileKind.BAUXITE, profile.profileKind());
    assertEquals(LateriteProfileState.SourceBasis.PARENT_ALUMINUM_MASS_PPM, profile.sourceBasis());
    assertEquals(1, profile.commodityBudgets().size());
    LateriteProfileState.CommodityBudget aluminum = profile.commodityBudgets().getFirst();
    assertEquals(LateriteProfileState.Commodity.ALUMINUM, aluminum.commodity());
    assertEquals(
        aluminum.sourceFixedUnits(),
        aluminum.residualAllocationFixedUnits() + aluminum.dissolvedLossFixedUnits());
    assertEquals(
        aluminum.residualAllocationFixedUnits(),
        profile.horizons().stream()
            .mapToLong(
                horizon -> horizon.allocationFixedUnits(LateriteProfileState.Commodity.ALUMINUM))
            .sum());
    assertTrue(plan.hasLaterite());
    assertTrue(plan.hasBauxite());
    assertFalse(plan.hasNiCoLaterite());
    assertTrue(
        plan.intervals().stream()
            .allMatch(
                interval ->
                    interval.horizon().overprint()
                        == io.github.crunchybubbles.geological.model.Overprint.WEATHERED_REGOLITH));
    assertTrue(plan.at(plan.intervals().getFirst().minYInclusive()).isPresent());
  }

  @Test
  void targetChunkIsBoundedAndNiCoCannotAppearWithoutAnUltramaficParent() {
    OverworldLateritePlanner planner = planner(-11, 17);
    List<OverworldLateriteColumnPlan> columns = planner.planTargetChunk();
    ChunkBlockBounds bounds = planner.regolith().context().targetBounds();

    assertEquals(256, columns.size());
    assertEquals(bounds.minX(), columns.getFirst().blockX());
    assertEquals(bounds.minZ(), columns.getFirst().blockZ());
    assertEquals(bounds.maxXExclusive() - 1, columns.getLast().blockX());
    assertEquals(bounds.maxZExclusive() - 1, columns.getLast().blockZ());
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    column.intervals().stream()
                        .allMatch(
                            interval ->
                                interval.minYInclusive() >= column.minYInclusive()
                                    && interval.maxYExclusive() <= column.solidMaxYExclusive())));
    assertTrue(
        columns.stream()
            .noneMatch(
                column ->
                    column.profile().status() == FormationStatus.FORMED
                        && column.hasNiCoLaterite()));
  }

  @Test
  void adjacentChunkContextsProduceIdenticalLateriteColumns() {
    OverworldLateriteColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldLateriteColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldLateritePlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldLateritePlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldLateriteColumnPlan findFormedBauxite(OverworldLateritePlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldLateriteColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.profile().status() == FormationStatus.FORMED
            && candidate.profile().profileKind() == LateriteProfileState.ProfileKind.BAUXITE
            && candidate.hasLaterite()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed bauxite profile found in the bounded fixture");
  }
}
