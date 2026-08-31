package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.List;
import java.util.Optional;

/** Coordinate-independent Phase 2 material state suitable for exact vertical-run compression. */
public record PetrologicState(
    MaterialState geology,
    RockDefinition rock,
    MineralAssemblage primaryAssemblage,
    MineralAssemblage resolvedAssemblage,
    BulkComposition primaryComposition,
    BulkComposition resolvedComposition,
    ElementTransferLedger elementLedger,
    MaterialProcessLedger materialProcessLedger,
    MetamorphicHistory metamorphism,
    MaterialProcessClass processClass,
    double porosityFraction,
    double permeabilityIndex,
    double erodibilityIndex,
    Optional<MagmaLineageState> magmaLineage,
    Optional<SedimentaryState> sedimentaryState,
    List<ElementReservoirLedger> reservoirLedgers) {
  public PetrologicState {
    if (geology == null
        || rock == null
        || primaryAssemblage == null
        || resolvedAssemblage == null
        || primaryComposition == null
        || resolvedComposition == null
        || elementLedger == null
        || materialProcessLedger == null
        || metamorphism == null
        || processClass == null
        || magmaLineage == null
        || sedimentaryState == null
        || reservoirLedgers == null) {
      throw new IllegalArgumentException("petrologic state must be complete");
    }
    for (ChemicalElement element : ChemicalElement.values()) {
      if (materialProcessLedger.netTransferPpm(element)
          != elementLedger.transferPpm().getOrDefault(element, 0L)) {
        throw new IllegalArgumentException("material process does not match element ledger");
      }
    }
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
        sample.primaryAssemblage(),
        sample.resolvedAssemblage(),
        sample.primaryComposition(),
        sample.resolvedComposition(),
        sample.elementLedger(),
        sample.materialProcessLedger(),
        sample.metamorphism(),
        sample.processClass(),
        sample.porosityFraction(),
        sample.permeabilityIndex(),
        sample.erodibilityIndex(),
        sample.magmaLineage(),
        sample.sedimentaryState(),
        sample.reservoirLedgers());
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
