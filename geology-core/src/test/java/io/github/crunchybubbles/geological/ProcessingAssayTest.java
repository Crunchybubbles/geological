package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialConstituentKind;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.PetrologicState;
import io.github.crunchybubbles.geological.petrology.ProcessingAssay;
import io.github.crunchybubbles.geological.query.Phase2World;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessingAssayTest {
  @Test
  void resolvedSampleAssayPreservesSparseElementAndHostClosure() {
    MaterialQueryEngine query = Phase2World.create(8_675_309L);
    PetrologicSample sample = query.sample(new Point3(0.0, 0.0, 0.0));

    ProcessingAssay first = query.processingAssay(sample);
    ProcessingAssay second = query.processingAssay(sample);

    assertEquals(first, second);
    assertEquals(first, sample.processingAssay(query.catalog()));
    assertEquals(first, PetrologicState.from(sample).processingAssay(query.catalog()));
    assertEquals(sample.resolvedComposition().elementMassPpm(), first.elementMassPpm());
    assertEquals(sample.resolvedComposition().density(), first.bulkDensity());
    assertEquals(ProcessingAssay.VERSION, first.version());
    assertEquals(
        ProcessingAssay.LiberationModel.CONSTITUENT_IDEAL_UPPER_BOUND, first.liberationModel());
    for (ProcessingAssay.ElementAssay element : first.elements()) {
      assertEquals(
          element.totalPpm(),
          element.hostAllocations().stream()
              .mapToLong(ProcessingAssay.HostAllocation::hostedElementPpm)
              .sum());
      assertTrue(
          element.hostAllocations().stream()
              .allMatch(host -> host.idealLiberatedElementPpm() == host.hostedElementPpm()));
    }
  }

  @Test
  void nonCrystallineHostsRemainVisibleToProcessingCallers() {
    var catalog = Phase2World.materialCatalog();
    var coal = catalog.requireRock(io.github.crunchybubbles.geological.model.Lithology.COAL);
    ProcessingAssay assay =
        ProcessingAssay.proofFor(
            catalog, coal.primaryAssemblage(), catalog.composition(coal.primaryAssemblage()));

    ProcessingAssay.ElementAssay carbon = assay.element(ChemicalElement.C).orElseThrow();
    ProcessingAssay.HostAllocation organic =
        carbon.hostAllocations().stream()
            .filter(
                host -> host.constituentId().equals("geological:constituent/coal_organic_matter"))
            .findFirst()
            .orElseThrow();
    assertEquals(MaterialConstituentKind.ORGANIC_MATTER, organic.constituentKind());
    assertTrue(organic.hostedElementPpm() > 0L);
    assertEquals(organic.hostedElementPpm(), organic.idealLiberatedElementPpm());
  }

  @Test
  void mismatchedCompositionCannotBePresentedAsAnAssay() {
    var catalog = Phase2World.materialCatalog();
    var rock = catalog.requireRock(io.github.crunchybubbles.geological.model.Lithology.BASALTIC);
    var expected = catalog.composition(rock.primaryAssemblage());
    var mismatched =
        new io.github.crunchybubbles.geological.petrology.BulkComposition(
            Map.of(ChemicalElement.SI, MaterialAssemblage.SCALE), 2.7);

    assertThrows(
        IllegalArgumentException.class,
        () -> ProcessingAssay.proofFor(catalog, rock.primaryAssemblage(), mismatched));
    assertEquals(expected, catalog.composition(rock.primaryAssemblage()));
  }
}
