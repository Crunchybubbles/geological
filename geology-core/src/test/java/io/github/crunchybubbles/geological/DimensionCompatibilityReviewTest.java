package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.worldgen.DimensionCompatibilityReview;
import org.junit.jupiter.api.Test;

class DimensionCompatibilityReviewTest {
  @Test
  void passesNativeProfileAndPortalCompatibilityChecks() {
    DimensionCompatibilityReview review =
        DimensionCompatibilityReview.evaluate(8_675_309L, -7L, 11L);

    assertEquals(3, review.profileCount());
    assertEquals(10L, review.portalNetherChunkX());
    assertEquals(-8L, review.portalNetherChunkZ());
    assertTrue(review.profileIdentityDistinct());
    assertTrue(review.portalCoordinateIdentityIsolated());
    assertTrue(review.processContractsValid());
    assertTrue(review.mediumContractsValid());
    assertTrue(review.nativeBoundaryContractsValid());
    assertTrue(review.biomeSubstrateContractsValid());
    assertTrue(review.progressionContractsValid());
    assertTrue(review.traceSeamsStable());
    assertTrue(review.traceTopologiesValid());
    assertTrue(review.allChecksPassed());
    assertTrue(review.failedChecks().isEmpty());
  }

  @Test
  void repeatedReviewIsDeterministic() {
    DimensionCompatibilityReview first = DimensionCompatibilityReview.evaluate(8_675_309L, 3L, -4L);
    DimensionCompatibilityReview second =
        DimensionCompatibilityReview.evaluate(8_675_309L, 3L, -4L);

    assertEquals(first, second);
    assertEquals(first.failedChecks(), second.failedChecks());
  }
}
