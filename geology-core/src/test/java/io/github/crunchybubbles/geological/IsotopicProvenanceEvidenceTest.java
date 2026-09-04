package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.petrology.BulkComposition;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.IsotopicProvenanceEvidence;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IsotopicProvenanceEvidenceTest {
  @Test
  void derivesOnlyPresentParentSystemsWithExactIsotopeClosure() {
    BulkComposition composition =
        new BulkComposition(
            Map.of(
                ChemicalElement.SI, 400_000L,
                ChemicalElement.O, 200_000L,
                ChemicalElement.K, 100_000L,
                ChemicalElement.RB, 100_000L,
                ChemicalElement.TH, 100_000L,
                ChemicalElement.U, 100_000L),
            3.0);
    StableId source = StableId.parse("00000000000000000000000000000041");
    AgeKey age = new AgeKey(1_000.0, 2);

    List<IsotopicProvenanceEvidence.Evidence> evidence =
        IsotopicProvenanceEvidence.proofFor(composition, source, age);

    assertEquals(
        List.of(
            IsotopicProvenanceEvidence.ParentNuclide.K40,
            IsotopicProvenanceEvidence.ParentNuclide.RB87,
            IsotopicProvenanceEvidence.ParentNuclide.TH232,
            IsotopicProvenanceEvidence.ParentNuclide.U238),
        evidence.stream().map(IsotopicProvenanceEvidence.Evidence::parentNuclide).toList());
    for (IsotopicProvenanceEvidence.Evidence item : evidence) {
      assertEquals(source, item.sourceReservoirId());
      assertEquals(age, item.formationAge());
      assertEquals(
          item.initialIsotopeInventoryPpm(),
          item.daughterPotentialPpm() + item.retainedIsotopePpm());
      assertTrue(item.decayFractionPpm() > 0L);
      assertTrue(item.daughterPotentialPpm() >= 0L);
    }
    assertEquals("He-4", evidence.getLast().daughterProduct());
  }

  @Test
  void zeroAgeProducesNoAccumulatedDaughterPotential() {
    BulkComposition composition =
        new BulkComposition(Map.of(ChemicalElement.K, 100_000L, ChemicalElement.SI, 900_000L), 2.7);
    IsotopicProvenanceEvidence.Evidence evidence =
        IsotopicProvenanceEvidence.proofFor(
                composition, StableId.parse("00000000000000000000000000000042"), new AgeKey(0.0, 0))
            .getFirst();
    assertEquals(0L, evidence.daughterPotentialPpm());
    assertEquals(evidence.initialIsotopeInventoryPpm(), evidence.retainedIsotopePpm());
  }

  @Test
  void provenanceInputsMustBeComplete() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IsotopicProvenanceEvidence.proofFor(
                null, StableId.parse("00000000000000000000000000000043"), new AgeKey(1.0, 0)));
  }
}
