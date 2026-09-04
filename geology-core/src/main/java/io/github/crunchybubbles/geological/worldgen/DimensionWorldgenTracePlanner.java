package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds deterministic cross-dimensional debug traces without giving a platform adapter terrain
 * write authority.
 *
 * <p>The planner intentionally routes each dimension through its native compiler: the Overworld
 * uses its bounded base-terrain planner, the Nether uses thermal terrain plus material/resource
 * history, and the End uses parent fragments plus progression protection. This makes identity and
 * seam checks observable at the same boundary that a loader adapter consumes.
 */
public final class DimensionWorldgenTracePlanner {
  private final long worldSeed;

  private DimensionWorldgenTracePlanner(long worldSeed) {
    this.worldSeed = worldSeed;
  }

  public static DimensionWorldgenTracePlanner fromSeed(long worldSeed) {
    return new DimensionWorldgenTracePlanner(worldSeed);
  }

  /** Builds a trace for one canonical dimension and target chunk. */
  public DimensionWorldgenTrace trace(String dimensionKey, long chunkX, long chunkZ) {
    DimensionGeologyProfile profile =
        DimensionGeologyProfiles.require(Objects.requireNonNull(dimensionKey, "dimension key"));
    return switch (profile.dimensionKey()) {
      case "minecraft:overworld" -> traceOverworld(profile, chunkX, chunkZ);
      case "minecraft:the_nether" -> traceNether(profile, chunkX, chunkZ);
      case "minecraft:the_end" -> traceEnd(profile, chunkX, chunkZ);
      default ->
          throw new IllegalStateException(
              "unhandled canonical dimension " + profile.dimensionKey());
    };
  }

  /** Builds traces for every canonical dimension in stable profile order. */
  public List<DimensionWorldgenTrace> traceAll(long chunkX, long chunkZ) {
    return DimensionGeologyProfiles.all().stream()
        .map(profile -> trace(profile.dimensionKey(), chunkX, chunkZ))
        .toList();
  }

  private DimensionWorldgenTrace traceOverworld(
      DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    OverworldBaseTerrainPlanner planner = overworldPlanner(profile, chunkX, chunkZ);
    List<OverworldBaseTerrainColumnPlan> columns = planner.planTargetChunk();
    Set<StableId> ownerIds = new HashSet<>();
    int solidColumns = 0;
    int solidIntervals = 0;
    for (OverworldBaseTerrainColumnPlan column : columns) {
      ownerIds.add(column.terrainControls().provinceId());
      if (column.hasSolidTerrain()) {
        solidColumns++;
      }
      solidIntervals = Math.addExact(solidIntervals, column.lithologyRuns().size());
    }
    return createTrace(
        profile,
        chunkX,
        chunkZ,
        "province",
        ownerIds,
        columns.size(),
        solidColumns,
        0,
        0,
        solidIntervals,
        0,
        0,
        0,
        profileContractNames(profile.allowedProcessFamilies()),
        profileContractNames(profile.forbiddenProcessFamilies()),
        fluidNames(profile),
        profile.boundaryTerrainModel(),
        overworldSeamStable(profile, chunkX, chunkZ),
        profile.atlasTopology() == DimensionProfile.SurfaceTopology.SINGLE_VALUED_SURFACE);
  }

  private DimensionWorldgenTrace traceNether(
      DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forChunk(worldSeed, profile, chunkX, chunkZ);
    NetherResourcePlanner planner =
        NetherResourcePlanner.from(NetherThermalTerrainCompiler.from(request.worldIdentity()));
    NetherResourceChunkPlan chunk = planner.plan(chunkX, chunkZ);
    Set<StableId> ownerIds = new HashSet<>();
    int solidColumns = 0;
    int lavaColumns = 0;
    int solidIntervals = 0;
    int provenanceIntervals = 0;
    int resourceColumns = 0;
    for (NetherResourceColumnPlan column : chunk.columns()) {
      ownerIds.add(column.thermal().provinceId());
      if (!column.thermal().solidIntervals().isEmpty()) {
        solidColumns++;
      }
      if (column.thermal().hasLava()) {
        lavaColumns++;
      }
      solidIntervals = Math.addExact(solidIntervals, column.thermal().solidIntervals().size());
      provenanceIntervals = Math.addExact(provenanceIntervals, column.intervals().size());
      if (column.hasResource()) {
        resourceColumns++;
      }
    }
    return createTrace(
        profile,
        chunkX,
        chunkZ,
        "province",
        ownerIds,
        chunk.columns().size(),
        solidColumns,
        0,
        lavaColumns,
        solidIntervals,
        provenanceIntervals,
        resourceColumns,
        0,
        profileContractNames(profile.allowedProcessFamilies()),
        profileContractNames(profile.forbiddenProcessFamilies()),
        fluidNames(profile),
        profile.boundaryTerrainModel(),
        netherSeamStable(planner, chunkX, chunkZ),
        profile.atlasTopology() == DimensionProfile.SurfaceTopology.CAVERN_VOLUME);
  }

  private DimensionWorldgenTrace traceEnd(
      DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forChunk(worldSeed, profile, chunkX, chunkZ);
    EndFragmentTerrainCompiler compiler = EndFragmentTerrainCompiler.from(request.worldIdentity());
    EndProgressionPlanner progression = EndProgressionPlanner.from(compiler);
    EndFragmentChunkPlan chunk = compiler.plan(chunkX, chunkZ);
    Set<StableId> ownerIds = new HashSet<>();
    int specialColumnCount = 0;
    int solidIntervals = 0;
    int provenanceIntervals = 0;
    int protectedColumns = 0;
    for (EndFragmentColumnPlan column : chunk.columns()) {
      column.parentBodyId().ifPresent(ownerIds::add);
      solidIntervals = Math.addExact(solidIntervals, column.solidIntervals().size());
      provenanceIntervals =
          Math.addExact(
              provenanceIntervals,
              column.regolithIntervals().size() + column.impactMeltIntervals().size());
      if (!column.regolithIntervals().isEmpty() || !column.impactMeltIntervals().isEmpty()) {
        specialColumnCount++;
      }
      if (!progression.canWriteTerrain(column.blockX(), column.blockZ())) {
        protectedColumns++;
      }
    }
    return createTrace(
        profile,
        chunkX,
        chunkZ,
        "parent_body",
        ownerIds,
        chunk.columns().size(),
        Math.toIntExact(chunk.islandColumnCount()),
        Math.toIntExact(chunk.voidColumnCount()),
        Math.toIntExact(chunk.voidColumnCount()),
        solidIntervals,
        provenanceIntervals,
        specialColumnCount,
        protectedColumns,
        profileContractNames(profile.allowedProcessFamilies()),
        profileContractNames(profile.forbiddenProcessFamilies()),
        fluidNames(profile),
        profile.boundaryTerrainModel(),
        endSeamStable(compiler, chunkX, chunkZ),
        progression.validateTopology());
  }

  private OverworldBaseTerrainPlanner overworldPlanner(
      DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forChunk(worldSeed, profile, chunkX, chunkZ);
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            request,
            WorldgenStage.BASE_TERRAIN,
            WorldgenSnapshot.forProfile(profile),
            Runnable::run);
    return OverworldBaseTerrainPlanner.from(context);
  }

  private boolean overworldSeamStable(DimensionGeologyProfile profile, long chunkX, long chunkZ) {
    OverworldBaseTerrainPlanner current = overworldPlanner(profile, chunkX, chunkZ);
    OverworldBaseTerrainPlanner east = overworldPlanner(profile, chunkX + 1L, chunkZ);
    OverworldBaseTerrainPlanner south = overworldPlanner(profile, chunkX, chunkZ + 1L);
    ChunkBlockBounds bounds = current.context().targetBounds();
    for (int offset = 0; offset < 16; offset++) {
      long eastX = bounds.maxXExclusive();
      long eastZ = bounds.minZ() + offset;
      if (!current.plan(eastX, eastZ).equals(east.plan(eastX, eastZ))) {
        return false;
      }
      long southX = bounds.minX() + offset;
      long southZ = bounds.maxZExclusive();
      if (!current.plan(southX, southZ).equals(south.plan(southX, southZ))) {
        return false;
      }
    }
    return true;
  }

  private static boolean netherSeamStable(NetherResourcePlanner planner, long chunkX, long chunkZ) {
    NetherResourceChunkPlan current = planner.plan(chunkX, chunkZ);
    NetherResourceChunkPlan east = planner.plan(chunkX + 1L, chunkZ);
    NetherResourceChunkPlan south = planner.plan(chunkX, chunkZ + 1L);
    ChunkBlockBounds bounds = current.bounds();
    for (int offset = 0; offset < 16; offset++) {
      long eastX = bounds.maxXExclusive();
      long eastZ = bounds.minZ() + offset;
      if (!planner.planColumn(eastX, eastZ).equals(east.at(eastX, eastZ))) {
        return false;
      }
      long southX = bounds.minX() + offset;
      long southZ = bounds.maxZExclusive();
      if (!planner.planColumn(southX, southZ).equals(south.at(southX, southZ))) {
        return false;
      }
    }
    return true;
  }

  private static boolean endSeamStable(
      EndFragmentTerrainCompiler compiler, long chunkX, long chunkZ) {
    EndFragmentChunkPlan current = compiler.plan(chunkX, chunkZ);
    EndFragmentChunkPlan east = compiler.plan(chunkX + 1L, chunkZ);
    EndFragmentChunkPlan south = compiler.plan(chunkX, chunkZ + 1L);
    ChunkBlockBounds bounds = current.bounds();
    for (int offset = 0; offset < 16; offset++) {
      long eastX = bounds.maxXExclusive();
      long eastZ = bounds.minZ() + offset;
      if (!compiler.planColumn(eastX, eastZ).equals(east.at(eastX, eastZ))) {
        return false;
      }
      long southX = bounds.minX() + offset;
      long southZ = bounds.maxZExclusive();
      if (!compiler.planColumn(southX, southZ).equals(south.at(southX, southZ))) {
        return false;
      }
    }
    return true;
  }

  private DimensionWorldgenTrace createTrace(
      DimensionGeologyProfile profile,
      long chunkX,
      long chunkZ,
      String ownerKind,
      Set<StableId> ownerIds,
      int columnsVisited,
      int solidColumns,
      int voidColumns,
      int fluidOrVoidColumns,
      int solidIntervalCount,
      int provenanceIntervalCount,
      int specialColumnCount,
      int protectedColumnCount,
      List<String> allowedProcessFamilies,
      List<String> forbiddenProcessFamilies,
      List<String> fluidMedia,
      String boundaryTerrainModel,
      boolean seamStable,
      boolean topologyValid) {
    WorldgenChunkRequest request =
        WorldgenChunkRequest.forChunk(worldSeed, profile, chunkX, chunkZ);
    return new DimensionWorldgenTrace(
        profile.dimensionKey(),
        profile.profileId(),
        profile.version(),
        profile.scientificDigest(),
        worldSeed,
        chunkX,
        chunkZ,
        request.chunkId(),
        ownerKind,
        new ArrayList<>(ownerIds),
        columnsVisited,
        solidColumns,
        voidColumns,
        fluidOrVoidColumns,
        solidIntervalCount,
        provenanceIntervalCount,
        specialColumnCount,
        protectedColumnCount,
        allowedProcessFamilies,
        forbiddenProcessFamilies,
        fluidMedia,
        boundaryTerrainModel,
        seamStable,
        topologyValid);
  }

  private static List<String> profileContractNames(
      Set<DimensionGeologyProfile.DimensionProcessFamily> families) {
    return families.stream().map(Enum::name).sorted().toList();
  }

  private static List<String> fluidNames(DimensionGeologyProfile profile) {
    return profile.fluidMedia().stream().map(Enum::name).sorted().toList();
  }
}
