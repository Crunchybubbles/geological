package io.github.crunchybubbles.geological.petrology;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded composition and polymorph refinements for the Phase 2 mineral vocabulary.
 *
 * <p>This catalog makes the known limits of the ideal endmember resolver explicit. It does not add
 * activity coefficients, a solvus solver, or a thermodynamic equilibrium claim.
 */
public final class MineralPhaseRefinementCatalog {
  private static final List<SolidSolutionRefinement> SOLID_SOLUTIONS = solidSolutionRefinements();
  private static final List<PolymorphFamily> POLYMORPHS = polymorphFamilies();

  private MineralPhaseRefinementCatalog() {}

  /** Returns all solid-solution refinements in stable definition-ID order. */
  public static List<SolidSolutionRefinement> solidSolutions() {
    return SOLID_SOLUTIONS;
  }

  /** Returns all authored polymorph families in stable family-ID order. */
  public static List<PolymorphFamily> polymorphs() {
    return POLYMORPHS;
  }

  /** Returns one refinement by its solid-solution definition ID. */
  public static SolidSolutionRefinement requireSolidSolution(String definitionId) {
    return SOLID_SOLUTIONS.stream()
        .filter(refinement -> refinement.definitionId().equals(definitionId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("unknown phase refinement " + definitionId));
  }

  /** Returns one polymorph family by ID. */
  public static PolymorphFamily requirePolymorph(String familyId) {
    return POLYMORPHS.stream()
        .filter(family -> family.familyId().equals(familyId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown polymorph family " + familyId));
  }

  /**
   * Selects the highest-priority variant whose coarse stability window contains the supplied
   * conditions. A missing result is deliberate evidence that no authored variant is justified.
   */
  public static Optional<PolymorphVariant> selectPolymorph(
      String familyId, double temperatureCelsius, double pressureGPa, boolean hydrated) {
    if (familyId == null || familyId.isBlank()) {
      throw new IllegalArgumentException("polymorph family ID is required");
    }
    if (!Double.isFinite(temperatureCelsius) || !Double.isFinite(pressureGPa)) {
      throw new IllegalArgumentException("polymorph conditions must be finite");
    }
    return requirePolymorph(familyId).variants().stream()
        .filter(variant -> variant.stability().matches(temperatureCelsius, pressureGPa, hydrated))
        .findFirst();
  }

  private static List<SolidSolutionRefinement> solidSolutionRefinements() {
    List<SolidSolutionRefinement> refinements =
        List.of(
            refinement(
                "geological:solid_solution/plagioclase",
                ExsolutionClass.CONDITIONAL_LOW_TEMPERATURE,
                "Sodic-calcic feldspar interpolation; low-temperature unmixing remains a routing flag.",
                range("geological:mineral/albite", "albite-rich endmember"),
                range("geological:mineral/anorthite", "anorthite-rich endmember")),
            refinement(
                "geological:solid_solution/biotite",
                ExsolutionClass.CONDITIONAL_LOW_TEMPERATURE,
                "Phlogopite-annite interpolation; Fe-Mg ordering and exsolution are unresolved.",
                range("geological:mineral/annite", "iron-rich mica endmember"),
                range("geological:mineral/phlogopite", "magnesium-rich mica endmember")),
            refinement(
                "geological:solid_solution/calcic_amphibole",
                ExsolutionClass.UNRESOLVED,
                "Tremolite-ferroactinolite interpolation; hydroxyl and cation ordering are unresolved.",
                range("geological:mineral/ferroactinolite", "iron-rich amphibole endmember"),
                range("geological:mineral/tremolite", "magnesium-rich amphibole endmember")),
            refinement(
                "geological:solid_solution/hornblende",
                ExsolutionClass.CONDITIONAL_COOLING,
                "Magnesiohornblende-ferrohornblende interpolation; cooling exsolution is conditional.",
                range("geological:mineral/ferrohornblende", "iron-rich hornblende endmember"),
                range(
                    "geological:mineral/magnesiohornblende",
                    "magnesium-rich hornblende endmember")),
            refinement(
                "geological:solid_solution/calcic_clinopyroxene",
                ExsolutionClass.CONDITIONAL_COOLING,
                "Diopside-hedenbergite interpolation; augite substitutions are outside this envelope.",
                range("geological:mineral/diopside", "magnesium-rich clinopyroxene endmember"),
                range("geological:mineral/hedenbergite", "iron-rich clinopyroxene endmember")),
            refinement(
                "geological:solid_solution/olivine",
                ExsolutionClass.CONDITIONAL_COOLING,
                "Forsterite-fayalite interpolation; ordering and low-temperature exsolution are unresolved.",
                range("geological:mineral/fayalite", "iron-rich olivine endmember"),
                range("geological:mineral/forsterite", "magnesium-rich olivine endmember")),
            refinement(
                "geological:solid_solution/orthopyroxene",
                ExsolutionClass.CONDITIONAL_COOLING,
                "Enstatite-ferrosilite interpolation; calcium-bearing pyroxene substitutions are excluded.",
                range("geological:mineral/enstatite", "magnesium-rich orthopyroxene endmember"),
                range("geological:mineral/ferrosilite", "iron-rich orthopyroxene endmember")),
            refinement(
                "geological:solid_solution/garnet",
                ExsolutionClass.UNRESOLVED,
                "Pyrope-almandine interpolation; grossular and full multicomponent garnet mixing are unresolved.",
                range("geological:mineral/almandine", "iron-rich garnet endmember"),
                range("geological:mineral/pyrope", "magnesium-rich garnet endmember")));
    return refinements.stream()
        .sorted(java.util.Comparator.comparing(SolidSolutionRefinement::definitionId))
        .toList();
  }

  private static SolidSolutionRefinement refinement(
      String definitionId,
      ExsolutionClass exsolutionClass,
      String condition,
      EndmemberRange... ranges) {
    return new SolidSolutionRefinement(
        definitionId,
        SolidSolutionMixingModel.IDEAL_ENDMEMBER_INTERPOLATION,
        List.of(ranges),
        exsolutionClass,
        condition);
  }

  private static EndmemberRange range(String endmemberId, String condition) {
    return new EndmemberRange(endmemberId, 0L, MaterialAssemblage.SCALE, condition);
  }

  private static List<PolymorphFamily> polymorphFamilies() {
    return List.of(
        new PolymorphFamily(
            "geological:polymorph_family/serpentine",
            List.of(
                new PolymorphVariant(
                    "geological:mineral/lizardite",
                    new StabilityWindow(
                        TemperatureBand.LOW, PressureBand.VERY_LOW, HydrationRequirement.REQUIRED),
                    0),
                new PolymorphVariant(
                    "geological:mineral/chrysotile",
                    new StabilityWindow(
                        TemperatureBand.LOW, PressureBand.LOW, HydrationRequirement.REQUIRED),
                    1),
                new PolymorphVariant(
                    "geological:mineral/antigorite",
                    new StabilityWindow(
                        TemperatureBand.MODERATE,
                        PressureBand.MODERATE,
                        HydrationRequirement.REQUIRED),
                    2)),
            "Serpentine-group variants share a simplified formula; the windows are bounded routing evidence."));
  }

  public record SolidSolutionRefinement(
      String definitionId,
      SolidSolutionMixingModel mixingModel,
      List<EndmemberRange> endmemberRanges,
      ExsolutionClass exsolutionClass,
      String condition) {
    public SolidSolutionRefinement {
      if (definitionId == null
          || definitionId.isBlank()
          || mixingModel == null
          || endmemberRanges == null
          || exsolutionClass == null
          || condition == null
          || condition.isBlank()) {
        throw new IllegalArgumentException("solid-solution refinement must be complete");
      }
      endmemberRanges =
          List.copyOf(endmemberRanges).stream()
              .sorted(java.util.Comparator.comparing(EndmemberRange::endmemberId))
              .toList();
      if (endmemberRanges.size() < 2
          || endmemberRanges.stream().map(EndmemberRange::endmemberId).distinct().count()
              != endmemberRanges.size()) {
        throw new IllegalArgumentException("solid-solution refinement members must be unique");
      }
      long minimum = endmemberRanges.stream().mapToLong(EndmemberRange::minimumFractionPpm).sum();
      long maximum = endmemberRanges.stream().mapToLong(EndmemberRange::maximumFractionPpm).sum();
      if (minimum > MaterialAssemblage.SCALE || maximum < MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException("solid-solution refinement ranges cannot close");
      }
    }

    /** Returns whether a normalized endmember mix lies within this authored envelope. */
    public boolean accepts(Map<String, Long> fractions) {
      if (fractions == null || fractions.size() != endmemberRanges.size()) {
        return false;
      }
      long sum = 0L;
      for (EndmemberRange range : endmemberRanges) {
        Long value = fractions.get(range.endmemberId());
        if (value == null || !range.contains(value)) {
          return false;
        }
        sum = Math.addExact(sum, value);
      }
      return sum == MaterialAssemblage.SCALE;
    }
  }

  public record EndmemberRange(
      String endmemberId, long minimumFractionPpm, long maximumFractionPpm, String condition) {
    public EndmemberRange {
      if (endmemberId == null
          || endmemberId.isBlank()
          || condition == null
          || condition.isBlank()
          || minimumFractionPpm < 0L
          || maximumFractionPpm < minimumFractionPpm
          || maximumFractionPpm > MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException("endmember refinement range is invalid");
      }
    }

    public boolean contains(long value) {
      return value >= minimumFractionPpm && value <= maximumFractionPpm;
    }
  }

  public record PolymorphFamily(
      String familyId, List<PolymorphVariant> variants, String condition) {
    public PolymorphFamily {
      if (familyId == null
          || familyId.isBlank()
          || variants == null
          || condition == null
          || condition.isBlank()) {
        throw new IllegalArgumentException("polymorph family must be complete");
      }
      variants =
          List.copyOf(variants).stream()
              .sorted(
                  java.util.Comparator.comparingInt(PolymorphVariant::priority)
                      .thenComparing(PolymorphVariant::mineralId))
              .toList();
      if (variants.size() < 2
          || variants.stream().anyMatch(Objects::isNull)
          || variants.stream().map(PolymorphVariant::mineralId).distinct().count()
              != variants.size()) {
        throw new IllegalArgumentException("polymorph variants must be unique and non-empty");
      }
    }
  }

  public record PolymorphVariant(String mineralId, StabilityWindow stability, int priority) {
    public PolymorphVariant {
      if (mineralId == null || mineralId.isBlank() || stability == null || priority < 0) {
        throw new IllegalArgumentException("polymorph variant must be complete");
      }
    }
  }

  public record StabilityWindow(
      TemperatureBand temperatureBand,
      PressureBand pressureBand,
      HydrationRequirement hydrationRequirement) {
    public StabilityWindow {
      if (temperatureBand == null || pressureBand == null || hydrationRequirement == null) {
        throw new IllegalArgumentException("polymorph stability window must be complete");
      }
    }

    public boolean matches(double temperatureCelsius, double pressureGPa, boolean hydrated) {
      return temperatureBand.contains(temperatureCelsius)
          && pressureBand.contains(pressureGPa)
          && hydrationRequirement.matches(hydrated);
    }
  }

  public enum ExsolutionClass {
    UNRESOLVED,
    CONDITIONAL_COOLING,
    CONDITIONAL_LOW_TEMPERATURE
  }

  public enum HydrationRequirement {
    ANY,
    REQUIRED,
    FORBIDDEN;

    private boolean matches(boolean hydrated) {
      return switch (this) {
        case ANY -> true;
        case REQUIRED -> hydrated;
        case FORBIDDEN -> !hydrated;
      };
    }
  }

  public enum TemperatureBand {
    LOW(0.0, 400.0),
    MODERATE(300.0, 650.0);

    private final double minimumCelsius;
    private final double maximumCelsius;

    TemperatureBand(double minimumCelsius, double maximumCelsius) {
      this.minimumCelsius = minimumCelsius;
      this.maximumCelsius = maximumCelsius;
    }

    private boolean contains(double value) {
      return value >= minimumCelsius && value < maximumCelsius;
    }
  }

  public enum PressureBand {
    VERY_LOW(0.0, 0.4),
    LOW(0.2, 0.8),
    MODERATE(0.6, 2.5);

    private final double minimumGPa;
    private final double maximumGPa;

    PressureBand(double minimumGPa, double maximumGPa) {
      this.minimumGPa = minimumGPa;
      this.maximumGPa = maximumGPa;
    }

    private boolean contains(double value) {
      return value >= minimumGPa && value < maximumGPa;
    }
  }
}
