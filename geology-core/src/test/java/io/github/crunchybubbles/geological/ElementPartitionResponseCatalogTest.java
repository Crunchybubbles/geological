package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ElementPartitionResponseCatalog;
import io.github.crunchybubbles.geological.petrology.MagmaDifferentiationState;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ElementPartitionResponseCatalogTest {
  @Test
  void sparseResponseSetCoversEveryTrackedElementAndSaturationState() {
    assertEquals(69, ElementPartitionResponseCatalog.all().size());
    assertEquals(
        "phase9-alpha.7-review-manifest-v1",
        ElementPartitionResponseCatalog.REVIEW_MANIFEST_VERSION);
    assertEquals(2, ElementPartitionResponseCatalog.reviewEvidence().size());
    assertTrue(
        ElementPartitionResponseCatalog.reviewEvidence().stream()
            .anyMatch(
                evidence ->
                    evidence.evidenceId().equals("earthref:germ/kdd")
                        && evidence.licenseDisposition()
                            == ElementPartitionResponseCatalog.LicenseDisposition
                                .REDISTRIBUTION_UNVERIFIED));
    assertEquals(
        23,
        new HashSet<>(
                ElementPartitionResponseCatalog.all().stream()
                    .map(ElementPartitionResponseCatalog.Response::element)
                    .toList())
            .size());
    assertTrue(
        ElementPartitionResponseCatalog.all().stream()
            .allMatch(
                response ->
                    response.reviewStatus()
                        == ElementPartitionResponseCatalog.ReviewStatus.AUTHORED_PROXY));
    assertEquals(
        700_000L,
        ElementPartitionResponseCatalog.require(
                ChemicalElement.CU, MagmaDifferentiationState.SulfurSaturationHistory.SATURATED)
            .crystalCapturePpm());
    assertEquals(
        650_000L,
        ElementPartitionResponseCatalog.require(
                ChemicalElement.MO, MagmaDifferentiationState.SulfurSaturationHistory.SATURATED)
            .crystalCapturePpm());
    assertEquals(
        "geological:phase9/partition-proxy",
        ElementPartitionResponseCatalog.all().getFirst().evidenceId());
  }

  @Test
  void responseLookupIsDeterministicAndRejectsUnreviewedElements() {
    assertEquals(ElementPartitionResponseCatalog.all(), ElementPartitionResponseCatalog.all());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ElementPartitionResponseCatalog.require(
                ChemicalElement.HE,
                MagmaDifferentiationState.SulfurSaturationHistory.UNDERSATURATED));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ElementPartitionResponseCatalog.require(
                null, MagmaDifferentiationState.SulfurSaturationHistory.SATURATED));
  }
}
