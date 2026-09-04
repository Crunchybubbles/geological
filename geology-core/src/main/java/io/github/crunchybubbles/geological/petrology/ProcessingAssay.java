package io.github.crunchybubbles.geological.petrology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Deterministic processing-facing assay and constituent-host allocation for a resolved material
 * parcel.
 *
 * <p>The assay preserves the normalized bulk element inventory and allocates each element back to
 * the authored modal constituents using the same density- and formula-derived composition as {@link
 * MaterialCatalogSnapshot#composition(MaterialAssemblage)}. The liberation values are an ideal
 * constituent-separation upper bound: without a grain-size, intergrowth, comminution, or recovery
 * model they are not a measured liberation curve, recovery, grade, reserve, or economic
 * classification.
 */
public record ProcessingAssay(
    String version,
    String basis,
    LiberationModel liberationModel,
    ReviewStatus reviewStatus,
    long confidencePpm,
    double bulkDensity,
    List<ElementAssay> elements) {
  public static final String VERSION = "phase9-alpha.6-host-assay-v1";
  public static final String BASIS = "resolved_bulk_mass_ppm_with_authored_modal_hosts";
  public static final long CONFIDENCE_PPM = 250_000L;

  public ProcessingAssay {
    if (version == null
        || version.isBlank()
        || basis == null
        || basis.isBlank()
        || liberationModel == null
        || reviewStatus == null
        || confidencePpm < 0L
        || confidencePpm > MaterialAssemblage.SCALE
        || !Double.isFinite(bulkDensity)
        || bulkDensity <= 0.0
        || elements == null) {
      throw new IllegalArgumentException("processing assay is incomplete or out of bounds");
    }
    elements =
        List.copyOf(elements).stream().sorted(Comparator.comparing(ElementAssay::element)).toList();
    if (elements.stream().map(ElementAssay::element).distinct().count() != elements.size()) {
      throw new IllegalArgumentException("processing assay elements must be unique");
    }
  }

  /** Derives an assay for the resolved state of a Phase 2 sample. */
  public static ProcessingAssay proofFor(MaterialCatalogSnapshot catalog, PetrologicSample sample) {
    if (sample == null) {
      throw new IllegalArgumentException("petrologic sample is required");
    }
    return proofFor(catalog, sample.resolvedAssemblage(), sample.resolvedComposition());
  }

  /** Derives an assay for a coordinate-independent vertical-run material state. */
  public static ProcessingAssay proofFor(MaterialCatalogSnapshot catalog, PetrologicState state) {
    if (state == null) {
      throw new IllegalArgumentException("petrologic state is required");
    }
    return proofFor(catalog, state.resolvedAssemblage(), state.resolvedComposition());
  }

  /**
   * Derives a normalized assay and host allocation from one catalog assemblage and its matching
   * bulk composition.
   */
  public static ProcessingAssay proofFor(
      MaterialCatalogSnapshot catalog, MaterialAssemblage assemblage, BulkComposition composition) {
    if (catalog == null || assemblage == null || composition == null) {
      throw new IllegalArgumentException("processing assay inputs must be complete");
    }
    BulkComposition expected = catalog.composition(assemblage);
    if (!expected.elementMassPpm().equals(composition.elementMassPpm())
        || StrictMath.abs(expected.density() - composition.density()) > 1.0e-12) {
      throw new IllegalArgumentException(
          "processing assay composition must match the supplied catalog assemblage");
    }

    List<HostTerm> hosts = new ArrayList<>();
    double bulkDensity = 0.0;
    for (Map.Entry<String, Long> mode : assemblage.modesPpm().entrySet()) {
      MaterialConstituentDefinition definition = catalog.requireConstituent(mode.getKey());
      double mass =
          mode.getValue()
              / (double) MaterialAssemblage.SCALE
              * definition.densityGramsPerCubicCentimeter();
      if (!Double.isFinite(mass) || mass <= 0.0) {
        throw new IllegalStateException("constituent produced an invalid assay mass");
      }
      bulkDensity += mass;
      hosts.add(
          new HostTerm(
              definition.id(),
              definition.kind(),
              mode.getValue(),
              mass,
              definition.elementMassFractions()));
    }
    if (!Double.isFinite(bulkDensity) || bulkDensity <= 0.0) {
      throw new IllegalStateException("assemblage produced an invalid assay density");
    }

    List<ElementAssay> assays = new ArrayList<>();
    for (Map.Entry<ChemicalElement, Long> element : composition.elementMassPpm().entrySet()) {
      List<HostRemainder> remainders = new ArrayList<>();
      for (HostTerm host : hosts) {
        double fraction = host.elementFractions().getOrDefault(element.getKey(), 0.0);
        if (fraction > 0.0) {
          double exact = host.massGramsPerCubicCentimeter() * fraction / bulkDensity;
          double exactPpm = exact * MaterialAssemblage.SCALE;
          if (!Double.isFinite(exactPpm) || exactPpm <= 0.0) {
            throw new IllegalStateException("constituent produced an invalid host allocation");
          }
          long whole = (long) StrictMath.floor(exactPpm);
          remainders.add(new HostRemainder(host, whole, exactPpm - whole));
        }
      }
      if (remainders.isEmpty()) {
        throw new IllegalStateException("bulk element has no authored constituent host");
      }

      long allocated = remainders.stream().mapToLong(HostRemainder::wholePpm).sum();
      long remainder = element.getValue() - allocated;
      if (remainder < 0L || remainder > remainders.size()) {
        throw new IllegalStateException(
            "host allocation cannot close for "
                + element.getKey()
                + ": total="
                + element.getValue()
                + ", floors="
                + allocated);
      }
      remainders.sort(
          Comparator.comparingDouble(HostRemainder::fractionalRemainder)
              .reversed()
              .thenComparing(item -> item.host().id()));
      for (int index = 0; index < remainder; index++) {
        HostRemainder current = remainders.get(index);
        remainders.set(index, current.withWholePpm(Math.addExact(current.wholePpm(), 1L)));
      }

      List<HostAllocation> allocations =
          remainders.stream()
              .sorted(Comparator.comparing(item -> item.host().id()))
              .map(
                  item ->
                      new HostAllocation(
                          item.host().id(),
                          item.host().kind(),
                          item.host().modePpm(),
                          item.wholePpm(),
                          item.wholePpm()))
              .toList();
      assays.add(new ElementAssay(element.getKey(), element.getValue(), allocations));
    }
    return new ProcessingAssay(
        VERSION,
        BASIS,
        LiberationModel.CONSTITUENT_IDEAL_UPPER_BOUND,
        ReviewStatus.AUTHORED_DERIVATION,
        CONFIDENCE_PPM,
        bulkDensity,
        assays);
  }

  /** Returns the sparse assay entry for an element, if the element is present. */
  public Optional<ElementAssay> element(ChemicalElement element) {
    if (element == null) {
      throw new IllegalArgumentException("assay element is required");
    }
    return elements.stream().filter(item -> item.element() == element).findFirst();
  }

  /** Returns the resolved element mass, or zero when it is absent from the sparse assay. */
  public long totalPpm(ChemicalElement element) {
    return element(element).map(ElementAssay::totalPpm).orElse(0L);
  }

  /** Returns a stable sparse map useful to review and processing callers. */
  public Map<ChemicalElement, Long> elementMassPpm() {
    EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
    elements.forEach(item -> result.put(item.element(), item.totalPpm()));
    return Collections.unmodifiableMap(result);
  }

  public record ElementAssay(
      ChemicalElement element, long totalPpm, List<HostAllocation> hostAllocations) {
    public ElementAssay {
      if (element == null
          || totalPpm <= 0L
          || totalPpm > MaterialAssemblage.SCALE
          || hostAllocations == null) {
        throw new IllegalArgumentException("element assay is incomplete or out of bounds");
      }
      hostAllocations =
          List.copyOf(hostAllocations).stream()
              .sorted(Comparator.comparing(HostAllocation::constituentId))
              .toList();
      if (hostAllocations.isEmpty()
          || hostAllocations.stream().map(HostAllocation::constituentId).distinct().count()
              != hostAllocations.size()
          || hostAllocations.stream().mapToLong(HostAllocation::hostedElementPpm).sum()
              != totalPpm) {
        throw new IllegalArgumentException("element host allocations must close exactly");
      }
    }
  }

  public record HostAllocation(
      String constituentId,
      MaterialConstituentKind constituentKind,
      long hostModePpm,
      long hostedElementPpm,
      long idealLiberatedElementPpm) {
    public HostAllocation {
      if (constituentId == null
          || constituentId.isBlank()
          || constituentKind == null
          || hostModePpm <= 0L
          || hostModePpm > MaterialAssemblage.SCALE
          || hostedElementPpm < 0L
          || hostedElementPpm > MaterialAssemblage.SCALE
          || idealLiberatedElementPpm < 0L
          || idealLiberatedElementPpm > hostedElementPpm) {
        throw new IllegalArgumentException("host allocation is incomplete or out of bounds");
      }
    }
  }

  private record HostTerm(
      String id,
      MaterialConstituentKind kind,
      long modePpm,
      double massGramsPerCubicCentimeter,
      Map<ChemicalElement, Double> elementFractions) {
    private HostTerm {
      elementFractions = Collections.unmodifiableMap(new TreeMap<>(elementFractions));
    }
  }

  private record HostRemainder(HostTerm host, long wholePpm, double fractionalRemainder) {
    private HostRemainder withWholePpm(long value) {
      return new HostRemainder(host, value, fractionalRemainder);
    }
  }

  public enum LiberationModel {
    /** Perfect constituent separation upper bound; grain texture and recovery are not modeled. */
    CONSTITUENT_IDEAL_UPPER_BOUND
  }

  public enum ReviewStatus {
    AUTHORED_DERIVATION,
    EXTERNALLY_REVIEWED
  }
}
