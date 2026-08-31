package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.query.GeologicalSample;
import java.util.List;
import java.util.Optional;

/** Derived Phase 2 material state; natural block/chunk storage is still unnecessary. */
public record PetrologicSample(
    GeologicalSample geology,
    RockDefinition rock,
    MineralAssemblage primaryAssemblage,
    MineralAssemblage resolvedAssemblage,
    BulkComposition primaryComposition,
    BulkComposition resolvedComposition,
    ElementTransferLedger elementLedger,
    MetamorphicHistory metamorphism,
    MaterialProcessClass processClass,
    double porosityFraction,
    double permeabilityIndex,
    double erodibilityIndex,
    Optional<MagmaLineageState> magmaLineage,
    Optional<SedimentaryState> sedimentaryState,
    List<ElementReservoirLedger> reservoirLedgers) {
  public PetrologicSample {
    if (geology == null
        || rock == null
        || primaryAssemblage == null
        || resolvedAssemblage == null
        || primaryComposition == null
        || resolvedComposition == null
        || elementLedger == null
        || metamorphism == null
        || processClass == null
        || magmaLineage == null
        || sedimentaryState == null
        || reservoirLedgers == null) {
      throw new IllegalArgumentException("petrologic sample must be complete");
    }
    reservoirLedgers =
        List.copyOf(reservoirLedgers).stream()
            .sorted(java.util.Comparator.comparing(ElementReservoirLedger::systemId))
            .toList();
    requireUnit(porosityFraction, "porosity");
    requireUnit(permeabilityIndex, "permeability");
    requireUnit(erodibilityIndex, "erodibility");
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
