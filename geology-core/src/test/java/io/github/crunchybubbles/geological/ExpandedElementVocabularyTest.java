package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.petrology.BulkComposition;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MagmaDifferentiationState;
import io.github.crunchybubbles.geological.petrology.MagmaResidualInventoryState;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.TraceElementVector;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpandedElementVocabularyTest {
  @Test
  void expandedTraceVectorKeepsNewPathfindersSparse() {
    BulkComposition composition =
        new BulkComposition(
            Map.of(
                ChemicalElement.SI, 300_000L,
                ChemicalElement.O, 300_000L,
                ChemicalElement.NI, 150_000L,
                ChemicalElement.MO, 100_000L,
                ChemicalElement.U, 100_000L,
                ChemicalElement.LI, 50_000L),
            3.0);

    TraceElementVector trace = TraceElementVector.from(composition);

    assertEquals(4, trace.concentrationPpm().size());
    assertEquals(150_000L, trace.concentrationPpm(ChemicalElement.NI));
    assertEquals(100_000L, trace.concentrationPpm(ChemicalElement.U));
    assertTrue(trace.log10PpmMicros(ChemicalElement.MO) > 0L);
    assertEquals(0L, trace.concentrationPpm(ChemicalElement.SI));
  }

  @Test
  void expandedResidualInventorySplitsNewIncompatibleElementsExactly() {
    BulkComposition composition =
        new BulkComposition(
            Map.of(
                ChemicalElement.SI, 300_000L,
                ChemicalElement.O, 300_000L,
                ChemicalElement.LI, 100_000L,
                ChemicalElement.MO, 100_000L,
                ChemicalElement.U, 100_000L,
                ChemicalElement.B, 100_000L),
            3.0);
    StableId source = StableId.parse("00000000000000000000000000000031");

    MagmaResidualInventoryState state =
        MagmaResidualInventoryState.proofFor(
            composition, MagmaDifferentiationState.arcProofFor(2, java.util.List.of(source)));

    assertEquals(4, state.sourceInventoryPpm().size());
    assertEquals(
        MaterialAssemblage.SCALE,
        state.cumulativeCrystalFractionPpm() + state.residualMeltFractionPpm());
    for (ChemicalElement element : state.sourceInventoryPpm().keySet()) {
      assertEquals(
          state.sourceInventoryPpm().get(element),
          state.crystallizedInventoryPpm().getOrDefault(element, 0L)
              + state.residualMeltInventoryPpm().getOrDefault(element, 0L)
              + state.residualFluidInventoryPpm().getOrDefault(element, 0L));
    }
    assertTrue(state.residualFluidInventoryPpm().containsKey(ChemicalElement.U));
  }
}
