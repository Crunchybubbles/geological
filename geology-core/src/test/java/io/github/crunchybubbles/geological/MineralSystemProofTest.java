package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GateStatus;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.mineral.MineralSystemProofs;
import io.github.crunchybubbles.geological.mineral.PorphyrySystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import io.github.crunchybubbles.geological.query.Phase1World;
import java.util.List;
import org.junit.jupiter.api.Test;

class MineralSystemProofTest {
  @Test
  void proofCatalogContainsFormedAndExplicitlyRejectedOutcomes() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    List<MineralSystemDecision> decisions = query.mineralDecisions(province);
    assertEquals(6, decisions.size());
    assertEquals(
        3, decisions.stream().filter(result -> result.status() == FormationStatus.FORMED).count());
    assertEquals(
        3, decisions.stream().filter(result -> result.status() != FormationStatus.FORMED).count());

    decisions.stream()
        .filter(result -> result.status() == FormationStatus.FORMED)
        .forEach(
            result -> {
              assertTrue(
                  result.gates().stream().allMatch(gate -> gate.status() == GateStatus.PASS));
              assertFalse(result.provenance().isEmpty());
              assertEquals(
                  result.ledger().sourceAmount(),
                  result.ledger().allocations().values().stream().mapToLong(Long::longValue).sum());
            });
    decisions.stream()
        .filter(result -> result.status() != FormationStatus.FORMED)
        .forEach(
            result ->
                assertTrue(
                    result.gates().stream().anyMatch(gate -> gate.status() == GateStatus.FAIL)));
  }

  @Test
  void placerNamesTheFormedPorphyryAsItsUpstreamSource() {
    GeologyQueryEngine query = Phase0World.create(26L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    List<MineralSystemDecision> decisions = query.mineralDecisions(province);
    MineralSystemDecision porphyry = formed(decisions, MineralSystemProofs.PORPHYRY_MODEL);
    MineralSystemDecision placer = formed(decisions, MineralSystemProofs.PLACER_MODEL);
    assertTrue(placer.deposit().sourceIds().contains(porphyry.deposit().id()));
    assertTrue(
        placer.provenance().stream()
            .anyMatch(step -> step.inputIds().contains(porphyry.deposit().id())));
  }

  @Test
  void formedPorphyryPublishesLinkedTopologyAndAlterationZoning() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    PorphyrySystemState state = query.porphyrySystemState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.geometry().plutonPulses().getLast().id(), state.intrusionId());
    assertEquals(
        PorphyrySystemState.FluidSourceClass.MAGMATIC_HYDROTHERMAL, state.fluidSourceClass());
    assertEquals(PorphyrySystemState.StockworkClass.CONNECTED_STOCKWORK, state.stockworkClass());
    assertEquals(3, state.alterationZones().size());
    Point3 center = state.localCenter();
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.POTASSIC_CORE,
        state.zoneAt(center).orElseThrow().kind());
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.PHYLLIC_INTERMEDIATE,
        state.zoneAt(new Point3(center.x() + 90.0, center.y(), center.z())).orElseThrow().kind());
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.PROPYLITIC_DISTAL,
        state.zoneAt(new Point3(center.x() + 170.0, center.y(), center.z())).orElseThrow().kind());
    assertTrue(state.zoneAt(new Point3(center.x() + 250.0, center.y(), center.z())).isEmpty());
    assertEquals(1_000_000L, state.sourceBudgetFixedUnits());
    assertEquals(105_000L, state.depositAllocationFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenPorphyryPublishesDisconnectedTopologyWithFailedGate() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    PorphyrySystemState state = query.porphyrySystemState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(PorphyrySystemState.StockworkClass.DISCONNECTED_STOCKWORK, state.stockworkClass());
    assertTrue(state.alterationZones().isEmpty());
    assertEquals("source", state.failedGate().orElseThrow());
    assertEquals(0L, state.depositAllocationFixedUnits());
  }

  private static MineralSystemDecision formed(
      List<MineralSystemDecision> decisions, String modelId) {
    return decisions.stream()
        .filter(result -> result.modelId().equals(modelId))
        .filter(result -> result.status() == FormationStatus.FORMED)
        .findFirst()
        .orElseThrow();
  }
}
