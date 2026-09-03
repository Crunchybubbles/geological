package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import java.util.Optional;

/**
 * Deterministic host-buffer and fluid-inventory proxies for a resolved material parcel.
 *
 * <p>Each value is a bounded fixed-point comparison on a one-million-part scale. These values are
 * authored state for routing and explainability, not measured mass, thermodynamic activity, or an
 * equilibrium solve.
 */
public record MaterialBufferState(
    long organicCarbonCapacityPpm,
    long reducedSulfurCapacityPpm,
    long ferrousIronCapacityPpm,
    long adsorptionCapacityPpm,
    long clayCapacityPpm,
    long ironManganeseOxideCapacityPpm,
    long waterInventoryPpm,
    long volatileInventoryPpm) {
  private static final String COAL_ORGANIC_MATTER = "geological:constituent/coal_organic_matter";
  private static final String PYRITE = "geological:mineral/pyrite";
  private static final String CHALCOPYRITE = "geological:mineral/chalcopyrite";
  private static final String SPHALERITE = "geological:mineral/sphalerite";
  private static final String KAOLINITE = "geological:mineral/kaolinite";
  private static final String CLINOCHLORE = "geological:mineral/clinochlore";
  private static final String MUSCOVITE = "geological:mineral/muscovite";
  private static final String GIBBSITE = "geological:mineral/gibbsite";
  private static final String HEMATITE = "geological:mineral/hematite";
  private static final String GOETHITE = "geological:mineral/goethite";
  private static final String MAGNETITE = "geological:mineral/magnetite";
  private static final String ILMENITE = "geological:mineral/ilmenite";
  private static final String FAYALITE = "geological:mineral/fayalite";

  public MaterialBufferState {
    requireBounded(organicCarbonCapacityPpm, "organic carbon capacity");
    requireBounded(reducedSulfurCapacityPpm, "reduced sulfur capacity");
    requireBounded(ferrousIronCapacityPpm, "ferrous iron capacity");
    requireBounded(adsorptionCapacityPpm, "adsorption capacity");
    requireBounded(clayCapacityPpm, "clay capacity");
    requireBounded(ironManganeseOxideCapacityPpm, "iron-manganese oxide capacity");
    requireBounded(waterInventoryPpm, "water inventory");
    requireBounded(volatileInventoryPpm, "volatile inventory");
    if (adsorptionCapacityPpm < clayCapacityPpm
        || adsorptionCapacityPpm < ironManganeseOxideCapacityPpm) {
      throw new IllegalArgumentException(
          "adsorption capacity must include clay and oxide capacity");
    }
    if (volatileInventoryPpm < waterInventoryPpm) {
      throw new IllegalArgumentException("volatile inventory must include water inventory");
    }
  }

  /**
   * Derives a canonical buffer state from constituent modes, bulk elements, and optional fluid
   * conditions. All arithmetic is integer fixed-point and therefore repeatable across callers.
   */
  public static MaterialBufferState proofFor(
      Lithology lithology,
      MaterialAssemblage assemblage,
      BulkComposition composition,
      MaterialProcessClass processClass,
      Optional<ProcessFluidState> fluidState) {
    if (lithology == null
        || assemblage == null
        || composition == null
        || processClass == null
        || fluidState == null) {
      throw new IllegalArgumentException("material buffer inputs must be complete");
    }
    requireFluidState(processClass, fluidState);

    long organicMode = mode(assemblage, COAL_ORGANIC_MATTER);
    long sulfideMode = sumModes(assemblage, PYRITE, CHALCOPYRITE, SPHALERITE);
    long clayMode = sumModes(assemblage, KAOLINITE, CLINOCHLORE, MUSCOVITE, GIBBSITE);
    long oxideMode = sumModes(assemblage, HEMATITE, GOETHITE, MAGNETITE, ILMENITE);
    long ferrousHostMode =
        sumModes(assemblage, MAGNETITE, FAYALITE, PYRITE, CHALCOPYRITE, SPHALERITE);
    long fluidBonus =
        fluidState.map(state -> (long) (state.integratedFluxClass() + 1) * 50_000L).orElse(0L);

    long carbon = element(composition, ChemicalElement.C);
    long sulfur = element(composition, ChemicalElement.S);
    long iron = element(composition, ChemicalElement.FE);
    long hydrogen = element(composition, ChemicalElement.H);
    long volatileBase =
        hydrogen
            + carbon
            + element(composition, ChemicalElement.N)
            + sulfur
            + element(composition, ChemicalElement.F)
            + element(composition, ChemicalElement.CL);
    long water = clamp(scaled(hydrogen, 900_000L) + fluidBonus);
    long volatileInventory = clamp(Math.max(water, volatileBase + fluidBonus / 2L));

    return new MaterialBufferState(
        Math.min(carbon, scaled(organicMode, 850_000L)),
        Math.min(sulfur, scaled(sulfideMode, 800_000L)),
        Math.min(iron, scaled(ferrousHostMode, 700_000L)),
        clamp(scaled(clayMode, 800_000L) + scaled(oxideMode, 700_000L)),
        scaled(clayMode, 800_000L),
        scaled(oxideMode, 700_000L),
        water,
        volatileInventory);
  }

  private static void requireBounded(long value, String name) {
    if (value < 0L || value > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(name + " must lie in [0, 1000000]");
    }
  }

  private static void requireFluidState(
      MaterialProcessClass processClass, Optional<ProcessFluidState> fluidState) {
    boolean requiresFluid =
        processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
            || processClass == MaterialProcessClass.WEATHERING;
    if (requiresFluid != fluidState.isPresent()) {
      throw new IllegalArgumentException("material process and fluid state do not agree");
    }
  }

  private static long element(BulkComposition composition, ChemicalElement element) {
    return composition.elementMassPpm().getOrDefault(element, 0L);
  }

  private static long mode(MaterialAssemblage assemblage, String constituentId) {
    return assemblage.modesPpm().getOrDefault(constituentId, 0L);
  }

  private static long sumModes(MaterialAssemblage assemblage, String... constituentIds) {
    long sum = 0L;
    for (String constituentId : constituentIds) {
      sum = Math.addExact(sum, mode(assemblage, constituentId));
    }
    return sum;
  }

  private static long scaled(long amountPpm, long factorPpm) {
    return Math.multiplyExact(amountPpm, factorPpm) / MaterialAssemblage.SCALE;
  }

  private static long clamp(long value) {
    return Math.max(0L, Math.min(MaterialAssemblage.SCALE, value));
  }
}
