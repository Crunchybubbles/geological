package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Phase 3 porphyry-system topology derived from the existing province and mineral-system proof.
 *
 * <p>The state is a bounded explanatory envelope: it records which intrusion, fluid, and stockwork
 * are linked and exposes coarse alteration zoning without claiming a voxel-scale vein solve or
 * measured grade distribution.
 */
public record PorphyrySystemState(
    StableId systemId,
    FormationStatus status,
    StableId intrusionId,
    StableId fluidSystemId,
    StableId stockworkPathId,
    IntrusionClass intrusionClass,
    FluidSourceClass fluidSourceClass,
    StockworkClass stockworkClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    List<AlterationZone> alterationZones,
    long sourceBudgetFixedUnits,
    long depositAllocationFixedUnits,
    Optional<String> failedGate) {
  public PorphyrySystemState {
    if (systemId == null
        || status == null
        || intrusionId == null
        || fluidSystemId == null
        || stockworkPathId == null
        || intrusionClass == null
        || fluidSourceClass == null
        || stockworkClass == null
        || localCenter == null
        || alterationZones == null
        || failedGate == null) {
      throw new IllegalArgumentException("porphyry system state must be complete");
    }
    if (!Double.isFinite(lateralExtentBlocks)
        || lateralExtentBlocks <= 0.0
        || !Double.isFinite(verticalExtentBlocks)
        || verticalExtentBlocks <= 0.0) {
      throw new IllegalArgumentException("porphyry extents must be finite and positive");
    }
    if (sourceBudgetFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || depositAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("porphyry budgets are out of bounds");
    }
    alterationZones =
        List.copyOf(alterationZones).stream()
            .sorted(Comparator.comparingDouble(AlterationZone::innerRadiusBlocks))
            .toList();
    double previousOuter = 0.0;
    for (AlterationZone zone : alterationZones) {
      if (zone.innerRadiusBlocks() < previousOuter) {
        throw new IllegalArgumentException("porphyry alteration zones must not overlap");
      }
      previousOuter = zone.outerRadiusBlocks();
      if (zone.outerRadiusBlocks() > lateralExtentBlocks) {
        throw new IllegalArgumentException("porphyry alteration zone exceeds system extent");
      }
    }
    if (status == FormationStatus.FORMED && alterationZones.isEmpty()) {
      throw new IllegalArgumentException("formed porphyry systems require alteration zoning");
    }
    if (status != FormationStatus.FORMED && !alterationZones.isEmpty()) {
      throw new IllegalArgumentException("barren porphyry systems cannot publish formed zoning");
    }
    if (status == FormationStatus.FORMED && failedGate.isPresent()) {
      throw new IllegalArgumentException("formed porphyry systems cannot have a failed gate");
    }
    if (status != FormationStatus.FORMED && failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren porphyry systems require a failed gate");
    }
  }

  /** Derives the topology from the primary porphyry decision for one immutable province. */
  public static PorphyrySystemState proofFor(Province province, MineralSystemDecision decision) {
    if (province == null || decision == null) {
      throw new IllegalArgumentException("province and porphyry decision are required");
    }
    if (!MineralSystemProofs.PORPHYRY_MODEL.equals(decision.modelId())
        || !province.proofIds().porphyrySystemId().equals(decision.candidateId())) {
      throw new IllegalArgumentException(
          "decision does not identify this province's porphyry system");
    }
    if (decision.status() == FormationStatus.REJECTED) {
      throw new IllegalArgumentException("primary porphyry state cannot be rejected");
    }
    RiftArcGeometry geometry = province.geometry();
    RiftArcGeometry.PlutonPulse intrusion = geometry.plutonPulses().getLast();
    long sourceBudget = decision.ledger() == null ? 0L : decision.ledger().sourceAmount();
    long depositBudget =
        decision.ledger() == null
            ? 0L
            : decision.ledger().allocations().getOrDefault("deposit", 0L);
    Optional<String> failedGate =
        decision.gates().stream()
            .filter(gate -> gate.status() == GateStatus.FAIL)
            .map(GateEvidence::gate)
            .findFirst();
    List<AlterationZone> zones =
        decision.status() == FormationStatus.FORMED
            ? List.of(
                new AlterationZone(
                    AlterationZoneKind.POTASSIC_CORE,
                    Overprint.POTASSIC_ALTERATION,
                    0.0,
                    65.0,
                    900_000L,
                    province.proofIds().porphyrySystemId()),
                new AlterationZone(
                    AlterationZoneKind.PHYLLIC_INTERMEDIATE,
                    Overprint.PHYLLIC_ALTERATION,
                    65.0,
                    125.0,
                    650_000L,
                    province.proofIds().porphyrySystemId()),
                new AlterationZone(
                    AlterationZoneKind.PROPYLITIC_DISTAL,
                    Overprint.PROPYLITIC_ALTERATION,
                    125.0,
                    205.0,
                    400_000L,
                    province.proofIds().porphyrySystemId()))
            : List.of();
    return new PorphyrySystemState(
        decision.candidateId(),
        decision.status(),
        intrusion.id(),
        decision.candidateId(),
        geometry.fault().id(),
        IntrusionClass.MULTI_PULSE_FELSIC_STOCK,
        decision.status() == FormationStatus.FORMED
            ? FluidSourceClass.MAGMATIC_HYDROTHERMAL
            : FluidSourceClass.LOW_VOLATILE_MAGMATIC,
        decision.status() == FormationStatus.FORMED
            ? StockworkClass.CONNECTED_STOCKWORK
            : StockworkClass.DISCONNECTED_STOCKWORK,
        geometry.porphyryCenter(),
        205.0,
        160.0,
        zones,
        sourceBudget,
        depositBudget,
        failedGate);
  }

  /** Returns the alteration zone containing a local point, if the system is formed. */
  public Optional<AlterationZone> zoneAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > verticalExtentBlocks) {
      return Optional.empty();
    }
    double dx = localPoint.x() - localCenter.x();
    double dz = localPoint.z() - localCenter.z();
    double radius = StrictMath.sqrt(dx * dx + dz * dz);
    return alterationZones.stream()
        .filter(
            zone ->
                radius >= zone.innerRadiusBlocks()
                    && (radius < zone.outerRadiusBlocks()
                        || radius == zone.outerRadiusBlocks()
                            && zone.outerRadiusBlocks() == lateralExtentBlocks))
        .findFirst();
  }

  public enum IntrusionClass {
    MULTI_PULSE_FELSIC_STOCK
  }

  public enum FluidSourceClass {
    MAGMATIC_HYDROTHERMAL,
    LOW_VOLATILE_MAGMATIC
  }

  public enum StockworkClass {
    CONNECTED_STOCKWORK,
    DISCONNECTED_STOCKWORK
  }

  public enum AlterationZoneKind {
    POTASSIC_CORE,
    PHYLLIC_INTERMEDIATE,
    PROPYLITIC_DISTAL
  }

  public record AlterationZone(
      AlterationZoneKind kind,
      Overprint overprint,
      double innerRadiusBlocks,
      double outerRadiusBlocks,
      long intensityPpm,
      StableId anchorId) {
    public AlterationZone {
      if (kind == null || overprint == null || anchorId == null) {
        throw new IllegalArgumentException("porphyry alteration zone identity is required");
      }
      if (!Double.isFinite(innerRadiusBlocks)
          || !Double.isFinite(outerRadiusBlocks)
          || innerRadiusBlocks < 0.0
          || outerRadiusBlocks <= innerRadiusBlocks) {
        throw new IllegalArgumentException("porphyry alteration radii are invalid");
      }
      if (intensityPpm < 0L || intensityPpm > 1_000_000L) {
        throw new IllegalArgumentException("porphyry alteration intensity must be bounded");
      }
    }
  }
}
