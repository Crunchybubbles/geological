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
  private final Map<Lithology, RockDefinition> rocks;
  private final Map<Overprint, AlterationDefinition> alterations;

  public MaterialCatalogSnapshot(
      String digest,
      String canonicalJson,
      CatalogEvidence evidence,
      List<MineralDefinition> minerals,
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

  public BulkComposition composition(MineralAssemblage assemblage) {
    double bulkDensity = 0.0;
    EnumMap<ChemicalElement, Double> unrounded = new EnumMap<>(ChemicalElement.class);
    for (Map.Entry<String, Long> mode : assemblage.modesPpm().entrySet()) {
      MineralDefinition mineral = requireMineral(mode.getKey());
      double volumeFraction = mode.getValue() / (double) MineralAssemblage.SCALE;
      double mineralMass = volumeFraction * mineral.densityGramsPerCubicCentimeter();
      bulkDensity += mineralMass;
      mineral
          .elementMassFractions()
          .forEach(
              (element, fraction) -> unrounded.merge(element, mineralMass * fraction, Double::sum));
    }
    if (!(bulkDensity > 0.0) || !Double.isFinite(bulkDensity)) {
      throw new IllegalStateException("assemblage produced an invalid bulk density");
    }

    EnumMap<ChemicalElement, Long> rounded = new EnumMap<>(ChemicalElement.class);
    List<ElementRemainder> remainders = new ArrayList<>();
    long allocated = 0;
    for (ChemicalElement element : ChemicalElement.values()) {
      double exact = unrounded.getOrDefault(element, 0.0) / bulkDensity * MineralAssemblage.SCALE;
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
    long missing = MineralAssemblage.SCALE - allocated;
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
    rocks.values().forEach(rock -> validateAssemblage(rock.primaryAssemblage(), rock.id()));
    alterations.values().stream()
        .filter(alteration -> alteration.targetAssemblage() != null)
        .forEach(
            alteration ->
                validateAssemblage(
                    alteration.targetAssemblage(), "overprint " + alteration.overprint()));
  }

  private void validateAssemblage(MineralAssemblage assemblage, String owner) {
    assemblage
        .modesPpm()
        .keySet()
        .forEach(
            mineral -> {
              if (!minerals.containsKey(mineral)) {
                throw new IllegalArgumentException(
                    owner + " references unknown mineral " + mineral);
              }
            });
  }

  private record ElementRemainder(ChemicalElement element, double remainder) {}
}
