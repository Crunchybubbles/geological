package io.github.crunchybubbles.geological.registry;

import java.util.List;
import java.util.Map;

/** Logical definition compiled into the frozen scientific snapshot. */
public record RegistryDefinition(
    String id,
    String schemaId,
    int schemaVersion,
    DefinitionKind kind,
    String modelVersion,
    String confidence,
    List<String> dependencies,
    List<String> citations,
    Map<String, ScientificQuantity> parameters) {
  public RegistryDefinition {
    if (id == null
        || id.isBlank()
        || schemaId == null
        || schemaId.isBlank()
        || schemaVersion <= 0
        || kind == null
        || modelVersion == null
        || modelVersion.isBlank()
        || confidence == null
        || confidence.isBlank()) {
      throw new IllegalArgumentException("registry definition metadata must be complete");
    }
    dependencies = List.copyOf(dependencies);
    citations = List.copyOf(citations);
    parameters = Map.copyOf(parameters);
  }
}
