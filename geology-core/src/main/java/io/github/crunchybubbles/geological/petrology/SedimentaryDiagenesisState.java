package io.github.crunchybubbles.geological.petrology;

/**
 * Bounded burial, cementation, dissolution, and fluid-response evidence for sedimentary material.
 */
public record SedimentaryDiagenesisState(
    CompactionClass compactionClass,
    CementationClass cementationClass,
    DissolutionClass dissolutionClass,
    DolomitizationClass dolomitizationClass,
    OrganicMaturityClass organicMaturityClass,
    SalinityClass fluidSalinity,
    long retainedPorosityPpm) {
  public SedimentaryDiagenesisState {
    if (compactionClass == null
        || cementationClass == null
        || dissolutionClass == null
        || dolomitizationClass == null
        || organicMaturityClass == null
        || fluidSalinity == null
        || retainedPorosityPpm < 0
        || retainedPorosityPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sedimentary diagenesis state is incomplete or out of bounds");
    }
  }

  /** Returns the deterministic proof state associated with the existing facies vocabulary. */
  public static SedimentaryDiagenesisState proofFor(
      String faciesClass, SedimentaryBasinState basinState) {
    if (faciesClass == null || faciesClass.isBlank() || basinState == null) {
      throw new IllegalArgumentException("sedimentary diagenesis inputs are required");
    }
    SalinityClass salinity = basinState.salinityClass();
    return switch (faciesClass) {
      case "rift_margin_alluvial_fan" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.CARBONATE_MIXED,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              350_000L);
      case "submarine_volcanic_apron" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.CHLORITE_CALCITE,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              300_000L);
      case "offshore_low_energy" ->
          proof(
              CompactionClass.HIGH,
              CementationClass.CLAY_RICH,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              120_000L);
      case "shallow_marine_shoreface" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.CARBONATE_MIXED,
              DissolutionClass.MODERATE,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              250_000L);
      case "delta_front_to_offshore_transition" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.CARBONATE_CLAY,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              220_000L);
      case "carbonate_platform" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.CALCITE,
              DissolutionClass.STRONG,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              300_000L);
      case "dolomitized_carbonate_platform" ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.DOLOMITE,
              DissolutionClass.MODERATE,
              DolomitizationClass.ACTIVE_REPLACEMENT,
              OrganicMaturityClass.NONE,
              salinity,
              280_000L);
      case "marine_bedded_silica" ->
          proof(
              CompactionClass.HIGH,
              CementationClass.SILICA,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              150_000L);
      case "ancient_iron_silica_precipitation_basin" ->
          proof(
              CompactionClass.HIGH,
              CementationClass.IRON_SILICA,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              100_000L);
      case "restricted_evaporite_margin", "restricted_evaporite_basin_center" ->
          proof(
              CompactionClass.LOW,
              CementationClass.EVAPORITIC,
              DissolutionClass.STRONG,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              450_000L);
      case "buried_peat_mire" ->
          proof(
              CompactionClass.HIGH,
              CementationClass.ORGANIC_COMPACTION,
              DissolutionClass.LIMITED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.PEAT_DERIVED_UNRESOLVED,
              salinity,
              180_000L);
      default ->
          proof(
              CompactionClass.MODERATE,
              CementationClass.UNSPECIFIED,
              DissolutionClass.UNSPECIFIED,
              DolomitizationClass.NONE,
              OrganicMaturityClass.NONE,
              salinity,
              250_000L);
    };
  }

  private static SedimentaryDiagenesisState proof(
      CompactionClass compactionClass,
      CementationClass cementationClass,
      DissolutionClass dissolutionClass,
      DolomitizationClass dolomitizationClass,
      OrganicMaturityClass organicMaturityClass,
      SalinityClass fluidSalinity,
      long retainedPorosityPpm) {
    return new SedimentaryDiagenesisState(
        compactionClass,
        cementationClass,
        dissolutionClass,
        dolomitizationClass,
        organicMaturityClass,
        fluidSalinity,
        retainedPorosityPpm);
  }

  public enum CompactionClass {
    LOW,
    MODERATE,
    HIGH
  }

  public enum CementationClass {
    UNSPECIFIED,
    CARBONATE_MIXED,
    CHLORITE_CALCITE,
    CLAY_RICH,
    CARBONATE_CLAY,
    CALCITE,
    DOLOMITE,
    SILICA,
    IRON_SILICA,
    EVAPORITIC,
    ORGANIC_COMPACTION
  }

  public enum DissolutionClass {
    UNSPECIFIED,
    NONE,
    LIMITED,
    MODERATE,
    STRONG
  }

  public enum DolomitizationClass {
    NONE,
    ACTIVE_REPLACEMENT
  }

  public enum OrganicMaturityClass {
    NONE,
    PEAT_DERIVED_UNRESOLVED
  }
}
