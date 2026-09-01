package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Overprint;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/** Authored response for a younger metamorphic, hydrothermal, or weathering overprint. */
public record AlterationDefinition(
    Overprint overprint,
    MaterialProcessClass processClass,
    Optional<ProcessFluidState> fluidState,
    long replacementPpm,
    List<AlterationAssemblageRecipe> targetRecipes,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double minimumTemperatureCelsius,
    double maximumTemperatureCelsius,
    double minimumPressureMpa,
    double maximumPressureMpa,
    double porosityMultiplier,
    double erodibilityDelta) {
  public AlterationDefinition {
    if (overprint == null
        || processClass == null
        || fluidState == null
        || targetRecipes == null
        || facies == null
        || path == null) {
      throw new IllegalArgumentException("alteration definition identity must be complete");
    }
    boolean requiresFluid =
        processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
            || processClass == MaterialProcessClass.WEATHERING;
    if (requiresFluid != fluidState.isPresent()) {
      throw new IllegalArgumentException(
          "hydrothermal and weathering processes require an explicit fluid state");
    }
    targetRecipes =
        List.copyOf(targetRecipes).stream()
            .sorted(Comparator.comparing(recipe -> recipe.protolithFamilies().getFirst().name()))
            .toList();
    if (replacementPpm < 0 || replacementPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("replacement fraction must lie in [0, 1000000]");
    }
    if ((replacementPpm == 0) != targetRecipes.isEmpty()) {
      throw new IllegalArgumentException(
          "target recipes are required exactly when replacement is non-zero");
    }
    EnumSet<GeneticFamily> covered = EnumSet.noneOf(GeneticFamily.class);
    for (AlterationAssemblageRecipe recipe : targetRecipes) {
      for (GeneticFamily family : recipe.protolithFamilies()) {
        if (!covered.add(family)) {
          throw new IllegalArgumentException(
              "alteration target recipes overlap for protolith family " + family);
        }
      }
    }
    if (replacementPpm > 0 && covered.size() != GeneticFamily.values().length) {
      throw new IllegalArgumentException(
          "alteration target recipes must cover every protolith family");
    }
    requireOrdered(
        minimumTemperatureCelsius, maximumTemperatureCelsius, -100.0, 1_500.0, "temperature");
    requireOrdered(minimumPressureMpa, maximumPressureMpa, 0.0, 5_000.0, "pressure");
    if (!Double.isFinite(porosityMultiplier) || porosityMultiplier < 0.0) {
      throw new IllegalArgumentException("porosity multiplier must be finite and non-negative");
    }
    if (!Double.isFinite(erodibilityDelta) || erodibilityDelta < -1.0 || erodibilityDelta > 1.0) {
      throw new IllegalArgumentException("erodibility delta must lie in [-1, 1]");
    }
  }

  public MaterialAssemblage targetAssemblage(GeneticFamily family) {
    if (family == null) {
      throw new IllegalArgumentException("protolith family is required");
    }
    return targetRecipes.stream()
        .filter(recipe -> recipe.appliesTo(family))
        .map(AlterationAssemblageRecipe::targetAssemblage)
        .findFirst()
        .orElse(null);
  }

  private static void requireOrdered(
      double minimum, double maximum, double floor, double ceiling, String name) {
    if (!Double.isFinite(minimum)
        || !Double.isFinite(maximum)
        || minimum < floor
        || maximum > ceiling
        || minimum > maximum) {
      throw new IllegalArgumentException(name + " interval is invalid");
    }
  }
}
