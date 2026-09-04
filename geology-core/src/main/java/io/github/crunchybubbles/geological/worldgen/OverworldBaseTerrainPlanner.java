package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a bounded, read-only Overworld base-terrain plan from the frozen worldgen context.
 *
 * <p>Every column is evaluated from the same world identity and is clipped at the geology-owned
 * surface. The planner does not access a live world or write a {@code ChunkAccess}; the separate
 * writer bridge owns that side effect and the presentation palette remains injectable.
 */
public final class OverworldBaseTerrainPlanner {
  private static final String OVERWORLD_DIMENSION = "minecraft:overworld";

  private final WorldgenExecutionContext context;
  private final OverworldTerrainControlSampler terrainControls;
  private final GeologyQueryEngine geology;

  private OverworldBaseTerrainPlanner(WorldgenExecutionContext context) {
    this.context = context;
    GeologyAtlas atlas =
        new GeologyAtlas(context.request().worldIdentity(), DimensionProfile.overworldPhase4());
    this.geology = new GeologyQueryEngine(atlas);
    this.terrainControls = OverworldTerrainControlSampler.from(context);
  }

  /** Creates a planner only for the writable base-terrain stage of the canonical Overworld. */
  public static OverworldBaseTerrainPlanner from(WorldgenExecutionContext context) {
    Objects.requireNonNull(context, "worldgen execution context");
    if (context.stage() != WorldgenStage.BASE_TERRAIN) {
      throw new IllegalArgumentException("base terrain requires the base_terrain stage");
    }
    if (!OVERWORLD_DIMENSION.equals(context.request().dimensionKey())) {
      throw new IllegalArgumentException("base terrain is only defined for the Overworld");
    }
    context.requireWritableTarget();
    return new OverworldBaseTerrainPlanner(context);
  }

  /** Plans one block column; the returned runs never extend above the geology-owned surface. */
  public OverworldBaseTerrainColumnPlan plan(long blockX, long blockZ) {
    OverworldTerrainControlSample controls = terrainControls.sample(blockX, blockZ);
    int minY = context.targetBounds().minY();
    int maxY = context.targetBounds().maxYExclusive();
    int solidMaxY = clampSurface(controls.elevation(), minY, maxY);
    ColumnQueryResult geologicalColumn =
        geology.column(new ColumnRequest(blockX + 0.5, blockZ + 0.5, minY, maxY));
    if (!geologicalColumn.provinceId().equals(controls.provinceId())) {
      throw new IllegalStateException("terrain and lithology owners changed between query stages");
    }
    List<MaterialRun> clippedRuns = clipRuns(geologicalColumn.runs(), solidMaxY);
    return new OverworldBaseTerrainColumnPlan(
        blockX,
        blockZ,
        minY,
        maxY,
        solidMaxY,
        controls,
        clippedRuns,
        geologicalColumn.pointEvaluations());
  }

  /** Plans exactly the 16×16 target footprint in stable X-then-Z order. */
  public List<OverworldBaseTerrainColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = context.targetBounds();
    List<OverworldBaseTerrainColumnPlan> columns = new ArrayList<>(256);
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        columns.add(plan(blockX, blockZ));
      }
    }
    return List.copyOf(columns);
  }

  public WorldgenExecutionContext context() {
    return context;
  }

  private static int clampSurface(double elevation, int minY, int maxY) {
    long candidate = (long) StrictMath.floor(elevation + 0.5);
    return (int) Math.max(minY, Math.min(maxY, candidate));
  }

  private static List<MaterialRun> clipRuns(List<MaterialRun> runs, int solidMaxY) {
    List<MaterialRun> clipped = new ArrayList<>();
    for (MaterialRun run : runs) {
      int end = Math.min(run.maxYExclusive(), solidMaxY);
      if (run.minYInclusive() >= end) {
        break;
      }
      if (!clipped.isEmpty() && clipped.getLast().state().equals(run.state())) {
        MaterialRun previous = clipped.removeLast();
        clipped.add(new MaterialRun(previous.minYInclusive(), end, previous.state()));
      } else {
        clipped.add(new MaterialRun(run.minYInclusive(), end, run.state()));
      }
    }
    return List.copyOf(clipped);
  }
}
