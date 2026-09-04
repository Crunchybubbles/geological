package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebookEntry;
import io.github.crunchybubbles.geological.worldgen.NotebookEvidenceKind;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class GeologicalDiscoverySavedDataTest {
  @Test
  void savedDataRoundTripsOnlyBoundedNotebookEvidence() {
    GeologicalDiscoverySavedData data = GeologicalDiscoverySavedData.load(new CompoundTag(), null);
    UUID player = UUID.fromString("00000000-0000-0000-0000-000000000042");
    StableId evidence = StableId.parse("00000000000000000000000000000009");
    ExplorationNotebookEntry entry =
        new ExplorationNotebookEntry(
            evidence,
            NotebookEvidenceKind.OBSERVATION,
            evidence,
            "minecraft:overworld",
            -31,
            72,
            47,
            "outcrop visible",
            "check the contact",
            List.of(StableId.parse("00000000000000000000000000000001")),
            750_000);

    data.record(player, List.of(entry));
    assertTrue(data.isDirty());
    CompoundTag saved = data.save(new CompoundTag(), null);
    GeologicalDiscoverySavedData restored = GeologicalDiscoverySavedData.load(saved, null);

    assertEquals(data.notebook(player), restored.notebook(player));
    assertEquals(1, restored.playerCount());
    assertEquals(1, saved.getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
    assertTrue(saved.getString("scientific_digest").startsWith("sha256:"));
  }
}
