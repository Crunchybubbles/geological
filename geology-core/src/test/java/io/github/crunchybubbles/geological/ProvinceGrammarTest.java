package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import io.github.crunchybubbles.geological.query.Phase1World;
import org.junit.jupiter.api.Test;

class ProvinceGrammarTest {
  @Test
  void phase1GrammarUsesANewWorldIdentityWithoutChangingThePhase0Fixture() {
    GeologyQueryEngine phase0 = Phase0World.create(123L);
    GeologyQueryEngine phase1 = Phase1World.create(123L);
    Province phase0Province = phase0.atlas().provinceAt(new Point2(0.0, 0.0));
    Province phase1Province = phase1.atlas().provinceAt(new Point2(0.0, 0.0));

    assertEquals(ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC, phase0Province.grammar());
    assertNotEquals(phase0.atlas().profile().id(), phase1.atlas().profile().id());
    assertNotEquals(phase0Province.id(), phase1Province.id());
  }

  @Test
  void variedGrammarProducesFertileBuriedAndBarrenHistories() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province exhumed =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Province buried =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC);
    Province barren =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);

    assertEquals(3, formedCount(query, exhumed));
    assertEquals(2, formedCount(query, buried));
    assertEquals(0, formedCount(query, barren));
    assertEquals(EventType.ERODE_TRANSPORT_DEPOSIT, exhumed.chronicle().events().getLast().type());
    assertEquals(EventType.ERODE_TRANSPORT, buried.chronicle().events().getLast().type());
    assertEquals(EventType.ERODE_TRANSPORT, barren.chronicle().events().getLast().type());
    assertTrue(
        barren.chronicle().events().stream()
            .noneMatch(event -> event.type() == EventType.MINERALIZE));
    assertTrue(exhumed.chronicle().events().size() > barren.chronicle().events().size());
  }

  @Test
  void barrenCandidatesNeverLeakDepositsIntoPointOrSurfaceQueries() {
    GeologyQueryEngine query = Phase1World.create(4_242L);
    Province fertile =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Province barren =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);

    GeologicalSample fertileVms = atVmsCenter(query, fertile);
    GeologicalSample barrenVms = atVmsCenter(query, barren);
    assertEquals(Lithology.VMS_MASSIVE_SULFIDE, fertileVms.lithology());
    assertNotEquals(Lithology.VMS_MASSIVE_SULFIDE, barrenVms.lithology());
    assertFalse(barrenVms.depositIds().contains(barren.proofIds().vmsDepositId()));

    Point2 fertilePlacer = fertile.frame().toWorld(fertile.geometry().placerCenter());
    Point2 barrenPlacer = barren.frame().toWorld(barren.geometry().placerCenter());
    assertEquals(Lithology.ALLUVIAL_GRAVEL, query.surface(fertilePlacer).surfaceMaterial());
    assertNotEquals(Lithology.ALLUVIAL_GRAVEL, query.surface(barrenPlacer).surfaceMaterial());
  }

  private static long formedCount(GeologyQueryEngine query, Province province) {
    return query.mineralDecisions(province).stream()
        .filter(decision -> decision.status() == FormationStatus.FORMED)
        .count();
  }

  private static GeologicalSample atVmsCenter(GeologyQueryEngine query, Province province) {
    Point3 presentLocal =
        province.geometry().pushForward(province.geometry().vmsCenter(), new AgeKey(241.0, 0));
    Point3 world = province.frame().toWorld(presentLocal);
    return query.sample(province, world);
  }
}
