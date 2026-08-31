package io.github.crunchybubbles.geological.model;

public record Point3(double x, double y, double z) {
  public Point3 {
    requireFinite(x, "x");
    requireFinite(y, "y");
    requireFinite(z, "z");
  }

  public Point3 withY(double newY) {
    return new Point3(x, newY, z);
  }

  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }
}
