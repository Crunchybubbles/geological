package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Bounded reaction-family and volatile/melt-response evidence for one metamorphic path. */
public record MetamorphicReactionState(
    ReactionMechanism reactionMechanism,
    RetrogressionClass retrogressionClass,
    long dehydrationPpm,
    long decarbonationPpm,
    long partialMeltingPpm,
    SerpentinizationBalance serpentinizationBalance,
    List<MetamorphicFluidContribution> fluidContributions) {
  /** Compatibility constructor for callers without typed fluid evidence. */
  public MetamorphicReactionState(
      ReactionMechanism reactionMechanism,
      RetrogressionClass retrogressionClass,
      long dehydrationPpm,
      long decarbonationPpm,
      long partialMeltingPpm,
      SerpentinizationBalance serpentinizationBalance) {
    this(
        reactionMechanism,
        retrogressionClass,
        dehydrationPpm,
        decarbonationPpm,
        partialMeltingPpm,
        serpentinizationBalance,
        proofFluidContributions(
            reactionMechanism, dehydrationPpm, decarbonationPpm, serpentinizationBalance));
  }

  public MetamorphicReactionState {
    if (reactionMechanism == null
        || retrogressionClass == null
        || serpentinizationBalance == null
        || fluidContributions == null
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
    if (fluidContributions.stream().anyMatch(java.util.Objects::isNull)) {
      throw new IllegalArgumentException("metamorphic fluid contributions may not be null");
    }
    fluidContributions =
        List.copyOf(fluidContributions).stream()
            .sorted(
                java.util.Comparator.comparing(MetamorphicFluidContribution::fluidSpecies)
                    .thenComparing(MetamorphicFluidContribution::direction))
            .toList();
    if (fluidContributions.size()
        != new HashSet<>(
                fluidContributions.stream()
                    .map(
                        contribution ->
                            contribution.fluidSpecies().name() + ":" + contribution.direction())
                    .toList())
            .size()) {
      throw new IllegalArgumentException("metamorphic fluid contributions must be unique");
    }
    validateFluidSpecies(reactionMechanism, fluidContributions);
    validateFluidContributions(
        reactionMechanism,
        dehydrationPpm,
        decarbonationPpm,
        serpentinizationBalance,
        fluidContributions);
  }

  public static MetamorphicReactionState none() {
    return new MetamorphicReactionState(
        ReactionMechanism.NONE,
        RetrogressionClass.NONE,
        0L,
        0L,
        0L,
        SerpentinizationBalance.none(),
        List.of());
  }

  /** Derives the bounded proof response from existing grade, path, and process classes. */
  public static MetamorphicReactionState proofFor(
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      MaterialProcessClass processClass,
      long replacementPpm) {
    return proofFor(grade, facies, path, processClass, replacementPpm, Optional.empty());
  }

  /** Derives the proof response with optional host composition context for reactive contacts. */
  public static MetamorphicReactionState proofFor(
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      MaterialProcessClass processClass,
      long replacementPpm,
      Optional<Lithology> hostLithology) {
    if (grade == null
        || facies == null
        || path == null
        || processClass == null
        || hostLithology == null
        || replacementPpm < 0
        || replacementPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("metamorphic reaction inputs are incomplete");
    }
    if (grade == MetamorphicGrade.HIGH
        && path == MetamorphicPath.CONTACT_LOW_P
        && hostLithology.filter(MetamorphicReactionState::isCarbonateRich).isPresent()) {
      return new MetamorphicReactionState(
          ReactionMechanism.DECARBONATION,
          RetrogressionClass.LOW,
          0L,
          250_000L,
          0L,
          SerpentinizationBalance.none(),
          proofFluidContributions(
              ReactionMechanism.DECARBONATION, 0L, 250_000L, SerpentinizationBalance.none()));
    }
    if (grade == MetamorphicGrade.HIGH
        && facies == MetamorphicFacies.AMPHIBOLITE
        && path == MetamorphicPath.COLLISION_CLOCKWISE
        && hostLithology.filter(MetamorphicReactionState::isFelsicAnatecticHost).isPresent()) {
      return new MetamorphicReactionState(
          ReactionMechanism.PARTIAL_MELTING,
          RetrogressionClass.LOW,
          0L,
          0L,
          150_000L,
          SerpentinizationBalance.none(),
          List.of());
    }
    if (path == MetamorphicPath.HYDROTHERMAL_HYDRATION) {
      return new MetamorphicReactionState(
          ReactionMechanism.SERPENTINIZATION,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.proof(),
          proofFluidContributions(
              ReactionMechanism.SERPENTINIZATION, 0L, 0L, SerpentinizationBalance.proof()));
    }
    if (facies == MetamorphicFacies.GRANULITE && grade == MetamorphicGrade.HIGH) {
      return new MetamorphicReactionState(
          ReactionMechanism.DEHYDRATION,
          RetrogressionClass.LOW,
          450_000L,
          0L,
          0L,
          SerpentinizationBalance.none(),
          proofFluidContributions(
              ReactionMechanism.DEHYDRATION, 450_000L, 0L, SerpentinizationBalance.none()));
    }
    if (processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM) {
      return new MetamorphicReactionState(
          ReactionMechanism.METASOMATIC_REPLACEMENT,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none(),
          List.of());
    }
    if (processClass == MaterialProcessClass.WEATHERING) {
      return new MetamorphicReactionState(
          ReactionMechanism.SURFACE_ALTERATION,
          RetrogressionClass.HIGH,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none(),
          List.of());
    }
    if (path == MetamorphicPath.CONTACT_LOW_P
        || processClass == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM) {
      return new MetamorphicReactionState(
          ReactionMechanism.THERMAL_RECRYSTALLIZATION,
          RetrogressionClass.LOW,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none(),
          List.of());
    }
    if (path == MetamorphicPath.COLLISION_CLOCKWISE) {
      return new MetamorphicReactionState(
          ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
          grade == MetamorphicGrade.LOW ? RetrogressionClass.MODERATE : RetrogressionClass.LOW,
          0L,
          0L,
          0L,
          SerpentinizationBalance.none(),
          List.of());
    }
    return none();
  }

  private static boolean isCarbonateRich(Lithology lithology) {
    return switch (lithology) {
      case LIMESTONE, DOLOSTONE, MARBLE, CARBONATITIC -> true;
      default -> false;
    };
  }

  private static boolean isFelsicAnatecticHost(Lithology lithology) {
    return lithology == Lithology.GRANITIC_GNEISS;
  }

  public enum ReactionMechanism {
    NONE,
    REGIONAL_RECRYSTALLIZATION,
    THERMAL_RECRYSTALLIZATION,
    DEHYDRATION,
    DECARBONATION,
    PARTIAL_MELTING,
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

  private static List<MetamorphicFluidContribution> proofFluidContributions(
      ReactionMechanism reactionMechanism,
      long dehydrationPpm,
      long decarbonationPpm,
      SerpentinizationBalance balance) {
    List<MetamorphicFluidContribution> result = new java.util.ArrayList<>();
    if (reactionMechanism == ReactionMechanism.DEHYDRATION && dehydrationPpm > 0) {
      result.add(
          new MetamorphicFluidContribution(
              MetamorphicFluidContribution.FluidSpecies.WATER,
              MetamorphicFluidContribution.Direction.OUTPUT,
              dehydrationPpm));
    }
    if (reactionMechanism == ReactionMechanism.DECARBONATION && decarbonationPpm > 0) {
      result.add(
          new MetamorphicFluidContribution(
              MetamorphicFluidContribution.FluidSpecies.CARBON_DIOXIDE,
              MetamorphicFluidContribution.Direction.OUTPUT,
              decarbonationPpm));
    }
    if (reactionMechanism == ReactionMechanism.SERPENTINIZATION) {
      if (balance != null && balance.fluidInputPpm() > 0) {
        result.add(
            new MetamorphicFluidContribution(
                MetamorphicFluidContribution.FluidSpecies.WATER,
                MetamorphicFluidContribution.Direction.INPUT,
                balance.fluidInputPpm()));
      }
      if (balance != null && balance.residualFluidPpm() > 0) {
        result.add(
            new MetamorphicFluidContribution(
                MetamorphicFluidContribution.FluidSpecies.WATER,
                MetamorphicFluidContribution.Direction.OUTPUT,
                balance.residualFluidPpm()));
      }
    }
    return List.copyOf(result);
  }

  private static void validateFluidSpecies(
      ReactionMechanism reactionMechanism, List<MetamorphicFluidContribution> contributions) {
    for (MetamorphicFluidContribution contribution : contributions) {
      boolean supported =
          switch (reactionMechanism) {
            case SERPENTINIZATION ->
                contribution.fluidSpecies() == MetamorphicFluidContribution.FluidSpecies.WATER;
            case DEHYDRATION ->
                contribution.fluidSpecies() == MetamorphicFluidContribution.FluidSpecies.WATER
                    && contribution.direction() == MetamorphicFluidContribution.Direction.OUTPUT;
            case DECARBONATION ->
                contribution.fluidSpecies()
                        == MetamorphicFluidContribution.FluidSpecies.CARBON_DIOXIDE
                    && contribution.direction() == MetamorphicFluidContribution.Direction.OUTPUT;
            default -> false;
          };
      if (!supported) {
        throw new IllegalArgumentException(
            "fluid species or direction is unsupported for reaction mechanism");
      }
    }
  }

  private static void validateFluidContributions(
      ReactionMechanism reactionMechanism,
      long dehydrationPpm,
      long decarbonationPpm,
      SerpentinizationBalance balance,
      List<MetamorphicFluidContribution> contributions) {
    long waterInput =
        amount(
            contributions,
            MetamorphicFluidContribution.FluidSpecies.WATER,
            MetamorphicFluidContribution.Direction.INPUT);
    long waterOutput =
        amount(
            contributions,
            MetamorphicFluidContribution.FluidSpecies.WATER,
            MetamorphicFluidContribution.Direction.OUTPUT);
    long carbonDioxideOutput =
        amount(
            contributions,
            MetamorphicFluidContribution.FluidSpecies.CARBON_DIOXIDE,
            MetamorphicFluidContribution.Direction.OUTPUT);
    if (reactionMechanism == ReactionMechanism.SERPENTINIZATION) {
      if (waterInput != balance.fluidInputPpm() || waterOutput != balance.residualFluidPpm()) {
        throw new IllegalArgumentException(
            "serpentinization fluid evidence disagrees with balance");
      }
    } else if (reactionMechanism == ReactionMechanism.DEHYDRATION) {
      if (waterOutput != dehydrationPpm) {
        throw new IllegalArgumentException("dehydration fluid evidence disagrees with reaction");
      }
    } else if (reactionMechanism == ReactionMechanism.DECARBONATION) {
      if (carbonDioxideOutput != decarbonationPpm) {
        throw new IllegalArgumentException("decarbonation fluid evidence disagrees with reaction");
      }
    } else if (!contributions.isEmpty()) {
      throw new IllegalArgumentException("unsupported reaction cannot carry fluid evidence");
    }
  }

  private static long amount(
      List<MetamorphicFluidContribution> contributions,
      MetamorphicFluidContribution.FluidSpecies species,
      MetamorphicFluidContribution.Direction direction) {
    return contributions.stream()
        .filter(
            contribution ->
                contribution.fluidSpecies() == species && contribution.direction() == direction)
        .mapToLong(MetamorphicFluidContribution::amountPpm)
        .sum();
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
