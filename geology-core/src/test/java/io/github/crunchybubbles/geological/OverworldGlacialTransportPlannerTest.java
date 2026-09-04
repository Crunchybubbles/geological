package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GlacialTransportState;
import io.github.crunchybubbles.geological.worldgen.ChunkBlockBounds;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.GlacialHistoryPolicy;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverworldGlacialTransportPlannerTest {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");

  @Test
  void defaultOverworldHasExplicitNoIceGate() {
    OverworldGlacialTransportPlanner planner = planner(-11, 17);
    List<OverworldGlacialTransportColumnPlan> columns = planner.planTargetChunk();
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
                    column.profile().status() == FormationStatus.BARREN_SYSTEM
                        && column.profile().failedGate().orElseThrow().equals("ice_history")
                        && column.intervals().isEmpty()));
  }

  @Test
  void explicitFixtureIceHistoryFormsClosedTillProfile() {
    OverworldGlacialTransportPlanner planner = planner(0, 0, GlacialHistoryPolicy.fixture());
    OverworldGlacialTransportColumnPlan formed = findFormed(planner);
    GlacialTransportState profile = formed.profile();

    assertEquals(FormationStatus.FORMED, profile.status());
    assertEquals(GlacialTransportState.TransportKind.CONTINENTAL_TILL, profile.transportKind());
    assertEquals(
        GlacialTransportState.SourceBasis.EXPLICIT_ICE_HISTORY_SOURCE, profile.sourceBasis());
    assertEquals(
        profile.releasedInventoryFixedUnits(),
        profile.transportLossFixedUnits() + profile.depositAllocationFixedUnits());
    assertTrue(profile.depositAllocationFixedUnits() > 0);
    assertEquals(3, profile.horizons().size());
    assertTrue(formed.hasGlacialTransport());
    assertTrue(formed.at(formed.intervals().getFirst().minYInclusive()).isPresent());
  }

  @Test
  void adjacentChunkContextsProduceIdenticalGlacialColumns() {
    OverworldGlacialTransportColumnPlan fromWest =
        planner(-1, 0, GlacialHistoryPolicy.fixture()).plan(-1, 3);
    OverworldGlacialTransportColumnPlan fromEast =
        planner(0, 0, GlacialHistoryPolicy.fixture()).plan(-1, 3);

    assertEquals(fromWest, fromEast);
    assertFalse(
        fromWest.profile().status() == FormationStatus.FORMED && fromWest.intervals().isEmpty());
  }

  private static OverworldGlacialTransportPlanner planner(long chunkX, long chunkZ) {
    return planner(chunkX, chunkZ, GlacialHistoryPolicy.none());
  }

  private static OverworldGlacialTransportPlanner planner(
      long chunkX, long chunkZ, GlacialHistoryPolicy policy) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                8_675_309L, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldGlacialTransportPlanner.from(OverworldRegolithPlanner.from(context), policy);
  }

  private static OverworldGlacialTransportColumnPlan findFormed(
      OverworldGlacialTransportPlanner planner) {
    for (long blockX = -5_000L; blockX <= -3_000L; blockX += 32L) {
      for (long blockZ = -300L; blockZ <= 1_700L; blockZ += 32L) {
        OverworldGlacialTransportColumnPlan candidate = planner.plan(blockX, blockZ);
        if (candidate.profile().status() == FormationStatus.FORMED
            && candidate.hasGlacialTransport()) {
          return candidate;
        }
      }
    }
    throw new AssertionError("no formed glacial fixture profile found");
  }
}
