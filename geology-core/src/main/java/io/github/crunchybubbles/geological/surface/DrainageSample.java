package io.github.crunchybubbles.geological.surface;

import io.github.crunchybubbles.geological.model.Point2;

public record DrainageSample(
    double channelDistance,
    double flowAccumulation,
    Point2 flowDirection,
    double downstreamCoordinate,
    double hydraulicTrapScore,
    boolean channel,
    boolean sourceLinkedPlacer) {
  public DrainageSample {
    if (!Double.isFinite(channelDistance)
        || channelDistance < 0.0
        || !Double.isFinite(flowAccumulation)
        || flowAccumulation < 0.0
        || flowAccumulation > 1.0
        || flowDirection == null
        || !Double.isFinite(downstreamCoordinate)
        || !Double.isFinite(hydraulicTrapScore)
        || hydraulicTrapScore < 0.0
        || hydraulicTrapScore > 1.0) {
      throw new IllegalArgumentException("drainage sample values are outside their contracts");
    }
  }
}
