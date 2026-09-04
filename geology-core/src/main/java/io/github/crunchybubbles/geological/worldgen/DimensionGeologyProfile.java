package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.model.DimensionProfile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

/**
 * Platform-neutral identity and capability contract for one canonical Minecraft dimension.
 *
 * <p>This contract deliberately carries fictional Nether/End premises without routing them through
 * the Overworld's Earth-analogue process model. A NeoForge adapter consumes these frozen profiles
 * in a later Phase 4 slice.
 */
public record DimensionGeologyProfile(
    String dimensionKey,
    String profileId,
    String version,
    String premiseClass,
    ConfidencePolicy confidencePolicy,
    DimensionProfile.SurfaceTopology atlasTopology,
    VerticalEnvelope verticalEnvelope,
    GravityFrame gravityFrame,
    Set<DimensionProcessFamily> allowedProcessFamilies,
    Set<DimensionProcessFamily> forbiddenProcessFamilies,
    Set<FluidMedium> fluidMedia,
    String boundaryTerrainModel,
    Set<String> materialRegistryIds,
    Set<String> mineralSystemRegistryIds,
    String biomeFieldAdapter,
    String structureProgressionContract,
    String scaleProfileId,
    String scientificDigest) {
  public DimensionGeologyProfile {
    requireText(dimensionKey, "dimension key");
    requireText(profileId, "profile ID");
    requireText(version, "profile version");
    requireText(premiseClass, "premise class");
    if (confidencePolicy == null
        || atlasTopology == null
        || verticalEnvelope == null
        || gravityFrame == null) {
      throw new IllegalArgumentException("dimension profile identity fields must be present");
    }
    if (allowedProcessFamilies == null
        || forbiddenProcessFamilies == null
        || fluidMedia == null
        || materialRegistryIds == null
        || mineralSystemRegistryIds == null) {
      throw new IllegalArgumentException("dimension profile capability sets must be present");
    }
    EnumSet<DimensionProcessFamily> allowed =
        allowedProcessFamilies.isEmpty()
            ? EnumSet.noneOf(DimensionProcessFamily.class)
            : EnumSet.copyOf(allowedProcessFamilies);
    EnumSet<DimensionProcessFamily> forbidden =
        forbiddenProcessFamilies.isEmpty()
            ? EnumSet.noneOf(DimensionProcessFamily.class)
            : EnumSet.copyOf(forbiddenProcessFamilies);
    if (!java.util.Collections.disjoint(allowed, forbidden)) {
      throw new IllegalArgumentException("allowed and forbidden process families must be disjoint");
    }
    allowedProcessFamilies = Set.copyOf(allowed);
    forbiddenProcessFamilies = Set.copyOf(forbidden);
    fluidMedia = Set.copyOf(fluidMedia);
    materialRegistryIds = canonicalIds(materialRegistryIds, "material registry IDs");
    mineralSystemRegistryIds =
        canonicalIds(mineralSystemRegistryIds, "mineral-system registry IDs");
    requireText(boundaryTerrainModel, "boundary terrain model");
    requireText(biomeFieldAdapter, "biome field adapter");
    requireText(structureProgressionContract, "structure progression contract");
    requireText(scaleProfileId, "scale profile ID");
    if (scientificDigest == null || !scientificDigest.matches("sha256:[0-9a-f]{64}")) {
      throw new IllegalArgumentException("dimension profile scientific digest must be sha256 hex");
    }
  }

  /** Initial Overworld profile, pinned to the existing Phase 2 scientific material identity. */
  public static DimensionGeologyProfile overworldPhase4(String scientificDigest) {
    return new DimensionGeologyProfile(
        "minecraft:overworld",
        "geological:overworld_phase4",
        "phase4-alpha.1",
        "earth_analogue",
        ConfidencePolicy.EARTH_ANALOGUE_WITH_TUNABLE_PROXIES,
        DimensionProfile.SurfaceTopology.SINGLE_VALUED_SURFACE,
        new VerticalEnvelope(-64, 319),
        GravityFrame.WORLD_Y_UP,
        EnumSet.allOf(DimensionProcessFamily.class),
        EnumSet.noneOf(DimensionProcessFamily.class),
        Set.of(FluidMedium.SURFACE_WATER, FluidMedium.GROUNDWATER),
        "coarse_uplift_lithology_drainage_surface",
        Set.of("geological:phase2-materials", "geological:phase3-mineral-systems"),
        Set.of(
            "geological:mineral/porphyry_cu",
            "geological:mineral/vms",
            "geological:mineral/lct_pegmatite",
            "geological:mineral/bif",
            "geological:mineral/evaporite_potash",
            "geological:mineral/placer_au"),
        "minecraft:overworld_climate_bridge",
        "minecraft:overworld_structure_progression_v1",
        "geological:overworld_scale_v1",
        scientificDigest);
  }

  /** Initial Nether profile: a water-poor, high-temperature cavern/roof/floor realm. */
  public static DimensionGeologyProfile netherPhase4() {
    return new DimensionGeologyProfile(
        "minecraft:the_nether",
        "geological:nether_phase4",
        "phase4-alpha.1",
        "fictional_high_temperature_magmatic_cavern",
        ConfidencePolicy.FICTIONAL_PREMISE_CONSTRAINED,
        DimensionProfile.SurfaceTopology.CAVERN_VOLUME,
        new VerticalEnvelope(-64, 127),
        GravityFrame.WORLD_Y_UP,
        EnumSet.of(
            DimensionProcessFamily.BASEMENT,
            DimensionProcessFamily.THERMAL_MAGMATISM,
            DimensionProcessFamily.VOLATILE_TRANSPORT,
            DimensionProcessFamily.DEFORMATION,
            DimensionProcessFamily.HYDROTHERMAL,
            DimensionProcessFamily.CAVERN_FORMATION,
            DimensionProcessFamily.METAMORPHISM),
        EnumSet.of(
            DimensionProcessFamily.RIFTING,
            DimensionProcessFamily.SEDIMENTATION,
            DimensionProcessFamily.UPLIFT,
            DimensionProcessFamily.WEATHERING,
            DimensionProcessFamily.SURFACE_WATER_DRAINAGE,
            DimensionProcessFamily.SEDIMENT_TRANSPORT),
        Set.of(FluidMedium.LAVA, FluidMedium.MAGMATIC_VOLATILES),
        "three_dimensional_solid_cavern_with_roof_floor_and_lava_base",
        Set.of(),
        Set.of(),
        "geological:nether_thermal_substrate_bridge",
        "minecraft:nether_fortress_bastion_portal_progression_v1",
        "geological:nether_scale_v1",
        digest(
            "geological:nether_phase4|phase4-alpha.1|fictional_high_temperature_magmatic_cavern"));
  }

  /** Initial End profile: bounded parent fragments and impact/regolith fields in the void. */
  public static DimensionGeologyProfile endPhase4() {
    return new DimensionGeologyProfile(
        "minecraft:the_end",
        "geological:end_phase4",
        "phase4-alpha.1",
        "fictional_fragmented_parent_bodies_in_void",
        ConfidencePolicy.FICTIONAL_PREMISE_CONSTRAINED,
        DimensionProfile.SurfaceTopology.BOUNDED_BODIES_IN_VOID,
        new VerticalEnvelope(-64, 319),
        GravityFrame.LOCAL_ISLAND_DOWN,
        EnumSet.of(
            DimensionProcessFamily.BASEMENT,
            DimensionProcessFamily.THERMAL_MAGMATISM,
            DimensionProcessFamily.DEFORMATION,
            DimensionProcessFamily.METAMORPHISM,
            DimensionProcessFamily.FRAGMENTATION,
            DimensionProcessFamily.IMPACT_BRECCIA,
            DimensionProcessFamily.VOID_REGOLITH),
        EnumSet.of(
            DimensionProcessFamily.RIFTING,
            DimensionProcessFamily.SEDIMENTATION,
            DimensionProcessFamily.ARC_MAGMATISM,
            DimensionProcessFamily.HYDROTHERMAL,
            DimensionProcessFamily.UPLIFT,
            DimensionProcessFamily.SURFACE_WATER_DRAINAGE,
            DimensionProcessFamily.SEDIMENT_TRANSPORT),
        Set.of(FluidMedium.VOID),
        "bounded_islands_in_void_with_central_gap_and_outer_ring",
        Set.of(),
        Set.of(),
        "geological:end_fragment_provenance_bridge",
        "minecraft:end_central_island_gateway_outer_island_progression_v1",
        "geological:end_scale_v1",
        digest("geological:end_phase4|phase4-alpha.1|fictional_fragmented_parent_bodies_in_void"));
  }

  private static Set<String> canonicalIds(Set<String> ids, String label) {
    TreeSet<String> canonical = new TreeSet<>();
    for (String id : ids) {
      requireText(id, label);
      canonical.add(id);
    }
    return Set.copyOf(canonical);
  }

  private static void requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must be present");
    }
  }

  private static String digest(String canonical) {
    try {
      return "sha256:"
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }

  public enum ConfidencePolicy {
    EARTH_ANALOGUE_WITH_TUNABLE_PROXIES,
    FICTIONAL_PREMISE_CONSTRAINED
  }

  public enum GravityFrame {
    WORLD_Y_UP,
    LOCAL_ISLAND_DOWN
  }

  public enum FluidMedium {
    SURFACE_WATER,
    GROUNDWATER,
    LAVA,
    MAGMATIC_VOLATILES,
    VOID
  }

  public enum DimensionProcessFamily {
    BASEMENT,
    RIFTING,
    SEDIMENTATION,
    ARC_MAGMATISM,
    DEFORMATION,
    HYDROTHERMAL,
    METAMORPHISM,
    UPLIFT,
    WEATHERING,
    SURFACE_WATER_DRAINAGE,
    SEDIMENT_TRANSPORT,
    THERMAL_MAGMATISM,
    VOLATILE_TRANSPORT,
    CAVERN_FORMATION,
    FRAGMENTATION,
    IMPACT_BRECCIA,
    VOID_REGOLITH
  }

  public record VerticalEnvelope(int minimumY, int maximumY) {
    public VerticalEnvelope {
      if (minimumY >= maximumY) {
        throw new IllegalArgumentException("vertical envelope must have positive height");
      }
    }
  }
}
