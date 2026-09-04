package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Frozen End progression/structure protection contract layered over parent-fragment terrain. */
public record EndProgressionContract(
    StableId contractId,
    StableId centralIslandBodyId,
    List<StableId> gatewayBodyIds,
    List<StableId> outerIslandBodyIds,
    List<StructureSlot> structureSlots,
    String structureProgressionContract,
    boolean portalArrivalViable,
    boolean dragonArenaProtected,
    double centralIslandRadiusBlocks,
    double minimumVoidGapBlocks) {
  public EndProgressionContract {
    if (contractId == null
        || centralIslandBodyId == null
        || gatewayBodyIds == null
        || outerIslandBodyIds == null
        || structureSlots == null
        || structureProgressionContract == null
        || structureProgressionContract.isBlank()) {
      throw new IllegalArgumentException("End progression contract identities are required");
    }
    gatewayBodyIds = List.copyOf(gatewayBodyIds).stream().sorted().toList();
    outerIslandBodyIds = List.copyOf(outerIslandBodyIds).stream().sorted().toList();
    structureSlots = List.copyOf(structureSlots);
    if (gatewayBodyIds.isEmpty()
        || outerIslandBodyIds.isEmpty()
        || gatewayBodyIds.stream().anyMatch(id -> id == null)
        || outerIslandBodyIds.stream().anyMatch(id -> id == null)
        || gatewayBodyIds.size() != new HashSet<>(gatewayBodyIds).size()
        || outerIslandBodyIds.size() != new HashSet<>(outerIslandBodyIds).size()
        || !java.util.Collections.disjoint(gatewayBodyIds, outerIslandBodyIds)
        || structureSlots.isEmpty()
        || structureSlots.stream().anyMatch(slot -> slot == null)
        || !Double.isFinite(centralIslandRadiusBlocks)
        || centralIslandRadiusBlocks <= 0.0
        || !Double.isFinite(minimumVoidGapBlocks)
        || minimumVoidGapBlocks <= 0.0) {
      throw new IllegalArgumentException("End progression topology is invalid");
    }
    HashSet<StableId> slotIds = new HashSet<>();
    for (StructureSlot slot : structureSlots) {
      if (!slotIds.add(slot.structureId())) {
        throw new IllegalArgumentException("End progression structure slots must be unique");
      }
      if (!slot.protectedFromTerrainWrites()
          || !(slot.anchorBodyId().equals(centralIslandBodyId)
              || gatewayBodyIds.contains(slot.anchorBodyId())
              || outerIslandBodyIds.contains(slot.anchorBodyId()))) {
        throw new IllegalArgumentException(
            "End progression slots must be protected and anchored to a known fragment");
      }
    }
    if (!portalArrivalViable || !dragonArenaProtected) {
      throw new IllegalArgumentException("End progression safety flags must be enabled");
    }
  }

  /** Derives stable progression anchors from the frozen End fragment compiler. */
  public static EndProgressionContract from(EndFragmentTerrainCompiler compiler) {
    if (compiler == null) {
      throw new IllegalArgumentException("End fragment compiler is required");
    }
    WorldIdentity identity = compiler.worldIdentity();
    CellKey progressionCell = new CellKey("end:progression", 0L, 0L);
    StableId contractId =
        identity.stream("geological", "end-progression-contract", progressionCell, 0).stableId();
    EndParentBodyState central = compiler.parentBodyAtCell(0L, 0L).orElseThrow();
    List<EndParentBodyState> gateways =
        List.of(
            compiler.parentBodyAtCell(1L, 0L).orElseThrow(),
            compiler.parentBodyAtCell(-1L, 0L).orElseThrow(),
            compiler.parentBodyAtCell(0L, 1L).orElseThrow(),
            compiler.parentBodyAtCell(0L, -1L).orElseThrow());
    List<EndParentBodyState> outer =
        List.of(
            compiler.parentBodyAtCell(3L, 0L).orElseThrow(),
            compiler.parentBodyAtCell(-3L, 0L).orElseThrow(),
            compiler.parentBodyAtCell(0L, 3L).orElseThrow(),
            compiler.parentBodyAtCell(0L, -3L).orElseThrow(),
            compiler.parentBodyAtCell(3L, 3L).orElseThrow(),
            compiler.parentBodyAtCell(-3L, 3L).orElseThrow(),
            compiler.parentBodyAtCell(3L, -3L).orElseThrow(),
            compiler.parentBodyAtCell(-3L, -3L).orElseThrow());
    if (gateways.stream()
            .anyMatch(body -> body.role() != EndParentBodyState.FragmentRole.GATEWAY_RING)
        || outer.stream()
            .anyMatch(body -> body.role() != EndParentBodyState.FragmentRole.OUTER_ISLAND)) {
      throw new IllegalStateException("End progression lattice roles changed");
    }
    List<StableId> gatewayIds = gateways.stream().map(EndParentBodyState::parentBodyId).toList();
    List<StableId> outerIds = outer.stream().map(EndParentBodyState::parentBodyId).toList();
    List<StructureSlot> slots = new java.util.ArrayList<>();
    slots.add(
        slot(
            identity,
            progressionCell,
            0,
            StructureKind.EXIT_PORTAL,
            central,
            new Point3(
                central.center().x(),
                central.center().y() + central.verticalRadiusBlocks(),
                central.center().z()),
            56.0));
    slots.add(
        slot(
            identity,
            progressionCell,
            1,
            StructureKind.DRAGON_ARENA,
            central,
            central.center(),
            176.0));
    for (int index = 0; index < gateways.size(); index++) {
      EndParentBodyState body = gateways.get(index);
      slots.add(
          slot(
              identity,
              progressionCell,
              2 + index,
              StructureKind.END_GATEWAY,
              body,
              body.center(),
              52.0));
    }
    for (int index = 0; index < outer.size(); index++) {
      EndParentBodyState body = outer.get(index);
      slots.add(
          slot(
              identity,
              progressionCell,
              6 + index,
              index % 2 == 0 ? StructureKind.OUTER_END_CITY : StructureKind.CHORUS_HABITAT,
              body,
              body.center(),
              index % 2 == 0 ? 96.0 : 128.0));
    }
    return new EndProgressionContract(
        contractId,
        central.parentBodyId(),
        gatewayIds,
        outerIds,
        slots,
        compiler.profile().structureProgressionContract(),
        true,
        true,
        central.horizontalRadiusBlocks(),
        512.0 - central.horizontalRadiusBlocks());
  }

  public boolean protects(long blockX, long blockZ) {
    return structureSlots.stream().anyMatch(slot -> slot.contains(blockX, blockZ));
  }

  public Optional<StructureSlot> slotAt(long blockX, long blockZ) {
    return structureSlots.stream().filter(slot -> slot.contains(blockX, blockZ)).findFirst();
  }

  private static StructureSlot slot(
      WorldIdentity identity,
      CellKey cell,
      int index,
      StructureKind kind,
      EndParentBodyState body,
      Point3 anchor,
      double radius) {
    return new StructureSlot(
        identity.stream("geological", "end-structure-slot", cell, index).stableId(),
        kind,
        body.parentBodyId(),
        anchor,
        radius,
        true);
  }

  public enum StructureKind {
    EXIT_PORTAL,
    DRAGON_ARENA,
    END_GATEWAY,
    OUTER_END_CITY,
    CHORUS_HABITAT
  }

  public record StructureSlot(
      StableId structureId,
      StructureKind kind,
      StableId anchorBodyId,
      Point3 anchor,
      double horizontalRadiusBlocks,
      boolean protectedFromTerrainWrites) {
    public StructureSlot {
      if (structureId == null || kind == null || anchorBodyId == null || anchor == null) {
        throw new IllegalArgumentException("End structure slot identities are required");
      }
      if (!Double.isFinite(horizontalRadiusBlocks) || horizontalRadiusBlocks <= 0.0) {
        throw new IllegalArgumentException("End structure slot radius must be positive");
      }
    }

    private boolean contains(long blockX, long blockZ) {
      double dx = blockX + 0.5 - anchor.x();
      double dz = blockZ + 0.5 - anchor.z();
      return dx * dx + dz * dz <= horizontalRadiusBlocks * horizontalRadiusBlocks;
    }
  }
}
