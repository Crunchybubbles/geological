package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.registry.RegistryAuthoringException;
import io.github.crunchybubbles.geological.registry.RegistryJsonLoader;
import io.github.crunchybubbles.geological.registry.RegistrySnapshot;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RegistryJsonLoaderTest {
  private static final String RESOURCE = "/data/geological/registry/phase1-scientific.json";

  @Test
  void publicAuthoredResourceRecompilesToTheFrozenSnapshot() {
    RegistrySnapshot loaded =
        new RegistryJsonLoader().loadResource(RegistryJsonLoaderTest.class, RESOURCE);

    assertEquals(Phase1World.scientificSnapshot().canonicalJson(), loaded.canonicalJson());
    assertEquals(Phase1World.SCIENTIFIC_DIGEST, loaded.digest());
  }

  @Test
  void insignificantWhitespaceDoesNotChangeEffectiveIdentity() {
    String source = source();
    RegistrySnapshot compact = load(source);
    RegistrySnapshot padded = load("\n\t  " + source + "  \r\n");

    assertEquals(compact.canonicalJson(), padded.canonicalJson());
    assertEquals(compact.digest(), padded.digest());
  }

  @Test
  void duplicateAndUnknownFieldsAreRejectedBeforeCompilation() {
    String source = source();
    RegistryAuthoringException duplicate =
        assertThrows(
            RegistryAuthoringException.class,
            () ->
                load(
                    source.replaceFirst(
                        "\"authoring_schema\"",
                        "\"authoring_schema\": \"geological:registry_authoring:v1\","
                            + " \"authoring_schema\"")));
    RegistryAuthoringException unknown =
        assertThrows(
            RegistryAuthoringException.class,
            () -> load(source.replaceFirst("\\{", "{\"surprise\": true,")));

    assertTrue(duplicate.getMessage().contains("Duplicate Object property"));
    assertTrue(unknown.getMessage().contains("$.surprise: unknown field"));
  }

  @Test
  void authoredUnitVocabularyCannotDriftFromRuntimeSemantics() {
    RegistryAuthoringException exception =
        assertThrows(
            RegistryAuthoringException.class,
            () -> load(source().replaceFirst("\"symbol\": \"block\"", "\"symbol\": \"m\"")));

    assertTrue(exception.getMessage().contains("$.units[0].symbol"));
    assertTrue(exception.getMessage().contains("supported unit vocabulary"));
  }

  @Test
  void trailingDocumentsAreRejected() {
    RegistryAuthoringException exception =
        assertThrows(RegistryAuthoringException.class, () -> load(source() + "{}"));

    assertTrue(exception.getMessage().contains("invalid JSON"));
  }

  private static RegistrySnapshot load(String json) {
    return new RegistryJsonLoader()
        .load(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
            "inline-registry.json");
  }

  private static String source() {
    try (InputStream input = RegistryJsonLoaderTest.class.getResourceAsStream(RESOURCE)) {
      if (input == null) {
        throw new AssertionError("missing test registry resource");
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new AssertionError("could not read test registry resource", exception);
    }
  }
}
