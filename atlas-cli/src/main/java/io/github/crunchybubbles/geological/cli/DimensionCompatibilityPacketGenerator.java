package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.worldgen.DimensionCompatibilityReview;
import io.github.crunchybubbles.geological.worldgen.DimensionWorldgenTrace;
import io.github.crunchybubbles.geological.worldgen.DimensionWorldgenTracePlanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes deterministic cross-dimensional compatibility and premise-relative lore evidence. */
final class DimensionCompatibilityPacketGenerator {
  private static final long SAMPLE_CHUNK_X = -7L;
  private static final long SAMPLE_CHUNK_Z = 11L;
  private final long seed;

  DimensionCompatibilityPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    DimensionCompatibilityReview review =
        DimensionCompatibilityReview.evaluate(seed, SAMPLE_CHUNK_X, SAMPLE_CHUNK_Z);
    List<DimensionWorldgenTrace> traces =
        DimensionWorldgenTracePlanner.fromSeed(seed).traceAll(SAMPLE_CHUNK_X, SAMPLE_CHUNK_Z);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_cross_dimensional_compatibility_lore_review",
            "worldSeed",
            seed,
            "sampleChunk",
            JsonWriter.object("chunkX", SAMPLE_CHUNK_X, "chunkZ", SAMPLE_CHUNK_Z),
            "profileCount",
            review.profileCount(),
            "profileIdentityDistinct",
            review.profileIdentityDistinct(),
            "portalCoordinateProbe",
            JsonWriter.object(
                "overworldChunkX",
                review.portalOverworldChunkX(),
                "overworldChunkZ",
                review.portalOverworldChunkZ(),
                "netherChunkX",
                review.portalNetherChunkX(),
                "netherChunkZ",
                review.portalNetherChunkZ()),
            "checks",
            JsonWriter.object(
                "portalCoordinateIdentityIsolated",
                review.portalCoordinateIdentityIsolated(),
                "processContractsValid",
                review.processContractsValid(),
                "mediumContractsValid",
                review.mediumContractsValid(),
                "nativeBoundaryContractsValid",
                review.nativeBoundaryContractsValid(),
                "progressionContractsValid",
                review.progressionContractsValid(),
                "traceSeamsStable",
                review.traceSeamsStable(),
                "traceTopologiesValid",
                review.traceTopologiesValid(),
                "allChecksPassed",
                review.allChecksPassed()),
            "failedChecks",
            review.failedChecks(),
            "generatorBindings",
            JsonWriter.object(
                "minecraft:overworld",
                "geological:overworld",
                "minecraft:the_nether",
                "geological:nether",
                "minecraft:the_end",
                "geological:end"),
            "traceSummaries",
            traces.stream().map(DimensionWorldgenTrace::summary).toList(),
            "loreGuardrails",
            List.of(
                "nether_is_fictional_premise_relative_not_earth_geology",
                "end_is_fictional_premise_relative_not_asteroid_claim",
                "portal_ratio_is_topology_only_not_shared_geology",
                "biome_names_do_not_generate_deep_materials",
                "structures_remain_platform_owned"),
            "expertReviewRequired",
            true);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("dimension-compatibility.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static String digest(String value) {
    try {
      return "sha256:"
          + java.util.HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
