package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.List;

/** Exact proof-level mixture of bounded source samples and generic weathered matrix. */
public record ColluvialSourceMix(
    List<ColluvialSourceContribution> sourceContributions, long weatheredMatrixFractionPpm) {
  public ColluvialSourceMix {
    if (sourceContributions == null) {
      throw new IllegalArgumentException("colluvial source contributions are required");
    }
    sourceContributions =
        List.copyOf(sourceContributions).stream()
            .sorted(
                Comparator.comparingInt(ColluvialSourceContribution::upstreamDistanceBlocks)
                    .thenComparing(ColluvialSourceContribution::sourceBodyId)
                    .thenComparing(ColluvialSourceContribution::sourceLithology)
                    .thenComparing(ColluvialSourceContribution::sourceOverprint))
            .toList();
    if (sourceContributions.isEmpty()
        || sourceContributions.getFirst().upstreamDistanceBlocks() != 0) {
      throw new IllegalArgumentException("colluvial mixture must include its local source");
    }
    if (sourceContributions.stream()
            .map(ColluvialSourceContribution::upstreamDistanceBlocks)
            .distinct()
            .count()
        != sourceContributions.size()) {
      throw new IllegalArgumentException("colluvial source distances must be unique");
    }
    if (weatheredMatrixFractionPpm <= 0 || weatheredMatrixFractionPpm >= MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "colluvial weathered-matrix fraction must lie inside (0, scale)");
    }
    if (sourceAssemblageFractionPpm(sourceContributions) + weatheredMatrixFractionPpm
        != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "colluvial mixture fractions must close to " + MaterialAssemblage.SCALE);
    }
  }

  public long sourceAssemblageFractionPpm() {
    return sourceAssemblageFractionPpm(sourceContributions);
  }

  public List<StableId> sourceBodyIds() {
    return sourceContributions.stream()
        .map(ColluvialSourceContribution::sourceBodyId)
        .distinct()
        .sorted()
        .toList();
  }

  public ColluvialSourceContribution localSource() {
    return sourceContributions.getFirst();
  }

  private static long sourceAssemblageFractionPpm(List<ColluvialSourceContribution> contributions) {
    long total = 0;
    for (ColluvialSourceContribution contribution : contributions) {
      total = Math.addExact(total, contribution.assemblageFractionPpm());
    }
    return total;
  }
}
