package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.query.MaterialRun;
import java.util.List;

/**
 * Transient base-terrain plan for one authorized Overworld column.
 *
 * <p>The surface boundary is a presentation-independent block height. Material runs are clipped to
 * the solid part of the column; air, fluids, and Minecraft block states are deliberately left to
 * the platform palette/writer increment.
 */
public record OverworldBaseTerrainColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    OverworldTerrainControlSample terrainControls,
    List<MaterialRun> lithologyRuns,
    int geologicalPointEvaluations) {
  public OverworldBaseTerrainColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || terrainControls == null
        || terrainControls.blockX() != blockX
        || terrainControls.blockZ() != blockZ
        || geologicalPointEvaluations < 0) {
      throw new IllegalArgumentException("base-terrain column plan bounds are invalid");
    }
    lithologyRuns = List.copyOf(lithologyRuns);
    int expectedY = minYInclusive;
    for (MaterialRun run : lithologyRuns) {
      if (run.minYInclusive() != expectedY
          || run.maxYExclusive() > solidMaxYExclusive
          || run.maxYExclusive() <= run.minYInclusive()) {
        throw new IllegalArgumentException("lithology runs must cover the clipped solid interval");
      }
      expectedY = run.maxYExclusive();
    }
    if (expectedY != solidMaxYExclusive) {
      throw new IllegalArgumentException("lithology runs must cover the clipped solid interval");
    }
    if (solidMaxYExclusive == minYInclusive && !lithologyRuns.isEmpty()) {
      throw new IllegalArgumentException("an empty solid interval cannot contain lithology runs");
    }
  }

  public boolean hasSolidTerrain() {
    return solidMaxYExclusive > minYInclusive;
  }
}
