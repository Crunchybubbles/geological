package io.github.crunchybubbles.geological.cli;

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

/** Writes cross-dimensional identity, provenance, and seam evidence for the adapter boundary. */
final class DimensionWorldgenTracePacketGenerator {
  private static final long SAMPLE_CHUNK_X = -7L;
  private static final long SAMPLE_CHUNK_Z = 11L;
  private final long seed;

  DimensionWorldgenTracePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    List<DimensionWorldgenTrace> traces =
        DimensionWorldgenTracePlanner.fromSeed(seed).traceAll(SAMPLE_CHUNK_X, SAMPLE_CHUNK_Z);
    List<String> profileIds = traces.stream().map(DimensionWorldgenTrace::profileId).toList();
    List<String> chunkIds = traces.stream().map(trace -> trace.chunkId().toString()).toList();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_cross_dimensional_identity_adapter_trace",
            "worldSeed",
            seed,
            "sampleChunk",
            JsonWriter.object("chunkX", SAMPLE_CHUNK_X, "chunkZ", SAMPLE_CHUNK_Z),
            "profileCount",
            traces.size(),
            "profileIds",
            profileIds,
            "chunkIds",
            chunkIds,
            "crossDimensionalIdentityDistinct",
            profileIds.stream().distinct().count() == traces.size()
                && chunkIds.stream().distinct().count() == traces.size(),
            "allSeamsStable",
            traces.stream().allMatch(DimensionWorldgenTrace::seamStable),
            "allTopologyValid",
            traces.stream().allMatch(DimensionWorldgenTrace::topologyValid),
            "platformWriteAuthority",
            "adapter_and_structure_system_only",
            "traces",
            traces.stream().map(DimensionWorldgenTracePacketGenerator::traceJson).toList());
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("dimension-traces.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static Map<String, Object> traceJson(DimensionWorldgenTrace trace) {
    return JsonWriter.object(
        "dimensionKey",
        trace.dimensionKey(),
        "profileId",
        trace.profileId(),
        "modelVersion",
        trace.modelVersion(),
        "scientificDigest",
        trace.scientificDigest(),
        "worldSeed",
        trace.worldSeed(),
        "chunkX",
        trace.chunkX(),
        "chunkZ",
        trace.chunkZ(),
        "chunkId",
        trace.chunkId().toString(),
        "ownerKind",
        trace.ownerKind(),
        "ownerIds",
        trace.ownerIds().stream().map(Object::toString).toList(),
        "columnsVisited",
        trace.columnsVisited(),
        "solidColumns",
        trace.solidColumns(),
        "voidColumns",
        trace.voidColumns(),
        "fluidOrVoidColumns",
        trace.fluidOrVoidColumns(),
        "solidIntervalCount",
        trace.solidIntervalCount(),
        "provenanceIntervalCount",
        trace.provenanceIntervalCount(),
        "specialColumnCount",
        trace.specialColumnCount(),
        "protectedColumnCount",
        trace.protectedColumnCount(),
        "allowedProcessFamilies",
        trace.allowedProcessFamilies(),
        "forbiddenProcessFamilies",
        trace.forbiddenProcessFamilies(),
        "fluidMedia",
        trace.fluidMedia(),
        "boundaryTerrainModel",
        trace.boundaryTerrainModel(),
        "seamStable",
        trace.seamStable(),
        "topologyValid",
        trace.topologyValid());
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
