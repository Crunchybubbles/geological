package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.worldgen.OverworldGenerationBenchmark;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Writes the Phase 4 Overworld generation-order/seam/server observation artifact. */
final class WorldgenBenchmarkPacketGenerator {
  private final long seed;

  WorldgenBenchmarkPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    OverworldGenerationBenchmark.Report report = OverworldGenerationBenchmark.run(seed, -11, 17, 4);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase4_server_worldgen_engineering_observation_not_microbenchmark",
            "worldSeed",
            report.worldSeed(),
            "targetChunk",
            JsonWriter.object("chunkX", report.chunkX(), "chunkZ", report.chunkZ()),
            "columnsPerChunk",
            report.columnsPerChunk(),
            "warmIterations",
            report.warmIterations(),
            "shuffleSeed",
            report.shuffleSeed(),
            "generationOrderStable",
            report.generationOrderStable(),
            "warmResultStable",
            report.warmResultStable(),
            "seamStable",
            report.seamStable(),
            "seamColumnsChecked",
            report.seamColumnsChecked(),
            "coldNanos",
            report.coldNanos(),
            "shuffledNanos",
            report.shuffledNanos(),
            "warmNanosP50",
            report.warmNanosP50(),
            "warmNanosP95",
            report.warmNanosP95(),
            "stableSignatureHex",
            report.stableSignatureHex(),
            "javaRuntime",
            System.getProperty("java.runtime.version"),
            "os",
            System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            "processors",
            Runtime.getRuntime().availableProcessors());
    Path reportPath = outputDirectory.resolve("worldgen-benchmark.json");
    JsonWriter.write(reportPath, json);
    return reportPath;
  }
}
