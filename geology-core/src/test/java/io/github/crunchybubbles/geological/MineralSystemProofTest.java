package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GateStatus;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.mineral.MineralSystemProofs;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
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

  private static MineralSystemDecision formed(
      List<MineralSystemDecision> decisions, String modelId) {
    return decisions.stream()
        .filter(result -> result.modelId().equals(modelId))
        .filter(result -> result.status() == FormationStatus.FORMED)
        .findFirst()
        .orElseThrow();
  }
}
