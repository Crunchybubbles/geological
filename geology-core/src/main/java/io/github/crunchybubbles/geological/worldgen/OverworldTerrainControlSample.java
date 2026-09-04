package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;

/**
 * Read-only coarse terrain controls for one Overworld block-column sample.
 *
 * <p>The values are geology-owned controls, not Minecraft block states. A platform adapter may use
 * them to shape a density function, but this record never authorizes a terrain write.
 */
public record OverworldTerrainControlSample(
    long blockX,
    long blockZ,
    StableId provinceId,
    StableId macroDomainId,
    double elevation,
    double uplift,
    double slope,
    double weatheringDepth,
    double flowAccumulation,
    double channelDistance,
    boolean channel,
    boolean outcrop) {
  public OverworldTerrainControlSample {
    if (provinceId == null || macroDomainId == null) {
      throw new IllegalArgumentException("terrain control provenance must be present");
    }
    if (!Double.isFinite(elevation)
        || !Double.isFinite(uplift)
        || !Double.isFinite(slope)
        || slope < 0.0
        || !Double.isFinite(weatheringDepth)
        || weatheringDepth < 0.0
        || !Double.isFinite(flowAccumulation)
        || flowAccumulation < 0.0
        || flowAccumulation > 1.0
        || !Double.isFinite(channelDistance)
        || channelDistance < 0.0) {
      throw new IllegalArgumentException("terrain control values are outside their contracts");
    }
  }
}
