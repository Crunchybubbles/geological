package io.github.crunchybubbles.geological.registry;

import java.net.URI;

/** Immutable bibliographic pointer retained in the effective scientific snapshot. */
public record Citation(String id, String title, URI uri, int publicationYear) {
  public Citation {
    requireText(id, "id");
    requireText(title, "title");
    if (uri == null) {
      throw new IllegalArgumentException("citation URI must be present");
    }
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("citation " + name + " must be present");
    }
  }
}
