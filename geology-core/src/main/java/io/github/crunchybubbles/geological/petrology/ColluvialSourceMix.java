package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Comparator;
import java.util.List;

/** Exact proof-level mixture along one bounded adaptive terrain route. */
public record ColluvialSourceMix(
    Point2 initialUpslopeDirection,
    List<ColluvialSourceContribution> sourceContributions,
    long weatheredMatrixFractionPpm,
    ColluvialTextureState textureState,
    ColluvialPhysicalState physicalState,
    ColluvialSedimentBudget sedimentBudget,
    ColluvialHorizonState horizonState) {
  public static final double MAXIMUM_ROUTE_DEFLECTION_DEGREES = 60.0;

  public ColluvialSourceMix(
      Point2 initialUpslopeDirection,
      List<ColluvialSourceContribution> sourceContributions,
      long weatheredMatrixFractionPpm,
      ColluvialTextureState textureState,
      ColluvialPhysicalState physicalState,
      ColluvialSedimentBudget sedimentBudget) {
    this(
        initialUpslopeDirection,
        sourceContributions,
        weatheredMatrixFractionPpm,
        textureState,
        physicalState,
        sedimentBudget,
        ColluvialHorizonState.from(sedimentBudget));
  }

  public ColluvialSourceMix {
    if (initialUpslopeDirection == null
        || sourceContributions == null
        || textureState == null
        || physicalState == null
        || sedimentBudget == null
        || horizonState == null) {
      throw new IllegalArgumentException(
          "colluvial initial direction, sources, texture, physical state, budget, and horizon state are required");
    }
    if (!textureState.equals(physicalState.textureState())) {
      throw new IllegalArgumentException("colluvial texture and physical state must agree");
    }
    double directionLength =
        StrictMath.hypot(initialUpslopeDirection.x(), initialUpslopeDirection.z());
    if (StrictMath.abs(directionLength - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException(
          "colluvial initial upslope direction must be a unit vector");
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
    List<ColluvialSedimentBudget.SourceBalance> sourceBalances = sedimentBudget.sourceBalances();
    for (int index = 0; index < sourceContributions.size(); index++) {
      if (!sourceBalances
          .get(index)
          .balance()
          .input()
          .terrainPath()
          .sourcePoint()
          .equals(sourceContributions.get(index).sourcePoint())) {
        throw new IllegalArgumentException(
            "colluvial source point must terminate its sediment path");
      }
    }
    ColluvialSedimentBudget.TerrainPath longestPath =
        sourceBalances.getLast().balance().input().terrainPath();
    if (longestPath.maximumDeflectionFromInitialDegrees()
        > MAXIMUM_ROUTE_DEFLECTION_DEGREES + 1.0e-8) {
      throw new IllegalArgumentException(
          "colluvial route exceeds its maximum deflection from the initial direction");
    }
    if (longestPath.reachCount() > 0) {
      Point2 origin = longestPath.samples().getFirst().point();
      Point2 next = longestPath.samples().get(1).point();
      double routedDirectionX = (next.x() - origin.x()) / longestPath.reachLengthBlocks();
      double routedDirectionZ = (next.z() - origin.z()) / longestPath.reachLengthBlocks();
      if (StrictMath.hypot(
              routedDirectionX - initialUpslopeDirection.x(),
              routedDirectionZ - initialUpslopeDirection.z())
          > 1.0e-9) {
        throw new IllegalArgumentException(
            "colluvial initial direction must match the first routed reach");
      }
    }
    if (!textureState.grainSize().equals(sedimentBudget.depositedGrainSize())) {
      throw new IllegalArgumentException(
          "colluvial texture must match the grain-resolved deposited inventory");
    }
    if (!horizonState.matches(sedimentBudget)) {
      throw new IllegalArgumentException("colluvial horizon state must match its sediment budget");
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
