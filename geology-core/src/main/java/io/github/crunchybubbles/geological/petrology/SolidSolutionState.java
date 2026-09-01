package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/** Resolved ideal phase composition derived from endmember proxy modes in a bulk assemblage. */
public record SolidSolutionState(
    String definitionId,
    SolidSolutionMixingModel mixingModel,
    long phaseModePpm,
    Map<String, Long> endmemberVolumeFractionsPpm,
    Map<String, Long> endmemberMoleFractionsPpm,
    Map<ChemicalElement, Double> idealFormulaAtoms,
    BulkComposition bulkComposition,
    double hardnessMohs,
    double weatheringResistance) {
  public SolidSolutionState {
    if (definitionId == null
        || definitionId.isBlank()
        || mixingModel == null
        || endmemberVolumeFractionsPpm == null
        || endmemberMoleFractionsPpm == null
        || idealFormulaAtoms == null
        || bulkComposition == null) {
      throw new IllegalArgumentException("solid-solution state must be complete");
    }
    if (phaseModePpm <= 0 || phaseModePpm > MineralAssemblage.SCALE) {
      throw new IllegalArgumentException("solid-solution phase mode must lie in (0, 1000000]");
    }
    endmemberVolumeFractionsPpm = exactFractions(endmemberVolumeFractionsPpm, "volume");
    endmemberMoleFractionsPpm = exactFractions(endmemberMoleFractionsPpm, "mole");
    if (!endmemberVolumeFractionsPpm.keySet().equals(endmemberMoleFractionsPpm.keySet())) {
      throw new IllegalArgumentException("solid-solution fraction maps must name the same members");
    }
    EnumMap<ChemicalElement, Double> formula = new EnumMap<>(ChemicalElement.class);
    idealFormulaAtoms.forEach(
        (element, atoms) -> {
          if (element == null || atoms == null || !Double.isFinite(atoms) || atoms <= 0.0) {
            throw new IllegalArgumentException(
                "solid-solution ideal formula coefficients must be positive and finite");
          }
          formula.put(element, atoms);
        });
    if (formula.isEmpty()) {
      throw new IllegalArgumentException("solid-solution ideal formula must not be empty");
    }
    idealFormulaAtoms = Collections.unmodifiableMap(formula);
    requireRange(hardnessMohs, 1.0, 10.0, "solid-solution hardness");
    requireRange(weatheringResistance, 0.0, 1.0, "solid-solution weathering resistance");
  }

  private static Map<String, Long> exactFractions(Map<String, Long> source, String name) {
    TreeMap<String, Long> copied = new TreeMap<>();
    source.forEach(
        (id, fraction) -> {
          if (id == null
              || id.isBlank()
              || fraction == null
              || fraction < 0
              || fraction > MineralAssemblage.SCALE) {
            throw new IllegalArgumentException(
                "solid-solution " + name + " fractions must be named and bounded");
          }
          copied.put(id, fraction);
        });
    if (copied.size() < 2
        || copied.values().stream().mapToLong(Long::longValue).sum() != MineralAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "solid-solution " + name + " fractions must close to " + MineralAssemblage.SCALE);
    }
    return Collections.unmodifiableMap(copied);
  }

  private static void requireRange(double value, double minimum, double maximum, String name) {
    if (!Double.isFinite(value) || value < minimum || value > maximum) {
      throw new IllegalArgumentException(name + " must lie in [" + minimum + ", " + maximum + "]");
    }
  }
}
