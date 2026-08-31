package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class JsonWriterTest {
  @Test
  void objectKeysAndEscapesAreDeterministic() {
    String json =
        JsonWriter.stringify(JsonWriter.object("z", List.of(2, 3), "a", "line\n\"quoted\""));
    assertEquals(
        "{\n"
            + "  \"a\": \"line\\n\\\"quoted\\\"\",\n"
            + "  \"z\": [\n"
            + "    2,\n"
            + "    3\n"
            + "  ]\n"
            + "}",
        json);
  }
}
