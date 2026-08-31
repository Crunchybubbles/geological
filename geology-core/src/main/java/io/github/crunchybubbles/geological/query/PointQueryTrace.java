package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.List;

/** Explainable point result with candidate filtering and deformation round-trip evidence. */
public record PointQueryTrace(
    GeologicalSample sample,
    ProvinceGrammar provinceGrammar,
    List<SpatialCandidate> candidates,
    Point3 presentLocalPoint,
    Point3 afterFaultPullback,
    Point3 formationPoint,
    Point3 reconstructedPresentPoint,
    double roundTripResidual) {
  public PointQueryTrace {
    if (sample == null
        || provinceGrammar == null
        || presentLocalPoint == null
        || afterFaultPullback == null
        || formationPoint == null
        || reconstructedPresentPoint == null
        || !Double.isFinite(roundTripResidual)
        || roundTripResidual < 0.0) {
      throw new IllegalArgumentException("point query trace must be complete and finite");
    }
    candidates = List.copyOf(candidates);
  }
}
