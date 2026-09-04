package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import java.util.List;
import java.util.Objects;

/**
 * Optional parent-isotope and daughter-potential evidence for a resolved material parcel.
 *
 * <p>The result is a coarse accumulated-potential proxy. It is not an isotope ratio, an age
 * estimate, a radiometric assay, or a simulation of individual decay atoms.
 */
public final class IsotopicProvenanceEvidence {
  private static final long SCALE = MaterialAssemblage.SCALE;
  private static final double LN_TWO = StrictMath.log(2.0);
  private static final List<ParentNuclide> NUCLIDES = List.of(ParentNuclide.values());

  private IsotopicProvenanceEvidence() {}

  /**
   * Derives optional evidence from a normalized composition, source reservoir, and formation age.
   * Parent systems absent from the sparse composition are omitted.
   */
  public static List<Evidence> proofFor(
      BulkComposition composition, StableId sourceReservoirId, AgeKey formationAge) {
    if (composition == null || sourceReservoirId == null || formationAge == null) {
      throw new IllegalArgumentException("isotopic provenance inputs are required");
    }
    return NUCLIDES.stream()
        .map(
            nuclide -> {
              long parentInventory =
                  composition.elementMassPpm().getOrDefault(nuclide.parentElement(), 0L);
              return parentInventory == 0L
                  ? null
                  : evidence(nuclide, parentInventory, sourceReservoirId, formationAge);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private static Evidence evidence(
      ParentNuclide nuclide,
      long parentInventory,
      StableId sourceReservoirId,
      AgeKey formationAge) {
    long initialIsotopeInventory =
        Math.multiplyExact(parentInventory, nuclide.naturalAbundancePpm()) / SCALE;
    long decayFractionPpm = decayFractionPpm(formationAge.ageMa(), nuclide.halfLifeMa());
    long daughterPotential = Math.multiplyExact(initialIsotopeInventory, decayFractionPpm) / SCALE;
    long retainedIsotope = initialIsotopeInventory - daughterPotential;
    return new Evidence(
        nuclide,
        sourceReservoirId,
        formationAge,
        parentInventory,
        initialIsotopeInventory,
        daughterPotential,
        retainedIsotope,
        decayFractionPpm,
        nuclide.confidencePpm());
  }

  private static long decayFractionPpm(double ageMa, double halfLifeMa) {
    if (ageMa <= 0.0) {
      return 0L;
    }
    double fraction = -StrictMath.expm1(-LN_TWO * ageMa / halfLifeMa);
    return Math.min(SCALE, Math.max(0L, Math.round(fraction * SCALE)));
  }

  public record Evidence(
      ParentNuclide parentNuclide,
      StableId sourceReservoirId,
      AgeKey formationAge,
      long parentInventoryPpm,
      long initialIsotopeInventoryPpm,
      long daughterPotentialPpm,
      long retainedIsotopePpm,
      long decayFractionPpm,
      long confidencePpm) {
    public Evidence {
      if (parentNuclide == null
          || sourceReservoirId == null
          || formationAge == null
          || parentInventoryPpm <= 0L
          || parentInventoryPpm > SCALE
          || initialIsotopeInventoryPpm < 0L
          || initialIsotopeInventoryPpm > parentInventoryPpm
          || daughterPotentialPpm < 0L
          || retainedIsotopePpm < 0L
          || daughterPotentialPpm + retainedIsotopePpm != initialIsotopeInventoryPpm
          || decayFractionPpm < 0L
          || decayFractionPpm > SCALE
          || confidencePpm < 0L
          || confidencePpm > SCALE) {
        throw new IllegalArgumentException("isotopic provenance evidence is invalid");
      }
    }

    /** Returns the daughter product label used by the coarse evidence channel. */
    public String daughterProduct() {
      return parentNuclide.daughterProduct();
    }
  }

  public enum ParentNuclide {
    K40(ChemicalElement.K, "K-40", "Ar-40", 117L, 1_248_000.0, 250_000L),
    RB87(ChemicalElement.RB, "Rb-87", "Sr-87", 278_300L, 48_800_000.0, 250_000L),
    TH232(ChemicalElement.TH, "Th-232", "He-4", SCALE, 14_050.0, 200_000L),
    U238(ChemicalElement.U, "U-238", "He-4", 992_745L, 4_468.0, 300_000L);

    private final ChemicalElement parentElement;
    private final String isotope;
    private final String daughterProduct;
    private final long naturalAbundancePpm;
    private final double halfLifeMa;
    private final long confidencePpm;

    ParentNuclide(
        ChemicalElement parentElement,
        String isotope,
        String daughterProduct,
        long naturalAbundancePpm,
        double halfLifeMa,
        long confidencePpm) {
      this.parentElement = parentElement;
      this.isotope = isotope;
      this.daughterProduct = daughterProduct;
      this.naturalAbundancePpm = naturalAbundancePpm;
      this.halfLifeMa = halfLifeMa;
      this.confidencePpm = confidencePpm;
    }

    public ChemicalElement parentElement() {
      return parentElement;
    }

    public String isotope() {
      return isotope;
    }

    public String daughterProduct() {
      return daughterProduct;
    }

    public long naturalAbundancePpm() {
      return naturalAbundancePpm;
    }

    public double halfLifeMa() {
      return halfLifeMa;
    }

    public long confidencePpm() {
      return confidencePpm;
    }
  }
}
