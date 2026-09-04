package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.EndProgressionContract;
import io.github.crunchybubbles.geological.worldgen.EndProgressionPlanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Writes deterministic End progression and protected-structure review evidence. */
final class EndProgressionPacketGenerator {
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");
  private final long seed;

  EndProgressionPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    WorldIdentity identity =
        new WorldIdentity(seed, END.version(), END.scientificDigest(), END.profileId());
    EndProgressionPlanner planner =
        EndProgressionPlanner.from(EndFragmentTerrainCompiler.from(identity));
    EndProgressionContract contract = planner.contract();
    Map<String, Integer> structureCounts = new TreeMap<>();
    for (EndProgressionContract.StructureSlot slot : contract.structureSlots()) {
      structureCounts.merge(slot.kind().name(), 1, Math::addExact);
    }
    List<Map<String, Object>> slots =
        contract.structureSlots().stream()
            .map(
                slot ->
                    JsonWriter.object(
                        "structureId",
                        slot.structureId().toString(),
                        "kind",
                        slot.kind().name(),
                        "anchorBodyId",
                        slot.anchorBodyId().toString(),
                        "anchorX",
                        slot.anchor().x(),
                        "anchorY",
                        slot.anchor().y(),
                        "anchorZ",
                        slot.anchor().z(),
                        "horizontalRadiusBlocks",
                        slot.horizontalRadiusBlocks(),
                        "protectedFromTerrainWrites",
                        slot.protectedFromTerrainWrites()))
            .toList();
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_end_progression_structure_protection_not_earth_geology",
            "worldSeed",
            seed,
            "dimensionKey",
            END.dimensionKey(),
            "profileId",
            END.profileId(),
            "profileScientificDigest",
            END.scientificDigest(),
            "contractId",
            contract.contractId().toString(),
            "structureProgressionContract",
            contract.structureProgressionContract(),
            "centralIslandBodyId",
            contract.centralIslandBodyId().toString(),
            "gatewayBodyIds",
            contract.gatewayBodyIds().stream().map(Object::toString).toList(),
            "outerIslandBodyIds",
            contract.outerIslandBodyIds().stream().map(Object::toString).toList(),
            "centralIslandRadiusBlocks",
            contract.centralIslandRadiusBlocks(),
            "minimumVoidGapBlocks",
            contract.minimumVoidGapBlocks(),
            "portalArrivalViable",
            contract.portalArrivalViable(),
            "dragonArenaProtected",
            contract.dragonArenaProtected(),
            "structureCounts",
            structureCounts,
            "protectedStructureCount",
            contract.structureSlots().stream()
                .filter(EndProgressionContract.StructureSlot::protectedFromTerrainWrites)
                .count(),
            "structureSlots",
            slots,
            "topologyValid",
            planner.validateTopology(),
            "centralArenaWriteBlocked",
            !planner.canWriteTerrain(0L, 0L),
            "voidGapWriteAllowed",
            planner.canWriteTerrain(512L, 512L),
            "forbiddenProcessFamilies",
            END.forbiddenProcessFamilies().stream().map(Enum::name).sorted().toList(),
            "seamStable",
            seamStable(planner));
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("end-progression.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static boolean seamStable(EndProgressionPlanner planner) {
    var compiler = planner.terrain();
    long[][] chunks = {{0L, 0L}, {32L, 0L}, {-32L, 16L}};
    for (long[] chunk : chunks) {
      var plan = compiler.plan(chunk[0], chunk[1]);
      for (int offset = 0; offset < 16; offset++) {
        long originX = plan.bounds().minX();
        long originZ = plan.bounds().minZ();
        if (!compiler
            .planColumn(originX + 16L, originZ + offset)
            .equals(compiler.plan(chunk[0] + 1L, chunk[1]).at(originX + 16L, originZ + offset))) {
          return false;
        }
        if (!compiler
            .planColumn(originX + offset, originZ + 16L)
            .equals(compiler.plan(chunk[0], chunk[1] + 1L).at(originX + offset, originZ + 16L))) {
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
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
