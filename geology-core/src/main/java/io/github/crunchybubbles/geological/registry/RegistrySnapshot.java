package io.github.crunchybubbles.geological.registry;

import java.util.List;

/** Validated, canonically ordered, immutable scientific content used for world identity. */
public record RegistrySnapshot(
    String digest,
    String canonicalJson,
    List<ScientificUnit> units,
    List<Citation> citations,
    List<DefinitionSchema> schemas,
    List<RegistryDefinition> definitions) {
  public RegistrySnapshot {
    if (digest == null || digest.isBlank() || canonicalJson == null || canonicalJson.isBlank()) {
      throw new IllegalArgumentException(
          "registry snapshot digest and canonical form are required");
    }
    units = List.copyOf(units);
    citations = List.copyOf(citations);
    schemas = List.copyOf(schemas);
    definitions = List.copyOf(definitions);
  }

  public RegistryDefinition requireDefinition(String id) {
    return definitions.stream()
        .filter(definition -> definition.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown registry definition " + id));
  }

  public double requireQuantity(String definitionId, String parameterName, ScientificUnit unit) {
    ScientificQuantity quantity = requireDefinition(definitionId).parameters().get(parameterName);
    if (quantity == null) {
      throw new IllegalArgumentException(
          "unknown registry parameter " + definitionId + "." + parameterName);
    }
    if (quantity.unit() != unit) {
      throw new IllegalArgumentException(
          "registry parameter " + definitionId + "." + parameterName + " has the wrong unit");
    }
    return quantity.value();
  }
}
