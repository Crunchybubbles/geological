package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Comparator;
import java.util.List;

/** Exact proof-level mixture of bounded source samples and generic weathered matrix. */
public record ColluvialSourceMix(
    Point2 upslopeDirection,
    List<ColluvialSourceContribution> sourceContributions,
    long weatheredMatrixFractionPpm,
    ColluvialTextureState textureState,
    ColluvialPhysicalState physicalState,
    ColluvialSedimentBudget sedimentBudget) {
  public ColluvialSourceMix {
    if (upslopeDirection == null
        || sourceContributions == null
        || textureState == null
        || physicalState == null
        || sedimentBudget == null) {
      throw new IllegalArgumentException(
          "colluvial direction, sources, texture, physical state, and budget are required");
    }
    if (!textureState.equals(physicalState.textureState())) {
      throw new IllegalArgumentException("colluvial texture and physical state must agree");
    }
    double directionLength = StrictMath.hypot(upslopeDirection.x(), upslopeDirection.z());
    if (StrictMath.abs(directionLength - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("colluvial upslope direction must be a unit vector");
    }
    sourceContributions =
        List.copyOf(sourceContributions).stream()
            .sorted(
                Comparator.comparingInt(ColluvialSourceContribution::upslopeDistanceBlocks)
                    .thenComparing(ColluvialSourceContribution::sourceBodyId)
                    .thenComparing(ColluvialSourceContribution::sourceLithology)
                    .thenComparing(ColluvialSourceContribution::sourceOverprint))
            .toList();
    if (sourceContributions.isEmpty()
        || sourceContributions.getFirst().upslopeDistanceBlocks() != 0) {
      throw new IllegalArgumentException("colluvial mixture must include its local source");
    }
    Point2 localPoint = sourceContributions.getFirst().sourcePoint();
    for (ColluvialSourceContribution contribution : sourceContributions) {
      Point2 expected =
          localPoint.add(
              upslopeDirection.x() * contribution.upslopeDistanceBlocks(),
              upslopeDirection.z() * contribution.upslopeDistanceBlocks());
      if (!expected.equals(contribution.sourcePoint())) {
        throw new IllegalArgumentException(
            "colluvial source point must follow its exact upslope distance");
      }
    }
    if (sourceContributions.stream()
            .map(ColluvialSourceContribution::upslopeDistanceBlocks)
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
    if (!sedimentBudget.matches(sourceContributions, weatheredMatrixFractionPpm)) {
      throw new IllegalArgumentException("colluvial sediment budget must match the source mixture");
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
