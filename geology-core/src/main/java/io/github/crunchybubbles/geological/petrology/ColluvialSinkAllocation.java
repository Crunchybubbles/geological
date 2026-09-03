package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Point2;

/** Representative route positions for colluvial loss and bypass sinks. */
public record ColluvialSinkAllocation(
    ColluvialSinkState sinkState,
    double transportLossDistanceBlocks,
    Point2 transportLossPoint,
    double bypassDistanceBlocks,
    Point2 bypassPoint) {
  public ColluvialSinkAllocation {
    if (sinkState == null
        || !Double.isFinite(transportLossDistanceBlocks)
        || transportLossDistanceBlocks < 0.0
        || transportLossPoint == null
        || !Double.isFinite(bypassDistanceBlocks)
        || bypassDistanceBlocks < 0.0
        || bypassPoint == null
        || transportLossDistanceBlocks > bypassDistanceBlocks + 1.0e-9) {
      throw new IllegalArgumentException("colluvial sink allocation is invalid");
    }
  }

  /** Derives a bounded loss centroid and endpoint bypass position from one input balance. */
  public static ColluvialSinkAllocation from(ColluvialSedimentBudget.InputBalance balance) {
    if (balance == null) {
      throw new IllegalArgumentException("colluvial input balance is required");
    }
    ColluvialSedimentBudget.TerrainPath path = balance.input().terrainPath();
    double routeDistance = path.routedDistanceBlocks();
    double lossDistance = transportLossCentroidDistance(balance);
    return new ColluvialSinkAllocation(
        balance.sinkState(),
        lossDistance,
        path.pointAtRoutedDistance(lossDistance),
        routeDistance,
        path.sourcePoint());
  }

  public boolean hasTransportLoss() {
    return sinkState.transportLossFraction() > 0.0;
  }

  public boolean hasBypass() {
    return sinkState.bypassFraction() > 0.0;
  }

  private static double transportLossCentroidDistance(
      ColluvialSedimentBudget.InputBalance balance) {
    long lossTotal = balance.transportLossFixedUnits();
    if (lossTotal <= 0) {
      return 0.0;
    }
    int distance = balance.input().terrainPath().distanceBlocks();
    if (distance <= 0) {
      return 0.0;
    }
    ColluvialSedimentBudget.GrainMass loss = balance.transportLossGrainMass();
    ColluvialSedimentBudget.GrainTransportLengths lengths = balance.grainTransportLengths();
    double weighted =
        loss.gravelAndCoarserFixedUnits()
                * exponentialLossCentroid(distance, lengths.gravelAndCoarserBlocks())
            + loss.sandFixedUnits() * exponentialLossCentroid(distance, lengths.sandBlocks())
            + loss.finesFixedUnits() * exponentialLossCentroid(distance, lengths.finesBlocks());
    return clamp(weighted / lossTotal, 0.0, distance);
  }

  private static double exponentialLossCentroid(double distance, double eFoldingDistance) {
    double survival = StrictMath.exp(-distance / eFoldingDistance);
    double lossProbability = -StrictMath.expm1(-distance / eFoldingDistance);
    if (lossProbability <= 0.0) {
      return 0.0;
    }
    return clamp(eFoldingDistance - distance * survival / lossProbability, 0.0, distance);
  }

  private static double clamp(double value, double minimum, double maximum) {
    return StrictMath.max(minimum, StrictMath.min(maximum, value));
  }
}
