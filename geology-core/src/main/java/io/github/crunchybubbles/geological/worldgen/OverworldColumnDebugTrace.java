package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.query.MaterialRun;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.List;
import java.util.Locale;

/**
 * Immutable read-only debug projection joining base, air/fluid, and regolith plans for one column.
 *
 * <p>The trace is presentation data only: it carries provenance and interval boundaries but never
 * stores a block or authorizes a world mutation.
 */
public record OverworldColumnDebugTrace(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    int regolithMinYInclusive,
    int surfaceWaterMaxYExclusive,
    int airMinYInclusive,
    List<MaterialRun> baseLithologyRuns,
    MaterialState surfaceMaterial,
    SurfaceClueKind clueKind,
    List<StableId> sourceBodyIds,
    List<StableId> depositIds,
    double weatheringDepth,
    double slope,
    double flowAccumulation,
    double channelDistance) {
  public OverworldColumnDebugTrace {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || regolithMinYInclusive < minYInclusive
        || regolithMinYInclusive > solidMaxYExclusive
        || surfaceWaterMaxYExclusive < solidMaxYExclusive
        || surfaceWaterMaxYExclusive > maxYExclusive
        || airMinYInclusive != surfaceWaterMaxYExclusive
        || baseLithologyRuns == null
        || surfaceMaterial == null
        || clueKind == null
        || sourceBodyIds == null
        || sourceBodyIds.isEmpty()
        || depositIds == null
        || !Double.isFinite(weatheringDepth)
        || weatheringDepth < 0.0
        || !Double.isFinite(slope)
        || slope < 0.0
        || !Double.isFinite(flowAccumulation)
        || flowAccumulation < 0.0
        || flowAccumulation > 1.0
        || !Double.isFinite(channelDistance)
        || channelDistance < 0.0) {
      throw new IllegalArgumentException("Overworld debug trace values are invalid");
    }
    baseLithologyRuns = List.copyOf(baseLithologyRuns);
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    depositIds = List.copyOf(depositIds).stream().sorted().toList();
    if (!surfaceMaterial.depositIds().equals(depositIds)) {
      throw new IllegalArgumentException("debug deposit provenance must match surface material");
    }
  }

  /** Joins plans produced from the same target column and rejects cross-column traces. */
  public static OverworldColumnDebugTrace from(
      OverworldBaseTerrainColumnPlan base,
      OverworldAirFluidColumnPlan air,
      OverworldRegolithColumnPlan regolith) {
    if (base == null || air == null || regolith == null) {
      throw new IllegalArgumentException("base, air/fluid, and regolith plans are required");
    }
    if (base.blockX() != air.blockX()
        || base.blockZ() != air.blockZ()
        || base.blockX() != regolith.blockX()
        || base.blockZ() != regolith.blockZ()
        || base.minYInclusive() != air.minYInclusive()
        || base.maxYExclusive() != air.maxYExclusive()
        || base.minYInclusive() != regolith.minYInclusive()
        || base.maxYExclusive() != regolith.maxYExclusive()
        || base.solidMaxYExclusive() != air.solidMaxYExclusive()
        || base.solidMaxYExclusive() != regolith.solidMaxYExclusive()) {
      throw new IllegalArgumentException("debug plans must describe the same column bounds");
    }
    return new OverworldColumnDebugTrace(
        base.blockX(),
        base.blockZ(),
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        regolith.regolithMinYInclusive(),
        air.surfaceWaterMaxYExclusive(),
        air.airMinYInclusive(),
        base.lithologyRuns(),
        regolith.surfaceMaterial(),
        regolith.clueKind(),
        regolith.sourceBodyIds(),
        regolith.depositIds(),
        regolith.weatheringDepth(),
        regolith.slope(),
        regolith.flowAccumulation(),
        regolith.channelDistance());
  }

  /** Compact deterministic text suitable for a server command or log line. */
  public String summary() {
    return String.format(
        Locale.ROOT,
        "column x=%d z=%d surface=%d regolith=%d..%d water=%d air=%d clue=%s lithology=%s sources=%d deposits=%d weathering=%.3f slope=%.3f flow=%.3f channelDistance=%.3f",
        blockX,
        blockZ,
        solidMaxYExclusive,
        regolithMinYInclusive,
        solidMaxYExclusive,
        surfaceWaterMaxYExclusive,
        airMinYInclusive,
        clueKind,
        surfaceMaterial.lithology(),
        sourceBodyIds.size(),
        depositIds.size(),
        weatheringDepth,
        slope,
        flowAccumulation,
        channelDistance);
  }
}
