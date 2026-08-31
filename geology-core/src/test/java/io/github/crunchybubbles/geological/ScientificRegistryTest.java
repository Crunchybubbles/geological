package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.registry.Citation;
import io.github.crunchybubbles.geological.registry.RegistryDefinition;
import io.github.crunchybubbles.geological.registry.RegistrySnapshot;
import io.github.crunchybubbles.geological.registry.RegistryValidationException;
import io.github.crunchybubbles.geological.registry.ScientificQuantity;
import io.github.crunchybubbles.geological.registry.ScientificRegistryCompiler;
import io.github.crunchybubbles.geological.registry.ScientificUnit;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ScientificRegistryTest {
  @Test
  void canonicalSnapshotIsOrderIndependentAndUsesFullSha256Identity() {
    RegistrySnapshot snapshot = Phase1World.scientificSnapshot();
    List<io.github.crunchybubbles.geological.registry.Citation> citations =
        reversed(snapshot.citations());
    List<io.github.crunchybubbles.geological.registry.DefinitionSchema> schemas =
        reversed(snapshot.schemas());
    List<RegistryDefinition> definitions = reversed(snapshot.definitions());

    RegistrySnapshot reordered =
        new ScientificRegistryCompiler().compile(citations, schemas, definitions);

    assertEquals(snapshot.canonicalJson(), reordered.canonicalJson());
    assertEquals(snapshot.digest(), reordered.digest());
    assertEquals(
        "sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4",
        snapshot.digest());
    assertTrue(snapshot.digest().matches("sha256:[0-9a-f]{64}"));
    assertEquals(snapshot.digest(), Phase1World.SCIENTIFIC_DIGEST);
  }

  @Test
  void invalidDimensionsFailWithAPreciseSchemaPath() {
    RegistrySnapshot snapshot = Phase1World.scientificSnapshot();
    RegistryDefinition original =
        snapshot.requireDefinition("geological:kernel/stratigraphic_package_v1");
    Map<String, ScientificQuantity> parameters = new HashMap<>(original.parameters());
    ScientificQuantity thickness = parameters.get("maximum_thickness");
    parameters.put(
        "maximum_thickness",
        new ScientificQuantity(thickness.value(), ScientificUnit.ONE, thickness.basis()));
    RegistryDefinition invalid =
        new RegistryDefinition(
            original.id(),
            original.schemaId(),
            original.schemaVersion(),
            original.kind(),
            original.modelVersion(),
            original.confidence(),
            original.dependencies(),
            original.citations(),
            parameters);
    List<RegistryDefinition> definitions = new ArrayList<>(snapshot.definitions());
    definitions.set(definitions.indexOf(original), invalid);

    RegistryValidationException exception =
        assertThrows(
            RegistryValidationException.class,
            () ->
                new ScientificRegistryCompiler()
                    .compile(snapshot.citations(), snapshot.schemas(), definitions));

    assertTrue(
        exception.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic
                        .path()
                        .equals(
                            "definitions[geological:kernel/stratigraphic_package_v1]"
                                + ".parameters.maximum_thickness")));
  }

  @Test
  void changingEffectiveScientificContentChangesWorldObjectIdentity() {
    RegistrySnapshot snapshot = Phase1World.scientificSnapshot();
    RegistryDefinition original =
        snapshot.requireDefinition("geological:kernel/stratigraphic_package_v1");
    Map<String, ScientificQuantity> parameters = new HashMap<>(original.parameters());
    ScientificQuantity thickness = parameters.get("maximum_thickness");
    parameters.put(
        "maximum_thickness", new ScientificQuantity(206.0, thickness.unit(), thickness.basis()));
    RegistryDefinition changed =
        new RegistryDefinition(
            original.id(),
            original.schemaId(),
            original.schemaVersion(),
            original.kind(),
            original.modelVersion(),
            original.confidence(),
            original.dependencies(),
            original.citations(),
            parameters);
    List<RegistryDefinition> definitions = new ArrayList<>(snapshot.definitions());
    definitions.set(definitions.indexOf(original), changed);
    RegistrySnapshot changedSnapshot =
        new ScientificRegistryCompiler()
            .compile(snapshot.citations(), snapshot.schemas(), definitions);

    WorldIdentity baseline =
        new WorldIdentity(
            99L, Phase1World.MODEL_VERSION, snapshot.digest(), "geological:overworld_phase1");
    WorldIdentity modified =
        new WorldIdentity(
            99L,
            Phase1World.MODEL_VERSION,
            changedSnapshot.digest(),
            "geological:overworld_phase1");

    assertNotEquals(snapshot.digest(), changedSnapshot.digest());
    assertNotEquals(
        baseline.stream("geological", "province", new CellKey("province", 0, 0), 0).stableId(),
        modified.stream("geological", "province", new CellKey("province", 0, 0), 0).stableId());
  }

  @Test
  void dependencyCyclesAndUnsafeCitationUrisAreRejected() {
    RegistrySnapshot snapshot = Phase1World.scientificSnapshot();
    RegistryDefinition original =
        snapshot.requireDefinition("geological:kernel/stratigraphic_package_v1");
    RegistryDefinition cyclic =
        new RegistryDefinition(
            original.id(),
            original.schemaId(),
            original.schemaVersion(),
            original.kind(),
            original.modelVersion(),
            original.confidence(),
            List.of("geological:scale/phase1_column_v1"),
            original.citations(),
            original.parameters());
    List<RegistryDefinition> definitions = new ArrayList<>(snapshot.definitions());
    definitions.set(definitions.indexOf(original), cyclic);

    RegistryValidationException cycle =
        assertThrows(
            RegistryValidationException.class,
            () ->
                new ScientificRegistryCompiler()
                    .compile(snapshot.citations(), snapshot.schemas(), definitions));
    assertTrue(
        cycle.diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.message().equals("dependency cycle detected")));

    Citation originalCitation = snapshot.citations().getFirst();
    Citation unsafeCitation =
        new Citation(
            originalCitation.id(),
            originalCitation.title(),
            URI.create("http://example.invalid/reference"),
            originalCitation.publicationYear());
    List<Citation> citations = new ArrayList<>(snapshot.citations());
    citations.set(0, unsafeCitation);
    RegistryValidationException unsafe =
        assertThrows(
            RegistryValidationException.class,
            () ->
                new ScientificRegistryCompiler()
                    .compile(citations, snapshot.schemas(), snapshot.definitions()));
    assertTrue(
        unsafe.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.path().endsWith(".uri")
                        && diagnostic.message().equals("citation URI must use HTTPS")));
  }

  @Test
  void canonicalCompilationSurvivesDeterministicAuthoringOrderFuzz() {
    RegistrySnapshot expected = Phase1World.scientificSnapshot();
    for (long seed = 0; seed < 64; seed++) {
      Random random = new Random(seed);
      List<Citation> citations = new ArrayList<>(expected.citations());
      List<io.github.crunchybubbles.geological.registry.DefinitionSchema> schemas =
          new ArrayList<>(expected.schemas());
      List<RegistryDefinition> definitions = new ArrayList<>(expected.definitions());
      Collections.shuffle(citations, random);
      Collections.shuffle(schemas, random);
      Collections.shuffle(definitions, random);

      RegistrySnapshot actual =
          new ScientificRegistryCompiler().compile(citations, schemas, definitions);

      assertEquals(expected.digest(), actual.digest(), "replay_seed=" + seed);
      assertEquals(expected.canonicalJson(), actual.canonicalJson(), "replay_seed=" + seed);
    }
  }

  private static <T> List<T> reversed(List<T> input) {
    List<T> result = new ArrayList<>(input);
    Collections.reverse(result);
    return result;
  }
}
