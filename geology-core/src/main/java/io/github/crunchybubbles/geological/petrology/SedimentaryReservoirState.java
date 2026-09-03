package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Typed chemistry and inventory proxies for sedimentary source reservoirs.
 *
 * <p>Each component is a normalized source share with an authored one-million-part composition. The
 * state distinguishes terrigenous, volcanic, biogenic, organic, chemical, and brine inputs without
 * claiming a measured basin mass, seawater analysis, or global reservoir balance.
 */
public record SedimentaryReservoirState(List<Component> components) {
  public SedimentaryReservoirState {
    if (components == null) {
      throw new IllegalArgumentException("sedimentary reservoir components are required");
    }
    components =
        List.copyOf(components).stream()
            .sorted(java.util.Comparator.comparing(Component::kind))
            .toList();
    if (components.isEmpty()
        || components.stream().anyMatch(component -> component == null)
        || components.stream().map(Component::kind).distinct().count() != components.size()
        || components.stream().mapToLong(Component::fractionPpm).sum()
            != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sedimentary reservoir components must be unique and close to 1000000");
    }
  }

  /** Builds typed reservoir chemistry from the existing normalized facies budget. */
  public static SedimentaryReservoirState proofFor(
      SedimentaryInputBudget budget,
      SedimentaryBasinState basinState,
      List<SedimentaryReservoirContribution> contributions) {
    if (budget == null || basinState == null || contributions == null) {
      throw new IllegalArgumentException("sedimentary reservoir state inputs are required");
    }
    List<SedimentaryReservoirContribution> canonical =
        List.copyOf(contributions).stream()
            .sorted(java.util.Comparator.comparing(SedimentaryReservoirContribution::kind))
            .toList();
    if (canonical.isEmpty()
        || canonical.stream().anyMatch(contribution -> contribution == null)
        || canonical.stream().map(SedimentaryReservoirContribution::kind).distinct().count()
            != canonical.size()) {
      throw new IllegalArgumentException("sedimentary reservoir contributions must be unique");
    }
    return new SedimentaryReservoirState(
        canonical.stream()
            .map(contribution -> componentFor(budget, basinState, contribution))
            .toList());
  }

  /** Returns the exact normalized aggregate element composition across all reservoir shares. */
  public Map<ChemicalElement, Long> aggregateCompositionPpm() {
    EnumMap<ChemicalElement, Long> numerators = new EnumMap<>(ChemicalElement.class);
    for (Component component : components) {
      component
          .compositionPpm()
          .forEach(
              (element, amount) ->
                  numerators.merge(
                      element,
                      Math.multiplyExact(amount, component.fractionPpm()),
                      Math::addExact));
    }
    EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
    EnumMap<ChemicalElement, Long> remainders = new EnumMap<>(ChemicalElement.class);
    long allocated = 0L;
    for (Map.Entry<ChemicalElement, Long> entry : numerators.entrySet()) {
      long whole = entry.getValue() / MaterialAssemblage.SCALE;
      result.put(entry.getKey(), whole);
      remainders.put(entry.getKey(), entry.getValue() % MaterialAssemblage.SCALE);
      allocated = Math.addExact(allocated, whole);
    }
    long missing = MaterialAssemblage.SCALE - allocated;
    remainders.entrySet().stream()
        .sorted(
            Map.Entry.<ChemicalElement, Long>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(missing)
        .forEach(entry -> result.merge(entry.getKey(), 1L, Long::sum));
    return Collections.unmodifiableMap(result);
  }

  public long waterInventoryPpm() {
    return weightedTotal(Component::waterInventoryPpm);
  }

  public long volatileInventoryPpm() {
    return weightedTotal(Component::volatileInventoryPpm);
  }

  public long organicCarbonCapacityPpm() {
    return weightedTotal(Component::organicCarbonCapacityPpm);
  }

  public long reducedSulfurCapacityPpm() {
    return weightedTotal(Component::reducedSulfurCapacityPpm);
  }

  private long weightedTotal(java.util.function.ToLongFunction<Component> value) {
    long numerator =
        components.stream()
            .mapToLong(
                component ->
                    Math.multiplyExact(value.applyAsLong(component), component.fractionPpm()))
            .sum();
    return numerator / MaterialAssemblage.SCALE;
  }

  private static Component componentFor(
      SedimentaryInputBudget budget,
      SedimentaryBasinState basinState,
      SedimentaryReservoirContribution contribution) {
    long expected = expectedFraction(budget, contribution.kind());
    if (expected <= 0L || contribution.fractionPpm() != expected) {
      throw new IllegalArgumentException("sedimentary reservoir component disagrees with budget");
    }
    Template template = template(contribution.kind(), basinState);
    return new Component(
        contribution.kind(),
        contribution.fractionPpm(),
        contribution.sourceBodyIds(),
        template.compositionPpm(),
        template.waterInventoryPpm(),
        template.volatileInventoryPpm(),
        template.organicCarbonCapacityPpm(),
        template.reducedSulfurCapacityPpm());
  }

  private static long expectedFraction(
      SedimentaryInputBudget budget, SedimentaryReservoirKind kind) {
    return switch (kind) {
      case CLASTIC_TERRIGENOUS -> budget.clasticPpm();
      case VOLCANIC_ASH -> budget.volcanicPpm();
      case CARBONATE_BIOGENIC -> budget.carbonatePpm();
      case ORGANIC_PEAT -> budget.organicPpm();
      case CHEMICAL_PRECIPITATE -> budget.chemicalPrecipitatePpm();
      case EVAPORITIC_BRINE -> budget.evaporiticBrinePpm();
    };
  }

  private static Template template(
      SedimentaryReservoirKind kind, SedimentaryBasinState basinState) {
    long reducingBonus =
        switch (basinState.redoxClass()) {
          case STRONGLY_REDUCING -> 100_000L;
          case REDUCING -> 50_000L;
          case BUFFERED, OXIDIZING, STRONGLY_OXIDIZING -> 0L;
        };
    return switch (kind) {
      case CLASTIC_TERRIGENOUS ->
          new Template(
              composition(500_000L, 150_000L, 300_000L, 50_000L),
              100_000L,
              150_000L,
              0L,
              reducingBonus / 2L);
      case VOLCANIC_ASH ->
          new Template(
              composition(550_000L, 180_000L, 220_000L, 50_000L),
              120_000L,
              160_000L,
              0L,
              reducingBonus / 2L);
      case CARBONATE_BIOGENIC ->
          new Template(
              composition(
                  ChemicalElement.C,
                  120_000L,
                  ChemicalElement.O,
                  600_000L,
                  ChemicalElement.CA,
                  280_000L),
              250_000L,
              350_000L,
              120_000L,
              reducingBonus / 2L);
      case ORGANIC_PEAT ->
          new Template(
              composition(
                  ChemicalElement.C,
                  600_000L,
                  ChemicalElement.H,
                  100_000L,
                  ChemicalElement.O,
                  250_000L,
                  ChemicalElement.N,
                  25_000L,
                  ChemicalElement.S,
                  25_000L),
              300_000L,
              800_000L,
              600_000L,
              25_000L + reducingBonus);
      case CHEMICAL_PRECIPITATE ->
          new Template(
              composition(
                  ChemicalElement.FE,
                  350_000L,
                  ChemicalElement.O,
                  400_000L,
                  ChemicalElement.SI,
                  100_000L,
                  ChemicalElement.C,
                  150_000L),
              150_000L,
              250_000L,
              0L,
              50_000L + reducingBonus / 2L);
      case EVAPORITIC_BRINE ->
          new Template(
              composition(
                  ChemicalElement.NA,
                  300_000L,
                  ChemicalElement.CL,
                  450_000L,
                  ChemicalElement.CA,
                  100_000L,
                  ChemicalElement.S,
                  100_000L,
                  ChemicalElement.H,
                  50_000L),
              850_000L,
              900_000L,
              0L,
              100_000L + reducingBonus);
    };
  }

  private static Map<ChemicalElement, Long> composition(long si, long al, long o, long fe) {
    return composition(
        ChemicalElement.SI,
        si,
        ChemicalElement.AL,
        al,
        ChemicalElement.O,
        o,
        ChemicalElement.FE,
        fe);
  }

  private static Map<ChemicalElement, Long> composition(Object... values) {
    EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
    for (int index = 0; index < values.length; index += 2) {
      result.put((ChemicalElement) values[index], (Long) values[index + 1]);
    }
    long sum = result.values().stream().mapToLong(Long::longValue).sum();
    if (sum != MaterialAssemblage.SCALE) {
      throw new IllegalStateException("authored reservoir composition must close to 1000000");
    }
    return Collections.unmodifiableMap(result);
  }

  private record Template(
      Map<ChemicalElement, Long> compositionPpm,
      long waterInventoryPpm,
      long volatileInventoryPpm,
      long organicCarbonCapacityPpm,
      long reducedSulfurCapacityPpm) {}

  public record Component(
      SedimentaryReservoirKind kind,
      long fractionPpm,
      List<StableId> sourceBodyIds,
      Map<ChemicalElement, Long> compositionPpm,
      long waterInventoryPpm,
      long volatileInventoryPpm,
      long organicCarbonCapacityPpm,
      long reducedSulfurCapacityPpm) {
    public Component {
      if (kind == null
          || fractionPpm <= 0L
          || fractionPpm > MaterialAssemblage.SCALE
          || sourceBodyIds == null
          || compositionPpm == null) {
        throw new IllegalArgumentException("sedimentary reservoir component is incomplete");
      }
      sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
      if (sourceBodyIds.stream().anyMatch(id -> id == null)
          || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()) {
        throw new IllegalArgumentException("sedimentary reservoir sources must be unique");
      }
      EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
      compositionPpm.forEach(
          (element, amount) -> {
            if (element == null
                || amount == null
                || amount <= 0L
                || amount > MaterialAssemblage.SCALE) {
              throw new IllegalArgumentException("sedimentary reservoir chemistry is invalid");
            }
            copied.put(element, amount);
          });
      if (copied.values().stream().mapToLong(Long::longValue).sum() != MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException("sedimentary reservoir chemistry must close to 1000000");
      }
      compositionPpm = Collections.unmodifiableMap(copied);
      requireBounded(waterInventoryPpm, "water inventory");
      requireBounded(volatileInventoryPpm, "volatile inventory");
      requireBounded(organicCarbonCapacityPpm, "organic carbon capacity");
      requireBounded(reducedSulfurCapacityPpm, "reduced sulfur capacity");
      if (volatileInventoryPpm < waterInventoryPpm) {
        throw new IllegalArgumentException("volatile inventory must include water inventory");
      }
    }

    private static void requireBounded(long value, String name) {
      if (value < 0L || value > MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException(name + " must lie in [0, 1000000]");
      }
    }
  }
}
