package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.Optional;

/**
 * Phase 3 VMS topology derived from a synvolcanic basin decision.
 *
 * <p>The lens and feeder are bounded geometry evidence in the basin's local frame. They are not a
 * block-placement recipe and do not imply a universal vent temperature or grade distribution.
 */
public record VmsSystemState(
    StableId systemId,
    FormationStatus status,
    StableId basinId,
    StableId heatSourceId,
    StableId feederPathId,
    AgeKey seafloorAge,
    FluidSourceClass fluidSourceClass,
    GeometryClass geometryClass,
    Point3 localCenter,
    double lensHalfWidthBlocks,
    double lensHalfHeightBlocks,
    double lensHalfLengthBlocks,
    double feederHalfWidthBlocks,
    double feederHalfLengthBlocks,
    double feederDepthBlocks,
    long sourceBudgetFixedUnits,
    long depositAllocationFixedUnits,
    Optional<String> failedGate) {
  public VmsSystemState {
    if (systemId == null
        || status == null
        || basinId == null
        || heatSourceId == null
        || feederPathId == null
        || seafloorAge == null
        || fluidSourceClass == null
        || geometryClass == null
        || localCenter == null
        || failedGate == null) {
      throw new IllegalArgumentException("VMS system state must be complete");
    }
    requirePositive(lensHalfWidthBlocks, "lensHalfWidthBlocks");
    requirePositive(lensHalfHeightBlocks, "lensHalfHeightBlocks");
    requirePositive(lensHalfLengthBlocks, "lensHalfLengthBlocks");
    requirePositive(feederHalfWidthBlocks, "feederHalfWidthBlocks");
    requirePositive(feederHalfLengthBlocks, "feederHalfLengthBlocks");
    requirePositive(feederDepthBlocks, "feederDepthBlocks");
    if (sourceBudgetFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || depositAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("VMS budgets are out of bounds");
    }
    if (status == FormationStatus.FORMED) {
      if (geometryClass != GeometryClass.STRATIFORM_LENS_WITH_FEEDER
          || fluidSourceClass != FluidSourceClass.SEAWATER_DOMINATED_HYDROTHERMAL
          || failedGate.isPresent()) {
        throw new IllegalArgumentException("formed VMS state must expose lens, feeder, and fluid");
      }
    } else if (geometryClass != GeometryClass.NO_LENS
        || fluidSourceClass != FluidSourceClass.NO_COEVAL_FLUID
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed VMS state must retain the failed gate");
    }
  }

  /** Derives the topology from the primary VMS decision for one immutable province. */
  public static VmsSystemState proofFor(Province province, MineralSystemDecision decision) {
    if (province == null || decision == null) {
      throw new IllegalArgumentException("province and VMS decision are required");
    }
    if (!MineralSystemProofs.VMS_MODEL.equals(decision.modelId())
        || !province.proofIds().vmsSystemId().equals(decision.candidateId())) {
      throw new IllegalArgumentException("decision does not identify this province's VMS system");
    }
    if (decision.status() == FormationStatus.REJECTED) {
      throw new IllegalArgumentException("primary VMS state cannot be rejected");
    }
    RiftArcGeometry geometry = province.geometry();
    Optional<String> failedGate =
        decision.gates().stream()
            .filter(gate -> gate.status() == GateStatus.FAIL)
            .map(GateEvidence::gate)
            .findFirst();
    long sourceBudget = decision.ledger() == null ? 0L : decision.ledger().sourceAmount();
    long depositBudget =
        decision.ledger() == null
            ? 0L
            : decision.ledger().allocations().getOrDefault("deposit", 0L);
    AgeKey seafloorAge =
        decision.deposit() == null ? new AgeKey(241.0, 0) : decision.deposit().formationAge();
    return new VmsSystemState(
        decision.candidateId(),
        decision.status(),
        geometry.basin().id(),
        geometry.plutonPulses().getFirst().id(),
        geometry.fault().id(),
        seafloorAge,
        decision.status() == FormationStatus.FORMED
            ? FluidSourceClass.SEAWATER_DOMINATED_HYDROTHERMAL
            : FluidSourceClass.NO_COEVAL_FLUID,
        decision.status() == FormationStatus.FORMED
            ? GeometryClass.STRATIFORM_LENS_WITH_FEEDER
            : GeometryClass.NO_LENS,
        geometry.vmsCenter(),
        112.0,
        15.0,
        72.0,
        28.0,
        34.0,
        95.0,
        sourceBudget,
        depositBudget,
        failedGate);
  }

  /** Classifies a local point as a stratiform lens or its chloritic feeder. */
  public Optional<VmsZone> zoneAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED) {
      return Optional.empty();
    }
    double lensU = (localPoint.x() - localCenter.x()) / lensHalfWidthBlocks;
    double lensY = (localPoint.y() - localCenter.y()) / lensHalfHeightBlocks;
    double lensV = (localPoint.z() - localCenter.z()) / lensHalfLengthBlocks;
    if (lensU * lensU + lensY * lensY + lensV * lensV <= 1.0) {
      return Optional.of(VmsZone.STRATIFORM_MASSIVE_SULFIDE_LENS);
    }
    double feederU = (localPoint.x() - localCenter.x()) / feederHalfWidthBlocks;
    double feederV = (localPoint.z() - localCenter.z()) / feederHalfLengthBlocks;
    if (feederU * feederU + feederV * feederV <= 1.0
        && localPoint.y() <= localCenter.y()
        && localPoint.y() >= localCenter.y() - feederDepthBlocks) {
      return Optional.of(VmsZone.CHLORITIC_FEEDER);
    }
    return Optional.empty();
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum FluidSourceClass {
    SEAWATER_DOMINATED_HYDROTHERMAL,
    NO_COEVAL_FLUID
  }

  public enum GeometryClass {
    STRATIFORM_LENS_WITH_FEEDER,
    NO_LENS
  }

  public enum VmsZone {
    STRATIFORM_MASSIVE_SULFIDE_LENS,
    CHLORITIC_FEEDER
  }
}
