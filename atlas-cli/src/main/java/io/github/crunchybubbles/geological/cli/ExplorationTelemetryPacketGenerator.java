package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.worldgen.ExplorationTelemetryReport;
import io.github.crunchybubbles.geological.worldgen.ExplorationTelemetryRequest;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationTelemetry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Writes the Phase 5 clue-sufficiency/travel-burden engineering observation artifact. */
final class ExplorationTelemetryPacketGenerator {
  private final long seed;

  ExplorationTelemetryPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    ExplorationTelemetryRequest request = new ExplorationTelemetryRequest(-176, 272, 64, 16);
    ExplorationTelemetryReport report = OverworldExplorationTelemetry.run(seed, request);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase5_exploration_clue_telemetry_engineering_observation_not_microbenchmark",
            "worldSeed",
            seed,
            "center",
            JsonWriter.object("x", report.centerX(), "z", report.centerZ()),
            "radiusBlocks",
            report.radiusBlocks(),
            "spacingBlocks",
            report.spacingBlocks(),
            "cellsVisited",
            report.cellsVisited(),
            "observationCells",
            report.observationCells(),
            "handSampleCells",
            report.handSampleCells(),
            "sampledCells",
            report.sampledCells(),
            "anomalyCells",
            report.anomalyCells(),
            "detectedAnomalyCells",
            report.detectedAnomalyCells(),
            "anomalousCells",
            report.anomalousCells(),
            "hypothesisCells",
            report.hypothesisCells(),
            "nearestObservationDistanceBlocks",
            report.nearestObservationDistanceBlocks(),
            "nearestSampleDistanceBlocks",
            report.nearestSampleDistanceBlocks(),
            "nearestAnomalyDistanceBlocks",
            report.nearestAnomalyDistanceBlocks(),
            "travelBurdenBlocks",
            report.travelBurdenBlocks(),
            "travelBurdenPpm",
            report.travelBurdenPpm(),
            "clueSufficiencyPpm",
            report.clueSufficiencyPpm(),
            "observationKindCounts",
            report.observationKindCounts(),
            "sampleKindCounts",
            report.sampleKindCounts(),
            "reportId",
            report.reportId().toString(),
            "digest",
            report.digest(),
            "javaRuntime",
            System.getProperty("java.runtime.version"),
            "os",
            System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            "processors",
            Runtime.getRuntime().availableProcessors());
    Path reportPath = outputDirectory.resolve("exploration-telemetry.json");
    JsonWriter.write(reportPath, json);
    return reportPath;
  }
}
