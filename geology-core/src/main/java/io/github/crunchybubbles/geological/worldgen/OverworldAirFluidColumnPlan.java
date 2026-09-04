package io.github.crunchybubbles.geological.worldgen;

/**
 * Bounded air and surface-water interval for one authorized Overworld column.
 *
 * <p>The interval begins exactly at the geology-owned solid surface. Surface water is optional and
 * ends at the configured sea level; air occupies the remaining interval through the profile
 * envelope. Groundwater and cave fluids are intentionally left to the later caves/aquifers stage.
 */
public record OverworldAirFluidColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    int surfaceWaterMaxYExclusive) {
  public OverworldAirFluidColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || surfaceWaterMaxYExclusive < solidMaxYExclusive
        || surfaceWaterMaxYExclusive > maxYExclusive) {
      throw new IllegalArgumentException("air/fluid column bounds are invalid");
    }
  }

  /** Whether the column has a non-empty surface-water interval. */
  public boolean hasSurfaceWater() {
    return surfaceWaterMaxYExclusive > solidMaxYExclusive;
  }

  /** First air block after the optional surface-water interval. */
  public int airMinYInclusive() {
    return surfaceWaterMaxYExclusive;
  }

  /** Whether the column has a non-empty air interval. */
  public boolean hasAir() {
    return airMinYInclusive() < maxYExclusive;
  }
}
