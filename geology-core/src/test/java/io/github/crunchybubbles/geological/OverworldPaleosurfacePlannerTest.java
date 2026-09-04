package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.PaleosurfaceState;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfaceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfacePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldPaleosurfacePlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void formedResidualAndBuriedProfilesRetainDistinctAgesAndHorizonProofs() {
    OverworldPaleosurfacePlanner planner = planner(0, 0);
    OverworldPaleosurfaceColumnPlan residual =
        findFormed(planner, PaleosurfaceState.RefinementKind.EXPOSED_RESIDUAL_REGOLITH);
    OverworldPaleosurfaceColumnPlan buried =
        findFormed(planner, PaleosurfaceState.RefinementKind.BURIED_PALEOSURFACE);

    PaleosurfaceState residualProfile =
        residual.profile(PaleosurfaceState.RefinementKind.EXPOSED_RESIDUAL_REGOLITH);
    PaleosurfaceState buriedProfile =
        buried.profile(PaleosurfaceState.RefinementKind.BURIED_PALEOSURFACE);
    assertEquals(FormationStatus.FORMED, residualProfile.status());
    assertEquals(
        PaleosurfaceState.SourceBasis.PRESENT_PARENT_WEATHERING, residualProfile.sourceBasis());
    assertEquals(
        new io.github.crunchybubbles.geological.model.AgeKey(0.02, 0),
        residualProfile.formationAge());
    assertEquals(3, residualProfile.horizons().size());
    assertTrue(residual.hasKind(PaleosurfaceState.RefinementKind.EXPOSED_RESIDUAL_REGOLITH));
    assertEquals(FormationStatus.FORMED, buriedProfile.status());
    assertEquals(
        PaleosurfaceState.SourceBasis.UNCONFORMITY_WEATHERING_PROFILE, buriedProfile.sourceBasis());
    assertTrue(buriedProfile.formationAge().ageMa() > residualProfile.formationAge().ageMa());
    assertEquals(2, buriedProfile.horizons().size());
    assertTrue(buried.hasKind(PaleosurfaceState.RefinementKind.BURIED_PALEOSURFACE));
    assertTrue(
        buriedProfile.horizons().stream()
            .allMatch(
                horizon ->
                    horizon.overprint()
                        == io.github.crunchybubbles.geological.model.Overprint
                            .WEATHERED_UNCONFORMITY));
  }

  @Test
  void karstRequiresCarbonateAndExternalAluminumSource() {
    OverworldPaleosurfacePlanner planner = planner(-11, 17);
    List<OverworldPaleosurfaceColumnPlan> columns = planner.planTargetChunk();
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
                    column.profile(PaleosurfaceState.RefinementKind.KARST_BAUXITE_POCKET).status()
                        == FormationStatus.FORMED));
  }

  @Test
  void adjacentChunkContextsProduceIdenticalPaleosurfaceColumns() {
    OverworldPaleosurfaceColumnPlan fromWest = planner(-1, 0).plan(-1, 3);
    OverworldPaleosurfaceColumnPlan fromEast = planner(0, 0).plan(-1, 3);

    assertEquals(fromWest, fromEast);
  }

  private static OverworldPaleosurfacePlanner planner(long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldPaleosurfacePlanner.from(OverworldRegolithPlanner.from(context));
  }

  private static OverworldPaleosurfaceColumnPlan findFormed(
      OverworldPaleosurfacePlanner planner, PaleosurfaceState.RefinementKind kind) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldPaleosurfaceColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.profile(kind).status() == FormationStatus.FORMED && candidate.hasKind(kind)) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed paleosurface profile found in bounded fixture: " + kind);
  }
}
