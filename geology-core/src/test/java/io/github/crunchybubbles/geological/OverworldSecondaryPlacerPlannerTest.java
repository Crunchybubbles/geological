package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.SecondaryPlacerState;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldSecondaryPlacerPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void formedCassiteriteAndHeavyProfilesRetainSourceEvidenceAndClosedBudgets() {
    OverworldSecondaryPlacerPlanner planner = planner(0, 0);
    Province province = fertileProvince(planner);
    Point2 trap = province.frame().toWorld(province.geometry().placerCenter());
    OverworldSecondaryPlacerColumnPlan plan =
        planner.plan((long) StrictMath.floor(trap.x()), (long) StrictMath.floor(trap.z()));

    SecondaryPlacerState cassiterite = profile(plan, SecondaryPlacerState.PlacerFamily.CASSITERITE);
    SecondaryPlacerState heavy =
        profile(plan, SecondaryPlacerState.PlacerFamily.HEAVY_MINERAL_SAND);
    assertEquals(FormationStatus.FORMED, cassiterite.status());
    assertEquals(
        SecondaryPlacerState.SourceBasis.LCT_RESIDUAL_CASSITERITE_PROXY, cassiterite.sourceBasis());
    assertEquals(FormationStatus.FORMED, heavy.status());
    assertEquals(
        SecondaryPlacerState.SourceBasis.DURABLE_HEAVY_MINERAL_VECTOR, heavy.sourceBasis());
    for (SecondaryPlacerState state : List.of(cassiterite, heavy)) {
      assertEquals(
          state.releasedBudgetFixedUnits(),
          state.transportLossFixedUnits() + state.depositAllocationFixedUnits());
      assertEquals(state.depositAllocationFixedUnits(), state.totalProfileAllocationFixedUnits());
      assertTrue(state.sourceBodyIds().size() >= 1);
    }
    assertTrue(plan.hasFamily(SecondaryPlacerState.PlacerFamily.CASSITERITE));
    assertTrue(plan.hasFamily(SecondaryPlacerState.PlacerFamily.HEAVY_MINERAL_SAND));
    assertFalse(plan.hasFamily(SecondaryPlacerState.PlacerFamily.DIAMOND));
  }

  @Test
  void diamondPlacersRemainBarrenWithoutExplicitDiamondiferousCargo() {
    OverworldSecondaryPlacerPlanner planner = planner(-11, 17);
    List<OverworldSecondaryPlacerColumnPlan> columns = planner.planTargetChunk();

    assertEquals(256, columns.size());
    assertTrue(
        columns.stream()
            .allMatch(
                column ->
                    column.profiles().stream()
                        .filter(
                            profile ->
                                profile.family() == SecondaryPlacerState.PlacerFamily.DIAMOND)
                        .allMatch(profile -> profile.status() != FormationStatus.FORMED)));
    ChunkBlockBounds bounds = planner.regolith().context().targetBounds();
    assertEquals(bounds.minX(), columns.getFirst().blockX());
    assertEquals(bounds.minZ(), columns.getFirst().blockZ());
    assertEquals(bounds.maxXExclusive() - 1, columns.getLast().blockX());
    assertEquals(bounds.maxZExclusive() - 1, columns.getLast().blockZ());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalSecondaryPlacerColumns() {
    OverworldSecondaryPlacerColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldSecondaryPlacerColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static SecondaryPlacerState profile(
      OverworldSecondaryPlacerColumnPlan plan, SecondaryPlacerState.PlacerFamily family) {
    return plan.profiles().stream()
        .filter(state -> state.family() == family)
        .findFirst()
        .orElseThrow();
  }

  private static Province fertileProvince(OverworldSecondaryPlacerPlanner planner) {
    var atlas = planner.regolith().material().geology().atlas();
    for (long cellX = -12; cellX <= 12; cellX++) {
      for (long cellZ = -12; cellZ <= 12; cellZ++) {
        Province candidate = atlas.province(new CellKey("province", cellX, cellZ));
        if (candidate.grammar() == ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC) {
          return candidate;
        }
      }
    }
    throw new AssertionError("fixture did not contain an exhumed fertile province");
  }

  private static OverworldSecondaryPlacerPlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldSecondaryPlacerPlanner.from(OverworldRegolithPlanner.from(context));
  }
}
