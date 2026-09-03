package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Exact proof-level mixture along one bounded adaptive terrain route. */
public record ColluvialSourceMix(
    Point2 initialUpslopeDirection,
    List<ColluvialSourceContribution> sourceContributions,
    long weatheredMatrixFractionPpm,
    ColluvialTextureState textureState,
    ColluvialPhysicalState physicalState,
    ColluvialSedimentBudget sedimentBudget,
    ColluvialHorizonState horizonState,
    ColluvialRoutePolicy routePolicy,
    List<ColluvialSinkDestination> sinkDestinations) {
  public static final double MAXIMUM_ROUTE_DEFLECTION_DEGREES =
      ColluvialRoutePolicy.DEFAULT.maximumDeflectionDegrees();

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
        ColluvialHorizonState.from(sedimentBudget),
        ColluvialRoutePolicy.DEFAULT,
        List.of());
  }

  public ColluvialSourceMix(
      Point2 initialUpslopeDirection,
      List<ColluvialSourceContribution> sourceContributions,
      long weatheredMatrixFractionPpm,
      ColluvialTextureState textureState,
      ColluvialPhysicalState physicalState,
      ColluvialSedimentBudget sedimentBudget,
      ColluvialHorizonState horizonState,
      ColluvialRoutePolicy routePolicy) {
    this(
        initialUpslopeDirection,
        sourceContributions,
        weatheredMatrixFractionPpm,
        textureState,
        physicalState,
        sedimentBudget,
        horizonState,
        routePolicy,
        List.of());
  }

  public ColluvialSourceMix {
    if (initialUpslopeDirection == null
        || sourceContributions == null
        || textureState == null
        || physicalState == null
        || sedimentBudget == null
        || horizonState == null
        || routePolicy == null
        || sinkDestinations == null) {
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
        > routePolicy.maximumDeflectionDegrees() + 1.0e-8) {
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
    sinkDestinations =
        List.copyOf(sinkDestinations).stream()
            .sorted(
                Comparator.comparingInt(ColluvialSinkDestination::upslopeDistanceBlocks)
                    .thenComparing(
                        destination ->
                            destination.sourceBodyId().map(StableId::toString).orElse(""))
                    .thenComparing(ColluvialSinkDestination::sinkRole))
            .toList();
    if (!sinkDestinations.isEmpty()) {
      validateSinkDestinations(sinkDestinations, sedimentBudget);
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

  private static void validateSinkDestinations(
      List<ColluvialSinkDestination> destinations, ColluvialSedimentBudget budget) {
    Set<String> actualKeys = new HashSet<>();
    for (ColluvialSinkDestination destination : destinations) {
      String key =
          destinationKey(
              destination.sinkRole(),
              destination.sourceBodyId(),
              destination.upslopeDistanceBlocks());
      if (!actualKeys.add(key)) {
        throw new IllegalArgumentException("colluvial sink destination evidence must be unique");
      }
      ColluvialSedimentBudget.InputBalance balance =
          balanceFor(destination.sourceBodyId(), destination.upslopeDistanceBlocks(), budget);
      ColluvialSinkAllocation allocation = balance.sinkAllocation();
      Point2 expectedPoint =
          switch (destination.sinkRole()) {
            case INTERMEDIATE_ROUTE_STORAGE -> {
              if (!allocation.hasTransportLoss()) {
                throw new IllegalArgumentException("transport-loss destination has no loss mass");
              }
              yield allocation.transportLossPoint();
            }
            case DOWNSTREAM_CONTINUATION -> {
              if (!allocation.hasBypass()) {
                throw new IllegalArgumentException("bypass destination has no bypass mass");
              }
              yield allocation.bypassPoint();
            }
            case NONE -> throw new IllegalArgumentException("inactive sink destination is invalid");
          };
      if (!expectedPoint.equals(destination.point())) {
        throw new IllegalArgumentException(
            "colluvial sink destination must match its route allocation");
      }
    }
    Set<String> expectedKeys = new HashSet<>();
    expectedSinkKey(expectedKeys, Optional.empty(), 0, budget.weatheredMatrixBalance());
    for (ColluvialSedimentBudget.SourceBalance source : budget.sourceBalances()) {
      expectedSinkKey(
          expectedKeys,
          Optional.of(source.sourceBodyId()),
          source.upslopeDistanceBlocks(),
          source.balance());
    }
    if (!actualKeys.equals(expectedKeys)) {
      throw new IllegalArgumentException(
          "colluvial sink destination evidence must cover active sinks");
    }
  }

  private static void expectedSinkKey(
      Set<String> keys,
      Optional<StableId> sourceBodyId,
      int distance,
      ColluvialSedimentBudget.InputBalance balance) {
    if (balance.sinkAllocation().hasTransportLoss()) {
      keys.add(
          destinationKey(
              ColluvialSinkState.SinkRole.INTERMEDIATE_ROUTE_STORAGE, sourceBodyId, distance));
    }
    if (balance.sinkAllocation().hasBypass()) {
      keys.add(
          destinationKey(
              ColluvialSinkState.SinkRole.DOWNSTREAM_CONTINUATION, sourceBodyId, distance));
    }
  }

  private static ColluvialSedimentBudget.InputBalance balanceFor(
      Optional<StableId> sourceBodyId, int distance, ColluvialSedimentBudget budget) {
    if (sourceBodyId.isEmpty()) {
      if (distance != 0) {
        throw new IllegalArgumentException("weathered-matrix sink must be local");
      }
      return budget.weatheredMatrixBalance();
    }
    return budget.sourceBalances().stream()
        .filter(
            source ->
                source.sourceBodyId().equals(sourceBodyId.orElseThrow())
                    && source.upslopeDistanceBlocks() == distance)
        .map(ColluvialSedimentBudget.SourceBalance::balance)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown colluvial sink source"));
  }

  private static String destinationKey(
      ColluvialSinkState.SinkRole role, Optional<StableId> sourceBodyId, int distance) {
    return role.name()
        + ":"
        + sourceBodyId.map(StableId::toString).orElse("matrix")
        + ":"
        + distance;
  }

  private static long sourceAssemblageFractionPpm(List<ColluvialSourceContribution> contributions) {
    long total = 0;
    for (ColluvialSourceContribution contribution : contributions) {
      total = Math.addExact(total, contribution.assemblageFractionPpm());
    }
    return total;
  }
}
