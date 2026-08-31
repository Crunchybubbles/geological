package io.github.crunchybubbles.geological.petrology;

import java.net.URI;

/** Evidence and calibration status shared by the compact Phase 2 proof catalog. */
public record CatalogEvidence(
    String citationId, String title, URI uri, int publicationYear, String parameterBasis) {
  public CatalogEvidence {
    if (citationId == null
        || citationId.isBlank()
        || title == null
        || title.isBlank()
        || uri == null
        || !"https".equalsIgnoreCase(uri.getScheme())
        || publicationYear < 1600
        || publicationYear > 3000
        || parameterBasis == null
        || parameterBasis.isBlank()) {
      throw new IllegalArgumentException("material catalog evidence must be complete and safe");
    }
  }
}
