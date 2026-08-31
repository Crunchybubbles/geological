package io.github.crunchybubbles.geological.model;

public record Bounds2D(double minX, double minZ, double maxX, double maxZ) {
  public Bounds2D {
    if (!Double.isFinite(minX)
        || !Double.isFinite(minZ)
        || !Double.isFinite(maxX)
        || !Double.isFinite(maxZ)) {
      throw new IllegalArgumentException("bounds must be finite");
    }
    if (maxX < minX || maxZ < minZ) {
      throw new IllegalArgumentException("bounds maxima must be greater than minima");
    }
  }

  public boolean contains(Point2 point) {
    return point.x() >= minX && point.x() <= maxX && point.z() >= minZ && point.z() <= maxZ;
  }

  public boolean intersects(Bounds2D other) {
    return maxX >= other.minX && other.maxX >= minX && maxZ >= other.minZ && other.maxZ >= minZ;
  }

  public Bounds2D expand(double amount) {
    if (!(amount >= 0.0) || !Double.isFinite(amount)) {
      throw new IllegalArgumentException("expansion must be finite and non-negative");
    }
    return new Bounds2D(minX - amount, minZ - amount, maxX + amount, maxZ + amount);
  }
}
