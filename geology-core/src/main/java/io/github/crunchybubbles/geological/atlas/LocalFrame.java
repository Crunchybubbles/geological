package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;

/** Stable province-local coordinates keep continuous evaluation precise far from the origin. */
public record LocalFrame(Point2 origin, double rotationRadians) {
  public LocalFrame {
    if (origin == null || !Double.isFinite(rotationRadians)) {
      throw new IllegalArgumentException("local frame values must be finite");
    }
  }

  public Point2 toLocal(Point2 world) {
    double cosine = StrictMath.cos(rotationRadians);
    double sine = StrictMath.sin(rotationRadians);
    double deltaX = world.x() - origin.x();
    double deltaZ = world.z() - origin.z();
    return new Point2(cosine * deltaX + sine * deltaZ, -sine * deltaX + cosine * deltaZ);
  }

  public Point3 toLocal(Point3 world) {
    Point2 horizontal = toLocal(new Point2(world.x(), world.z()));
    return new Point3(horizontal.x(), world.y(), horizontal.z());
  }

  public Point2 toWorld(Point2 local) {
    double cosine = StrictMath.cos(rotationRadians);
    double sine = StrictMath.sin(rotationRadians);
    return new Point2(
        origin.x() + cosine * local.x() - sine * local.z(),
        origin.z() + sine * local.x() + cosine * local.z());
  }

  public Point3 toWorld(Point3 local) {
    Point2 horizontal = toWorld(new Point2(local.x(), local.z()));
    return new Point3(horizontal.x(), local.y(), horizontal.z());
  }
}
