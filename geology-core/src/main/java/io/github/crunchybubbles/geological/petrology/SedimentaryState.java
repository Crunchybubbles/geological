package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** Source-to-sink and diagenetic summary for one resolved stratigraphic member. */
public record SedimentaryState(
    String faciesClass,
    String grainSizeClass,
    String maturityClass,
    String diagenesisClass,
    List<StableId> sourceBodyIds,
    SedimentaryBasinState basinState,
    SedimentaryInputBudget inputBudget,
    List<SedimentaryReservoirContribution> reservoirContributions,
    SedimentaryDiagenesisState diagenesisState) {
  /** Compatibility constructor for the pre-reservoir typed basin state. */
  public SedimentaryState(
      String faciesClass,
      String grainSizeClass,
      String maturityClass,
      String diagenesisClass,
      List<StableId> sourceBodyIds,
      SedimentaryBasinState basinState,
      SedimentaryInputBudget inputBudget) {
    this(
        faciesClass,
        grainSizeClass,
        maturityClass,
        diagenesisClass,
        sourceBodyIds,
        basinState,
        inputBudget,
        SedimentaryReservoirContribution.proofFor(inputBudget, sourceBodyIds),
        SedimentaryDiagenesisState.proofFor(faciesClass, basinState));
  }

  public SedimentaryState(
      String faciesClass,
      String grainSizeClass,
      String maturityClass,
      String diagenesisClass,
      List<StableId> sourceBodyIds) {
    this(
        faciesClass,
        grainSizeClass,
        maturityClass,
        diagenesisClass,
        sourceBodyIds,
        SedimentaryBasinState.proofFor(faciesClass, sourceBodyIds),
        SedimentaryInputBudget.proofFor(faciesClass),
        SedimentaryReservoirContribution.proofFor(
            SedimentaryInputBudget.proofFor(faciesClass), sourceBodyIds),
        SedimentaryDiagenesisState.proofFor(
            faciesClass, SedimentaryBasinState.proofFor(faciesClass, sourceBodyIds)));
  }

  public SedimentaryState(
      String faciesClass,
      String grainSizeClass,
      String maturityClass,
      String diagenesisClass,
      List<StableId> sourceBodyIds,
      SedimentaryBasinState basinState) {
    this(
        faciesClass,
        grainSizeClass,
        maturityClass,
        diagenesisClass,
        sourceBodyIds,
        basinState,
        SedimentaryInputBudget.proofFor(faciesClass),
        SedimentaryReservoirContribution.proofFor(
            SedimentaryInputBudget.proofFor(faciesClass), sourceBodyIds),
        SedimentaryDiagenesisState.proofFor(faciesClass, basinState));
  }

  /** Compatibility constructor for callers that provide all alpha.75 state explicitly. */
  public SedimentaryState(
      String faciesClass,
      String grainSizeClass,
      String maturityClass,
      String diagenesisClass,
      List<StableId> sourceBodyIds,
      SedimentaryBasinState basinState,
      SedimentaryInputBudget inputBudget,
      List<SedimentaryReservoirContribution> reservoirContributions) {
    this(
        faciesClass,
        grainSizeClass,
        maturityClass,
        diagenesisClass,
        sourceBodyIds,
        basinState,
        inputBudget,
        reservoirContributions,
        SedimentaryDiagenesisState.proofFor(faciesClass, basinState));
  }

  public SedimentaryState {
    if (faciesClass == null
        || faciesClass.isBlank()
        || grainSizeClass == null
        || grainSizeClass.isBlank()
        || maturityClass == null
        || maturityClass.isBlank()
        || diagenesisClass == null
        || diagenesisClass.isBlank()
        || basinState == null
        || inputBudget == null
        || reservoirContributions == null
        || diagenesisState == null) {
      throw new IllegalArgumentException("sedimentary state must be complete");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()) {
      throw new IllegalArgumentException("sedimentary state must name a source body");
    }
    if (!sourceBodyIds.equals(basinState.sourceCatchmentIds())) {
      throw new IllegalArgumentException("sedimentary basin sources must match sedimentary state");
    }
    reservoirContributions =
        List.copyOf(reservoirContributions).stream()
            .sorted(java.util.Comparator.comparing(SedimentaryReservoirContribution::kind))
            .toList();
    if (reservoirContributions.isEmpty()
        || reservoirContributions.stream().anyMatch(contribution -> contribution == null)
        || reservoirContributions.stream()
                .map(SedimentaryReservoirContribution::kind)
                .distinct()
                .count()
            != reservoirContributions.size()
        || reservoirContributions.stream()
                .mapToLong(SedimentaryReservoirContribution::fractionPpm)
                .sum()
            != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sedimentary reservoir contributions must be unique and close to "
              + MaterialAssemblage.SCALE);
    }
    long expectedClastic = inputBudget.clasticPpm();
    long expectedVolcanic = inputBudget.volcanicPpm();
    long expectedCarbonate = inputBudget.carbonatePpm();
    long expectedOrganic = inputBudget.organicPpm();
    long expectedChemical = inputBudget.chemicalPrecipitatePpm();
    long expectedBrine = inputBudget.evaporiticBrinePpm();
    for (SedimentaryReservoirContribution contribution : reservoirContributions) {
      long expected =
          switch (contribution.kind()) {
            case CLASTIC_TERRIGENOUS -> expectedClastic;
            case VOLCANIC_ASH -> expectedVolcanic;
            case CARBONATE_BIOGENIC -> expectedCarbonate;
            case ORGANIC_PEAT -> expectedOrganic;
            case CHEMICAL_PRECIPITATE -> expectedChemical;
            case EVAPORITIC_BRINE -> expectedBrine;
          };
      if (expected <= 0 || contribution.fractionPpm() != expected) {
        throw new IllegalArgumentException(
            "sedimentary reservoir contribution disagrees with budget");
      }
      if (!contribution.sourceBodyIds().stream().allMatch(sourceBodyIds::contains)) {
        throw new IllegalArgumentException(
            "sedimentary reservoir source is not a declared source body");
      }
    }
    if (diagenesisState.fluidSalinity() != basinState.salinityClass()) {
      throw new IllegalArgumentException("sedimentary diagenesis fluid salinity must match basin");
    }
  }
}
