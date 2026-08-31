package io.github.crunchybubbles.geological.registry;

import java.util.Map;

/** Versioned schema for one family of scientific or behavioral definitions. */
public record DefinitionSchema(
    String id,
    int schemaVersion,
    DefinitionKind kind,
    Map<String, ParameterConstraint> parameters) {
  public DefinitionSchema {
    if (id == null || id.isBlank() || schemaVersion <= 0 || kind == null) {
      throw new IllegalArgumentException("definition schema identity must be complete");
    }
    parameters = Map.copyOf(parameters);
    if (parameters.isEmpty()) {
      throw new IllegalArgumentException("definition schema must constrain at least one parameter");
    }
  }
}
