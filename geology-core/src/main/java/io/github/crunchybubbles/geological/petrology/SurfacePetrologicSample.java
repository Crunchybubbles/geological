package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.surface.SurfaceSample;

/** Present-surface fields plus the causally resolved Phase 2 bulk material. */
public record SurfacePetrologicSample(
    SurfaceSample surface, PetrologicSample material, SurfaceMaterialContext context) {
  public SurfacePetrologicSample {
    if (surface == null || material == null || context == null) {
      throw new IllegalArgumentException("surface petrologic sample must be complete");
    }
    if (surface.surfaceMaterial() != material.geology().lithology()
        || surface.surfaceOverprint() != material.geology().overprint()
        || !context.materialBodyId().equals(material.geology().rockBodyId())) {
      throw new IllegalArgumentException("surface and material identities do not agree");
    }
    if (context.kind() == SurfaceMaterialKind.COLLUVIAL_MANTLE) {
      ColluvialSourceMix mix = context.colluvialSourceMix().orElseThrow();
      if (surface.surfaceMaterial() != Lithology.SOIL_COLLUVIUM
          || !mix.sourceBodyId().equals(surface.bedrock().rockBodyId())
          || mix.sourceLithology() != surface.bedrock().lithology()
          || mix.sourceOverprint() != surface.bedrock().overprint()) {
        throw new IllegalArgumentException(
            "colluvial mixture must agree with its surface and local source");
      }
    }
  }
}
