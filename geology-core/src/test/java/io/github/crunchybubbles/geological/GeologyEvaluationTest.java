package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import org.junit.jupiter.api.Test;

class GeologyEvaluationTest {
  @Test
  void youngerPlutonPulseReplacesOlderHosts() {
    GeologyQueryEngine query = Phase0World.create(51L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    RiftArcGeometry.PlutonPulse youngest = province.geometry().plutonPulses().getLast();
    Point2 world =
        province.frame().toWorld(new Point2(youngest.center().x(), youngest.center().z()));
    GeologicalSample sample =
        query.sample(province, new Point3(world.x(), youngest.center().y(), world.z()));
    assertEquals(Lithology.FELSIC_STOCK, sample.lithology());
    assertEquals(youngest.id(), sample.rockBodyId());
  }

  @Test
  void finiteDeformationOnlyPullsBackOlderBodies() {
    GeologyQueryEngine query = Phase0World.create(84L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    RiftArcGeometry geometry = province.geometry();
    Point3 present = new Point3(geometry.fault().planeU() + 10.0, 20.0, 0.0);
    Point3 oldBody = geometry.fault().pullBack(present, new AgeKey(500.0, 0));
    Point3 youngBody = geometry.fault().pullBack(present, new AgeKey(20.0, 0));
    assertNotEquals(present, oldBody);
    assertEquals(present, youngBody);
    assertNotEquals(oldBody, geometry.fold().pullBack(oldBody, new AgeKey(500.0, 0)));
  }

  @Test
  void surfaceEvaluationProducesTerrainWeatheringDrainageAndOutcropState() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    Point2 placerWorld = province.frame().toWorld(province.geometry().placerCenter());
    SurfaceSample sample = query.surface(placerWorld);
    assertTrue(Double.isFinite(sample.fields().elevation()));
    assertTrue(sample.fields().uplift() > 0.0);
    assertTrue(sample.fields().weatheringDepth() >= 0.0);
    assertTrue(sample.fields().drainage().sourceLinkedPlacer());
    assertEquals(Lithology.ALLUVIAL_GRAVEL, sample.surfaceMaterial());
  }
}
