package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.FluidTransportState;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 3 porphyry fluid-phase and metal-distribution refinement.
 *
 * <p>Fluid pulses and zone metal vectors are normalized explanatory proxies. Their contained
 * deposit allocation is still capped by the primary porphyry ledger; this state is not an assay or
 * a thermodynamic equilibrium solve.
 */
public record PorphyryFluidMetalState(
    StableId systemId,
    FormationStatus status,
    StableId sourceReservoirId,
    StableId fluidPathId,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    Map<ChemicalElement, Long> sourceMetalFractionsPpm,
    List<FluidPulse> fluidPulses,
    List<MetalDistribution> metalDistributions,
    long sourceBudgetFixedUnits,
    long depositAllocationFixedUnits,
    Optional<String> failedGate) {
  private static final long SCALE = 1_000_000L;

  public PorphyryFluidMetalState {
    if (systemId == null
        || status == null
        || sourceReservoirId == null
        || fluidPathId == null
        || localCenter == null
        || sourceMetalFractionsPpm == null
        || fluidPulses == null
        || metalDistributions == null
        || failedGate == null) {
      throw new IllegalArgumentException("porphyry fluid/metal state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceMetalFractionsPpm = normalizedMap(sourceMetalFractionsPpm, "source metal fractions");
    fluidPulses = List.copyOf(fluidPulses);
    metalDistributions = List.copyOf(metalDistributions);
    if (fluidPulses.stream().anyMatch(pulse -> pulse == null)
        || metalDistributions.stream().anyMatch(distribution -> distribution == null)) {
      throw new IllegalArgumentException("porphyry fluid and metal entries cannot be null");
    }
    if (sourceBudgetFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || depositAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("porphyry fluid/metal budgets are out of bounds");
    }
    long distributionAllocation =
        metalDistributions.stream().mapToLong(MetalDistribution::allocationFixedUnits).sum();
    if (distributionAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "porphyry metal distributions must close to deposit allocation");
    }
    if (status == FormationStatus.FORMED) {
      if (sourceMetalFractionsPpm.isEmpty()
          || fluidPulses.size() != 3
          || metalDistributions.size() != 3
          || failedGate.isPresent()
          || sourceBudgetFixedUnits <= 0L
          || depositAllocationFixedUnits <= 0L) {
        throw new IllegalArgumentException(
            "formed porphyry refinement requires fluid and metal zones");
      }
    } else if (!sourceMetalFractionsPpm.isEmpty()
        || !fluidPulses.isEmpty()
        || !metalDistributions.isEmpty()
        || sourceBudgetFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "non-formed porphyry refinement must retain the failed gate");
    }
  }

  /** Derives the refinement from the primary porphyry decision for one immutable province. */
  public static PorphyryFluidMetalState proofFor(
      Province province, MineralSystemDecision decision) {
    if (province == null || decision == null) {
      throw new IllegalArgumentException("province and porphyry decision are required");
    }
    if (!MineralSystemProofs.PORPHYRY_MODEL.equals(decision.modelId())
        || !province.proofIds().porphyrySystemId().equals(decision.candidateId())) {
      throw new IllegalArgumentException(
          "decision does not identify this province's porphyry system");
    }
    PorphyrySystemState topology = PorphyrySystemState.proofFor(province, decision);
    Optional<String> failedGate =
        decision.gates().stream()
            .filter(gate -> gate.status() == GateStatus.FAIL)
            .map(GateEvidence::gate)
            .findFirst();
    boolean formed = decision.status() == FormationStatus.FORMED;
    long sourceBudget = formed && decision.ledger() != null ? decision.ledger().sourceAmount() : 0L;
    long depositAllocation =
        formed && decision.ledger() != null
            ? decision.ledger().allocations().getOrDefault("deposit", 0L)
            : 0L;
    if (!formed) {
      return new PorphyryFluidMetalState(
          decision.candidateId(),
          decision.status(),
          province.proofIds().magmaLineageId(),
          province.geometry().fault().id(),
          topology.localCenter(),
          topology.lateralExtentBlocks(),
          topology.verticalExtentBlocks(),
          Map.of(),
          List.of(),
          List.of(),
          0L,
          0L,
          failedGate);
    }

    List<FluidPulse> pulses =
        List.of(
            new FluidPulse(
                FluidPhaseClass.MAGMATIC_BRINE,
                FluidTransportState.TemperatureClass.HOT,
                SalinityClass.CONCENTRATED_BRINE,
                FluidTransportState.PhaseBehaviorClass.SEPARATION,
                0.0,
                65.0,
                500_000L,
                topology.fluidSystemId()),
            new FluidPulse(
                FluidPhaseClass.VAPOR_RICH_SEPARATED,
                FluidTransportState.TemperatureClass.HOT,
                SalinityClass.CONCENTRATED_BRINE,
                FluidTransportState.PhaseBehaviorClass.SEPARATION,
                65.0,
                125.0,
                350_000L,
                topology.fluidSystemId()),
            new FluidPulse(
                FluidPhaseClass.METEORIC_MIXTURE,
                FluidTransportState.TemperatureClass.WARM,
                SalinityClass.MODERATE_BRINE,
                FluidTransportState.PhaseBehaviorClass.MIXING,
                125.0,
                205.0,
                250_000L,
                topology.fluidSystemId()));
    List<MetalDistribution> distributions =
        List.of(
            new MetalDistribution(
                PorphyrySystemState.AlterationZoneKind.POTASSIC_CORE,
                Map.of(
                    ChemicalElement.CU, 620_000L,
                    ChemicalElement.AU, 120_000L,
                    ChemicalElement.S, 220_000L,
                    ChemicalElement.ZN, 40_000L),
                55_000L,
                topology.systemId()),
            new MetalDistribution(
                PorphyrySystemState.AlterationZoneKind.PHYLLIC_INTERMEDIATE,
                Map.of(
                    ChemicalElement.CU, 500_000L,
                    ChemicalElement.AU, 100_000L,
                    ChemicalElement.S, 260_000L,
                    ChemicalElement.ZN, 140_000L),
                35_000L,
                topology.systemId()),
            new MetalDistribution(
                PorphyrySystemState.AlterationZoneKind.PROPYLITIC_DISTAL,
                Map.of(
                    ChemicalElement.CU, 300_000L,
                    ChemicalElement.AU, 60_000L,
                    ChemicalElement.S, 220_000L,
                    ChemicalElement.ZN, 420_000L),
                15_000L,
                topology.systemId()));
    return new PorphyryFluidMetalState(
        decision.candidateId(),
        decision.status(),
        province.proofIds().magmaLineageId(),
        topology.fluidSystemId(),
        topology.localCenter(),
        topology.lateralExtentBlocks(),
        topology.verticalExtentBlocks(),
        Map.of(
            ChemicalElement.CU, 700_000L,
            ChemicalElement.AU, 150_000L,
            ChemicalElement.S, 100_000L,
            ChemicalElement.ZN, 50_000L),
        pulses,
        distributions,
        sourceBudget,
        depositAllocation,
        failedGate);
  }

  /** Returns the fluid pulse active at a local point. */
  public Optional<FluidPulse> fluidAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED || !insideEnvelope(localPoint)) {
      return Optional.empty();
    }
    double radial = radialDistance(localPoint);
    return fluidPulses.stream().filter(pulse -> pulse.contains(radial)).findFirst();
  }

  /** Returns the normalized metal vector active at a local point. */
  public Optional<MetalDistribution> metalAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED || !insideEnvelope(localPoint)) {
      return Optional.empty();
    }
    double radial = radialDistance(localPoint);
    PorphyrySystemState.AlterationZoneKind zone =
        radial < 65.0
            ? PorphyrySystemState.AlterationZoneKind.POTASSIC_CORE
            : radial < 125.0
                ? PorphyrySystemState.AlterationZoneKind.PHYLLIC_INTERMEDIATE
                : radial <= 205.0 ? PorphyrySystemState.AlterationZoneKind.PROPYLITIC_DISTAL : null;
    if (zone == null) {
      return Optional.empty();
    }
    return metalDistributions.stream()
        .filter(distribution -> distribution.zone() == zone)
        .findFirst();
  }

  private boolean insideEnvelope(Point3 point) {
    return StrictMath.abs(point.y() - localCenter.y()) <= verticalExtentBlocks
        && radialDistance(point) <= lateralExtentBlocks;
  }

  private double radialDistance(Point3 point) {
    return StrictMath.hypot(point.x() - localCenter.x(), point.z() - localCenter.z());
  }

  private static Map<ChemicalElement, Long> normalizedMap(
      Map<ChemicalElement, Long> source, String name) {
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount <= 0L || amount > SCALE) {
            throw new IllegalArgumentException(name + " contain invalid values");
          }
          copied.put(element, amount);
        });
    if (!copied.isEmpty() && copied.values().stream().mapToLong(Long::longValue).sum() != SCALE) {
      throw new IllegalArgumentException(name + " must close to " + SCALE);
    }
    return Collections.unmodifiableMap(copied);
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum FluidPhaseClass {
    MAGMATIC_BRINE,
    VAPOR_RICH_SEPARATED,
    METEORIC_MIXTURE
  }

  public record FluidPulse(
      FluidPhaseClass phase,
      FluidTransportState.TemperatureClass temperature,
      SalinityClass salinity,
      FluidTransportState.PhaseBehaviorClass phaseBehavior,
      double innerRadiusBlocks,
      double outerRadiusBlocks,
      long integratedFluxPpm,
      StableId pathId) {
    public FluidPulse {
      if (phase == null
          || temperature == null
          || salinity == null
          || phaseBehavior == null
          || pathId == null) {
        throw new IllegalArgumentException("porphyry fluid pulse identity is required");
      }
      if (!Double.isFinite(innerRadiusBlocks)
          || !Double.isFinite(outerRadiusBlocks)
          || innerRadiusBlocks < 0.0
          || outerRadiusBlocks <= innerRadiusBlocks
          || integratedFluxPpm <= 0L
          || integratedFluxPpm > SCALE) {
        throw new IllegalArgumentException("porphyry fluid pulse bounds are invalid");
      }
    }

    private boolean contains(double radius) {
      return radius >= innerRadiusBlocks
          && (radius < outerRadiusBlocks
              || radius == outerRadiusBlocks && outerRadiusBlocks == 205.0);
    }
  }

  public record MetalDistribution(
      PorphyrySystemState.AlterationZoneKind zone,
      Map<ChemicalElement, Long> abundancePpm,
      long allocationFixedUnits,
      StableId hostId) {
    public MetalDistribution {
      if (zone == null || abundancePpm == null || hostId == null) {
        throw new IllegalArgumentException("porphyry metal distribution identity is required");
      }
      abundancePpm = normalizedMap(abundancePpm, "metal abundances");
      if (allocationFixedUnits <= 0L || allocationFixedUnits > SCALE) {
        throw new IllegalArgumentException("porphyry metal allocation is out of bounds");
      }
    }
  }
}
