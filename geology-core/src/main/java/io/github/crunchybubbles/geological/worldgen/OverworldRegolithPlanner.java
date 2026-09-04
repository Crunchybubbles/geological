package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Derives a deterministic, chunk-local regolith and surface-clue projection from Phase 2 surface
 * material state.
 *
 * <p>The planner retains source/provenance and transport kind in the plan while projecting only a
 * bounded top-of-solid interval. It does not write air, fluids, neighbors, or persistent clue data.
 */
public final class OverworldRegolithPlanner {
  private final WorldgenExecutionContext context;
  private final OverworldBaseTerrainPlanner baseTerrain;
  private final MaterialQueryEngine material;

  private OverworldRegolithPlanner(WorldgenExecutionContext context) {
    this.context = context;
    this.baseTerrain = OverworldBaseTerrainPlanner.from(context);
    this.material = materialEngine(context.request().worldIdentity());
  }

  /** Creates a planner only for the writable regolith/surface-clue stage of the Overworld. */
  public static OverworldRegolithPlanner from(WorldgenExecutionContext context) {
    Objects.requireNonNull(context, "worldgen execution context");
    if (context.stage() != WorldgenStage.REGOLITH_SURFACE_CLUES) {
      throw new IllegalArgumentException(
          "regolith projection requires the regolith_surface_clues stage");
    }
    if (!"minecraft:overworld".equals(context.request().dimensionKey())) {
      throw new IllegalArgumentException("regolith projection is only defined for the Overworld");
    }
    context.requireWritableTarget();
    return new OverworldRegolithPlanner(context);
  }

  /** Plans one column and clips its regolith interval to the solid terrain already realized. */
  public OverworldRegolithColumnPlan plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = baseTerrain.plan(blockX, blockZ);
    SurfacePetrologicSample surface = material.surface(new Point2(blockX + 0.5, blockZ + 0.5));
    if (!surface.surface().bedrock().provinceId().equals(base.terrainControls().provinceId())) {
      throw new IllegalStateException("regolith and base terrain owners changed between stages");
    }

    int available = base.solidMaxYExclusive() - base.minYInclusive();
    int depth = 0;
    SurfaceClueKind clueKind = SurfaceClueKind.from(surface.context().kind());
    if (clueKind != SurfaceClueKind.BEDROCK_OUTCROP && available > 0) {
      depth =
          Math.min(
              available,
              Math.max(1, (int) StrictMath.floor(surface.surface().fields().weatheringDepth())));
    }
    int regolithMin = base.solidMaxYExclusive() - depth;
    MaterialState state = MaterialState.from(surface.material().geology());
    return new OverworldRegolithColumnPlan(
        base.blockX(),
        base.blockZ(),
        base.minYInclusive(),
        base.maxYExclusive(),
        base.solidMaxYExclusive(),
        regolithMin,
        state,
        clueKind,
        surface.surface().fields().weatheringDepth(),
        surface.surface().fields().slope(),
        surface.surface().fields().drainage().flowAccumulation(),
        surface.surface().fields().drainage().channelDistance(),
        state.rockBodyId(),
        surface.context().sourceBodyIds(),
        state.depositIds());
  }

  /** Plans exactly the target 16×16 footprint in stable X-then-Z order. */
  public List<OverworldRegolithColumnPlan> planTargetChunk() {
    ChunkBlockBounds bounds = context.targetBounds();
    List<OverworldRegolithColumnPlan> columns = new ArrayList<>(256);
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

  public OverworldBaseTerrainPlanner baseTerrain() {
    return baseTerrain;
  }

  public MaterialQueryEngine material() {
    return material;
  }

  private static MaterialQueryEngine materialEngine(WorldIdentity identity) {
    GeologyQueryEngine geology =
        new GeologyQueryEngine(new GeologyAtlas(identity, DimensionProfile.overworldPhase4()));
    WorldIdentity materialIdentity =
        new WorldIdentity(
            identity.worldSeed(),
            Phase2World.MODEL_VERSION,
            Phase2World.SCIENTIFIC_DIGEST,
            identity.dimensionProfileId());
    return new MaterialQueryEngine(geology, Phase2World.materialCatalog(), materialIdentity);
  }
}
