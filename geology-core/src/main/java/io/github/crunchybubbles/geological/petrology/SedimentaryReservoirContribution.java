package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.HashSet;
import java.util.List;

/** One exact normalized contribution from a named sedimentary reservoir class. */
public record SedimentaryReservoirContribution(
    SedimentaryReservoirKind kind, long fractionPpm, List<StableId> sourceBodyIds) {
  public SedimentaryReservoirContribution {
    if (kind == null
        || fractionPpm <= 0
        || fractionPpm > MaterialAssemblage.SCALE
        || sourceBodyIds == null) {
      throw new IllegalArgumentException("sedimentary reservoir contribution is incomplete");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()) {
      throw new IllegalArgumentException("sedimentary reservoir sources must be unique");
    }
  }

  /** Builds canonical typed contributions from the existing facies budget and source evidence. */
  public static List<SedimentaryReservoirContribution> proofFor(
      SedimentaryInputBudget budget, List<StableId> sourceBodyIds) {
    if (budget == null || sourceBodyIds == null) {
      throw new IllegalArgumentException("sedimentary reservoir proof inputs are required");
    }
    List<StableId> canonicalSources = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (canonicalSources.isEmpty()
        || canonicalSources.stream().anyMatch(id -> id == null)
        || canonicalSources.size() != new HashSet<>(canonicalSources).size()) {
      throw new IllegalArgumentException("sedimentary reservoir sources must be non-empty");
    }
    return java.util.stream.Stream.of(
            contribution(
                SedimentaryReservoirKind.CLASTIC_TERRIGENOUS,
                budget.clasticPpm(),
                canonicalSources),
            contribution(
                SedimentaryReservoirKind.VOLCANIC_ASH, budget.volcanicPpm(), canonicalSources),
            contribution(
                SedimentaryReservoirKind.CARBONATE_BIOGENIC, budget.carbonatePpm(), List.of()),
            contribution(SedimentaryReservoirKind.ORGANIC_PEAT, budget.organicPpm(), List.of()),
            contribution(
                SedimentaryReservoirKind.CHEMICAL_PRECIPITATE,
                budget.chemicalPrecipitatePpm(),
                List.of()),
            contribution(
                SedimentaryReservoirKind.EVAPORITIC_BRINE, budget.evaporiticBrinePpm(), List.of()))
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private static SedimentaryReservoirContribution contribution(
      SedimentaryReservoirKind kind, long fractionPpm, List<StableId> sourceBodyIds) {
    return fractionPpm == 0
        ? null
        : new SedimentaryReservoirContribution(kind, fractionPpm, sourceBodyIds);
  }
}
