package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Strict JSON boundary for the first typed mineral, rock, and alteration data pack. */
public final class MaterialCatalogJsonLoader {
  public static final String AUTHORING_SCHEMA = "geological:material_catalog_authoring:v1";
  private static final int MAX_DOCUMENT_BYTES = 1_048_576;
  private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  public MaterialCatalogSnapshot loadResource(Class<?> anchor, String resourceName) {
    if (anchor == null || resourceName == null || resourceName.isBlank()) {
      throw new IllegalArgumentException("resource anchor and name are required");
    }
    try (InputStream input = anchor.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw error(resourceName, "$", "classpath resource does not exist");
      }
      return load(input, resourceName);
    } catch (IOException exception) {
      throw new MaterialCatalogAuthoringException(
          resourceName, "could not close material catalog resource", exception);
    }
  }

  public MaterialCatalogSnapshot load(InputStream input, String sourceName) {
    if (input == null || sourceName == null || sourceName.isBlank()) {
      throw new IllegalArgumentException("catalog input and source name are required");
    }
    try {
      byte[] document = input.readNBytes(MAX_DOCUMENT_BYTES + 1);
      if (document.length > MAX_DOCUMENT_BYTES) {
        throw error(sourceName, "$", "document exceeds the 1 MiB authoring limit");
      }
      JsonNode root = JSON.readTree(document);
      Set<String> rootFields =
          Set.of("authoring_schema", "evidence", "minerals", "overprints", "rocks");
      requireObject(root, sourceName, "$", rootFields, rootFields);
      if (!AUTHORING_SCHEMA.equals(text(root, "authoring_schema", sourceName, "$"))) {
        throw error(sourceName, "$.authoring_schema", "unsupported authoring schema");
      }
      CatalogEvidence evidence = parseEvidence(root.get("evidence"), sourceName);
      List<MineralDefinition> minerals = parseMinerals(root.get("minerals"), sourceName);
      List<RockDefinition> rocks = parseRocks(root.get("rocks"), sourceName);
      List<AlterationDefinition> alterations = parseAlterations(root.get("overprints"), sourceName);
      String canonical = canonicalJson(evidence, minerals, rocks, alterations);
      return new MaterialCatalogSnapshot(
          "sha256:" + sha256(canonical), canonical, evidence, minerals, rocks, alterations);
    } catch (MaterialCatalogAuthoringException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw new MaterialCatalogAuthoringException(
          sourceName, "invalid JSON: " + exception.getMessage(), exception);
    } catch (IOException exception) {
      throw new MaterialCatalogAuthoringException(sourceName, "could not read catalog", exception);
    } catch (IllegalArgumentException exception) {
      throw new MaterialCatalogAuthoringException(sourceName, "$", exception.getMessage());
    }
  }

  private static CatalogEvidence parseEvidence(JsonNode node, String source) {
    String path = "$.evidence";
    Set<String> fields =
        Set.of("citation_id", "parameter_basis", "publication_year", "title", "uri");
    requireObject(node, source, path, fields, fields);
    try {
      return new CatalogEvidence(
          text(node, "citation_id", source, path),
          text(node, "title", source, path),
          URI.create(text(node, "uri", source, path)),
          integer(node, "publication_year", source, path),
          text(node, "parameter_basis", source, path));
    } catch (IllegalArgumentException exception) {
      throw error(source, path, exception.getMessage());
    }
  }

  private static List<MineralDefinition> parseMinerals(JsonNode node, String source) {
    requireArray(node, source, "$.minerals");
    List<MineralDefinition> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.minerals[" + index + "]";
      Set<String> fields =
          Set.of("density_g_cm3", "formula", "hardness_mohs", "id", "weathering_resistance");
      requireObject(item, source, path, fields, fields);
      String id = identifier(text(item, "id", source, path), source, path + ".id");
      JsonNode formulaNode = item.get("formula");
      requireObject(formulaNode, source, path + ".formula", null, Set.of());
      EnumMap<ChemicalElement, Integer> formula = new EnumMap<>(ChemicalElement.class);
      for (Map.Entry<String, JsonNode> entry : formulaNode.properties()) {
        ChemicalElement element;
        try {
          element = ChemicalElement.fromSymbol(entry.getKey());
        } catch (IllegalArgumentException exception) {
          throw error(source, path + ".formula." + entry.getKey(), exception.getMessage());
        }
        if (!entry.getValue().isIntegralNumber()
            || !entry.getValue().canConvertToInt()
            || entry.getValue().intValue() <= 0) {
          throw error(source, path + ".formula." + entry.getKey(), "must be a positive integer");
        }
        formula.put(element, entry.getValue().intValue());
      }
      result.add(
          new MineralDefinition(
              id,
              formula,
              number(item, "density_g_cm3", source, path),
              number(item, "hardness_mohs", source, path),
              number(item, "weathering_resistance", source, path)));
      index++;
    }
    return List.copyOf(result);
  }

  private static List<RockDefinition> parseRocks(JsonNode node, String source) {
    requireArray(node, source, "$.rocks");
    List<RockDefinition> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.rocks[" + index + "]";
      Set<String> fields =
          Set.of(
              "erodibility_index",
              "genetic_family",
              "id",
              "lithology",
              "modal_spread_fraction",
              "mineral_modes_ppm",
              "permeability_index",
              "porosity_fraction");
      requireObject(item, source, path, fields, fields);
      result.add(
          new RockDefinition(
              identifier(text(item, "id", source, path), source, path + ".id"),
              enumValue(
                  Lithology.class,
                  text(item, "lithology", source, path),
                  source,
                  path + ".lithology"),
              enumValue(
                  GeneticFamily.class,
                  text(item, "genetic_family", source, path),
                  source,
                  path + ".genetic_family"),
              modes(item.get("mineral_modes_ppm"), source, path + ".mineral_modes_ppm", false),
              number(item, "modal_spread_fraction", source, path),
              number(item, "porosity_fraction", source, path),
              number(item, "permeability_index", source, path),
              number(item, "erodibility_index", source, path)));
      index++;
    }
    return List.copyOf(result);
  }

  private static List<AlterationDefinition> parseAlterations(JsonNode node, String source) {
    requireArray(node, source, "$.overprints");
    List<AlterationDefinition> result = new ArrayList<>();
    int index = 0;
    for (JsonNode item : node) {
      String path = "$.overprints[" + index + "]";
      Set<String> fields =
          Set.of(
              "erodibility_delta",
              "facies",
              "overprint",
              "path",
              "peak_pressure_mpa",
              "peak_temperature_c",
              "porosity_multiplier",
              "process_class",
              "replacement_ppm",
              "target_modes_ppm");
      requireObject(item, source, path, fields, fields);
      long replacement = longInteger(item, "replacement_ppm", source, path);
      MineralAssemblage target =
          modes(item.get("target_modes_ppm"), source, path + ".target_modes_ppm", true);
      if (replacement == 0) {
        if (target != null) {
          throw error(source, path + ".target_modes_ppm", "must be empty when replacement is zero");
        }
      } else if (target == null) {
        throw error(source, path + ".target_modes_ppm", "must close when replacement is non-zero");
      }
      double[] temperature = interval(item.get("peak_temperature_c"), source, path);
      double[] pressure = interval(item.get("peak_pressure_mpa"), source, path);
      result.add(
          new AlterationDefinition(
              enumValue(
                  Overprint.class,
                  text(item, "overprint", source, path),
                  source,
                  path + ".overprint"),
              enumValue(
                  MaterialProcessClass.class,
                  text(item, "process_class", source, path),
                  source,
                  path + ".process_class"),
              replacement,
              target,
              enumValue(
                  MetamorphicFacies.class,
                  text(item, "facies", source, path),
                  source,
                  path + ".facies"),
              enumValue(
                  MetamorphicPath.class, text(item, "path", source, path), source, path + ".path"),
              temperature[0],
              temperature[1],
              pressure[0],
              pressure[1],
              number(item, "porosity_multiplier", source, path),
              number(item, "erodibility_delta", source, path)));
      index++;
    }
    return List.copyOf(result);
  }

  private static MineralAssemblage modes(
      JsonNode node, String source, String path, boolean allowEmpty) {
    requireObject(node, source, path, null, Set.of());
    if (node.isEmpty()) {
      if (allowEmpty) {
        return null;
      }
      throw error(source, path, "must contain mineral modes");
    }
    TreeMap<String, Long> result = new TreeMap<>();
    for (Map.Entry<String, JsonNode> entry : node.properties()) {
      String id = identifier(entry.getKey(), source, path + "." + entry.getKey());
      JsonNode value = entry.getValue();
      if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
        throw error(source, path + "." + entry.getKey(), "must be a non-negative integer");
      }
      result.put(id, value.longValue());
    }
    return new MineralAssemblage(result);
  }

  private static double[] interval(JsonNode node, String source, String path) {
    requireArray(node, source, path);
    if (node.size() != 2
        || !node.get(0).isNumber()
        || !node.get(1).isNumber()
        || !Double.isFinite(node.get(0).doubleValue())
        || !Double.isFinite(node.get(1).doubleValue())) {
      throw error(source, path, "must contain exactly two finite numbers");
    }
    return new double[] {node.get(0).doubleValue(), node.get(1).doubleValue()};
  }

  private static String canonicalJson(
      CatalogEvidence evidence,
      List<MineralDefinition> minerals,
      List<RockDefinition> rocks,
      List<AlterationDefinition> alterations) {
    StringBuilder output = new StringBuilder();
    output.append(
        "{\"canonical_schema\":\"geological:material_catalog_snapshot:v1\",\"evidence\":{");
    field(output, "citation_id", evidence.citationId());
    output.append(',');
    field(output, "parameter_basis", evidence.parameterBasis());
    output.append(',');
    field(output, "publication_year", evidence.publicationYear());
    output.append(',');
    field(output, "title", evidence.title());
    output.append(',');
    field(output, "uri", evidence.uri().toASCIIString());
    output.append("},\"minerals\":[");
    List<MineralDefinition> sortedMinerals =
        minerals.stream().sorted(java.util.Comparator.comparing(MineralDefinition::id)).toList();
    appendSeparated(
        output,
        sortedMinerals,
        mineral -> {
          output.append('{');
          field(output, "density_g_cm3", mineral.densityGramsPerCubicCentimeter());
          output.append(",\"formula\":{");
          appendSeparated(
              output,
              mineral.formula().entrySet().stream()
                  .sorted(
                      Map.Entry.comparingByKey(
                          java.util.Comparator.comparing(ChemicalElement::symbol)))
                  .toList(),
              entry -> {
                string(output, entry.getKey().symbol());
                output.append(':').append(entry.getValue());
              });
          output.append("},");
          field(output, "hardness_mohs", mineral.hardnessMohs());
          output.append(',');
          field(output, "id", mineral.id());
          output.append(',');
          field(output, "weathering_resistance", mineral.weatheringResistance());
          output.append('}');
        });
    output.append("],\"overprints\":[");
    List<AlterationDefinition> sortedAlterations =
        alterations.stream()
            .sorted(java.util.Comparator.comparing(alteration -> alteration.overprint().name()))
            .toList();
    appendSeparated(
        output,
        sortedAlterations,
        alteration -> {
          output.append('{');
          field(output, "erodibility_delta", alteration.erodibilityDelta());
          output.append(',');
          field(output, "facies", alteration.facies().name());
          output.append(',');
          field(output, "overprint", alteration.overprint().name());
          output.append(',');
          field(output, "path", alteration.path().name());
          output
              .append(",\"peak_pressure_mpa\":[")
              .append(Double.toString(alteration.minimumPressureMpa()))
              .append(',')
              .append(Double.toString(alteration.maximumPressureMpa()))
              .append("],\"peak_temperature_c\":[")
              .append(Double.toString(alteration.minimumTemperatureCelsius()))
              .append(',')
              .append(Double.toString(alteration.maximumTemperatureCelsius()))
              .append("],");
          field(output, "porosity_multiplier", alteration.porosityMultiplier());
          output.append(',');
          field(output, "process_class", alteration.processClass().name());
          output.append(',');
          field(output, "replacement_ppm", alteration.replacementPpm());
          output.append(",\"target_modes_ppm\":");
          appendModes(output, alteration.targetAssemblage());
          output.append('}');
        });
    output.append("],\"rocks\":[");
    List<RockDefinition> sortedRocks =
        rocks.stream().sorted(java.util.Comparator.comparing(RockDefinition::id)).toList();
    appendSeparated(
        output,
        sortedRocks,
        rock -> {
          output.append('{');
          field(output, "erodibility_index", rock.erodibilityIndex());
          output.append(',');
          field(output, "genetic_family", rock.geneticFamily().name());
          output.append(',');
          field(output, "id", rock.id());
          output.append(',');
          field(output, "lithology", rock.lithology().name());
          output.append(',');
          field(output, "modal_spread_fraction", rock.modalSpreadFraction());
          output.append(",\"mineral_modes_ppm\":");
          appendModes(output, rock.primaryAssemblage());
          output.append(',');
          field(output, "permeability_index", rock.permeabilityIndex());
          output.append(',');
          field(output, "porosity_fraction", rock.porosityFraction());
          output.append('}');
        });
    output.append("]}");
    return Normalizer.normalize(output, Normalizer.Form.NFC);
  }

  private static void appendModes(StringBuilder output, MineralAssemblage assemblage) {
    output.append('{');
    if (assemblage != null) {
      appendSeparated(
          output,
          assemblage.modesPpm().entrySet().stream().toList(),
          entry -> {
            string(output, entry.getKey());
            output.append(':').append(entry.getValue());
          });
    }
    output.append('}');
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

  private static String identifier(String value, String source, String path) {
    if (!IDENTIFIER.matcher(value).matches()) {
      throw error(source, path, "must be a namespaced stable ID");
    }
    return value;
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

  private static long longInteger(JsonNode node, String field, String source, String path) {
    JsonNode value = node.get(field);
    if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
      throw error(source, path + "." + field, "must be a 64-bit integer");
    }
    return value.longValue();
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

  private static void field(StringBuilder output, String name, long value) {
    string(output, name);
    output.append(':').append(value);
  }

  private static void field(StringBuilder output, String name, double value) {
    string(output, name);
    output.append(':').append(Double.toString(value));
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

  private static MaterialCatalogAuthoringException error(
      String source, String path, String message) {
    return new MaterialCatalogAuthoringException(source, path, message);
  }
}
