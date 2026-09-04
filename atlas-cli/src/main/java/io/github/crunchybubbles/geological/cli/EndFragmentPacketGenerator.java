package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentColumnPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.EndParentBodyState;
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

/** Writes deterministic End parent-body, impact, and void-regolith review evidence. */
final class EndFragmentPacketGenerator {
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");
  private final long seed;

  EndFragmentPacketGenerator(long seed) {
    this.seed = seed;
  }

  Path generate(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    WorldIdentity identity =
        new WorldIdentity(seed, END.version(), END.scientificDigest(), END.profileId());
    EndFragmentTerrainCompiler compiler = EndFragmentTerrainCompiler.from(identity);
    List<EndFragmentColumnPlan> samples = new ArrayList<>();
    samples.add(compiler.planColumn(0L, 0L));
    samples.add(findRoleSample(compiler, EndParentBodyState.FragmentRole.GATEWAY_RING));
    samples.add(findRoleSample(compiler, EndParentBodyState.FragmentRole.OUTER_ISLAND));
    for (EndParentBodyState.ParentFamily family : EndParentBodyState.ParentFamily.values()) {
      samples.add(findFamilySample(compiler, family));
    }
    EndFragmentColumnPlan impact = findImpactSample(compiler);
    if (!samples.contains(impact)) {
      samples.add(impact);
    }
    EndFragmentColumnPlan voidSample = compiler.planColumn(512L, 512L);

    Map<String, Integer> roleCounts = new TreeMap<>();
    Map<String, Integer> familyCounts = new TreeMap<>();
    Map<String, Integer> differentiationCounts = new TreeMap<>();
    Map<String, Integer> impactCounts = new TreeMap<>();
    Map<String, Integer> eventCounts = new TreeMap<>();
    long parentBudget = 0L;
    long parentRetained = 0L;
    long impactLoss = 0L;
    long regolithBudget = 0L;
    long regolithRetained = 0L;
    long voidLoss = 0L;
    int islandSamples = 0;
    int voidSamples = 0;
    int regolithColumns = 0;
    int impactColumns = 0;
    for (EndFragmentColumnPlan sample : samples) {
      if (sample.isVoid()) {
        voidSamples++;
        continue;
      }
      islandSamples++;
      EndParentBodyState body = sample.body().orElseThrow();
      roleCounts.merge(body.role().name(), 1, Math::addExact);
      familyCounts.merge(body.parentFamily().name(), 1, Math::addExact);
      differentiationCounts.merge(body.differentiation().name(), 1, Math::addExact);
      impactCounts.merge(body.impactClass().name(), 1, Math::addExact);
      for (EndParentBodyState.Event event : body.events()) {
        eventCounts.merge(event.kind().name(), 1, Math::addExact);
      }
      parentBudget = Math.addExact(parentBudget, body.parentMaterialBudgetFixedUnits());
      parentRetained = Math.addExact(parentRetained, body.retainedParentMaterialFixedUnits());
      impactLoss = Math.addExact(impactLoss, body.impactLossFixedUnits());
      regolithBudget = Math.addExact(regolithBudget, body.regolithBudgetFixedUnits());
      regolithRetained = Math.addExact(regolithRetained, body.retainedRegolithFixedUnits());
      voidLoss = Math.addExact(voidLoss, body.voidExposureLossFixedUnits());
      regolithColumns += sample.regolithIntervals().isEmpty() ? 0 : 1;
      impactColumns += sample.impactMeltIntervals().isEmpty() ? 0 : 1;
    }
    voidSamples += voidSample.isVoid() ? 1 : 0;
    Map<String, Object> json =
        JsonWriter.object(
            "measurementKind",
            "phase8_end_parent_fragment_impact_regolith_projection_not_earth_geology",
            "worldSeed",
            seed,
            "dimensionKey",
            END.dimensionKey(),
            "profileId",
            END.profileId(),
            "profileScientificDigest",
            END.scientificDigest(),
            "verticalEnvelope",
            JsonWriter.object(
                "minimumY", END.verticalEnvelope().minimumY(),
                "maximumY", END.verticalEnvelope().maximumY()),
            "samples",
            samples.stream().map(EndFragmentPacketGenerator::sampleProfile).toList(),
            "voidSample",
            sampleProfile(voidSample),
            "islandSamples",
            islandSamples,
            "voidSamples",
            voidSamples,
            "regolithColumns",
            regolithColumns,
            "impactColumns",
            impactColumns,
            "roleCounts",
            roleCounts,
            "parentFamilies",
            familyCounts,
            "differentiationCounts",
            differentiationCounts,
            "impactCounts",
            impactCounts,
            "eventCounts",
            eventCounts,
            "parentMaterialBudgetFixedUnits",
            parentBudget,
            "retainedParentMaterialFixedUnits",
            parentRetained,
            "impactLossFixedUnits",
            impactLoss,
            "parentBudgetClosed",
            parentBudget == parentRetained + impactLoss,
            "regolithBudgetFixedUnits",
            regolithBudget,
            "retainedRegolithFixedUnits",
            regolithRetained,
            "voidExposureLossFixedUnits",
            voidLoss,
            "regolithBudgetClosed",
            regolithBudget == regolithRetained + voidLoss,
            "forbiddenProcessFamilies",
            END.forbiddenProcessFamilies().stream().map(Enum::name).sorted().toList(),
            "fluidMedia",
            END.fluidMedia().stream().map(Enum::name).sorted().toList(),
            "progressionContract",
            END.structureProgressionContract(),
            "seamStable",
            seamStable(compiler, samples));
    String digest = digest(JsonWriter.stringify(json));
    Map<String, Object> artifact = new LinkedHashMap<>(json);
    artifact.put("digest", digest);
    Path reportPath = outputDirectory.resolve("end-fragments.json");
    JsonWriter.write(reportPath, artifact);
    return reportPath;
  }

  private static EndFragmentColumnPlan findRoleSample(
      EndFragmentTerrainCompiler compiler, EndParentBodyState.FragmentRole role) {
    for (long cellX = -8L; cellX <= 8L; cellX++) {
      for (long cellZ = -8L; cellZ <= 8L; cellZ++) {
        EndParentBodyState body = compiler.parentBodyAtCell(cellX, cellZ).orElseThrow();
        if (body.role() != role) {
          continue;
        }
        EndFragmentColumnPlan column =
            compiler.planColumn(
                (long) StrictMath.floor(body.center().x()),
                (long) StrictMath.floor(body.center().z()));
        if (!column.isVoid()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no End sample found for role " + role);
  }

  private static EndFragmentColumnPlan findFamilySample(
      EndFragmentTerrainCompiler compiler, EndParentBodyState.ParentFamily family) {
    for (long cellX = -8L; cellX <= 8L; cellX++) {
      for (long cellZ = -8L; cellZ <= 8L; cellZ++) {
        EndParentBodyState body = compiler.parentBodyAtCell(cellX, cellZ).orElseThrow();
        if (body.parentFamily() != family) {
          continue;
        }
        EndFragmentColumnPlan column =
            compiler.planColumn(
                (long) StrictMath.floor(body.center().x()),
                (long) StrictMath.floor(body.center().z()));
        if (!column.isVoid()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no End sample found for parent family " + family);
  }

  private static EndFragmentColumnPlan findImpactSample(EndFragmentTerrainCompiler compiler) {
    for (long cellX = -8L; cellX <= 8L; cellX++) {
      for (long cellZ = -8L; cellZ <= 8L; cellZ++) {
        EndParentBodyState body = compiler.parentBodyAtCell(cellX, cellZ).orElseThrow();
        if (body.impactClass() == EndParentBodyState.ImpactClass.NONE) {
          continue;
        }
        EndFragmentColumnPlan column =
            compiler.planColumn(
                (long) StrictMath.floor(body.impactCenterX()),
                (long) StrictMath.floor(body.impactCenterZ()));
        if (!column.isVoid() && !column.impactMeltIntervals().isEmpty()) {
          return column;
        }
      }
    }
    throw new IllegalStateException("no impacted End sample found");
  }

  private static Map<String, Object> sampleProfile(EndFragmentColumnPlan column) {
    if (column.isVoid()) {
      return JsonWriter.object("blockX", column.blockX(), "blockZ", column.blockZ(), "void", true);
    }
    EndParentBodyState body = column.body().orElseThrow();
    return JsonWriter.object(
        "blockX",
        column.blockX(),
        "blockZ",
        column.blockZ(),
        "void",
        false,
        "baseY",
        column.baseY(),
        "surfaceY",
        column.surfaceY(),
        "parentBodyId",
        body.parentBodyId().toString(),
        "fragmentId",
        body.fragmentId().toString(),
        "role",
        body.role().name(),
        "parentFamily",
        body.parentFamily().name(),
        "differentiation",
        body.differentiation().name(),
        "impactClass",
        body.impactClass().name(),
        "sourceBodyIds",
        body.sourceBodyIds().stream().map(Object::toString).toList(),
        "events",
        body.events().stream().map(event -> event.kind().name()).toList(),
        "solidIntervals",
        column.solidIntervals().stream()
            .map(
                interval ->
                    JsonWriter.object(
                        "minY", interval.minYInclusive(), "maxY", interval.maxYExclusive()))
            .toList(),
        "regolithIntervals",
        column.regolithIntervals().stream()
            .map(
                interval ->
                    JsonWriter.object(
                        "minY", interval.minYInclusive(), "maxY", interval.maxYExclusive()))
            .toList(),
        "impactMeltIntervals",
        column.impactMeltIntervals().stream()
            .map(
                interval ->
                    JsonWriter.object(
                        "minY", interval.minYInclusive(), "maxY", interval.maxYExclusive()))
            .toList());
  }

  private static boolean seamStable(
      EndFragmentTerrainCompiler compiler, List<EndFragmentColumnPlan> samples) {
    for (EndFragmentColumnPlan sample : samples) {
      long chunkX = Math.floorDiv(sample.blockX(), 16L);
      long chunkZ = Math.floorDiv(sample.blockZ(), 16L);
      var chunk = compiler.plan(chunkX, chunkZ);
      if (!compiler
          .planColumn(sample.blockX(), sample.blockZ())
          .equals(chunk.at(sample.blockX(), sample.blockZ()))) {
        return false;
      }
      if (!compiler
          .planColumn(sample.blockX() + 16L, sample.blockZ())
          .equals(compiler.plan(chunkX + 1L, chunkZ).at(sample.blockX() + 16L, sample.blockZ()))) {
        return false;
      }
      if (!compiler
          .planColumn(sample.blockX(), sample.blockZ() + 16L)
          .equals(compiler.plan(chunkX, chunkZ + 1L).at(sample.blockX(), sample.blockZ() + 16L))) {
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
