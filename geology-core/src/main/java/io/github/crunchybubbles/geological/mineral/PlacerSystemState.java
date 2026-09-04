package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Optional;

/**
 * Phase 3 source-linked alluvial placer state derived from a primary deposit and drainage trap.
 *
 * <p>The state exposes a bounded catchment envelope and exact release/loss/trap accounting. It does
 * not turn a hydraulic-looking bar into ore without an exposed upstream source.
 */
public record PlacerSystemState(
    StableId systemId,
    FormationStatus status,
    StableId sourceDepositId,
    StableId weatheringProcessId,
    StableId trapId,
    AgeKey formationAge,
    SourceExposureClass sourceExposureClass,
    TransportClass transportClass,
    TrapClass trapClass,
    SortingClass sortingClass,
    Point2 sourceCenter,
    Point2 trapCenter,
    double sourceDistanceBlocks,
    double trapHalfLengthBlocks,
    double trapHalfWidthBlocks,
    double hydraulicTrapScore,
    long sourceBudgetFixedUnits,
    long releasedSourceBudgetFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    Optional<String> failedGate) {
  public PlacerSystemState {
    if (systemId == null
        || status == null
        || sourceDepositId == null
        || weatheringProcessId == null
        || trapId == null
        || formationAge == null
        || sourceExposureClass == null
        || transportClass == null
        || trapClass == null
        || sortingClass == null
        || sourceCenter == null
        || trapCenter == null
        || failedGate == null) {
      throw new IllegalArgumentException("placer system state must be complete");
    }
    requirePositive(sourceDistanceBlocks, "sourceDistanceBlocks");
    requirePositive(trapHalfLengthBlocks, "trapHalfLengthBlocks");
    requirePositive(trapHalfWidthBlocks, "trapHalfWidthBlocks");
    if (!Double.isFinite(hydraulicTrapScore)
        || hydraulicTrapScore < 0.0
        || hydraulicTrapScore > 1.0) {
      throw new IllegalArgumentException("placer hydraulic trap score must be bounded");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedSourceBudgetFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedSourceBudgetFixedUnits > sourceBudgetFixedUnits
        || depositAllocationFixedUnits > releasedSourceBudgetFixedUnits
        || transportLossFixedUnits > releasedSourceBudgetFixedUnits
        || Math.addExact(transportLossFixedUnits, depositAllocationFixedUnits)
            != releasedSourceBudgetFixedUnits) {
      throw new IllegalArgumentException("placer source and trap budgets are inconsistent");
    }
    if (status == FormationStatus.FORMED) {
      if (sourceExposureClass != SourceExposureClass.PARTLY_EXPOSED_PRIMARY
          || transportClass != TransportClass.CONNECTED_WATER_CATCHMENT
          || trapClass != TrapClass.HYDRAULIC_GRADIENT_BREAK
          || sortingClass != SortingClass.DENSE_MINERAL_HYDRAULIC_SORTING
          || hydraulicTrapScore <= 0.0
          || failedGate.isPresent()
          || sourceBudgetFixedUnits <= 0L
          || releasedSourceBudgetFixedUnits <= 0L
          || depositAllocationFixedUnits <= 0L) {
        throw new IllegalArgumentException(
            "formed placers require source, transport, and trap evidence");
      }
    } else if (sourceExposureClass != SourceExposureClass.NO_EXPOSED_SOURCE
        || transportClass != TransportClass.NO_SOURCE_LINKED_TRANSPORT
        || trapClass != TrapClass.NO_ALLOWABLE_TRAP
        || sortingClass != SortingClass.UNRESOLVED_SORTING
        || hydraulicTrapScore != 0.0
        || sourceBudgetFixedUnits != 0L
        || releasedSourceBudgetFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed placers must retain the failed source gate");
    }
  }

  /** Derives the state from the primary placer decision for one immutable province. */
  public static PlacerSystemState proofFor(Province province, MineralSystemDecision decision) {
    if (province == null || decision == null) {
      throw new IllegalArgumentException("province and placer decision are required");
    }
    if (!MineralSystemProofs.PLACER_MODEL.equals(decision.modelId())
        || !province.proofIds().placerSystemId().equals(decision.candidateId())) {
      throw new IllegalArgumentException(
          "decision does not identify this province's placer system");
    }
    RiftArcGeometry geometry = province.geometry();
    Point2 sourceCenter = new Point2(geometry.porphyryCenter().x(), geometry.porphyryCenter().z());
    Point2 trapCenter = geometry.placerCenter();
    double sourceDistance = StrictMath.sqrt(sourceCenter.squaredDistance(trapCenter));
    Optional<String> failedGate =
        decision.gates().stream()
            .filter(gate -> gate.status() == GateStatus.FAIL)
            .map(GateEvidence::gate)
            .findFirst();
    boolean formed = decision.status() == FormationStatus.FORMED;
    long sourceBudget = formed && decision.ledger() != null ? decision.ledger().sourceAmount() : 0L;
    long depositAllocation =
        formed && decision.ledger() != null
            ? decision.ledger().allocations().getOrDefault("placer_trap", 0L)
            : 0L;
    long transportLoss =
        formed && decision.ledger() != null
            ? decision.ledger().allocations().getOrDefault("transport_and_dilution_loss", 0L)
            : 0L;
    long released = Math.addExact(depositAllocation, transportLoss);
    AgeKey age =
        decision.deposit() == null ? new AgeKey(0.1, 0) : decision.deposit().formationAge();
    return new PlacerSystemState(
        decision.candidateId(),
        decision.status(),
        province.proofIds().porphyryDepositId(),
        province.proofIds().weatheringId(),
        formed ? decision.deposit().id() : decision.candidateId(),
        age,
        formed ? SourceExposureClass.PARTLY_EXPOSED_PRIMARY : SourceExposureClass.NO_EXPOSED_SOURCE,
        formed
            ? TransportClass.CONNECTED_WATER_CATCHMENT
            : TransportClass.NO_SOURCE_LINKED_TRANSPORT,
        formed ? TrapClass.HYDRAULIC_GRADIENT_BREAK : TrapClass.NO_ALLOWABLE_TRAP,
        formed ? SortingClass.DENSE_MINERAL_HYDRAULIC_SORTING : SortingClass.UNRESOLVED_SORTING,
        sourceCenter,
        trapCenter,
        sourceDistance,
        105.0,
        68.0,
        formed ? 1.0 : 0.0,
        sourceBudget,
        released,
        transportLoss,
        depositAllocation,
        failedGate);
  }

  /** Returns whether the local point lies in the bounded hydraulic trap envelope. */
  public boolean contains(Point2 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED) {
      return false;
    }
    double along = (localPoint.z() - trapCenter.z()) / trapHalfLengthBlocks;
    double across = (localPoint.x() - trapCenter.x()) / trapHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  /** Returns the trap zone when a local point receives the source-linked deposit allocation. */
  public Optional<PlacerZone> zoneAt(Point2 localPoint) {
    return contains(localPoint) ? Optional.of(PlacerZone.HYDRAULIC_TRAP) : Optional.empty();
  }

  /** Returns the source inventory left upstream after weathering release. */
  public long retainedSourceBudgetFixedUnits() {
    return sourceBudgetFixedUnits - releasedSourceBudgetFixedUnits;
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum SourceExposureClass {
    PARTLY_EXPOSED_PRIMARY,
    NO_EXPOSED_SOURCE
  }

  public enum TransportClass {
    CONNECTED_WATER_CATCHMENT,
    NO_SOURCE_LINKED_TRANSPORT
  }

  public enum TrapClass {
    HYDRAULIC_GRADIENT_BREAK,
    NO_ALLOWABLE_TRAP
  }

  public enum SortingClass {
    DENSE_MINERAL_HYDRAULIC_SORTING,
    UNRESOLVED_SORTING
  }

  public enum PlacerZone {
    HYDRAULIC_TRAP
  }
}
