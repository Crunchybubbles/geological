package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Condition-qualified affinity, host, and mobility defaults for the supported element vocabulary.
 *
 * <p>The profiles are routing metadata for reservoir and process rules, not immutable Goldschmidt
 * labels or equilibrium predictions. Individual systems may narrow a profile when temperature,
 * pressure, redox, ligands, or phase availability provide stronger evidence.
 */
public final class ElementBehaviorCatalog {
  private static final Map<ChemicalElement, ElementBehavior> PROFILES = profiles();

  private ElementBehaviorCatalog() {}

  /** Returns every supported profile in stable chemical-element order. */
  public static List<ElementBehavior> all() {
    return List.copyOf(PROFILES.values());
  }

  /** Returns the condition-qualified profile for one element. */
  public static ElementBehavior require(ChemicalElement element) {
    Objects.requireNonNull(element, "chemical element");
    return PROFILES.get(element);
  }

  private static Map<ChemicalElement, ElementBehavior> profiles() {
    EnumMap<ChemicalElement, ElementBehavior> profiles = new EnumMap<>(ChemicalElement.class);
    profiles.put(
        ChemicalElement.H,
        profile(
            ChemicalElement.H,
            MobilityClass.HIGH,
            true,
            false,
            Set.of(HostClass.VOLATILE, HostClass.ORGANIC),
            affinity(AffinityClass.ATMOPHILE, "volatile or organic-bearing fluid")));
    profiles.put(
        ChemicalElement.C,
        profile(
            ChemicalElement.C,
            MobilityClass.CONDITIONAL,
            true,
            false,
            Set.of(HostClass.CARBONATE, HostClass.ORGANIC, HostClass.VOLATILE),
            affinity(AffinityClass.LITHOPHILE, "carbonate or silicate phase"),
            affinity(AffinityClass.ATMOPHILE, "organic or dissolved-carbon system")));
    profiles.put(
        ChemicalElement.N,
        profile(
            ChemicalElement.N,
            MobilityClass.HIGH,
            true,
            false,
            Set.of(HostClass.ORGANIC, HostClass.VOLATILE),
            affinity(AffinityClass.ATMOPHILE, "fluid, organic, or volatile reservoir")));
    profiles.put(
        ChemicalElement.O,
        profile(
            ChemicalElement.O,
            MobilityClass.CONDITIONAL,
            true,
            false,
            Set.of(HostClass.SILICATE, HostClass.OXIDE, HostClass.CARBONATE, HostClass.VOLATILE),
            affinity(AffinityClass.LITHOPHILE, "silicate/oxide/carbonate phase")));
    profiles.put(
        ChemicalElement.F,
        profile(
            ChemicalElement.F,
            MobilityClass.CONDITIONAL,
            true,
            false,
            Set.of(HostClass.SILICATE, HostClass.VOLATILE),
            affinity(AffinityClass.LITHOPHILE, "volatile-rich silicate or fluoride phase"),
            affinity(AffinityClass.ATMOPHILE, "volatile-bearing fluid")));
    profiles.put(
        ChemicalElement.NA,
        profile(
            ChemicalElement.NA,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.CARBONATE),
            affinity(AffinityClass.LITHOPHILE, "feldspar, clay, or carbonate host")));
    profiles.put(
        ChemicalElement.MG,
        profile(
            ChemicalElement.MG,
            MobilityClass.LOW,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.CARBONATE),
            affinity(AffinityClass.LITHOPHILE, "mafic silicate or carbonate host")));
    profiles.put(
        ChemicalElement.AL,
        profile(
            ChemicalElement.AL,
            MobilityClass.LOW,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.OXIDE),
            affinity(AffinityClass.LITHOPHILE, "aluminosilicate or residual oxide host")));
    profiles.put(
        ChemicalElement.SI,
        profile(
            ChemicalElement.SI,
            MobilityClass.LOW,
            false,
            false,
            Set.of(HostClass.SILICATE),
            affinity(AffinityClass.LITHOPHILE, "silicate framework host")));
    profiles.put(
        ChemicalElement.P,
        profile(
            ChemicalElement.P,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.PHOSPHATE, HostClass.SILICATE),
            affinity(AffinityClass.LITHOPHILE, "apatite, phosphate, or melt reservoir")));
    profiles.put(
        ChemicalElement.S,
        profile(
            ChemicalElement.S,
            MobilityClass.HIGH,
            true,
            false,
            Set.of(HostClass.SULFIDE, HostClass.VOLATILE),
            affinity(AffinityClass.CHALCOPHILE, "reduced-sulfur or sulfide phase"),
            affinity(AffinityClass.ATMOPHILE, "oxidized/reduced volatile fluid")));
    profiles.put(
        ChemicalElement.CL,
        profile(
            ChemicalElement.CL,
            MobilityClass.HIGH,
            true,
            false,
            Set.of(HostClass.VOLATILE),
            affinity(AffinityClass.ATMOPHILE, "chloride-bearing fluid or volatile phase")));
    profiles.put(
        ChemicalElement.K,
        profile(
            ChemicalElement.K,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.VOLATILE),
            affinity(AffinityClass.LITHOPHILE, "feldspar/mica or evolved melt")));
    profiles.put(
        ChemicalElement.CA,
        profile(
            ChemicalElement.CA,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.CARBONATE),
            affinity(AffinityClass.LITHOPHILE, "calcium silicate or carbonate host")));
    profiles.put(
        ChemicalElement.TI,
        profile(
            ChemicalElement.TI,
            MobilityClass.LOW,
            false,
            false,
            Set.of(HostClass.OXIDE, HostClass.SILICATE),
            affinity(AffinityClass.LITHOPHILE, "oxide or mafic silicate host")));
    profiles.put(
        ChemicalElement.CR,
        profile(
            ChemicalElement.CR,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.OXIDE, HostClass.SULFIDE, HostClass.SILICATE),
            affinity(AffinityClass.SIDEROPHILE, "mafic-ultramafic oxide or metal phase"),
            affinity(AffinityClass.CHALCOPHILE, "sulfide-saturated system")));
    profiles.put(
        ChemicalElement.FE,
        profile(
            ChemicalElement.FE,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SILICATE, HostClass.OXIDE, HostClass.SULFIDE, HostClass.CARBONATE),
            affinity(AffinityClass.LITHOPHILE, "silicate or oxidized oxide host"),
            affinity(AffinityClass.CHALCOPHILE, "sulfide or reduced host"),
            affinity(AffinityClass.SIDEROPHILE, "metal-rich phase")));
    profiles.put(
        ChemicalElement.CU,
        profile(
            ChemicalElement.CU,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SULFIDE, HostClass.CARBONATE),
            affinity(AffinityClass.CHALCOPHILE, "sulfide-bearing magmatic or hydrothermal fluid")));
    profiles.put(
        ChemicalElement.ZN,
        profile(
            ChemicalElement.ZN,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SULFIDE, HostClass.CARBONATE),
            affinity(AffinityClass.CHALCOPHILE, "sulfide, carbonate, or chloride-bearing fluid")));
    profiles.put(
        ChemicalElement.AU,
        profile(
            ChemicalElement.AU,
            MobilityClass.CONDITIONAL,
            false,
            false,
            Set.of(HostClass.SULFIDE, HostClass.SILICATE),
            affinity(AffinityClass.CHALCOPHILE, "reduced-sulfur hydrothermal or sulfide phase"),
            affinity(AffinityClass.SIDEROPHILE, "metal/alloy or mafic-ultramafic phase")));
    if (profiles.size() != ChemicalElement.values().length) {
      throw new IllegalStateException("element behavior catalog does not cover the vocabulary");
    }
    return Collections.unmodifiableMap(profiles);
  }

  private static ElementBehavior profile(
      ChemicalElement element,
      MobilityClass mobility,
      boolean volatileElement,
      boolean radiogenic,
      Set<HostClass> hosts,
      ConditionedAffinity... affinities) {
    return new ElementBehavior(
        element, List.of(affinities), mobility, hosts, volatileElement, radiogenic);
  }

  private static ConditionedAffinity affinity(AffinityClass affinity, String condition) {
    return new ConditionedAffinity(affinity, condition);
  }

  public record ElementBehavior(
      ChemicalElement element,
      List<ConditionedAffinity> affinities,
      MobilityClass mobility,
      Set<HostClass> hostClasses,
      boolean volatileElement,
      boolean radiogenic) {
    public ElementBehavior {
      if (element == null
          || affinities == null
          || affinities.isEmpty()
          || mobility == null
          || hostClasses == null
          || hostClasses.isEmpty()) {
        throw new IllegalArgumentException("element behavior profile is incomplete");
      }
      affinities =
          List.copyOf(affinities).stream()
              .sorted(
                  java.util.Comparator.comparing(ConditionedAffinity::affinity)
                      .thenComparing(ConditionedAffinity::condition))
              .toList();
      if (affinities.stream().map(ConditionedAffinity::affinity).distinct().count()
          != affinities.size()) {
        throw new IllegalArgumentException("element affinity classes must be unique");
      }
      EnumSet<HostClass> canonicalHosts = EnumSet.copyOf(hostClasses);
      hostClasses = Collections.unmodifiableSet(canonicalHosts);
    }
  }

  public record ConditionedAffinity(AffinityClass affinity, String condition) {
    public ConditionedAffinity {
      if (affinity == null || condition == null || condition.isBlank()) {
        throw new IllegalArgumentException("condition-qualified affinity is incomplete");
      }
    }
  }

  public enum AffinityClass {
    ATMOPHILE,
    CHALCOPHILE,
    LITHOPHILE,
    SIDEROPHILE
  }

  public enum MobilityClass {
    LOW,
    CONDITIONAL,
    HIGH
  }

  public enum HostClass {
    CARBONATE,
    ORGANIC,
    OXIDE,
    PHOSPHATE,
    SILICATE,
    SULFIDE,
    VOLATILE
  }
}
