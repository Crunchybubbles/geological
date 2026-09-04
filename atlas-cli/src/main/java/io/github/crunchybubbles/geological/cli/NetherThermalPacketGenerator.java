package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.NetherThermalChunkPlan;
import io.github.crunchybubbles.geological.worldgen.NetherThermalColumnPlan;
import io.github.crunchybubbles.geological.worldgen.NetherThermalProvinceState;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Writes a deterministic Nether thermal/cavern terrain review artifact. */
final class NetherThermalPacketGenerator {
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");
  private final long seed;

  NetherThermalPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    WorldIdentity identity =
        new WorldIdentity(seed, NETHER.version(), NETHER.scientificDigest(), NETHER.profileId());
    NetherThermalTerrainCompiler compiler = NetherThermalTerrainCompiler.from(identity);
    List<NetherThermalChunkPlan> chunks =
        java.util.Arrays.stream(NetherThermalProvinceState.NetherProvinceKind.values())
            .map(kind -> findChunkForKind(compiler, kind))
            .toList();
    Map<String, Integer> provinceKinds = new TreeMap<>();
    Map<String, Integer> provinceIds = new TreeMap<>();
    int lavaColumns = 0;
    int bridgeColumns = 0;
    long floorTotal = 0L;
    long roofTotal = 0L;
    long lavaLevelTotal = 0L;
    int columnCount = 0;
    List<Map<String, Object>> chunkProfiles = new ArrayList<>();
    for (NetherThermalChunkPlan chunk : chunks) {
      int chunkLava = 0;
      int chunkBridges = 0;
      int chunkFloorMin = Integer.MAX_VALUE;
      int chunkRoofMax = Integer.MIN_VALUE;
      for (NetherThermalColumnPlan column : chunk.columns()) {
        provinceKinds.merge(column.provinceKind().name(), 1, Math::addExact);
        provinceIds.merge(column.provinceId().toString(), 1, Math::addExact);
        lavaColumns += column.hasLava() ? 1 : 0;
        bridgeColumns += column.hangingBridge() ? 1 : 0;
        chunkLava += column.hasLava() ? 1 : 0;
        chunkBridges += column.hangingBridge() ? 1 : 0;
        chunkFloorMin = Math.min(chunkFloorMin, column.floorY());
        chunkRoofMax = Math.max(chunkRoofMax, column.roofY());
        floorTotal = Math.addExact(floorTotal, column.floorY());
        roofTotal = Math.addExact(roofTotal, column.roofY());
        lavaLevelTotal = Math.addExact(lavaLevelTotal, column.lavaLevelY());
        columnCount++;
      }
      chunkProfiles.add(
          JsonWriter.object(
              "chunkX",
              chunk.chunkX(),
              "chunkZ",
              chunk.chunkZ(),
              "lavaColumns",
              chunkLava,
              "bridgeColumns",
              chunkBridges,
              "floorMinimumY",
              chunkFloorMin,
              "roofMaximumY",
              chunkRoofMax));
    }
    boolean seamStable = seamStable(compiler, chunks);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_nether_thermal_cavern_roof_floor_lava_projection_not_earth_geology",
            "worldSeed",
            seed,
            "dimensionKey",
            NETHER.dimensionKey(),
            "profileId",
            NETHER.profileId(),
            "profileScientificDigest",
            NETHER.scientificDigest(),
            "verticalEnvelope",
            JsonWriter.object(
                "minimumY", NETHER.verticalEnvelope().minimumY(),
                "maximumY", NETHER.verticalEnvelope().maximumY()),
            "chunksVisited",
            chunks.size(),
            "columnsVisited",
            columnCount,
            "lavaColumns",
            lavaColumns,
            "bridgeColumns",
            bridgeColumns,
            "meanFloorY",
            floorTotal / (double) columnCount,
            "meanRoofY",
            roofTotal / (double) columnCount,
            "meanLavaLevelY",
            lavaLevelTotal / (double) columnCount,
            "provinceKinds",
            provinceKinds,
            "provinceIds",
            provinceIds,
            "chunkProfiles",
            chunkProfiles,
            "forbiddenSurfaceProcesses",
            NETHER.forbiddenProcessFamilies().stream().map(Enum::name).sorted().toList(),
            "seamStable",
            seamStable);
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("nether-thermal.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static NetherThermalChunkPlan findChunkForKind(
      NetherThermalTerrainCompiler compiler, NetherThermalProvinceState.NetherProvinceKind kind) {
    for (long provinceCellX = -16L; provinceCellX <= 16L; provinceCellX++) {
      for (long provinceCellZ = -16L; provinceCellZ <= 16L; provinceCellZ++) {
        long sampleBlockX = Math.multiplyExact(provinceCellX, 512L);
        long sampleBlockZ = Math.multiplyExact(provinceCellZ, 512L);
        if (compiler.provinceAt(sampleBlockX, sampleBlockZ).kind() == kind) {
          return compiler.plan(provinceCellX * 32L, provinceCellZ * 32L);
        }
      }
    }
    throw new IllegalStateException("no Nether province sample found for " + kind);
  }

  private static boolean seamStable(
      NetherThermalTerrainCompiler compiler, List<NetherThermalChunkPlan> chunks) {
    for (NetherThermalChunkPlan chunk : chunks) {
      long originX = chunk.chunkX() * 16L;
      long originZ = chunk.chunkZ() * 16L;
      for (int offset = 0; offset < 16; offset++) {
        if (!compiler
            .planColumn(originX + 16L, originZ + offset)
            .equals(
                compiler
                    .plan(chunk.chunkX() + 1L, chunk.chunkZ())
                    .at(originX + 16L, originZ + offset))) {
          return false;
        }
        if (!compiler
            .planColumn(originX + offset, originZ + 16L)
            .equals(
                compiler
                    .plan(chunk.chunkX(), chunk.chunkZ() + 1L)
                    .at(originX + offset, originZ + 16L))) {
          return false;
        }
      }
    }
    return true;
  }

  private static String digest(String value) {
    try {
      return "sha256:"
          + java.util.HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }
}
