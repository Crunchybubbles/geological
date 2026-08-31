package io.github.crunchybubbles.geological.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Dependency-free deterministic JSON writer for review artifacts. */
final class JsonWriter {
  private JsonWriter() {}

  static Map<String, Object> object(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("JSON object requires key/value pairs");
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      if (!(keyValues[index] instanceof String key)) {
        throw new IllegalArgumentException("JSON object keys must be strings");
      }
      result.put(key, keyValues[index + 1]);
    }
    return result;
  }

  static List<Object> array(Object... values) {
    return new ArrayList<>(List.of(values));
  }

  static void write(Path path, Object value) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, stringify(value) + System.lineSeparator(), StandardCharsets.UTF_8);
  }

  static String stringify(Object value) {
    StringBuilder output = new StringBuilder();
    append(output, value, 0);
    return output.toString();
  }

  private static void append(StringBuilder output, Object value, int indentation) {
    switch (value) {
      case null -> output.append("null");
      case String text -> appendString(output, text);
      case Boolean bool -> output.append(bool);
      case Byte number -> output.append(number);
      case Short number -> output.append(number);
      case Integer number -> output.append(number);
      case Long number -> output.append(number);
      case Float number -> appendFiniteNumber(output, number.doubleValue());
      case Double number -> appendFiniteNumber(output, number);
      case Map<?, ?> map -> appendMap(output, map, indentation);
      case Iterable<?> values -> appendIterable(output, values, indentation);
      default ->
          throw new IllegalArgumentException(
              "Unsupported JSON value type: " + value.getClass().getName());
    }
  }

  private static void appendMap(StringBuilder output, Map<?, ?> map, int indentation) {
    TreeMap<String, Object> sorted = new TreeMap<>();
    map.forEach(
        (key, value) -> {
          if (!(key instanceof String text)) {
            throw new IllegalArgumentException("JSON object keys must be strings");
          }
          sorted.put(text, value);
        });
    output.append('{');
    if (!sorted.isEmpty()) {
      output.append('\n');
      int index = 0;
      for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        indent(output, indentation + 1);
        appendString(output, entry.getKey());
        output.append(": ");
        append(output, entry.getValue(), indentation + 1);
        if (++index < sorted.size()) {
          output.append(',');
        }
        output.append('\n');
      }
      indent(output, indentation);
    }
    output.append('}');
  }

  private static void appendIterable(StringBuilder output, Iterable<?> values, int indentation) {
    List<Object> collected = new ArrayList<>();
    values.forEach(collected::add);
    output.append('[');
    if (!collected.isEmpty()) {
      output.append('\n');
      for (int index = 0; index < collected.size(); index++) {
        indent(output, indentation + 1);
        append(output, collected.get(index), indentation + 1);
        if (index + 1 < collected.size()) {
          output.append(',');
        }
        output.append('\n');
      }
      indent(output, indentation);
    }
    output.append(']');
  }

  private static void appendString(StringBuilder output, String value) {
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
            output.append(String.format("\\u%04x", (int) character));
          } else {
            output.append(character);
          }
        }
      }
    }
    output.append('"');
  }

  private static void appendFiniteNumber(StringBuilder output, double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("JSON cannot represent non-finite numbers");
    }
    output.append(Double.toString(value));
  }

  private static void indent(StringBuilder output, int indentation) {
    output.append("  ".repeat(indentation));
  }
}
