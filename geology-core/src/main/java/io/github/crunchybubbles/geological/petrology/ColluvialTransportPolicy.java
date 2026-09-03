package io.github.crunchybubbles.geological.petrology;

/** Explicit bounded response policy for normalized colluvial production and transport. */
public record ColluvialTransportPolicy(
    double weatheringDepthReference,
    double slopeMobilityReference,
    double minimumSlopeMobility,
    double minimumRunoffMobilityResponse,
    double minimumTransportSlopeResponse,
    double minimumTransportRoughnessResponse,
    double minimumTransportPathResponse,
    double minimumTransportRouteGradeResponse,
    double minimumTransportRunoffResponse,
    double gravelAndCoarserReferenceEFoldingDistanceBlocks,
    double sandReferenceEFoldingDistanceBlocks,
    double finesReferenceEFoldingDistanceBlocks,
    double maximumBypassFraction) {
  public static final ColluvialTransportPolicy DEFAULT =
      new ColluvialTransportPolicy(
          12.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 0.50);

  public ColluvialTransportPolicy {
    requirePositive(weatheringDepthReference, "weathering-depth reference");
    requirePositive(slopeMobilityReference, "slope-mobility reference");
    requireUnit(minimumSlopeMobility, "minimum slope mobility");
    requireUnit(minimumRunoffMobilityResponse, "minimum runoff mobility response");
    requireUnit(minimumTransportSlopeResponse, "minimum transport slope response");
    requireUnit(minimumTransportRoughnessResponse, "minimum transport roughness response");
    requireUnit(minimumTransportPathResponse, "minimum transport path response");
    requireUnit(minimumTransportRouteGradeResponse, "minimum transport route-grade response");
    requireUnit(minimumTransportRunoffResponse, "minimum transport runoff response");
    requirePositive(
        gravelAndCoarserReferenceEFoldingDistanceBlocks,
        "gravel-and-coarser reference e-folding distance");
    requirePositive(sandReferenceEFoldingDistanceBlocks, "sand reference e-folding distance");
    requirePositive(finesReferenceEFoldingDistanceBlocks, "fines reference e-folding distance");
    requireUnit(maximumBypassFraction, "maximum bypass fraction");
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }
}
