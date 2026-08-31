package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.crunchybubbles.geological.determinism.CanonicalCbor;
import io.github.crunchybubbles.geological.determinism.ObjectRandomStream;
import io.github.crunchybubbles.geological.determinism.RandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class DeterminismGoldenTest {
  @Test
  void canonicalCborAndHmacOutputsMatchGoldenVectors() {
    byte[] encoded = CanonicalCbor.encodeTuple("geological:test", 0L, -1L, "é");
    assertEquals("846f67656f6c6f676963616c3a74657374002062c3a9", HexFormat.of().formatHex(encoded));

    WorldIdentity world =
        new WorldIdentity(
            123_456_789L,
            "phase0.1",
            "geological:phase0-scientific-v1",
            "geological:overworld_phase0");
    RandomStream stream = world.stream("geological", "province", new CellKey("province", -2, 7), 3);
    assertEquals("8ed2b1efdfdf606e530791ad205e0104", stream.stableId().toString());
    assertEquals(
        "9c3ddd70a092a03316e29a30ff59b7721a072a84f0ac57dcc9523a4cd0cf0ccc",
        HexFormat.of().formatHex(stream.bytes("golden", 42)));
    assertEquals(0.61031898498238, stream.unitDouble("golden", 42));
    assertEquals(2, stream.boundedInt("bounded", 9, 17));
  }

  @Test
  void purposesAndCountersAreDomainSeparatedWithoutConsumptionState() {
    WorldIdentity world = new WorldIdentity(1L, "m", "digest", "geological:overworld_phase0");
    RandomStream stream = world.stream("geological", "body", new CellKey("district", 4, -5), 9);
    double first = stream.unitDouble("shape", 12);
    stream.unitDouble("unrelated-new-purpose", 0);
    assertEquals(first, stream.unitDouble("shape", 12));
    assertNotEquals(first, stream.unitDouble("shape", 13));
    assertNotEquals(first, stream.unitDouble("grade", 12));
  }

  @Test
  void objectKeyedStreamsMatchGoldenVectorsAndDoNotConsumeState() {
    WorldIdentity world =
        new WorldIdentity(42L, "phase2-test", "digest", "geological:overworld_phase2");
    ObjectRandomStream stream =
        world.objectStream(
            "geological",
            "body_modal_composition",
            StableId.parse("00112233445566778899aabbccddeeff"));

    assertEquals(
        "f8c65fe46254fd1b91c23145ea3ffa1bc8246466a4c790f78362018aa6350c34",
        HexFormat.of().formatHex(stream.bytes("mode:quartz", 7)));
    assertEquals(0.9717769558504478, stream.unitDouble("mode:quartz", 7));
    assertEquals(15, stream.boundedInt("choice", 3, 19));
    double first = stream.unitDouble("stable", 2);
    stream.unitDouble("unrelated", 0);
    assertEquals(first, stream.unitDouble("stable", 2));
  }
}
