package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.model.Point2;

/** Integer block-column interval with samples evaluated at block centers. */
public record ColumnRequest(double x, double z, int minYInclusive, int maxYExclusive) {
  private static final int MAX_COLUMN_HEIGHT = 4096;

  public ColumnRequest {
    if (!Double.isFinite(x) || !Double.isFinite(z)) {
      throw new IllegalArgumentException("column coordinates must be finite");
    }
    if (maxYExclusive <= minYInclusive) {
      throw new IllegalArgumentException("column interval must be non-empty");
    }
    if ((long) maxYExclusive - minYInclusive > MAX_COLUMN_HEIGHT) {
      throw new IllegalArgumentException("column exceeds the bounded height cap");
    }
  }

  public Point2 horizontalPoint() {
    return new Point2(x, z);
  }

  public int height() {
    return maxYExclusive - minYInclusive;
  }
}
