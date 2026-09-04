package io.github.crunchybubbles.geological.worldgen;

/** Bounded deterministic survey window used to measure exploration clues and travel burden. */
public record ExplorationTelemetryRequest(
    long centerX, long centerZ, int radiusBlocks, int spacingBlocks) {
  public static final int MAX_RADIUS_BLOCKS = 256;
  public static final int MAX_SPACING_BLOCKS = 64;
  public static final int MAX_GRID_CELLS = 4096;

  public ExplorationTelemetryRequest {
    if (radiusBlocks < 0
        || radiusBlocks > MAX_RADIUS_BLOCKS
        || spacingBlocks < 1
        || spacingBlocks > MAX_SPACING_BLOCKS) {
      throw new IllegalArgumentException("exploration telemetry window values are invalid");
    }
    long steps = radiusBlocks / (long) spacingBlocks;
    long cells = Math.multiplyExact(steps * 2L + 1L, steps * 2L + 1L);
    if (cells > MAX_GRID_CELLS) {
      throw new IllegalArgumentException(
          "exploration telemetry window exceeds " + MAX_GRID_CELLS + " cells");
    }
  }

  public int gridSteps() {
    return radiusBlocks / spacingBlocks;
  }

  /** Actual radius reached by the integer-spaced survey grid. */
  public int effectiveRadiusBlocks() {
    return gridSteps() * spacingBlocks;
  }

  public int cells() {
    int side = gridSteps() * 2 + 1;
    return Math.multiplyExact(side, side);
  }
}
