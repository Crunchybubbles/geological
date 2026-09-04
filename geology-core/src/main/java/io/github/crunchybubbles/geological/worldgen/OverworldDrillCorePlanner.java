package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.PetrologicColumnResult;
import io.github.crunchybubbles.geological.petrology.PetrologicRun;
import io.github.crunchybubbles.geological.petrology.PetrologicState;
import io.github.crunchybubbles.geological.petrology.TraceElementVector;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Builds a bounded, transient vertical drill-core log from exact material runs. */
public final class OverworldDrillCorePlanner {
  public static final int MAX_CORE_DEPTH_BLOCKS = 256;
  private static final long VISIBLE_MODE_THRESHOLD_PPM = 20_000L;
  private static final int MAX_VISIBLE_CONSTITUENTS = 8;
  private final OverworldRegolithPlanner regolith;

  private OverworldDrillCorePlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldDrillCorePlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldDrillCorePlanner(Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Drills from the present solid surface downward by at most the bounded core depth. */
  public DrillCoreLog logSurface(long blockX, long blockZ, int depthBlocks) {
    if (depthBlocks < 1 || depthBlocks > MAX_CORE_DEPTH_BLOCKS) {
      throw new IllegalArgumentException(
          "core depth must be between 1 and " + MAX_CORE_DEPTH_BLOCKS);
    }
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    if (!base.hasSolidTerrain()) {
      throw new IllegalArgumentException("drilling requires a solid terrain column");
    }
    int maxY = base.solidMaxYExclusive();
    int minY = Math.max(base.minYInclusive(), maxY - depthBlocks);
    return log(blockX, blockZ, minY, maxY);
  }

  /** Drills an explicitly bounded solid interval, using half-open Y coordinates. */
  public DrillCoreLog log(long blockX, long blockZ, int minYInclusive, int maxYExclusive) {
    if (maxYExclusive <= minYInclusive
        || (long) maxYExclusive - minYInclusive > MAX_CORE_DEPTH_BLOCKS) {
      throw new IllegalArgumentException("core interval exceeds the bounded depth contract");
    }
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    if (minYInclusive < base.minYInclusive() || maxYExclusive > base.solidMaxYExclusive()) {
      throw new IllegalArgumentException("core interval must lie within solid terrain");
    }
    PetrologicColumnResult column =
        regolith
            .material()
            .column(new ColumnRequest(blockX + 0.5, blockZ + 0.5, minYInclusive, maxYExclusive));
    List<DrillCoreInterval> intervals = new ArrayList<>();
    List<StableId> provenance = new ArrayList<>();
    for (PetrologicRun run : column.runs()) {
      PetrologicState state = run.state();
      List<StableId> intervalProvenance = new ArrayList<>();
      intervalProvenance.add(state.geology().rockBodyId());
      intervalProvenance.addAll(state.geology().depositIds());
      intervalProvenance = intervalProvenance.stream().distinct().sorted().toList();
      provenance.addAll(intervalProvenance);
      intervals.add(interval(blockX, blockZ, run, state, intervalProvenance));
    }
    StableId logId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "drill-log",
                CellKey.containing("block", blockX, blockZ, 1),
                minYInclusive)
            .stableId();
    return new DrillCoreLog(
        logId,
        blockX,
        blockZ,
        minYInclusive,
        maxYExclusive,
        intervals,
        provenance.stream().distinct().sorted().toList(),
        column.materialEvaluations());
  }

  private DrillCoreInterval interval(
      long blockX,
      long blockZ,
      PetrologicRun run,
      PetrologicState state,
      List<StableId> provenance) {
    StableId intervalId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "drill-interval",
                CellKey.containing("block", blockX, blockZ, 1),
                run.minYInclusive())
            .stableId();
    Map<String, Long> visible = visibleModes(state.resolvedAssemblage());
    return new DrillCoreInterval(
        intervalId,
        run.minYInclusive(),
        run.maxYExclusive(),
        state.geology(),
        state.rock().id(),
        state.resolvedTexture(),
        visible,
        indicatorSignals(state.traceElementVector()),
        provenance,
        820_000);
  }

  private static Map<String, Long> visibleModes(MaterialAssemblage assemblage) {
    List<Map.Entry<String, Long>> candidates =
        assemblage.modesPpm().entrySet().stream()
            .filter(entry -> entry.getValue() >= VISIBLE_MODE_THRESHOLD_PPM)
            .sorted(
                Map.Entry.<String, Long>comparingByValue()
                    .reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
            .limit(MAX_VISIBLE_CONSTITUENTS)
            .toList();
    if (candidates.isEmpty()) {
      candidates =
          assemblage.modesPpm().entrySet().stream()
              .sorted(
                  Map.Entry.<String, Long>comparingByValue()
                      .reversed()
                      .thenComparing(Map.Entry.comparingByKey()))
              .limit(1)
              .toList();
    }
    TreeMap<String, Long> visible = new TreeMap<>();
    candidates.forEach(entry -> visible.put(entry.getKey(), entry.getValue()));
    return Collections.unmodifiableMap(visible);
  }

  private static Map<ChemicalElement, Long> indicatorSignals(TraceElementVector trace) {
    TreeMap<ChemicalElement, Long> result = new TreeMap<>();
    trace
        .concentrationPpm()
        .forEach((element, amount) -> result.put(element, coarseSignal(amount)));
    return Collections.unmodifiableMap(result);
  }

  private static long coarseSignal(long amount) {
    if (amount <= 10L) {
      return 1L;
    }
    if (amount <= 100L) {
      return 10L;
    }
    if (amount <= 1_000L) {
      return 100L;
    }
    if (amount <= 10_000L) {
      return 1_000L;
    }
    if (amount <= 100_000L) {
      return 10_000L;
    }
    if (amount <= 500_000L) {
      return 100_000L;
    }
    return MaterialAssemblage.SCALE;
  }
}
