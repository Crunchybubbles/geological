package io.github.crunchybubbles.geological.worldgen;

import java.util.Objects;
import java.util.Optional;

/** Read-only progression queries that keep platform structure writes outside geology ownership. */
public final class EndProgressionPlanner {
  private final EndFragmentTerrainCompiler terrain;
  private final EndProgressionContract contract;

  private EndProgressionPlanner(
      EndFragmentTerrainCompiler terrain, EndProgressionContract contract) {
    this.terrain = Objects.requireNonNull(terrain, "End fragment terrain compiler");
    this.contract = Objects.requireNonNull(contract, "End progression contract");
  }

  public static EndProgressionPlanner from(EndFragmentTerrainCompiler terrain) {
    Objects.requireNonNull(terrain, "End fragment terrain compiler");
    return new EndProgressionPlanner(terrain, EndProgressionContract.from(terrain));
  }

  public EndProgressionContract contract() {
    return contract;
  }

  public EndFragmentTerrainCompiler terrain() {
    return terrain;
  }

  public boolean canWriteTerrain(long blockX, long blockZ) {
    return !contract.protects(blockX, blockZ);
  }

  public Optional<EndProgressionContract.StructureSlot> structureAt(long blockX, long blockZ) {
    return contract.slotAt(blockX, blockZ);
  }

  /** Verifies central body, void gap, gateway anchors, and outer anchors remain available. */
  public boolean validateTopology() {
    if (terrain.parentBodyAtCell(0L, 0L).isEmpty()
        || terrain.parentBodyAt(512L, 512L).isPresent()) {
      return false;
    }
    return contract.gatewayBodyIds().stream()
            .allMatch(
                id ->
                    contract.structureSlots().stream()
                        .anyMatch(slot -> slot.anchorBodyId().equals(id)))
        && contract.outerIslandBodyIds().stream()
            .allMatch(
                id ->
                    contract.structureSlots().stream()
                        .anyMatch(slot -> slot.anchorBodyId().equals(id)));
  }
}
