package io.github.crunchybubbles.geological.model;

/** Finite axis-aligned bounds used for conservative candidate filtering. */
public record Bounds3D(
    double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
  public Bounds3D {
    if (!Double.isFinite(minX)
        || !Double.isFinite(minY)
        || !Double.isFinite(minZ)
        || !Double.isFinite(maxX)
        || !Double.isFinite(maxY)
        || !Double.isFinite(maxZ)) {
      throw new IllegalArgumentException("bounds must be finite");
    }
    if (maxX < minX || maxY < minY || maxZ < minZ) {
      throw new IllegalArgumentException("bounds maxima must be greater than minima");
    }
  }

  public Bounds2D horizontal() {
    return new Bounds2D(minX, minZ, maxX, maxZ);
  }

  public boolean contains(Point3 point) {
    return horizontal().contains(new Point2(point.x(), point.z()))
        && point.y() >= minY
        && point.y() <= maxY;
  }

  public boolean containsHorizontal(Point2 point) {
    return horizontal().contains(point);
  }

  public boolean intersects(Bounds3D other) {
    return horizontal().intersects(other.horizontal()) && maxY >= other.minY && other.maxY >= minY;
  }
}
