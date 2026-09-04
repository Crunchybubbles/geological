package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;

/** Coarse present-surface relationship retained for regolith and future clue presentation. */
public enum SurfaceClueKind {
  BEDROCK_OUTCROP,
  IN_SITU_REGOLITH,
  COLLUVIAL_MANTLE,
  ALLUVIAL_PLACER;

  public static SurfaceClueKind from(SurfaceMaterialKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("surface material kind is required");
    }
    return valueOf(kind.name());
  }
}
