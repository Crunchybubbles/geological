package io.github.crunchybubbles.geological.petrology;

/** Invalid external Phase 2 material authoring input. */
public final class MaterialCatalogAuthoringException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public MaterialCatalogAuthoringException(String source, String path, String message) {
    super(source + ":" + path + ": " + message);
  }

  public MaterialCatalogAuthoringException(String source, String message, Throwable cause) {
    super(source + ": " + message, cause);
  }
}
