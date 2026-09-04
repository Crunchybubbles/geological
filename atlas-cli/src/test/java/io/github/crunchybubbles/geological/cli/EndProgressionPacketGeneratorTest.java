package io.github.crunchybubbles.geological.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndProgressionPacketGeneratorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesRepeatableEndProgressionArtifact() throws Exception {
    Path first =
        new EndProgressionPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("a"));
    Path second =
        new EndProgressionPacketGenerator(8_675_309L).generate(temporaryDirectory.resolve("b"));

    String firstJson = Files.readString(first);
    assertEquals(firstJson, Files.readString(second));
    assertTrue(firstJson.contains("phase8_end_progression_structure_protection_not_earth_geology"));
    assertTrue(firstJson.contains("EXIT_PORTAL"));
    assertTrue(firstJson.contains("DRAGON_ARENA"));
    assertTrue(firstJson.contains("END_GATEWAY"));
    assertTrue(firstJson.contains("OUTER_END_CITY"));
    assertTrue(firstJson.contains("CHORUS_HABITAT"));
    assertTrue(firstJson.contains("\"portalArrivalViable\": true"));
    assertTrue(firstJson.contains("\"dragonArenaProtected\": true"));
    assertTrue(firstJson.contains("\"topologyValid\": true"));
    assertTrue(firstJson.contains("\"centralArenaWriteBlocked\": true"));
    assertTrue(firstJson.contains("\"voidGapWriteAllowed\": true"));
    assertTrue(firstJson.contains("\"seamStable\": true"));
  }
}
