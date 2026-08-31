package io.github.crunchybubbles.geological.registry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Strict boundary from public JSON authoring documents to the logical registry model. */
public final class RegistryJsonLoader {
  public static final String AUTHORING_SCHEMA = "geological:registry_authoring:v1";
  private static final int MAX_DOCUMENT_BYTES = 1_048_576;
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  public RegistrySnapshot loadResource(Class<?> anchor, String resourceName) {
    if (anchor == null || resourceName == null || resourceName.isBlank()) {
      throw new IllegalArgumentException("resource anchor and name are required");
    }
    try (InputStream input = anchor.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw error(resourceName, "$", "classpath resource does not exist");
      }
      return load(input, resourceName);
    } catch (IOException exception) {
      throw new RegistryAuthoringException(
          resourceName, "could not close registry resource", exception);
    }
  }

  public RegistrySnapshot load(InputStream input, String sourceName) {
    if (input == null || sourceName == null || sourceName.isBlank()) {
      throw new IllegalArgumentException("registry input and source name are required");
    }
    try {
      byte[] document = input.readNBytes(MAX_DOCUMENT_BYTES + 1);
      if (document.length > MAX_DOCUMENT_BYTES) {
        throw error(sourceName, "$", "document exceeds the 1 MiB authoring limit");
      }
      JsonNode root = JSON.readTree(document);
      Set<String> rootFields =
          Set.of("authoring_schema", "citations", "definitions", "schemas", "units");
      requireObject(root, sourceName, "$", rootFields, rootFields);
      if (!AUTHORING_SCHEMA.equals(text(root, "authoring_schema", sourceName, "$"))) {
        throw error(sourceName, "$.authoring_schema", "unsupported authoring schema");
      }
      Map<String, ScientificUnit> units = parseUnits(root.get("units"), sourceName);
      List<Citation> citations = parseCitations(root.get("citations"), sourceName);
      List<DefinitionSchema> schemas = parseSchemas(root.get("schemas"), sourceName);
      List<RegistryDefinition> definitions =
          parseDefinitions(root.get("definitions"), units, sourceName);
      return new ScientificRegistryCompiler().compile(citations, schemas, definitions);
    } catch (RegistryAuthoringException | RegistryValidationException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw new RegistryAuthoringException(
          sourceName, "invalid JSON: " + exception.getMessage(), exception);
    } catch (IOException exception) {
      throw new RegistryAuthoringException(
          sourceName, "could not read registry document", exception);
    }
  }

  private static Map<String, ScientificUnit> parseUnits(JsonNode node, String source) {
    requireArray(node, source, "$.units");
    Map<String, ScientificUnit> result = new HashMap<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.units[" + index + "]";
      Set<String> fields = Set.of("dimension", "id", "symbol");
      requireObject(item, source, path, fields, fields);
      String id = text(item, "id", source, path);
      ScientificUnit unit =
          java.util.Arrays.stream(ScientificUnit.values())
              .filter(candidate -> candidate.id().equals(id))
              .findFirst()
              .orElseThrow(() -> error(source, path + ".id", "unsupported unit " + id));
      if (!unit.symbol().equals(text(item, "symbol", source, path))) {
        throw error(source, path + ".symbol", "does not match the supported unit vocabulary");
      }
      QuantityDimension dimension =
          enumValue(
              QuantityDimension.class,
              text(item, "dimension", source, path),
              source,
              path + ".dimension");
      if (unit.dimension() != dimension) {
        throw error(source, path + ".dimension", "does not match the supported unit vocabulary");
      }
      if (result.putIfAbsent(id, unit) != null) {
        throw error(source, path + ".id", "duplicate unit ID");
      }
      index++;
    }
    Set<String> missing = new HashSet<>();
    for (ScientificUnit unit : ScientificUnit.values()) {
      if (!result.containsKey(unit.id())) {
        missing.add(unit.id());
      }
    }
    if (!missing.isEmpty()) {
      throw error(
          source, "$.units", "missing supported units " + missing.stream().sorted().toList());
    }
    return Map.copyOf(result);
  }

  private static List<Citation> parseCitations(JsonNode node, String source) {
    requireArray(node, source, "$.citations");
    List<Citation> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.citations[" + index + "]";
      Set<String> fields = Set.of("id", "publication_year", "title", "uri");
      requireObject(item, source, path, fields, fields);
      try {
        result.add(
            new Citation(
                text(item, "id", source, path),
                text(item, "title", source, path),
                URI.create(text(item, "uri", source, path)),
                integer(item, "publication_year", source, path)));
      } catch (IllegalArgumentException exception) {
        throw error(source, path, exception.getMessage());
      }
      index++;
    }
    return List.copyOf(result);
  }

  private static List<DefinitionSchema> parseSchemas(JsonNode node, String source) {
    requireArray(node, source, "$.schemas");
    List<DefinitionSchema> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.schemas[" + index + "]";
      Set<String> fields = Set.of("id", "kind", "parameters", "schema_version");
      requireObject(item, source, path, fields, fields);
      Map<String, ParameterConstraint> constraints = new HashMap<>();
      JsonNode parameters = item.get("parameters");
      requireObject(parameters, source, path + ".parameters", null, Set.of());
      for (Map.Entry<String, JsonNode> entry : parameters.properties()) {
        String parameterPath = path + ".parameters." + entry.getKey();
        Set<String> constraintFields =
            Set.of("dimension", "maximum_inclusive", "minimum_inclusive");
        requireObject(entry.getValue(), source, parameterPath, constraintFields, constraintFields);
        constraints.put(
            entry.getKey(),
            new ParameterConstraint(
                enumValue(
                    QuantityDimension.class,
                    text(entry.getValue(), "dimension", source, parameterPath),
                    source,
                    parameterPath + ".dimension"),
                number(entry.getValue(), "minimum_inclusive", source, parameterPath),
                number(entry.getValue(), "maximum_inclusive", source, parameterPath)));
      }
      result.add(
          new DefinitionSchema(
              text(item, "id", source, path),
              integer(item, "schema_version", source, path),
              enumValue(
                  DefinitionKind.class, text(item, "kind", source, path), source, path + ".kind"),
              constraints));
      index++;
    }
    return List.copyOf(result);
  }

  private static List<RegistryDefinition> parseDefinitions(
      JsonNode node, Map<String, ScientificUnit> units, String source) {
    requireArray(node, source, "$.definitions");
    List<RegistryDefinition> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.definitions[" + index + "]";
      Set<String> fields =
          Set.of(
              "citations",
              "confidence",
              "dependencies",
              "id",
              "kind",
              "model_version",
              "parameters",
              "schema_id",
              "schema_version");
      requireObject(item, source, path, fields, fields);
      Map<String, ScientificQuantity> parameters = new HashMap<>();
      JsonNode parameterNode = item.get("parameters");
      requireObject(parameterNode, source, path + ".parameters", null, Set.of());
      for (Map.Entry<String, JsonNode> entry : parameterNode.properties()) {
        String parameterPath = path + ".parameters." + entry.getKey();
        JsonNode quantity = entry.getValue();
        requireObject(
            quantity,
            source,
            parameterPath,
            Set.of("citation_id", "tunable_design_value", "unit", "value"),
            Set.of("unit", "value"));
        boolean cited = quantity.has("citation_id");
        boolean tunable = quantity.has("tunable_design_value");
        if (cited == tunable) {
          throw error(
              source,
              parameterPath,
              "exactly one of citation_id or tunable_design_value is required");
        }
        String unitId = text(quantity, "unit", source, parameterPath);
        ScientificUnit unit = units.get(unitId);
        if (unit == null) {
          throw error(source, parameterPath + ".unit", "unsupported unit " + unitId);
        }
        ParameterBasis basis =
            cited
                ? ParameterBasis.cited(text(quantity, "citation_id", source, parameterPath))
                : ParameterBasis.tunable(
                    text(quantity, "tunable_design_value", source, parameterPath));
        parameters.put(
            entry.getKey(),
            new ScientificQuantity(number(quantity, "value", source, parameterPath), unit, basis));
      }
      result.add(
          new RegistryDefinition(
              text(item, "id", source, path),
              text(item, "schema_id", source, path),
              integer(item, "schema_version", source, path),
              enumValue(
                  DefinitionKind.class, text(item, "kind", source, path), source, path + ".kind"),
              text(item, "model_version", source, path),
              text(item, "confidence", source, path),
              stringList(item.get("dependencies"), source, path + ".dependencies"),
              stringList(item.get("citations"), source, path + ".citations"),
              parameters));
      index++;
    }
    return List.copyOf(result);
  }

  private static List<String> stringList(JsonNode node, String source, String path) {
    requireArray(node, source, path);
    List<String> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      if (!item.isString() || item.stringValue().isBlank()) {
        throw error(source, path + "[" + index + "]", "must be a non-blank string");
      }
      result.add(item.stringValue());
      index++;
    }
    return List.copyOf(result);
  }

  private static void requireObject(
      JsonNode node,
      String source,
      String path,
      Set<String> allowedFields,
      Set<String> requiredFields) {
    if (node == null || !node.isObject()) {
      throw error(source, path, "must be an object");
    }
    if (allowedFields != null) {
      for (String field : node.propertyNames()) {
        if (!allowedFields.contains(field)) {
          throw error(source, path + "." + field, "unknown field");
        }
      }
    }
    for (String field : requiredFields) {
      if (!node.hasNonNull(field)) {
        throw error(source, path + "." + field, "required field is missing or null");
      }
    }
  }

  private static void requireArray(JsonNode node, String source, String path) {
    if (node == null || !node.isArray()) {
      throw error(source, path, "must be an array");
    }
  }

  private static String text(JsonNode node, String field, String source, String path) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString() || value.stringValue().isBlank()) {
      throw error(source, path + "." + field, "must be a non-blank string");
    }
    return value.stringValue();
  }

  private static int integer(JsonNode node, String field, String source, String path) {
    JsonNode value = node.get(field);
    if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
      throw error(source, path + "." + field, "must be a 32-bit integer");
    }
    return value.intValue();
  }

  private static double number(JsonNode node, String field, String source, String path) {
    JsonNode value = node.get(field);
    if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
      throw error(source, path + "." + field, "must be a finite number");
    }
    return value.doubleValue();
  }

  private static <T extends Enum<T>> T enumValue(
      Class<T> type, String value, String source, String path) {
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException exception) {
      throw error(source, path, "unsupported " + type.getSimpleName() + " " + value);
    }
  }

  private static RegistryAuthoringException error(String source, String path, String message) {
    return new RegistryAuthoringException(source, path, message);
  }
}
