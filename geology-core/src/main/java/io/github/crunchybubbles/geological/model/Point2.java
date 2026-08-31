package io.github.crunchybubbles.geological.model;

public record Point2(double x, double z) {
  public Point2 {
    requireFinite(x, "x");
    requireFinite(z, "z");
  }

  public double squaredDistance(Point2 other) {
    double dx = x - other.x;
    double dz = z - other.z;
    return dx * dx + dz * dz;
  }

  public Point2 add(double deltaX, double deltaZ) {
    return new Point2(x + deltaX, z + deltaZ);
  }

  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }
}
