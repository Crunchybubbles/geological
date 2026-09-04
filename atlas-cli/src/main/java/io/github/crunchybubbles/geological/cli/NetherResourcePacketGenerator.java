package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.NetherMaterialHistoryState;
import io.github.crunchybubbles.geological.worldgen.NetherResourceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.NetherResourcePlanner;
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

/** Writes a deterministic Nether material-history and resource review artifact. */
final class NetherResourcePacketGenerator {
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");
  private final long seed;

  NetherResourcePacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    WorldIdentity identity =
        new WorldIdentity(seed, NETHER.version(), NETHER.scientificDigest(), NETHER.profileId());
    NetherResourcePlanner planner =
        NetherResourcePlanner.from(NetherThermalTerrainCompiler.from(identity));
    List<NetherResourceColumnPlan> samples = new ArrayList<>();
    for (NetherResourceSystemState.ResourceFamily family :
        NetherResourceSystemState.ResourceFamily.values()) {
      if (family != NetherResourceSystemState.ResourceFamily.NONE) {
        samples.add(findSample(planner, family));
      }
    }

    Map<String, Integer> materialFamilies = new TreeMap<>();
    Map<String, Integer> eventKinds = new TreeMap<>();
    Map<String, Integer> resourceFamilies = new TreeMap<>();
    Map<String, Integer> statusCounts = new TreeMap<>();
    Map<String, Integer> pathwayCounts = new TreeMap<>();
    Map<String, Integer> trapCounts = new TreeMap<>();
    Map<String, Integer> horizonCounts = new TreeMap<>();
    long sourceBudget = 0L;
    long released = 0L;
    long transportLoss = 0L;
    long deposit = 0L;
    int columnsVisited = 0;
    int columnsWithResource = 0;
    int intervalCount = 0;
    for (NetherResourceColumnPlan sample : samples) {
      columnsVisited++;
      materialFamilies.merge(sample.history().primaryMaterial().name(), 1, Math::addExact);
      for (NetherMaterialHistoryState.Event event : sample.history().events()) {
        eventKinds.merge(event.kind().name(), 1, Math::addExact);
      }
      NetherResourceSystemState resource = sample.resource();
      resourceFamilies.merge(resource.family().name(), 1, Math::addExact);
      statusCounts.merge(resource.status().name(), 1, Math::addExact);
      pathwayCounts.merge(resource.pathwayClass().name(), 1, Math::addExact);
      trapCounts.merge(resource.trapClass().name(), 1, Math::addExact);
      if (sample.hasResource()) {
        columnsWithResource++;
      }
      intervalCount = Math.addExact(intervalCount, sample.intervals().size());
      for (var interval : sample.intervals()) {
        horizonCounts.merge(interval.horizon().kind().name(), 1, Math::addExact);
      }
      sourceBudget = Math.addExact(sourceBudget, resource.sourceBudgetFixedUnits());
      released = Math.addExact(released, resource.releasedResourceFixedUnits());
      transportLoss = Math.addExact(transportLoss, resource.transportLossFixedUnits());
      deposit = Math.addExact(deposit, resource.depositAllocationFixedUnits());
    }

    NetherResourceColumnPlan negativeProbe = planner.planColumn(0L, 0L);
    NetherMaterialHistoryState negativeHistory = negativeProbe.history();
    NetherMaterialHistoryState incoherentHistory =
        new NetherMaterialHistoryState(
            negativeHistory.historyId(),
            negativeHistory.provinceId(),
            negativeHistory.refractoryBasementId(),
            negativeHistory.magmaProvinceId(),
            negativeHistory.sourceBodyIds(),
            negativeHistory.provinceKind(),
            incompatibleMaterial(negativeHistory.primaryMaterial()),
            negativeHistory.events(),
            negativeHistory.sourceBudgetFixedUnits(),
            negativeHistory.retainedMaterialFixedUnits(),
            negativeHistory.alterationLossFixedUnits());
    NetherResourceSystemState defaultNegative =
        NetherResourceSystemState.proofFor(
            planner.terrain().provinceAt(0L, 0L), incoherentHistory, identity);
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_nether_material_history_resources_not_earth_geology",
            "worldSeed",
            seed,
            "dimensionKey",
            NETHER.dimensionKey(),
            "profileId",
            NETHER.profileId(),
            "profileScientificDigest",
            NETHER.scientificDigest(),
            "samples",
            samples.stream().map(NetherResourcePacketGenerator::sampleProfile).toList(),
            "columnsVisited",
            columnsVisited,
            "columnsWithResource",
            columnsWithResource,
            "intervalCount",
            intervalCount,
            "materialFamilies",
            materialFamilies,
            "eventKinds",
            eventKinds,
            "resourceFamilies",
            resourceFamilies,
            "statusCounts",
            statusCounts,
            "pathwayCounts",
            pathwayCounts,
            "trapCounts",
            trapCounts,
            "horizonCounts",
            horizonCounts,
            "sourceBudgetFixedUnits",
            sourceBudget,
            "releasedResourceFixedUnits",
            released,
            "transportLossFixedUnits",
            transportLoss,
            "depositAllocationFixedUnits",
            deposit,
            "budgetClosed",
            released == transportLoss + deposit && released <= sourceBudget,
            "defaultNegativeProof",
            JsonWriter.object(
                "status",
                defaultNegative.status().name(),
                "family",
                defaultNegative.family().name(),
                "failedGate",
                defaultNegative.failedGate().orElse(null)),
            "seamStable",
            seamStable(planner, samples));
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("nether-resources.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static NetherResourceColumnPlan findSample(
      NetherResourcePlanner planner, NetherResourceSystemState.ResourceFamily family) {
    for (long cellX = -16L; cellX <= 16L; cellX++) {
      for (long cellZ = -16L; cellZ <= 16L; cellZ++) {
        NetherResourceColumnPlan probe = planner.planColumn(cellX * 512L, cellZ * 512L);
        if (probe.resource().family() != family
            || probe.resource().status() != FormationStatus.FORMED) {
          continue;
        }
        long centerX = (long) StrictMath.floor(probe.resource().localCenter().x());
        long centerZ = (long) StrictMath.floor(probe.resource().localCenter().z());
        NetherResourceColumnPlan centered = planner.planColumn(centerX, centerZ);
        if (centered.resource().family() == family && centered.hasResource()) {
          return centered;
        }
      }
    }
    throw new IllegalStateException("no formed Nether resource sample found for " + family);
  }

  private static NetherMaterialHistoryState.MaterialFamily incompatibleMaterial(
      NetherMaterialHistoryState.MaterialFamily material) {
    return material == NetherMaterialHistoryState.MaterialFamily.BASALT_LAVA
        ? NetherMaterialHistoryState.MaterialFamily.POROUS_NETHERRACK
        : NetherMaterialHistoryState.MaterialFamily.BASALT_LAVA;
  }

  private static Map<String, Object> sampleProfile(NetherResourceColumnPlan column) {
    NetherResourceSystemState resource = column.resource();
    return JsonWriter.object(
        "blockX",
        column.blockX(),
        "blockZ",
        column.blockZ(),
        "materialFamily",
        column.history().primaryMaterial().name(),
        "historyId",
        column.history().historyId().toString(),
        "provinceId",
        column.history().provinceId().toString(),
        "eventKinds",
        column.history().events().stream().map(event -> event.kind().name()).toList(),
        "resourceFamily",
        resource.family().name(),
        "status",
        resource.status().name(),
        "sourceBodyId",
        resource.sourceBodyId().toString(),
        "pathwayId",
        resource.pathwayId().toString(),
        "hostBodyId",
        resource.hostBodyId().toString(),
        "hostMaterial",
        resource.hostMaterial().name(),
        "pathwayClass",
        resource.pathwayClass().name(),
        "trapClass",
        resource.trapClass().name(),
        "sourceBudgetFixedUnits",
        resource.sourceBudgetFixedUnits(),
        "releasedResourceFixedUnits",
        resource.releasedResourceFixedUnits(),
        "transportLossFixedUnits",
        resource.transportLossFixedUnits(),
        "depositAllocationFixedUnits",
        resource.depositAllocationFixedUnits(),
        "horizons",
        resource.horizons().stream().map(horizon -> horizon.kind().name()).toList(),
        "intervals",
        column.intervals().stream()
            .map(
                interval ->
                    JsonWriter.object(
                        "minY",
                        interval.minYInclusive(),
                        "maxY",
                        interval.maxYExclusive(),
                        "horizon",
                        interval.horizon().kind().name()))
            .toList());
  }

  private static boolean seamStable(
      NetherResourcePlanner planner, List<NetherResourceColumnPlan> samples) {
    for (NetherResourceColumnPlan sample : samples) {
      long chunkX = Math.floorDiv(sample.blockX(), 16L);
      long chunkZ = Math.floorDiv(sample.blockZ(), 16L);
      var chunk = planner.plan(chunkX, chunkZ);
      if (!planner
          .planColumn(sample.blockX(), sample.blockZ())
          .equals(chunk.at(sample.blockX(), sample.blockZ()))) {
        return false;
      }
      if (!planner
          .planColumn(sample.blockX() + 16L, sample.blockZ())
          .equals(planner.plan(chunkX + 1L, chunkZ).at(sample.blockX() + 16L, sample.blockZ()))) {
        return false;
      }
      if (!planner
          .planColumn(sample.blockX(), sample.blockZ() + 16L)
          .equals(planner.plan(chunkX, chunkZ + 1L).at(sample.blockX(), sample.blockZ() + 16L))) {
        return false;
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
