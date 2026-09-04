package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.OverworldGenerationBenchmark;
import org.junit.jupiter.api.Test;

class OverworldGenerationBenchmarkTest {
  @Test
  void serialAndShuffledGenerationShareAStableSeamSafeResult() {
    OverworldGenerationBenchmark.Report report =
        OverworldGenerationBenchmark.run(8_675_309L, -11, 17, 2);

    assertEquals(256, report.columnsPerChunk());
    assertTrue(report.generationOrderStable());
    assertTrue(report.warmResultStable());
    assertTrue(report.seamStable());
    assertEquals(32, report.seamColumnsChecked());
    assertTrue(report.warmNanosP95() >= report.warmNanosP50());
    assertTrue(report.stableSignatureHex().matches("[0-9a-f]+"));
  }
}
