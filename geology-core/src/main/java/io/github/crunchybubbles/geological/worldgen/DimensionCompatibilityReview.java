package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic compatibility and premise-relative lore guardrails for the three native profiles.
 */
public record DimensionCompatibilityReview(
    long worldSeed,
    long sampleChunkX,
    long sampleChunkZ,
    long portalOverworldChunkX,
    long portalOverworldChunkZ,
    long portalNetherChunkX,
    long portalNetherChunkZ,
    int profileCount,
    boolean profileIdentityDistinct,
    boolean portalCoordinateIdentityIsolated,
    boolean processContractsValid,
    boolean mediumContractsValid,
    boolean nativeBoundaryContractsValid,
    boolean progressionContractsValid,
    boolean traceSeamsStable,
    boolean traceTopologiesValid) {
  public DimensionCompatibilityReview {
    if (profileCount <= 0) {
      throw new IllegalArgumentException("compatibility review needs at least one profile");
    }
  }

  /** Evaluates the frozen profile contracts and native traces for one deterministic sample. */
  public static DimensionCompatibilityReview evaluate(
      long worldSeed, long sampleChunkX, long sampleChunkZ) {
    List<DimensionGeologyProfile> profiles = DimensionGeologyProfiles.all();
    DimensionGeologyProfile overworld = DimensionGeologyProfiles.require("minecraft:overworld");
    DimensionGeologyProfile nether = DimensionGeologyProfiles.require("minecraft:the_nether");
    DimensionGeologyProfile end = DimensionGeologyProfiles.require("minecraft:the_end");

    long portalOverworldChunkX = 80L;
    long portalOverworldChunkZ = -64L;
    long portalNetherChunkX = Math.floorDiv(portalOverworldChunkX, 8L);
    long portalNetherChunkZ = Math.floorDiv(portalOverworldChunkZ, 8L);
    WorldgenChunkIdentity overworldPortal =
        WorldgenChunkIdentity.forSeed(
            worldSeed, overworld, portalOverworldChunkX, portalOverworldChunkZ);
    WorldgenChunkIdentity netherPortal =
        WorldgenChunkIdentity.forSeed(worldSeed, nether, portalNetherChunkX, portalNetherChunkZ);
    WorldgenChunkIdentity endPortal =
        WorldgenChunkIdentity.forSeed(worldSeed, end, portalOverworldChunkX, portalOverworldChunkZ);

    Set<String> profileIds =
        new HashSet<>(profiles.stream().map(DimensionGeologyProfile::profileId).toList());
    Set<String> scientificDigests =
        profiles.stream()
            .map(DimensionGeologyProfile::scientificDigest)
            .collect(java.util.stream.Collectors.toSet());
    Set<String> chunkIds = new HashSet<>();
    chunkIds.add(overworldPortal.chunkId().toString());
    chunkIds.add(netherPortal.chunkId().toString());
    chunkIds.add(endPortal.chunkId().toString());
    boolean profileIdentityDistinct =
        profileIds.size() == profiles.size()
            && scientificDigests.size() == profiles.size()
            && chunkIds.size() == profiles.size();
    boolean portalCoordinateIdentityIsolated =
        !overworldPortal
                .worldIdentity()
                .dimensionProfileId()
                .equals(netherPortal.worldIdentity().dimensionProfileId())
            && !overworldPortal.chunkId().equals(netherPortal.chunkId())
            && !netherPortal.chunkId().equals(endPortal.chunkId());
    boolean processContractsValid = processContractsValid(overworld, nether, end);
    boolean mediumContractsValid = mediumContractsValid(overworld, nether, end);
    boolean nativeBoundaryContractsValid = nativeBoundaryContractsValid(overworld, nether, end);

    WorldIdentity endIdentity =
        WorldgenChunkRequest.forChunk(worldSeed, end, sampleChunkX, sampleChunkZ).worldIdentity();
    EndProgressionPlanner progression =
        EndProgressionPlanner.from(EndFragmentTerrainCompiler.from(endIdentity));
    boolean progressionContractsValid =
        progression.validateTopology()
            && progression.contract().portalArrivalViable()
            && progression.contract().dragonArenaProtected()
            && !progression.canWriteTerrain(0L, 0L)
            && progression.canWriteTerrain(512L, 512L);

    List<DimensionWorldgenTrace> traces =
        DimensionWorldgenTracePlanner.fromSeed(worldSeed).traceAll(sampleChunkX, sampleChunkZ);
    return new DimensionCompatibilityReview(
        worldSeed,
        sampleChunkX,
        sampleChunkZ,
        portalOverworldChunkX,
        portalOverworldChunkZ,
        portalNetherChunkX,
        portalNetherChunkZ,
        profiles.size(),
        profileIdentityDistinct,
        portalCoordinateIdentityIsolated,
        processContractsValid,
        mediumContractsValid,
        nativeBoundaryContractsValid,
        progressionContractsValid,
        traces.stream().allMatch(DimensionWorldgenTrace::seamStable),
        traces.stream().allMatch(DimensionWorldgenTrace::topologyValid));
  }

  public boolean allChecksPassed() {
    return profileIdentityDistinct
        && portalCoordinateIdentityIsolated
        && processContractsValid
        && mediumContractsValid
        && nativeBoundaryContractsValid
        && progressionContractsValid
        && traceSeamsStable
        && traceTopologiesValid;
  }

  /** Returns stable check names that need attention, preserving review order. */
  public List<String> failedChecks() {
    List<String> failed = new ArrayList<>();
    if (!profileIdentityDistinct) {
      failed.add("profile_identity_distinct");
    }
    if (!portalCoordinateIdentityIsolated) {
      failed.add("portal_coordinate_identity_isolated");
    }
    if (!processContractsValid) {
      failed.add("process_contracts_valid");
    }
    if (!mediumContractsValid) {
      failed.add("medium_contracts_valid");
    }
    if (!nativeBoundaryContractsValid) {
      failed.add("native_boundary_contracts_valid");
    }
    if (!progressionContractsValid) {
      failed.add("progression_contracts_valid");
    }
    if (!traceSeamsStable) {
      failed.add("trace_seams_stable");
    }
    if (!traceTopologiesValid) {
      failed.add("trace_topologies_valid");
    }
    return List.copyOf(failed);
  }

  private static boolean processContractsValid(
      DimensionGeologyProfile overworld,
      DimensionGeologyProfile nether,
      DimensionGeologyProfile end) {
    List<DimensionGeologyProfile> profiles = List.of(overworld, nether, end);
    return profiles.stream()
            .allMatch(
                profile ->
                    java.util.Collections.disjoint(
                        profile.allowedProcessFamilies(), profile.forbiddenProcessFamilies()))
        && nether
            .allowedProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.THERMAL_MAGMATISM)
        && nether
            .allowedProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.CAVERN_FORMATION)
        && nether
            .forbiddenProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.SURFACE_WATER_DRAINAGE)
        && end.allowedProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.FRAGMENTATION)
        && end.allowedProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.IMPACT_BRECCIA)
        && end.allowedProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.VOID_REGOLITH)
        && end.forbiddenProcessFamilies()
            .contains(DimensionGeologyProfile.DimensionProcessFamily.SURFACE_WATER_DRAINAGE);
  }

  private static boolean mediumContractsValid(
      DimensionGeologyProfile overworld,
      DimensionGeologyProfile nether,
      DimensionGeologyProfile end) {
    return overworld
            .fluidMedia()
            .equals(
                Set.of(
                    DimensionGeologyProfile.FluidMedium.SURFACE_WATER,
                    DimensionGeologyProfile.FluidMedium.GROUNDWATER))
        && nether
            .fluidMedia()
            .equals(
                Set.of(
                    DimensionGeologyProfile.FluidMedium.LAVA,
                    DimensionGeologyProfile.FluidMedium.MAGMATIC_VOLATILES))
        && end.fluidMedia().equals(Set.of(DimensionGeologyProfile.FluidMedium.VOID));
  }

  private static boolean nativeBoundaryContractsValid(
      DimensionGeologyProfile overworld,
      DimensionGeologyProfile nether,
      DimensionGeologyProfile end) {
    return overworld.atlasTopology()
            == io.github.crunchybubbles.geological.model.DimensionProfile.SurfaceTopology
                .SINGLE_VALUED_SURFACE
        && nether.atlasTopology()
            == io.github.crunchybubbles.geological.model.DimensionProfile.SurfaceTopology
                .CAVERN_VOLUME
        && end.atlasTopology()
            == io.github.crunchybubbles.geological.model.DimensionProfile.SurfaceTopology
                .BOUNDED_BODIES_IN_VOID
        && overworld.gravityFrame() == DimensionGeologyProfile.GravityFrame.WORLD_Y_UP
        && nether.gravityFrame() == DimensionGeologyProfile.GravityFrame.WORLD_Y_UP
        && end.gravityFrame() == DimensionGeologyProfile.GravityFrame.LOCAL_ISLAND_DOWN
        && nether.boundaryTerrainModel().contains("cavern")
        && end.boundaryTerrainModel().contains("void");
  }
}
