package io.github.crunchybubbles.geological.petrology;

/** Bounded reaction-family and volatile/melt-response evidence for one metamorphic path. */
public record MetamorphicReactionState(
    ReactionMechanism reactionMechanism,
    RetrogressionClass retrogressionClass,
    long dehydrationPpm,
    long decarbonationPpm,
    long partialMeltingPpm,
    SerpentinizationBalance serpentinizationBalance) {
  public MetamorphicReactionState {
    if (reactionMechanism == null
        || retrogressionClass == null
        || serpentinizationBalance == null
        || dehydrationPpm < 0
        || dehydrationPpm > MaterialAssemblage.SCALE
        || decarbonationPpm < 0
        || decarbonationPpm > MaterialAssemblage.SCALE
        || partialMeltingPpm < 0
        || partialMeltingPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "metamorphic reaction state is incomplete or out of bounds");
    }
    if (reactionMechanism != ReactionMechanism.SERPENTINIZATION
        && serpentinizationBalance.serpentineProductPpm() != 0) {
      throw new IllegalArgumentException(
          "only a serpentinization reaction may produce serpentine product");
    }
  }

  public static MetamorphicReactionState none() {
    return new MetamorphicReactionState(
        ReactionMechanism.NONE,
        RetrogressionClass.NONE,
        0L,
        0L,
        0L,
        SerpentinizationBalance.none());
  }

  /** Derives the bounded proof response from existing grade, path, and process classes. */
  public static MetamorphicReactionState proofFor(
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      MaterialProcessClass processClass,
      long replacementPpm) {
    if (grade == null
        || facies == null
        || path == null
        || processClass == null
        || replacementPpm < 0
        || replacementPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("metamorphic reaction inputs are incomplete");
    }
    if (path == MetamorphicPath.HYDROTHERMAL_HYDRATION) {
      return new MetamorphicReactionState(
          ReactionMechanism.SERPENTINIZATION,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.proof());
    }
    if (facies == MetamorphicFacies.GRANULITE && grade == MetamorphicGrade.HIGH) {
      return new MetamorphicReactionState(
          ReactionMechanism.DEHYDRATION,
          RetrogressionClass.LOW,
          450_000L,
          0L,
          0L,
          SerpentinizationBalance.none());
    }
    if (processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM) {
      return new MetamorphicReactionState(
          ReactionMechanism.METASOMATIC_REPLACEMENT,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none());
    }
    if (processClass == MaterialProcessClass.WEATHERING) {
      return new MetamorphicReactionState(
          ReactionMechanism.SURFACE_ALTERATION,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none());
    }
    if (path == MetamorphicPath.CONTACT_LOW_P
        || processClass == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM) {
      return new MetamorphicReactionState(
          ReactionMechanism.THERMAL_RECRYSTALLIZATION,
          RetrogressionClass.LOW,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none());
    }
    if (path == MetamorphicPath.COLLISION_CLOCKWISE) {
      return new MetamorphicReactionState(
          ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
          grade == MetamorphicGrade.LOW ? RetrogressionClass.MODERATE : RetrogressionClass.LOW,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none());
    }
    return none();
  }

  public enum ReactionMechanism {
    NONE,
    REGIONAL_RECRYSTALLIZATION,
    THERMAL_RECRYSTALLIZATION,
    DEHYDRATION,
    SERPENTINIZATION,
    METASOMATIC_REPLACEMENT,
    SURFACE_ALTERATION
  }

  public enum RetrogressionClass {
    NONE,
    LOW,
    MODERATE,
    HIGH
  }

  /** Exact normalized reaction-inventory proof for hydration of an ultramafic protolith. */
  public record SerpentinizationBalance(
      long rockReactantPpm,
      long fluidInputPpm,
      long serpentineProductPpm,
      long residualRockPpm,
      long residualFluidPpm) {
    public SerpentinizationBalance {
      if (rockReactantPpm < 0
          || rockReactantPpm > MaterialAssemblage.SCALE
          || fluidInputPpm < 0
          || fluidInputPpm > MaterialAssemblage.SCALE
          || serpentineProductPpm < 0
          || serpentineProductPpm > MaterialAssemblage.SCALE
          || residualRockPpm < 0
          || residualRockPpm > MaterialAssemblage.SCALE
          || residualFluidPpm < 0
          || residualFluidPpm > MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException("serpentinization balance is out of bounds");
      }
      if (Math.addExact(rockReactantPpm, fluidInputPpm)
          != Math.addExact(
              Math.addExact(serpentineProductPpm, residualRockPpm), residualFluidPpm)) {
        throw new IllegalArgumentException("serpentinization balance does not close");
      }
    }

    private static SerpentinizationBalance none() {
      return new SerpentinizationBalance(
          MaterialAssemblage.SCALE, 0L, 0L, MaterialAssemblage.SCALE, 0L);
    }

    private static SerpentinizationBalance proof() {
      return new SerpentinizationBalance(700_000L, 300_000L, 900_000L, 0L, 100_000L);
    }
  }
}
