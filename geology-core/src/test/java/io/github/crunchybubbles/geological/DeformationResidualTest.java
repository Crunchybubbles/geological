package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.PointQueryTrace;
import org.junit.jupiter.api.Test;

class DeformationResidualTest {
  private static final double RESIDUAL_LIMIT = 1.0 / 256.0;

  @Test
  void forwardAndPullbackRoundTripWithinTheInvertibilityBudget() {
    GeologyQueryEngine query = Phase1World.create(2_025L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    AgeKey oldBody = new AgeKey(500.0, 0);
    for (double x = -700.0; x <= 700.0; x += 73.0) {
      for (double y = -290.0; y <= 290.0; y += 41.0) {
        for (double z = -850.0; z <= 850.0; z += 113.0) {
          Point3 present = new Point3(x, y, z);
          Point3 formation = province.geometry().pullBack(present, oldBody);
          Point3 reconstructed = province.geometry().pushForward(formation, oldBody);
          assertTrue(distance(present, reconstructed) < RESIDUAL_LIMIT);

          Point3 presentFromFormation = province.geometry().pushForward(present, oldBody);
          Point3 reconstructedFormation =
              province.geometry().pullBack(presentFromFormation, oldBody);
          assertTrue(distance(present, reconstructedFormation) < RESIDUAL_LIMIT);

          double determinant =
              province.geometry().fault().pullBackVerticalJacobianDeterminant(present, oldBody);
          assertTrue(determinant >= 0.25 && determinant <= 4.0);
        }
      }
    }
  }

  @Test
  void youngBodiesRemainUndeformedAndPointTraceReportsResidual() {
    GeologyQueryEngine query = Phase1World.create(333L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    Point3 local = new Point3(province.geometry().fault().planeU() + 4.0, 12.0, 0.0);
    AgeKey youngBody = new AgeKey(20.0, 0);
    assertEquals(local, province.geometry().pullBack(local, youngBody));
    assertEquals(local, province.geometry().pushForward(local, youngBody));

    Point3 world = province.frame().toWorld(local);
    PointQueryTrace trace = query.trace(world);
    assertTrue(trace.roundTripResidual() < RESIDUAL_LIMIT);
    assertCandidateEvidenceUsesContainingBounds(trace);
  }

  private static double distance(Point3 first, Point3 second) {
    double x = first.x() - second.x();
    double y = first.y() - second.y();
    double z = first.z() - second.z();
    return StrictMath.sqrt(x * x + y * y + z * z);
  }

  private static void assertCandidateEvidenceUsesContainingBounds(PointQueryTrace trace) {
    assertFalse(trace.candidates().isEmpty());
    assertTrue(
        trace.candidates().stream()
            .allMatch(
                candidate ->
                    candidate
                        .bounds()
                        .containsHorizontal(
                            new Point2(trace.sample().point().x(), trace.sample().point().z()))));
  }
}
