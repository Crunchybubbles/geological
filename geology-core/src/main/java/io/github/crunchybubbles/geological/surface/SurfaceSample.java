package io.github.crunchybubbles.geological.surface;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.query.GeologicalSample;

public record SurfaceSample(
    SurfaceFields fields,
    GeologicalSample bedrock,
    Lithology surfaceMaterial,
    Overprint surfaceOverprint) {
  public SurfaceSample {
    if (fields == null || bedrock == null || surfaceMaterial == null || surfaceOverprint == null) {
      throw new IllegalArgumentException("surface sample must be complete");
    }
  }
}
