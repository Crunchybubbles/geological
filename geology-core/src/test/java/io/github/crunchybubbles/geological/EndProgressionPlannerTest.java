package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.EndParentBodyState;
import io.github.crunchybubbles.geological.worldgen.EndProgressionContract;
import io.github.crunchybubbles.geological.worldgen.EndProgressionPlanner;
import org.junit.jupiter.api.Test;

class EndProgressionPlannerTest {
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");

  @Test
  void freezesCentralGatewayOuterAndProtectedStructureTopology() {
    EndProgressionPlanner planner = planner();
    var contract = planner.contract();

    assertTrue(planner.validateTopology());
    assertEquals(
        1,
        contract.structureSlots().stream()
            .filter(slot -> slot.kind() == EndProgressionContract.StructureKind.EXIT_PORTAL)
            .count());
    assertEquals(
        1,
        contract.structureSlots().stream()
            .filter(slot -> slot.kind() == EndProgressionContract.StructureKind.DRAGON_ARENA)
            .count());
    assertEquals(4, contract.gatewayBodyIds().size());
    assertEquals(8, contract.outerIslandBodyIds().size());
    assertTrue(
        contract.structureSlots().stream()
            .allMatch(EndProgressionContract.StructureSlot::protectedFromTerrainWrites));
    assertFalse(planner.canWriteTerrain(0, 0));
    assertTrue(planner.structureAt(0, 0).isPresent());
    assertTrue(planner.canWriteTerrain(512, 512));
  }

  @Test
  void anchorsRemainOnCorrectParentRoles() {
    EndProgressionPlanner planner = planner();
    var compiler = planner.terrain();
    for (var slot : planner.contract().structureSlots()) {
      EndParentBodyState body =
          compiler
              .parentBodyAt(
                  (long) StrictMath.floor(slot.anchor().x()),
                  (long) StrictMath.floor(slot.anchor().z()))
              .orElseThrow();
      assertEquals(slot.anchorBodyId(), body.parentBodyId());
      if (slot.kind() == EndProgressionContract.StructureKind.END_GATEWAY) {
        assertEquals(EndParentBodyState.FragmentRole.GATEWAY_RING, body.role());
      }
      if (slot.kind() == EndProgressionContract.StructureKind.OUTER_END_CITY
          || slot.kind() == EndProgressionContract.StructureKind.CHORUS_HABITAT) {
        assertEquals(EndParentBodyState.FragmentRole.OUTER_ISLAND, body.role());
      }
    }
  }

  private static EndProgressionPlanner planner() {
    WorldIdentity identity =
        new WorldIdentity(8_675_309L, END.version(), END.scientificDigest(), END.profileId());
    return EndProgressionPlanner.from(EndFragmentTerrainCompiler.from(identity));
  }
}
