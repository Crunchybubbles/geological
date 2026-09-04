package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Validated, canonical Phase 2 material catalog and composition calculator. */
public final class MaterialCatalogSnapshot {
  private final String digest;
  private final String canonicalJson;
  private final CatalogEvidence evidence;
  private final Map<String, MineralDefinition> minerals;
  private final Map<String, NonCrystallineConstituentDefinition> nonCrystallineConstituents;
  private final Map<String, MaterialConstituentDefinition> constituents;
  private final Map<String, SolidSolutionDefinition> solidSolutions;
  private final Map<Lithology, RockDefinition> rocks;
  private final Map<Overprint, AlterationDefinition> alterations;

  public MaterialCatalogSnapshot(
      String digest,
      String canonicalJson,
      CatalogEvidence evidence,
      List<MineralDefinition> minerals,
      List<NonCrystallineConstituentDefinition> nonCrystallineConstituents,
      List<SolidSolutionDefinition> solidSolutions,
      List<RockDefinition> rocks,
      List<AlterationDefinition> alterations) {
    if (digest == null
        || digest.isBlank()
        || canonicalJson == null
        || canonicalJson.isBlank()
        || evidence == null) {
      throw new IllegalArgumentException("material catalog identity must be complete");
    }
    this.digest = digest;
    this.canonicalJson = canonicalJson;
    this.evidence = evidence;
    this.minerals = uniqueMinerals(minerals);
    this.nonCrystallineConstituents = uniqueNonCrystallineConstituents(nonCrystallineConstituents);
    this.constituents = combineConstituents(this.minerals, this.nonCrystallineConstituents);
    this.solidSolutions = uniqueSolidSolutions(solidSolutions);
    this.rocks = uniqueRocks(rocks);
    this.alterations = uniqueAlterations(alterations);
    validateReferences();
  }

  public String digest() {
    return digest;
  }

  public String canonicalJson() {
    return canonicalJson;
  }

  public CatalogEvidence evidence() {
    return evidence;
  }

  public List<MineralDefinition> minerals() {
    return List.copyOf(minerals.values());
  }

  public List<NonCrystallineConstituentDefinition> nonCrystallineConstituents() {
    return List.copyOf(nonCrystallineConstituents.values());
  }

  public List<MaterialConstituentDefinition> constituents() {
    return List.copyOf(constituents.values());
  }

  public List<SolidSolutionDefinition> solidSolutions() {
    return List.copyOf(solidSolutions.values());
  }

  public List<RockDefinition> rocks() {
    return List.copyOf(rocks.values());
  }

  public List<AlterationDefinition> alterations() {
    return List.copyOf(alterations.values());
  }

  public MineralDefinition requireMineral(String id) {
    MineralDefinition definition = minerals.get(id);
    if (definition == null) {
      throw new IllegalArgumentException("unknown mineral " + id);
    }
    return definition;
  }

  public NonCrystallineConstituentDefinition requireNonCrystallineConstituent(String id) {
    NonCrystallineConstituentDefinition definition = nonCrystallineConstituents.get(id);
    if (definition == null) {
      throw new IllegalArgumentException("unknown non-crystalline constituent " + id);
    }
    return definition;
  }

  public MaterialConstituentDefinition requireConstituent(String id) {
    MaterialConstituentDefinition definition = constituents.get(id);
    if (definition == null) {
      throw new IllegalArgumentException("unknown material constituent " + id);
    }
    return definition;
  }

  public RockDefinition requireRock(Lithology lithology) {
    RockDefinition definition = rocks.get(lithology);
    if (definition == null) {
      throw new IllegalArgumentException("unknown lithology " + lithology);
    }
    return definition;
  }

  public AlterationDefinition requireAlteration(Overprint overprint) {
    AlterationDefinition definition = alterations.get(overprint);
    if (definition == null) {
      throw new IllegalArgumentException("unknown overprint " + overprint);
    }
    return definition;
  }

  public SolidSolutionDefinition requireSolidSolution(String id) {
    SolidSolutionDefinition definition = solidSolutions.get(id);
    if (definition == null) {
      throw new IllegalArgumentException("unknown solid solution " + id);
    }
    return definition;
  }

  public List<SolidSolutionState> solidSolutionStates(MaterialAssemblage assemblage) {
    if (assemblage == null) {
      throw new IllegalArgumentException("material assemblage is required");
    }
    List<SolidSolutionState> states = new ArrayList<>();
    solidSolutions
        .values()
        .forEach(
            definition -> {
              SolidSolutionState state = resolveSolidSolution(definition, assemblage);
              if (state != null) {
                states.add(state);
              }
            });
    return List.copyOf(states);
  }

  public BulkComposition composition(MaterialAssemblage assemblage) {
    double bulkDensity = 0.0;
    EnumMap<ChemicalElement, Double> unrounded = new EnumMap<>(ChemicalElement.class);
    for (Map.Entry<String, Long> mode : assemblage.modesPpm().entrySet()) {
      MaterialConstituentDefinition constituent = requireConstituent(mode.getKey());
      double volumeFraction = mode.getValue() / (double) MaterialAssemblage.SCALE;
      double constituentMass = volumeFraction * constituent.densityGramsPerCubicCentimeter();
      bulkDensity += constituentMass;
      constituent
          .elementMassFractions()
          .forEach(
              (element, fraction) ->
                  unrounded.merge(element, constituentMass * fraction, Double::sum));
    }
    if (!(bulkDensity > 0.0) || !Double.isFinite(bulkDensity)) {
      throw new IllegalStateException("assemblage produced an invalid bulk density");
    }

    EnumMap<ChemicalElement, Long> rounded = new EnumMap<>(ChemicalElement.class);
    List<ElementRemainder> remainders = new ArrayList<>();
    long allocated = 0;
    // Only authored constituent elements can receive mass. Iterating the complete Phase 9
    // vocabulary would allow a newly added, currently absent element to receive a rounding unit
    // and silently change an older catalog's composition.
    for (ChemicalElement element : unrounded.keySet()) {
      double exact = unrounded.getOrDefault(element, 0.0) / bulkDensity * MaterialAssemblage.SCALE;
      long whole = (long) StrictMath.floor(exact);
      if (whole > 0) {
        rounded.put(element, whole);
      }
      allocated += whole;
      remainders.add(new ElementRemainder(element, exact - whole));
    }
    remainders.sort(
        Comparator.comparingDouble(ElementRemainder::remainder)
            .reversed()
            .thenComparing(ElementRemainder::element));
    long missing = MaterialAssemblage.SCALE - allocated;
    for (int index = 0; index < missing; index++) {
      rounded.merge(remainders.get(index).element(), 1L, Long::sum);
    }
    return new BulkComposition(rounded, bulkDensity);
  }

  private Map<String, MineralDefinition> uniqueMinerals(List<MineralDefinition> definitions) {
    TreeMap<String, MineralDefinition> result = new TreeMap<>();
    definitions.forEach(
        definition -> {
          if (result.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("duplicate mineral " + definition.id());
          }
        });
    if (result.isEmpty()) {
      throw new IllegalArgumentException("material catalog must contain minerals");
    }
    return Collections.unmodifiableMap(result);
  }

  private Map<String, NonCrystallineConstituentDefinition> uniqueNonCrystallineConstituents(
      List<NonCrystallineConstituentDefinition> definitions) {
    TreeMap<String, NonCrystallineConstituentDefinition> result = new TreeMap<>();
    definitions.forEach(
        definition -> {
          if (result.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException(
                "duplicate non-crystalline constituent " + definition.id());
          }
        });
    return Collections.unmodifiableMap(result);
  }

  private static Map<String, MaterialConstituentDefinition> combineConstituents(
      Map<String, MineralDefinition> minerals,
      Map<String, NonCrystallineConstituentDefinition> nonCrystallineConstituents) {
    TreeMap<String, MaterialConstituentDefinition> result = new TreeMap<>();
    minerals.forEach(result::put);
    nonCrystallineConstituents.forEach(
        (id, definition) -> {
          if (result.putIfAbsent(id, definition) != null) {
            throw new IllegalArgumentException(
                "material constituent ID is shared by mineral and non-crystalline definitions "
                    + id);
          }
        });
    return Collections.unmodifiableMap(result);
  }

  private Map<String, SolidSolutionDefinition> uniqueSolidSolutions(
      List<SolidSolutionDefinition> definitions) {
    TreeMap<String, SolidSolutionDefinition> result = new TreeMap<>();
    definitions.forEach(
        definition -> {
          if (result.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("duplicate solid-solution ID " + definition.id());
          }
        });
    if (result.isEmpty()) {
      throw new IllegalArgumentException("material catalog must contain solid solutions");
    }
    return Collections.unmodifiableMap(result);
  }

  private Map<Lithology, RockDefinition> uniqueRocks(List<RockDefinition> definitions) {
    EnumMap<Lithology, RockDefinition> result = new EnumMap<>(Lithology.class);
    Map<String, RockDefinition> ids = new HashMap<>();
    definitions.forEach(
        definition -> {
          if (result.putIfAbsent(definition.lithology(), definition) != null) {
            throw new IllegalArgumentException("duplicate lithology " + definition.lithology());
          }
          if (ids.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("duplicate rock ID " + definition.id());
          }
        });
    if (result.size() != Lithology.values().length) {
      throw new IllegalArgumentException("material catalog must map every lithology exactly once");
    }
    return Collections.unmodifiableMap(result);
  }

  private Map<Overprint, AlterationDefinition> uniqueAlterations(
      List<AlterationDefinition> definitions) {
    EnumMap<Overprint, AlterationDefinition> result = new EnumMap<>(Overprint.class);
    definitions.forEach(
        definition -> {
          if (result.putIfAbsent(definition.overprint(), definition) != null) {
            throw new IllegalArgumentException("duplicate overprint " + definition.overprint());
          }
        });
    if (result.size() != Overprint.values().length) {
      throw new IllegalArgumentException("material catalog must map every overprint exactly once");
    }
    return Collections.unmodifiableMap(result);
  }

  private void validateReferences() {
    Map<String, RockDefinition> rocksById = new HashMap<>();
    rocks.values().forEach(rock -> rocksById.put(rock.id(), rock));
    rocks
        .values()
        .forEach(
            rock ->
                rock.primaryMetamorphism()
                    .ifPresent(
                        metamorphism -> {
                          RockDefinition protolith = rocksById.get(metamorphism.protolithRockId());
                          if (protolith == null) {
                            throw new IllegalArgumentException(
                                rock.id()
                                    + " references unknown metamorphic protolith "
                                    + metamorphism.protolithRockId());
                          }
                          if (protolith.geneticFamily() == GeneticFamily.METAMORPHIC) {
                            throw new IllegalArgumentException(
                                rock.id() + " must reference a non-metamorphic original protolith");
                          }
                        }));
    Map<String, String> endmemberOwners = new HashMap<>();
    solidSolutions
        .values()
        .forEach(
            solution ->
                solution
                    .endmemberIds()
                    .forEach(
                        endmember -> {
                          if (!minerals.containsKey(endmember)) {
                            throw new IllegalArgumentException(
                                "solid solution "
                                    + solution.id()
                                    + " references unknown mineral "
                                    + endmember);
                          }
                          String existing = endmemberOwners.putIfAbsent(endmember, solution.id());
                          if (existing != null) {
                            throw new IllegalArgumentException(
                                "solid-solution endmember "
                                    + endmember
                                    + " belongs to both "
                                    + existing
                                    + " and "
                                    + solution.id());
                          }
                        }));
    rocks.values().forEach(rock -> validateAssemblage(rock.primaryAssemblage(), rock.id()));
    alterations
        .values()
        .forEach(
            alteration ->
                alteration
                    .targetRecipes()
                    .forEach(
                        recipe ->
                            validateAssemblage(
                                recipe.targetAssemblage(),
                                "overprint "
                                    + alteration.overprint()
                                    + " for "
                                    + recipe.protolithFamilies())));
  }

  private void validateAssemblage(MaterialAssemblage assemblage, String owner) {
    assemblage
        .modesPpm()
        .keySet()
        .forEach(
            constituent -> {
              if (!constituents.containsKey(constituent)) {
                throw new IllegalArgumentException(
                    owner + " references unknown material constituent " + constituent);
              }
            });
  }

  private SolidSolutionState resolveSolidSolution(
      SolidSolutionDefinition definition, MaterialAssemblage assemblage) {
    TreeMap<String, Long> componentModes = new TreeMap<>();
    long phaseMode = 0;
    for (String endmemberId : definition.endmemberIds()) {
      long mode = assemblage.modesPpm().getOrDefault(endmemberId, 0L);
      componentModes.put(endmemberId, mode);
      phaseMode += mode;
    }
    if (phaseMode == 0) {
      return null;
    }

    Map<String, Long> volumeFractions = exactIntegerFractions(componentModes, phaseMode);
    TreeMap<String, Double> moleWeights = new TreeMap<>();
    for (Map.Entry<String, Long> component : componentModes.entrySet()) {
      MineralDefinition mineral = requireMineral(component.getKey());
      double weight =
          component.getValue() * mineral.densityGramsPerCubicCentimeter() / mineral.formulaMass();
      moleWeights.put(component.getKey(), weight);
    }
    Map<String, Long> moleFractions = exactDoubleFractions(moleWeights);

    EnumMap<ChemicalElement, Double> formula = new EnumMap<>(ChemicalElement.class);
    double hardness = 0.0;
    double weathering = 0.0;
    for (String endmemberId : definition.endmemberIds()) {
      MineralDefinition mineral = requireMineral(endmemberId);
      double moleFraction = moleFractions.get(endmemberId) / (double) MaterialAssemblage.SCALE;
      double volumeFraction = volumeFractions.get(endmemberId) / (double) MaterialAssemblage.SCALE;
      if (moleFraction > 0.0) {
        mineral
            .formula()
            .forEach((element, count) -> formula.merge(element, moleFraction * count, Double::sum));
      }
      hardness += volumeFraction * mineral.hardnessMohs();
      weathering += volumeFraction * mineral.weatheringResistance();
    }
    BulkComposition bulkComposition = composition(new MaterialAssemblage(volumeFractions));
    return new SolidSolutionState(
        definition.id(),
        definition.mixingModel(),
        phaseMode,
        volumeFractions,
        moleFractions,
        formula,
        bulkComposition,
        hardness,
        weathering);
  }

  private static Map<String, Long> exactIntegerFractions(Map<String, Long> weights, long total) {
    TreeMap<String, Long> fractions = new TreeMap<>();
    List<StringRemainder> remainders = new ArrayList<>();
    long allocated = 0;
    for (Map.Entry<String, Long> entry : weights.entrySet()) {
      long numerator = entry.getValue() * MaterialAssemblage.SCALE;
      long whole = numerator / total;
      fractions.put(entry.getKey(), whole);
      allocated += whole;
      remainders.add(new StringRemainder(entry.getKey(), numerator % total));
    }
    allocateMissing(fractions, remainders, MaterialAssemblage.SCALE - allocated);
    return Collections.unmodifiableMap(fractions);
  }

  private static Map<String, Long> exactDoubleFractions(Map<String, Double> weights) {
    double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
    if (!(total > 0.0) || !Double.isFinite(total)) {
      throw new IllegalStateException("solid-solution mole weights are invalid");
    }
    TreeMap<String, Long> fractions = new TreeMap<>();
    List<DoubleRemainder> remainders = new ArrayList<>();
    long allocated = 0;
    for (Map.Entry<String, Double> entry : weights.entrySet()) {
      double exact = entry.getValue() / total * MaterialAssemblage.SCALE;
      long whole = (long) StrictMath.floor(exact);
      fractions.put(entry.getKey(), whole);
      allocated += whole;
      remainders.add(new DoubleRemainder(entry.getKey(), exact - whole));
    }
    remainders.sort(
        Comparator.comparingDouble(DoubleRemainder::remainder)
            .reversed()
            .thenComparing(DoubleRemainder::id));
    long missing = MaterialAssemblage.SCALE - allocated;
    for (int index = 0; index < missing; index++) {
      fractions.merge(remainders.get(index).id(), 1L, Long::sum);
    }
    return Collections.unmodifiableMap(fractions);
  }

  private static void allocateMissing(
      Map<String, Long> fractions, List<StringRemainder> remainders, long missing) {
    remainders.sort(
        Comparator.comparingLong(StringRemainder::remainder)
            .reversed()
            .thenComparing(StringRemainder::id));
    for (int index = 0; index < missing; index++) {
      fractions.merge(remainders.get(index).id(), 1L, Long::sum);
    }
  }

  private record ElementRemainder(ChemicalElement element, double remainder) {}

  private record StringRemainder(String id, long remainder) {}

  private record DoubleRemainder(String id, double remainder) {}
}
