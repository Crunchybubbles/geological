package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bulk rock classification, central mineral recipe, and body-scale property distributions. */
public record RockDefinition(
    String id,
    Lithology lithology,
    GeneticFamily geneticFamily,
    RockTexture texture,
    MineralAssemblage primaryAssemblage,
    double modalSpreadFraction,
    List<ModalVariationAxis> modalVariationAxes,
    UnitIntervalDistribution porosityDistribution,
    UnitIntervalDistribution permeabilityDistribution,
    UnitIntervalDistribution erodibilityDistribution) {
  public RockDefinition {
    if (id == null
        || id.isBlank()
        || lithology == null
        || geneticFamily == null
        || texture == null
        || primaryAssemblage == null
        || modalVariationAxes == null
        || porosityDistribution == null
        || permeabilityDistribution == null
        || erodibilityDistribution == null) {
      throw new IllegalArgumentException("rock definition must be complete");
    }
    if (!Double.isFinite(modalSpreadFraction)
        || modalSpreadFraction < 0.0
        || modalSpreadFraction > 0.5) {
      throw new IllegalArgumentException("modal spread fraction must lie in [0, 0.5]");
    }
    modalVariationAxes =
        List.copyOf(modalVariationAxes).stream()
            .sorted(Comparator.comparing(ModalVariationAxis::id))
            .toList();
    Set<String> axisIds = new HashSet<>();
    Map<String, Long> modes = primaryAssemblage.modesPpm();
    HashMap<String, Long> maximumOffsets = new HashMap<>();
    for (ModalVariationAxis axis : modalVariationAxes) {
      if (!axisIds.add(axis.id())) {
        throw new IllegalArgumentException("duplicate modal variation axis " + axis.id());
      }
      axis.loadingsPpm()
          .forEach(
              (mineralId, loading) -> {
                if (!modes.containsKey(mineralId)) {
                  throw new IllegalArgumentException(
                      "modal variation axis references non-primary mineral " + mineralId);
                }
                maximumOffsets.merge(mineralId, StrictMath.abs(loading), Math::addExact);
              });
    }
    if ((modalSpreadFraction == 0.0) != modalVariationAxes.isEmpty()) {
      throw new IllegalArgumentException(
          "non-zero modal spread requires authored variation axes and zero spread forbids them");
    }
    maximumOffsets.forEach(
        (mineralId, offset) -> {
          long limit = (long) StrictMath.floor(modes.get(mineralId) * modalSpreadFraction + 1.0e-9);
          if (offset > limit) {
            throw new IllegalArgumentException(
                "modal variation exceeds spread bound for " + mineralId);
          }
        });
  }

  public double porosityFraction() {
    return porosityDistribution.mode();
  }

  public double permeabilityIndex() {
    return permeabilityDistribution.mode();
  }

  public double erodibilityIndex() {
    return erodibilityDistribution.mode();
  }
}
