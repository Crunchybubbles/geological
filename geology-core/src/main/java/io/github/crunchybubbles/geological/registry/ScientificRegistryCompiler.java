package io.github.crunchybubbles.geological.registry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Validates logical definitions and emits canonical JSON plus a full SHA-256 digest. */
public final class ScientificRegistryCompiler {
  private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

  public RegistrySnapshot compile(
      List<Citation> inputCitations,
      List<DefinitionSchema> inputSchemas,
      List<RegistryDefinition> inputDefinitions) {
    List<Citation> citations =
        inputCitations.stream().sorted(java.util.Comparator.comparing(Citation::id)).toList();
    List<DefinitionSchema> schemas =
        inputSchemas.stream().sorted(java.util.Comparator.comparing(DefinitionSchema::id)).toList();
    List<RegistryDefinition> definitions =
        inputDefinitions.stream()
            .sorted(java.util.Comparator.comparing(RegistryDefinition::id))
            .toList();
    List<ScientificUnit> units =
        java.util.Arrays.stream(ScientificUnit.values())
            .sorted(java.util.Comparator.comparing(ScientificUnit::id))
            .toList();
    List<RegistryDiagnostic> diagnostics = new ArrayList<>();

    Map<String, Citation> citationsById = unique(citations, Citation::id, "citations", diagnostics);
    Map<String, DefinitionSchema> schemasById =
        unique(schemas, DefinitionSchema::id, "schemas", diagnostics);
    Map<String, RegistryDefinition> definitionsById =
        unique(definitions, RegistryDefinition::id, "definitions", diagnostics);
    unique(units, ScientificUnit::id, "units", diagnostics);
    units.forEach(unit -> validateIdentifier(unit.id(), "units", diagnostics));
    citations.forEach(citation -> validateCitation(citation, diagnostics));
    schemas.forEach(schema -> validateIdentifier(schema.id(), "schemas", diagnostics));
    definitions.forEach(
        definition -> {
          validateIdentifier(definition.id(), "definitions", diagnostics);
          validateDefinition(definition, schemasById, definitionsById, citationsById, diagnostics);
        });
    validateDependencyCycles(definitionsById, diagnostics);

    if (!diagnostics.isEmpty()) {
      diagnostics.sort(RegistryDiagnostic::compareTo);
      throw new RegistryValidationException(diagnostics);
    }
    String canonicalJson = canonicalJson(units, citations, schemas, definitions);
    return new RegistrySnapshot(
        "sha256:" + sha256(canonicalJson), canonicalJson, units, citations, schemas, definitions);
  }

  private static void validateDefinition(
      RegistryDefinition definition,
      Map<String, DefinitionSchema> schemas,
      Map<String, RegistryDefinition> definitions,
      Map<String, Citation> citations,
      List<RegistryDiagnostic> diagnostics) {
    String path = "definitions[" + definition.id() + "]";
    DefinitionSchema schema = schemas.get(definition.schemaId());
    if (schema == null) {
      diagnostics.add(new RegistryDiagnostic(path + ".schemaId", "unresolved schema reference"));
    } else {
      if (schema.schemaVersion() != definition.schemaVersion()) {
        diagnostics.add(
            new RegistryDiagnostic(
                path + ".schemaVersion", "does not match the referenced schema"));
      }
      if (schema.kind() != definition.kind()) {
        diagnostics.add(
            new RegistryDiagnostic(path + ".kind", "does not match the referenced schema"));
      }
      for (Map.Entry<String, ParameterConstraint> expected : schema.parameters().entrySet()) {
        ScientificQuantity actual = definition.parameters().get(expected.getKey());
        if (actual == null) {
          diagnostics.add(
              new RegistryDiagnostic(
                  path + ".parameters." + expected.getKey(), "required parameter is missing"));
        } else if (!expected.getValue().accepts(actual)) {
          diagnostics.add(
              new RegistryDiagnostic(
                  path + ".parameters." + expected.getKey(),
                  "unit dimension or value is outside the schema constraint"));
        }
      }
      definition.parameters().keySet().stream()
          .filter(parameter -> !schema.parameters().containsKey(parameter))
          .forEach(
              parameter ->
                  diagnostics.add(
                      new RegistryDiagnostic(
                          path + ".parameters." + parameter,
                          "parameter is not declared by the schema")));
    }
    definition
        .dependencies()
        .forEach(
            dependency -> {
              if (!definitions.containsKey(dependency)) {
                diagnostics.add(
                    new RegistryDiagnostic(
                        path + ".dependencies", "unresolved reference " + dependency));
              }
            });
    Set<String> usedCitations = new HashSet<>(definition.citations());
    definition.parameters().values().stream()
        .map(ScientificQuantity::basis)
        .map(ParameterBasis::citationId)
        .filter(java.util.Objects::nonNull)
        .forEach(usedCitations::add);
    usedCitations.forEach(
        citation -> {
          if (!citations.containsKey(citation)) {
            diagnostics.add(
                new RegistryDiagnostic(path + ".citations", "unresolved citation " + citation));
          }
        });
  }

  private static void validateDependencyCycles(
      Map<String, RegistryDefinition> definitions, List<RegistryDiagnostic> diagnostics) {
    Set<String> complete = new HashSet<>();
    Set<String> visiting = new HashSet<>();
    definitions.keySet().stream()
        .sorted()
        .forEach(id -> visit(id, definitions, complete, visiting, diagnostics));
  }

  private static void visit(
      String id,
      Map<String, RegistryDefinition> definitions,
      Set<String> complete,
      Set<String> visiting,
      List<RegistryDiagnostic> diagnostics) {
    if (complete.contains(id) || !definitions.containsKey(id)) {
      return;
    }
    if (!visiting.add(id)) {
      diagnostics.add(
          new RegistryDiagnostic(
              "definitions[" + id + "].dependencies", "dependency cycle detected"));
      return;
    }
    definitions.get(id).dependencies().stream()
        .sorted()
        .forEach(dependency -> visit(dependency, definitions, complete, visiting, diagnostics));
    visiting.remove(id);
    complete.add(id);
  }

  private static void validateIdentifier(
      String id, String collection, List<RegistryDiagnostic> diagnostics) {
    if (!IDENTIFIER.matcher(id).matches()) {
      diagnostics.add(
          new RegistryDiagnostic(collection + "[" + id + "].id", "must be a namespaced stable ID"));
    }
  }

  private static void validateCitation(Citation citation, List<RegistryDiagnostic> diagnostics) {
    String path = "citations[" + citation.id() + "]";
    validateIdentifier(citation.id(), "citations", diagnostics);
    if (!"https".equalsIgnoreCase(citation.uri().getScheme())) {
      diagnostics.add(new RegistryDiagnostic(path + ".uri", "citation URI must use HTTPS"));
    }
    if (citation.publicationYear() < 1600 || citation.publicationYear() > 3000) {
      diagnostics.add(
          new RegistryDiagnostic(path + ".publicationYear", "year is outside the accepted range"));
    }
  }

  private static <T> Map<String, T> unique(
      List<T> values,
      java.util.function.Function<T, String> id,
      String collection,
      List<RegistryDiagnostic> diagnostics) {
    Map<String, T> result = new HashMap<>();
    for (T value : values) {
      String key = id.apply(value);
      if (result.putIfAbsent(key, value) != null) {
        diagnostics.add(new RegistryDiagnostic(collection + "[" + key + "]", "duplicate ID"));
      }
    }
    return result;
  }

  private static String canonicalJson(
      List<ScientificUnit> units,
      List<Citation> citations,
      List<DefinitionSchema> schemas,
      List<RegistryDefinition> definitions) {
    StringBuilder output = new StringBuilder();
    output.append("{\"canonical_schema\":\"geological:registry_snapshot:v1\",\"citations\":[");
    appendSeparated(
        output,
        citations,
        citation -> {
          output.append('{');
          field(output, "id", citation.id());
          output.append(',');
          field(output, "publication_year", citation.publicationYear());
          output.append(',');
          field(output, "title", citation.title());
          output.append(',');
          field(output, "uri", citation.uri().toASCIIString());
          output.append('}');
        });
    output.append("],\"definitions\":[");
    appendSeparated(output, definitions, definition -> appendDefinition(output, definition));
    output.append("],\"schemas\":[");
    appendSeparated(output, schemas, schema -> appendSchema(output, schema));
    output.append("],\"units\":[");
    appendSeparated(
        output,
        units,
        unit -> {
          output.append('{');
          field(output, "dimension", unit.dimension().name());
          output.append(',');
          field(output, "id", unit.id());
          output.append(',');
          field(output, "symbol", unit.symbol());
          output.append('}');
        });
    output.append("]}");
    return Normalizer.normalize(output, Normalizer.Form.NFC);
  }

  private static void appendDefinition(StringBuilder output, RegistryDefinition definition) {
    output.append('{');
    field(output, "citations", definition.citations().stream().sorted().toList());
    output.append(',');
    field(output, "confidence", definition.confidence());
    output.append(',');
    field(output, "dependencies", definition.dependencies().stream().sorted().toList());
    output.append(',');
    field(output, "id", definition.id());
    output.append(',');
    field(output, "kind", definition.kind().name());
    output.append(',');
    field(output, "model_version", definition.modelVersion());
    output.append(",\"parameters\":{");
    appendSeparated(
        output,
        new TreeMap<>(definition.parameters()).entrySet().stream().toList(),
        entry -> {
          string(output, entry.getKey());
          output.append(":{");
          ParameterBasis basis = entry.getValue().basis();
          if (basis.citationId() != null) {
            field(output, "citation_id", basis.citationId());
          } else {
            field(output, "tunable_design_value", basis.tunableDesignRationale());
          }
          output.append(',');
          field(output, "unit", entry.getValue().unit().id());
          output.append(',');
          field(output, "value", entry.getValue().value());
          output.append('}');
        });
    output.append("},");
    field(output, "schema_id", definition.schemaId());
    output.append(',');
    field(output, "schema_version", definition.schemaVersion());
    output.append('}');
  }

  private static void appendSchema(StringBuilder output, DefinitionSchema schema) {
    output.append('{');
    field(output, "id", schema.id());
    output.append(',');
    field(output, "kind", schema.kind().name());
    output.append(",\"parameters\":{");
    appendSeparated(
        output,
        new TreeMap<>(schema.parameters()).entrySet().stream().toList(),
        entry -> {
          string(output, entry.getKey());
          output.append(":{");
          field(output, "dimension", entry.getValue().dimension().name());
          output.append(',');
          field(output, "maximum_inclusive", entry.getValue().maximumInclusive());
          output.append(',');
          field(output, "minimum_inclusive", entry.getValue().minimumInclusive());
          output.append('}');
        });
    output.append("},");
    field(output, "schema_version", schema.schemaVersion());
    output.append('}');
  }

  private static <T> void appendSeparated(
      StringBuilder output, List<T> values, java.util.function.Consumer<T> consumer) {
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        output.append(',');
      }
      consumer.accept(values.get(index));
    }
  }

  private static void field(StringBuilder output, String name, String value) {
    string(output, name);
    output.append(':');
    string(output, value);
  }

  private static void field(StringBuilder output, String name, int value) {
    string(output, name);
    output.append(':').append(value);
  }

  private static void field(StringBuilder output, String name, double value) {
    string(output, name);
    output.append(':').append(Double.toString(value));
  }

  private static void field(StringBuilder output, String name, List<String> values) {
    string(output, name);
    output.append(':');
    output.append('[');
    appendSeparated(output, values, value -> string(output, value));
    output.append(']');
  }

  private static void string(StringBuilder output, String value) {
    output.append('"');
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> output.append("\\\"");
        case '\\' -> output.append("\\\\");
        case '\b' -> output.append("\\b");
        case '\f' -> output.append("\\f");
        case '\n' -> output.append("\\n");
        case '\r' -> output.append("\\r");
        case '\t' -> output.append("\\t");
        default -> {
          if (character < 0x20) {
            output
                .append("\\u")
                .append(String.format(java.util.Locale.ROOT, "%04x", (int) character));
          } else {
            output.append(character);
          }
        }
      }
    }
    output.append('"');
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }
}
