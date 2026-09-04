package io.github.crunchybubbles.geological.worldgen;

/** Logical chunk-generation order shared by every platform adapter. */
public enum WorldgenStage {
  ACQUIRE_CONTEXT("acquire_context", false),
  COARSE_TERRAIN_CONTROLS("coarse_terrain_controls", false),
  BASE_TERRAIN("base_terrain", true),
  LITHOLOGY("lithology", true),
  STRUCTURES_DEPOSITS_ALTERATION("structures_deposits_alteration", true),
  CAVES_AQUIFERS("caves_aquifers", true),
  REGOLITH_SURFACE_CLUES("regolith_surface_clues", true),
  BIOME_DECORATION("biome_decoration", true),
  VALIDATE_METRICS("validate_metrics", false);

  private final String id;
  private final boolean writesChunk;

  WorldgenStage(String id, boolean writesChunk) {
    this.id = id;
    this.writesChunk = writesChunk;
  }

  /** Stable serialized identity used for domain-separated stage streams. */
  public String id() {
    return id;
  }

  /** Whether this stage is allowed to mutate the currently authorized target chunk. */
  public boolean writesChunk() {
    return writesChunk;
  }

  /** Whether this stage is reached when a request is authorized through {@code other}. */
  public boolean isAtOrBefore(WorldgenStage other) {
    if (other == null) {
      throw new IllegalArgumentException("comparison stage must be present");
    }
    return ordinal() <= other.ordinal();
  }
}
