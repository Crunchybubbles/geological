package io.github.crunchybubbles.geological.petrology;

/** Explicit bounded sampling and threshold policy for the generated colluvial route proof. */
public record ColluvialRoutePolicy(
    double minimumSlope,
    double minimumWeatheringDepth,
    double minimumChannelDistance,
    double gradientStepBlocks,
    double roughnessStencilRadiusBlocks,
    int pathReachLengthBlocks,
    int nearSourceDistanceBlocks,
    int farSourceDistanceBlocks,
    double maximumDeflectionDegrees,
    long weatheredMatrixCapacityFixedUnits,
    long localSourceCapacityFixedUnits,
    long nearSourceCapacityFixedUnits,
    long farSourceCapacityFixedUnits) {
  public static final ColluvialRoutePolicy DEFAULT =
      new ColluvialRoutePolicy(
          0.10, 4.0, 32.0, 4.0, 8.0, 32, 96, 192, 60.0, 350_000L, 350_000L, 200_000L, 100_000L);

  public ColluvialRoutePolicy {
    if (!Double.isFinite(minimumSlope)
        || minimumSlope < 0.0
        || !Double.isFinite(minimumWeatheringDepth)
        || minimumWeatheringDepth < 0.0
        || !Double.isFinite(minimumChannelDistance)
        || minimumChannelDistance < 0.0
        || !Double.isFinite(gradientStepBlocks)
        || gradientStepBlocks <= 0.0
        || !Double.isFinite(roughnessStencilRadiusBlocks)
        || roughnessStencilRadiusBlocks <= 0.0
        || pathReachLengthBlocks <= 0
        || nearSourceDistanceBlocks <= 0
        || farSourceDistanceBlocks <= nearSourceDistanceBlocks
        || nearSourceDistanceBlocks % pathReachLengthBlocks != 0
        || farSourceDistanceBlocks % pathReachLengthBlocks != 0
        || !Double.isFinite(maximumDeflectionDegrees)
        || maximumDeflectionDegrees <= 0.0
        || maximumDeflectionDegrees > 180.0
        || weatheredMatrixCapacityFixedUnits <= 0
        || localSourceCapacityFixedUnits <= 0
        || nearSourceCapacityFixedUnits <= 0
        || farSourceCapacityFixedUnits <= 0
        || Math.addExact(
                Math.addExact(
                    Math.addExact(weatheredMatrixCapacityFixedUnits, localSourceCapacityFixedUnits),
                    nearSourceCapacityFixedUnits),
                farSourceCapacityFixedUnits)
            != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("colluvial route policy is invalid");
    }
  }

  public int routeReachCount() {
    return farSourceDistanceBlocks / pathReachLengthBlocks;
  }
}
