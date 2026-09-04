package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Normalized residual-incompatible-element inventory for one magma differentiation pulse.
 *
 * <p>The split is a deterministic partition proxy: each selected element closes exactly between
 * crystallized, residual-melt, and residual-fluid portions. It is not an absolute magma mass,
 * measured partition coefficient, or MELTS/equilibrium calculation.
 */
public record MagmaResidualInventoryState(
    long cumulativeCrystalFractionPpm,
    long residualMeltFractionPpm,
    long residualFluidFractionPpm,
    Map<ChemicalElement, Long> sourceInventoryPpm,
    Map<ChemicalElement, Long> crystallizedInventoryPpm,
    Map<ChemicalElement, Long> residualMeltInventoryPpm,
    Map<ChemicalElement, Long> residualFluidInventoryPpm) {
  private static final Set<ChemicalElement> TRACKED_ELEMENTS =
      Collections.unmodifiableSet(
          EnumSet.of(
              ChemicalElement.P,
              ChemicalElement.S,
              ChemicalElement.F,
              ChemicalElement.CL,
              ChemicalElement.K,
              ChemicalElement.CU,
              ChemicalElement.ZN,
              ChemicalElement.AU,
              ChemicalElement.LI,
              ChemicalElement.BE,
              ChemicalElement.B,
              ChemicalElement.RB,
              ChemicalElement.CS,
              ChemicalElement.NB,
              ChemicalElement.MO,
              ChemicalElement.AG,
              ChemicalElement.SN,
              ChemicalElement.TA,
              ChemicalElement.W,
              ChemicalElement.RE,
              ChemicalElement.PB,
              ChemicalElement.TH,
              ChemicalElement.U));

  public MagmaResidualInventoryState {
    requireFraction(cumulativeCrystalFractionPpm, "cumulative crystal fraction");
    requireFraction(residualMeltFractionPpm, "residual melt fraction");
    requireFraction(residualFluidFractionPpm, "residual fluid fraction");
    if (cumulativeCrystalFractionPpm + residualMeltFractionPpm != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("magma fractions must close to 1000000");
    }
    if (residualFluidFractionPpm > residualMeltFractionPpm) {
      throw new IllegalArgumentException("residual fluid must lie within residual melt");
    }
    sourceInventoryPpm = immutable(sourceInventoryPpm, "source inventory");
    crystallizedInventoryPpm = immutable(crystallizedInventoryPpm, "crystallized inventory");
    residualMeltInventoryPpm = immutable(residualMeltInventoryPpm, "residual melt inventory");
    residualFluidInventoryPpm = immutable(residualFluidInventoryPpm, "residual fluid inventory");
    for (ChemicalElement element : sourceInventoryPpm.keySet()) {
      long source = sourceInventoryPpm.get(element);
      long crystallized = crystallizedInventoryPpm.getOrDefault(element, 0L);
      long melt = residualMeltInventoryPpm.getOrDefault(element, 0L);
      long fluid = residualFluidInventoryPpm.getOrDefault(element, 0L);
      if (Math.addExact(Math.addExact(crystallized, melt), fluid) != source) {
        throw new IllegalArgumentException(
            "magma residual inventory does not close for " + element);
      }
    }
    Set<ChemicalElement> allElements = EnumSet.noneOf(ChemicalElement.class);
    allElements.addAll(crystallizedInventoryPpm.keySet());
    allElements.addAll(residualMeltInventoryPpm.keySet());
    allElements.addAll(residualFluidInventoryPpm.keySet());
    if (!allElements.equals(sourceInventoryPpm.keySet())) {
      throw new IllegalArgumentException("magma inventory maps must share the source elements");
    }
  }

  /** Derives the tracked incompatible-element split from bulk composition and pulse state. */
  public static MagmaResidualInventoryState proofFor(
      BulkComposition sourceComposition, MagmaDifferentiationState differentiationState) {
    if (sourceComposition == null || differentiationState == null) {
      throw new IllegalArgumentException("magma residual inventory inputs are required");
    }
    EnumMap<ChemicalElement, Long> source = new EnumMap<>(ChemicalElement.class);
    for (ChemicalElement element : TRACKED_ELEMENTS) {
      long amount = sourceComposition.elementMassPpm().getOrDefault(element, 0L);
      if (amount > 0L) {
        source.put(element, amount);
      }
    }
    EnumMap<ChemicalElement, Long> crystallized = new EnumMap<>(ChemicalElement.class);
    EnumMap<ChemicalElement, Long> melt = new EnumMap<>(ChemicalElement.class);
    EnumMap<ChemicalElement, Long> fluid = new EnumMap<>(ChemicalElement.class);
    for (Map.Entry<ChemicalElement, Long> entry : source.entrySet()) {
      long captureFraction =
          scaled(
              differentiationState.cumulativeCrystalFractionPpm(),
              captureCoefficient(entry.getKey(), differentiationState.sulfurSaturationHistory()));
      long crystallizedAmount = scaled(entry.getValue(), captureFraction);
      long residualAmount = entry.getValue() - crystallizedAmount;
      long fluidFraction =
          differentiationState.residualMeltFractionPpm() == 0L
              ? 0L
              : Math.multiplyExact(
                      differentiationState.residualFluidFractionPpm(), MaterialAssemblage.SCALE)
                  / differentiationState.residualMeltFractionPpm();
      long fluidAmount = scaled(residualAmount, fluidFraction);
      long meltAmount = residualAmount - fluidAmount;
      putPositive(crystallized, entry.getKey(), crystallizedAmount);
      putPositive(melt, entry.getKey(), meltAmount);
      putPositive(fluid, entry.getKey(), fluidAmount);
    }
    return new MagmaResidualInventoryState(
        differentiationState.cumulativeCrystalFractionPpm(),
        differentiationState.residualMeltFractionPpm(),
        differentiationState.residualFluidFractionPpm(),
        source,
        crystallized,
        melt,
        fluid);
  }

  private static long captureCoefficient(
      ChemicalElement element, MagmaDifferentiationState.SulfurSaturationHistory sulfurHistory) {
    return ElementPartitionResponseCatalog.require(element, sulfurHistory).crystalCapturePpm();
  }

  private static void putPositive(
      Map<ChemicalElement, Long> target, ChemicalElement element, long value) {
    if (value > 0L) {
      target.put(element, value);
    }
  }

  private static long scaled(long amount, long factor) {
    return Math.multiplyExact(amount, factor) / MaterialAssemblage.SCALE;
  }

  private static void requireFraction(long value, String name) {
    if (value < 0L || value > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(name + " must lie in [0, 1000000]");
    }
  }

  private static Map<ChemicalElement, Long> immutable(
      Map<ChemicalElement, Long> source, String name) {
    if (source == null) {
      throw new IllegalArgumentException(name + " is required");
    }
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null
              || amount == null
              || !TRACKED_ELEMENTS.contains(element)
              || amount <= 0L
              || amount > MaterialAssemblage.SCALE) {
            throw new IllegalArgumentException(name + " contains an invalid entry");
          }
          copied.put(element, amount);
        });
    return Collections.unmodifiableMap(copied);
  }
}
