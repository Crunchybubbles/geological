package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.worldgen.ExplorationMapSnapshot;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebook;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebookEntry;
import io.github.crunchybubbles.geological.worldgen.NotebookEvidenceKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplorationNotebookTest {
  private static final String MODEL = "phase5-alpha.7";
  private static final String DIGEST =
      "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  private static final String DIMENSION = "minecraft:overworld";
  private static final StableId BODY = StableId.parse("00000000000000000000000000000001");

  @Test
  void notebookIsDeterministicBoundedAndMapDerived() {
    ExplorationNotebookEntry first = entry("00000000000000000000000000000002", 17, 32);
    ExplorationNotebookEntry second = entry("00000000000000000000000000000003", -18, -7);
    ExplorationNotebook left =
        ExplorationNotebook.empty(MODEL, DIGEST, DIMENSION).record(first).record(second);
    ExplorationNotebook right =
        ExplorationNotebook.empty(MODEL, DIGEST, DIMENSION).record(second).record(first);

    assertEquals(left, right);
    assertEquals(left.digest(), right.digest());
    assertEquals(2, left.entries().size());
    ExplorationMapSnapshot map = left.map(0, 0, 32);
    assertEquals(2, map.markers().size());
    assertEquals(2, map.markerIds().size());
    assertTrue(map.summary().contains("notebook-map"));
  }

  @Test
  void recordingSameEvidenceReplacesAndCapacityIsHard() {
    ExplorationNotebook notebook =
        ExplorationNotebook.empty(MODEL, DIGEST, DIMENSION)
            .record(entry("00000000000000000000000000000002", 1, 1));
    ExplorationNotebook replaced =
        notebook.record(
            new ExplorationNotebookEntry(
                StableId.parse("00000000000000000000000000000002"),
                NotebookEvidenceKind.OBSERVATION,
                StableId.parse("00000000000000000000000000000002"),
                DIMENSION,
                9,
                64,
                9,
                "updated evidence",
                "new note",
                List.of(BODY),
                900_000));
    assertEquals(1, replaced.entries().size());
    assertEquals(9, replaced.entries().getFirst().blockX());

    List<ExplorationNotebookEntry> additions = new ArrayList<>();
    for (int index = 0; index <= ExplorationNotebook.MAX_ENTRIES; index++) {
      String id = "%032x".formatted(index + 10L);
      additions.add(entry(id, index, index));
    }
    assertThrows(
        IllegalStateException.class,
        () -> ExplorationNotebook.empty(MODEL, DIGEST, DIMENSION).recordAll(additions));
  }

  private static ExplorationNotebookEntry entry(String id, long blockX, long blockZ) {
    StableId stableId = StableId.parse(id);
    return new ExplorationNotebookEntry(
        stableId,
        NotebookEvidenceKind.OBSERVATION,
        stableId,
        DIMENSION,
        blockX,
        70,
        blockZ,
        "visible evidence",
        "",
        List.of(BODY),
        500_000);
  }
}
