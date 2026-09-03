package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.HashSet;
import java.util.List;

/**
 * Reduced basin, environment, water-chemistry, and source-catchment state for one sedimentary
 * parcel.
 */
public record SedimentaryBasinState(
    String basinType,
    String depositionalEnvironment,
    String waterBodyConnectivity,
    String waterDepthClass,
    String accommodationTrend,
    SalinityClass salinityClass,
    RedoxClass redoxClass,
    long clasticDilutionPpm,
    long carbonateProductivityPpm,
    List<StableId> sourceCatchmentIds) {
  public SedimentaryBasinState {
    if (basinType == null
        || basinType.isBlank()
        || depositionalEnvironment == null
        || depositionalEnvironment.isBlank()
        || waterBodyConnectivity == null
        || waterBodyConnectivity.isBlank()
        || waterDepthClass == null
        || waterDepthClass.isBlank()
        || accommodationTrend == null
        || accommodationTrend.isBlank()
        || salinityClass == null
        || redoxClass == null
        || clasticDilutionPpm < 0
        || clasticDilutionPpm > MaterialAssemblage.SCALE
        || carbonateProductivityPpm < 0
        || carbonateProductivityPpm > MaterialAssemblage.SCALE
        || sourceCatchmentIds == null) {
      throw new IllegalArgumentException("sedimentary basin state is incomplete or out of bounds");
    }
    sourceCatchmentIds = List.copyOf(sourceCatchmentIds).stream().sorted().toList();
    if (sourceCatchmentIds.isEmpty()
        || sourceCatchmentIds.stream().anyMatch(id -> id == null)
        || sourceCatchmentIds.size() != new HashSet<>(sourceCatchmentIds).size()) {
      throw new IllegalArgumentException("sedimentary basin sources must be non-empty and unique");
    }
  }

  /** Creates the deterministic proof context associated with the existing facies vocabulary. */
  public static SedimentaryBasinState proofFor(
      String faciesClass, List<StableId> sourceCatchmentIds) {
    if (faciesClass == null || faciesClass.isBlank()) {
      throw new IllegalArgumentException("sedimentary facies is required");
    }
    return switch (faciesClass) {
      case "rift_margin_alluvial_fan" ->
          proof(
              "rift_margin_basin",
              "alluvial_fan",
              "open_rift_margin",
              "continental",
              "syn_rift_subsidence",
              SalinityClass.FRESH,
              RedoxClass.BUFFERED,
              900_000,
              0,
              sourceCatchmentIds);
      case "submarine_volcanic_apron" ->
          proof(
              "submarine_volcanic_basin",
              "submarine_apron",
              "open_marine",
              "shallow_to_slope",
              "synvolcanic_subsidence",
              SalinityClass.SEAWATER,
              RedoxClass.REDUCING,
              600_000,
              100_000,
              sourceCatchmentIds);
      case "offshore_low_energy" ->
          proof(
              "marine_shelf_basin",
              "offshore",
              "open_marine",
              "outer_shelf",
              "thermal_subsidence",
              SalinityClass.SEAWATER,
              RedoxClass.REDUCING,
              750_000,
              0,
              sourceCatchmentIds);
      case "shallow_marine_shoreface" ->
          proof(
              "marine_shelf_basin",
              "shoreface",
              "open_marine",
              "shallow",
              "relative_sea_level_rise",
              SalinityClass.SEAWATER,
              RedoxClass.BUFFERED,
              650_000,
              100_000,
              sourceCatchmentIds);
      case "delta_front_to_offshore_transition" ->
          proof(
              "deltaic_basin",
              "delta_front",
              "connected_estuary",
              "shallow",
              "accommodation_plus_supply",
              SalinityClass.SEAWATER,
              RedoxClass.BUFFERED,
              850_000,
              50_000,
              sourceCatchmentIds);
      case "carbonate_platform" ->
          proof(
              "carbonate_platform",
              "carbonate_platform",
              "open_marine",
              "shallow_photic",
              "stable_shelf",
              SalinityClass.SEAWATER,
              RedoxClass.BUFFERED,
              150_000,
              850_000,
              sourceCatchmentIds);
      case "dolomitized_carbonate_platform" ->
          proof(
              "carbonate_platform",
              "restricted_dolomitizing_platform",
              "restricted_marine",
              "shallow_photic",
              "relative_sea_level_fall",
              SalinityClass.MODERATE_BRINE,
              RedoxClass.BUFFERED,
              100_000,
              800_000,
              sourceCatchmentIds);
      case "marine_bedded_silica" ->
          proof(
              "marine_silica_basin",
              "bedded_silica",
              "open_marine",
              "slope_to_basin",
              "steady_subsidence",
              SalinityClass.SEAWATER,
              RedoxClass.REDUCING,
              200_000,
              0,
              sourceCatchmentIds);
      case "ancient_iron_silica_precipitation_basin" ->
          proof(
              "iron_formation_basin",
              "chemical_precipitate",
              "open_to_restricted_marine",
              "deep_to_shallow",
              "redox_cycling",
              SalinityClass.SEAWATER,
              RedoxClass.REDUCING,
              100_000,
              0,
              sourceCatchmentIds);
      case "restricted_evaporite_margin" ->
          proof(
              "restricted_evaporite_basin",
              "evaporite_margin",
              "restricted",
              "littoral_sabkha",
              "subsidence_and_evaporation",
              SalinityClass.HYPERSALINE,
              RedoxClass.OXIDIZING,
              200_000,
              0,
              sourceCatchmentIds);
      case "restricted_evaporite_basin_center" ->
          proof(
              "restricted_evaporite_basin",
              "basin_center_salt",
              "restricted",
              "basin_center",
              "subsidence_and_evaporation",
              SalinityClass.HYPERSALINE,
              RedoxClass.BUFFERED,
              50_000,
              0,
              sourceCatchmentIds);
      case "buried_peat_mire" ->
          proof(
              "peat_basin",
              "peat_mire",
              "closed_to_restricted",
              "shallow_water_table",
              "subsidence_with_mire_aggradation",
              SalinityClass.FRESH,
              RedoxClass.STRONGLY_REDUCING,
              100_000,
              0,
              sourceCatchmentIds);
      default ->
          proof(
              "undifferentiated_sedimentary_basin",
              faciesClass,
              "unknown_connectivity",
              "unknown_depth",
              "proof_accommodation",
              SalinityClass.FRESH,
              RedoxClass.BUFFERED,
              500_000,
              0,
              sourceCatchmentIds);
    };
  }

  private static SedimentaryBasinState proof(
      String basinType,
      String depositionalEnvironment,
      String waterBodyConnectivity,
      String waterDepthClass,
      String accommodationTrend,
      SalinityClass salinityClass,
      RedoxClass redoxClass,
      long clasticDilutionPpm,
      long carbonateProductivityPpm,
      List<StableId> sourceCatchmentIds) {
    return new SedimentaryBasinState(
        basinType,
        depositionalEnvironment,
        waterBodyConnectivity,
        waterDepthClass,
        accommodationTrend,
        salinityClass,
        redoxClass,
        clasticDilutionPpm,
        carbonateProductivityPpm,
        sourceCatchmentIds);
  }
}
