package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.petrology.MineralPhaseRefinementCatalog;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MineralPhaseRefinementCatalogTest {
  @Test
  void solidSolutionRefinementsMatchEveryAuthoredDefinition() {
    assertEquals(
        Phase2World.materialCatalog().solidSolutions().size(),
        MineralPhaseRefinementCatalog.solidSolutions().size());
    assertTrue(
        Phase2World.materialCatalog().solidSolutions().stream()
            .allMatch(
                definition ->
                    MineralPhaseRefinementCatalog.solidSolutions().stream()
                        .anyMatch(
                            refinement -> refinement.definitionId().equals(definition.id()))));

    MineralPhaseRefinementCatalog.SolidSolutionRefinement plagioclase =
        MineralPhaseRefinementCatalog.requireSolidSolution("geological:solid_solution/plagioclase");
    assertTrue(
        plagioclase.accepts(
            Map.of(
                "geological:mineral/albite", 400_000L,
                "geological:mineral/anorthite", 600_000L)));
    assertFalse(
        plagioclase.accepts(
            Map.of(
                "geological:mineral/albite", 400_000L,
                "geological:mineral/anorthite", 500_000L)));
  }

  @Test
  void serpentinePolymorphSelectionUsesStableCoarseWindows() {
    String family = "geological:polymorph_family/serpentine";
    assertEquals(
        "geological:mineral/lizardite",
        MineralPhaseRefinementCatalog.selectPolymorph(family, 100.0, 0.1, true)
            .orElseThrow()
            .mineralId());
    assertEquals(
        "geological:mineral/antigorite",
        MineralPhaseRefinementCatalog.selectPolymorph(family, 450.0, 1.2, true)
            .orElseThrow()
            .mineralId());
    assertTrue(MineralPhaseRefinementCatalog.selectPolymorph(family, 450.0, 1.2, false).isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () -> MineralPhaseRefinementCatalog.selectPolymorph(family, Double.NaN, 1.0, true));
  }
}
