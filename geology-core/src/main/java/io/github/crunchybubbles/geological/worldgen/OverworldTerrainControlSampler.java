package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.surface.DrainageSample;
import io.github.crunchybubbles.geological.surface.OverworldSurfaceModel;
import io.github.crunchybubbles.geological.surface.SurfaceFields;
import java.util.Objects;

/**
 * Deterministic, random-access Overworld terrain-control evaluator for one worldgen context.
 *
 * <p>The sampler reconstructs only bounded atlas descriptors and coarse surface fields. It does not
 * retain a Minecraft world, chunk, server, or mutable random generator, and it never writes a
 * block. Its caches are the same bounded caches used by the platform-neutral atlas/surface proof.
 */
public final class OverworldTerrainControlSampler {
  private static final String OVERWORLD_DIMENSION = "minecraft:overworld";

  private final WorldgenExecutionContext context;
  private final GeologyAtlas atlas;
  private final OverworldSurfaceModel surface;

  private OverworldTerrainControlSampler(WorldgenExecutionContext context) {
    this.context = context;
    this.atlas =
        new GeologyAtlas(context.request().worldIdentity(), DimensionProfile.overworldPhase4());
    this.surface = new OverworldSurfaceModel(context.request().worldIdentity());
  }

  /** Creates a sampler only for the read-only coarse-terrain stage of the canonical Overworld. */
  public static OverworldTerrainControlSampler from(WorldgenExecutionContext context) {
    Objects.requireNonNull(context, "worldgen execution context");
    if (context.stage() != WorldgenStage.COARSE_TERRAIN_CONTROLS) {
      throw new IllegalArgumentException(
          "terrain controls require the coarse_terrain_controls stage");
    }
    if (!OVERWORLD_DIMENSION.equals(context.request().dimensionKey())) {
      throw new IllegalArgumentException("terrain controls are only defined for the Overworld");
    }
    return new OverworldTerrainControlSampler(context);
  }

  /** Evaluates a block-column center without changing the context or consuming shared RNG state. */
  public OverworldTerrainControlSample sample(long blockX, long blockZ) {
    Point2 point = new Point2(blockX + 0.5, blockZ + 0.5);
    Province province = atlas.provinceAt(point);
    SurfaceFields fields = surface.evaluate(province, point);
    DrainageSample drainage = fields.drainage();
    return new OverworldTerrainControlSample(
        blockX,
        blockZ,
        province.id(),
        province.macroDomainId(),
        fields.elevation(),
        fields.uplift(),
        fields.slope(),
        fields.weatheringDepth(),
        drainage.flowAccumulation(),
        drainage.channelDistance(),
        drainage.channel(),
        fields.outcrop());
  }

  /** Discards bounded reconstruction caches; the next sample is identical after reconstruction. */
  public void clearCaches() {
    atlas.clearCaches();
    surface.clearCaches();
  }

  public WorldgenExecutionContext context() {
    return context;
  }
}
