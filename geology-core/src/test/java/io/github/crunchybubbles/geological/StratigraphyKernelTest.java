package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.stratigraphy.StratigraphicCoordinate;
import io.github.crunchybubbles.geological.stratigraphy.StratigraphicPackageKernel;
import org.junit.jupiter.api.Test;

class StratigraphyKernelTest {
  @Test
  void packageOnlapsABoundedWeatheredUnconformity() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    StratigraphicPackageKernel packageKernel = province.geometry().stratigraphicPackage();
    Point2 center = packageKernel.center();
    double base = packageKernel.unconformity().elevation(center);
    StratigraphicCoordinate centerCoordinate =
        packageKernel.evaluate(new Point3(center.x(), base + 1.0, center.z()));
    Point2 nearMargin = new Point2(center.x() + packageKernel.radiusU() * 0.95, center.z());
    StratigraphicCoordinate marginCoordinate =
        packageKernel.evaluate(
            new Point3(
                nearMargin.x(),
                packageKernel.unconformity().elevation(nearMargin) + 0.1,
                nearMargin.z()));

    assertTrue(centerCoordinate.insidePackage());
    assertTrue(marginCoordinate.insidePackage());
    assertTrue(centerCoordinate.thickness() > marginCoordinate.thickness());
    assertEquals(
        Lithology.BASAL_CONGLOMERATE,
        packageKernel.lithologyAt(new Point3(center.x(), base + 1.0, center.z())));
    assertNull(
        packageKernel.lithologyAt(
            new Point3(center.x() + packageKernel.radiusU() * 1.01, base, center.z())));
    assertTrue(
        packageKernel
            .unconformity()
            .insideWeatheringProfile(new Point3(center.x(), base - 1.0, center.z())));
    assertFalse(
        packageKernel
            .unconformity()
            .insideWeatheringProfile(new Point3(center.x(), base + 1.0, center.z())));
  }

  @Test
  void explicitUnconformityIsOrderedOnlyIntoTheNewPhase1Chronicle() {
    Province phase1 = Phase1World.create(42L).atlas().provinceAt(new Point2(0.0, 0.0));
    int erosion = indexOf(phase1, EventType.ERODE_UNCONFORMITY);
    int basin = indexOf(phase1, EventType.OPEN_BASIN);
    int deposition = indexOf(phase1, EventType.DEPOSIT_SEQUENCE);

    assertTrue(erosion < basin);
    assertTrue(basin < deposition);
    assertTrue(
        phase1
            .chronicle()
            .events()
            .get(basin)
            .inputs()
            .contains(phase1.geometry().unconformity().id()));
    assertFalse(
        Phase0World.create(42L)
            .atlas()
            .provinceAt(new Point2(0.0, 0.0))
            .chronicle()
            .events()
            .stream()
            .anyMatch(event -> event.type() == EventType.ERODE_UNCONFORMITY));
  }

  private static int indexOf(Province province, EventType type) {
    for (int index = 0; index < province.chronicle().events().size(); index++) {
      if (province.chronicle().events().get(index).type() == type) {
        return index;
      }
    }
    throw new AssertionError("chronicle does not contain " + type);
  }
}
