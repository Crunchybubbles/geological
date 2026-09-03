package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.List;
import java.util.Optional;

/** Coordinate-independent Phase 2 material state suitable for exact vertical-run compression. */
public record PetrologicState(
    MaterialState geology,
    RockDefinition rock,
    RockTexture resolvedTexture,
    MaterialAssemblage primaryAssemblage,
    MaterialAssemblage resolvedAssemblage,
    List<SolidSolutionState> primarySolidSolutions,
    List<SolidSolutionState> resolvedSolidSolutions,
    BulkComposition primaryComposition,
    BulkComposition resolvedComposition,
    ElementTransferLedger elementLedger,
    MaterialProcessLedger materialProcessLedger,
    AlterationContribution alterationContribution,
    MetamorphicHistory metamorphism,
    MaterialProcessClass processClass,
    Optional<ProcessFluidState> fluidState,
    double porosityFraction,
    double permeabilityIndex,
    double erodibilityIndex,
    Optional<MagmaLineageState> magmaLineage,
    Optional<MantleCargoState> mantleCargo,
    Optional<SedimentaryState> sedimentaryState,
    List<ElementReservoirLedger> reservoirLedgers) {
  public PetrologicState {
    if (geology == null
        || rock == null
        || resolvedTexture == null
        || primaryAssemblage == null
        || resolvedAssemblage == null
        || primarySolidSolutions == null
        || resolvedSolidSolutions == null
        || primaryComposition == null
        || resolvedComposition == null
        || elementLedger == null
        || materialProcessLedger == null
        || alterationContribution == null
        || metamorphism == null
        || processClass == null
        || fluidState == null
        || magmaLineage == null
        || mantleCargo == null
        || sedimentaryState == null
        || reservoirLedgers == null) {
      throw new IllegalArgumentException("petrologic state must be complete");
    }
    primarySolidSolutions = sortedSolidSolutions(primarySolidSolutions);
    resolvedSolidSolutions = sortedSolidSolutions(resolvedSolidSolutions);
    for (ChemicalElement element : ChemicalElement.values()) {
      if (materialProcessLedger.netTransferPpm(element)
          != elementLedger.transferPpm().getOrDefault(element, 0L)) {
        throw new IllegalArgumentException("material process does not match element ledger");
      }
    }
    if (alterationContribution.processClass() != processClass) {
      throw new IllegalArgumentException("alteration contribution does not match process class");
    }
    for (ChemicalElement element : ChemicalElement.values()) {
      if (alterationContribution.additionsPpm().getOrDefault(element, 0L)
              != materialProcessLedger.additionsPpm().getOrDefault(element, 0L)
          || alterationContribution.removalsPpm().getOrDefault(element, 0L)
              != materialProcessLedger.removalsPpm().getOrDefault(element, 0L)) {
        throw new IllegalArgumentException(
            "alteration contribution does not match element process");
      }
    }
    requireFluidState(processClass, fluidState);
    requireMantleCargo(geology.rockBodyId(), rock, mantleCargo);
    reservoirLedgers =
        List.copyOf(reservoirLedgers).stream()
            .sorted(java.util.Comparator.comparing(ElementReservoirLedger::systemId))
            .toList();
    requireUnit(porosityFraction, "porosity");
    requireUnit(permeabilityIndex, "permeability");
    requireUnit(erodibilityIndex, "erodibility");
  }

  public static PetrologicState from(PetrologicSample sample) {
    return new PetrologicState(
        MaterialState.from(sample.geology()),
        sample.rock(),
        sample.resolvedTexture(),
        sample.primaryAssemblage(),
        sample.resolvedAssemblage(),
        sample.primarySolidSolutions(),
        sample.resolvedSolidSolutions(),
        sample.primaryComposition(),
        sample.resolvedComposition(),
        sample.elementLedger(),
        sample.materialProcessLedger(),
        sample.alterationContribution(),
        sample.metamorphism(),
        sample.processClass(),
        sample.fluidState(),
        sample.porosityFraction(),
        sample.permeabilityIndex(),
        sample.erodibilityIndex(),
        sample.magmaLineage(),
        sample.mantleCargo(),
        sample.sedimentaryState(),
        sample.reservoirLedgers());
  }

  /**
   * Returns the deterministic normalized host-buffer state for this coordinate-independent state.
   */
  public MaterialBufferState materialBufferState() {
    return MaterialBufferState.proofFor(
        rock.lithology(), resolvedAssemblage, resolvedComposition, processClass, fluidState);
  }

  /**
   * Returns the deterministic normalized fracture-tensor proxy for this coordinate-independent
   * state.
   */
  public FractureTensorState fractureTensorState() {
    return FractureTensorState.proofFor(
        rock.lithology(), resolvedTexture, processClass, metamorphism.processState().strainState());
  }

  /** Returns sparse deterministic trace-element and log-concentration evidence. */
  public TraceElementVector traceElementVector() {
    return TraceElementVector.from(resolvedComposition);
  }

  /** Returns the optional deterministic transport axes for this state's process fluid. */
  public Optional<FluidTransportState> fluidTransportState() {
    return fluidState.map(FluidTransportState::proofFor);
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  private static List<SolidSolutionState> sortedSolidSolutions(List<SolidSolutionState> states) {
    List<SolidSolutionState> sorted =
        List.copyOf(states).stream()
            .sorted(java.util.Comparator.comparing(SolidSolutionState::definitionId))
            .toList();
    if (sorted.stream().map(SolidSolutionState::definitionId).distinct().count() != sorted.size()) {
      throw new IllegalArgumentException("solid-solution states must have unique definitions");
    }
    return sorted;
  }

  private static void requireFluidState(
      MaterialProcessClass processClass, Optional<ProcessFluidState> fluidState) {
    boolean requiresFluid =
        processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
            || processClass == MaterialProcessClass.WEATHERING;
    if (requiresFluid != fluidState.isPresent()) {
      throw new IllegalArgumentException("petrologic process and fluid state do not agree");
    }
  }

  private static void requireMantleCargo(
      StableId rockBodyId, RockDefinition rock, Optional<MantleCargoState> mantleCargo) {
    if ((rock.lithology() == Lithology.KIMBERLITIC) != mantleCargo.isPresent()) {
      throw new IllegalArgumentException("mantle cargo is required exactly for kimberlitic rock");
    }
    if (mantleCargo.isPresent() && !mantleCargo.orElseThrow().carrierBodyId().equals(rockBodyId)) {
      throw new IllegalArgumentException("mantle cargo carrier does not match rock body");
    }
  }
}
