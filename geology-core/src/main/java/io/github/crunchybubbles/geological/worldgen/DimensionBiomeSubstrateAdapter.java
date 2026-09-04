package io.github.crunchybubbles.geological.worldgen;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Converts native dimension compiler outputs into bounded biome/substrate controls.
 *
 * <p>Each adapter is profile-locked and read-only. The adapter never selects a biome, writes a
 * block, or treats a biome name as a material-history input.
 */
public final class DimensionBiomeSubstrateAdapter {
  private static final String OVERWORLD = "minecraft:overworld";
  private static final String NETHER = "minecraft:the_nether";
  private static final String END = "minecraft:the_end";

  private DimensionBiomeSubstrateAdapter() {}

  /** Projects Overworld elevation, slope, drainage, and surface flags into normalized controls. */
  public static DimensionBiomeSubstrateState overworld(
      DimensionGeologyProfile profile, OverworldTerrainControlSample sample) {
    requireProfile(profile, OVERWORLD, "minecraft:overworld_climate_bridge");
    Objects.requireNonNull(sample, "Overworld terrain-control sample");
    double elevation = clampUnit((sample.elevation() + 64.0) / 384.0);
    double slopeSignal = sample.slope() / (1.0 + sample.slope());
    double secondary = clampUnit(sample.flowAccumulation() * 0.65 + slopeSignal * 0.35);
    Set<String> tags =
        sample.channel()
            ? Set.of("channel", "earth_surface")
            : sample.outcrop()
                ? Set.of("earth_surface", "outcrop")
                : Set.of("earth_surface", "weathered_surface");
    return new DimensionBiomeSubstrateState(
        OVERWORLD,
        profile.biomeFieldAdapter(),
        Optional.of(sample.provinceId()),
        sample.channel()
            ? "overworld:alluvial_channel"
            : sample.outcrop() ? "overworld:bedrock_outcrop" : "overworld:weathered_surface",
        elevation,
        secondary,
        tags,
        true,
        false);
  }

  /** Projects Nether thermal and volatile potentials without introducing water semantics. */
  public static DimensionBiomeSubstrateState nether(
      DimensionGeologyProfile profile, NetherThermalProvinceState province) {
    requireProfile(profile, NETHER, "geological:nether_thermal_substrate_bridge");
    Objects.requireNonNull(province, "Nether thermal province");
    String kind = slug(province.kind().name());
    return new DimensionBiomeSubstrateState(
        NETHER,
        profile.biomeFieldAdapter(),
        Optional.of(province.provinceId()),
        "nether:thermal/" + kind,
        province.heatPotentialFixedUnits() / 1_000_000.0,
        province.volatilePotentialFixedUnits() / 1_000_000.0,
        Set.of("magmatic", "thermal", kind),
        false,
        false);
  }

  /** Projects End parent-fragment or void state into local-island controls. */
  public static DimensionBiomeSubstrateState end(
      DimensionGeologyProfile profile, EndFragmentColumnPlan column) {
    requireProfile(profile, END, "geological:end_fragment_provenance_bridge");
    Objects.requireNonNull(column, "End fragment column");
    if (column.isVoid()) {
      return new DimensionBiomeSubstrateState(
          END,
          profile.biomeFieldAdapter(),
          Optional.empty(),
          "end:void",
          0.0,
          1.0,
          Set.of("void"),
          false,
          true);
    }
    EndParentBodyState body = column.body().orElseThrow();
    String role = slug(body.role().name());
    String family = slug(body.parentFamily().name());
    double radiusSignal = clampUnit(body.horizontalRadiusBlocks() / 512.0);
    double impactSignal = body.impactClass() == EndParentBodyState.ImpactClass.NONE ? 0.0 : 1.0;
    Set<String> tags = new HashSet<>(Set.of("fragment", family, role));
    if (!column.impactMeltIntervals().isEmpty()) {
      tags.add("impact");
    }
    if (!column.regolithIntervals().isEmpty()) {
      tags.add("regolith");
    }
    return new DimensionBiomeSubstrateState(
        END,
        profile.biomeFieldAdapter(),
        column.parentBodyId(),
        "end:fragment/" + family,
        radiusSignal,
        impactSignal,
        tags,
        false,
        false);
  }

  private static void requireProfile(
      DimensionGeologyProfile profile, String dimensionKey, String adapterId) {
    Objects.requireNonNull(profile, "dimension geology profile");
    if (!dimensionKey.equals(profile.dimensionKey())
        || !adapterId.equals(profile.biomeFieldAdapter())) {
      throw new IllegalArgumentException(
          "biome/substrate adapter does not match " + dimensionKey + " profile");
    }
  }

  private static String slug(String value) {
    return value.toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static double clampUnit(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
