package io.github.crunchybubbles.geological.petrology;

/** Packaged typed catalog used by the first Phase 2 material-state increment. */
public final class Phase2MaterialCatalog {
  public static final String RESOURCE = "/data/geological/registry/phase2-materials.json";
  private static final MaterialCatalogSnapshot SNAPSHOT =
      new MaterialCatalogJsonLoader().loadResource(Phase2MaterialCatalog.class, RESOURCE);

  private Phase2MaterialCatalog() {}

  public static MaterialCatalogSnapshot snapshot() {
    return SNAPSHOT;
  }
}
