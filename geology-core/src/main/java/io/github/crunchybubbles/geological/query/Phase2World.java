package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.MaterialQueryEngine;
import io.github.crunchybubbles.geological.registry.Phase2ScientificManifest;
import io.github.crunchybubbles.geological.registry.RegistrySnapshot;

/** Frozen identity and factory for the current Phase 2 petrologic material increment. */
public final class Phase2World {
  public static final String MODEL_VERSION = "phase2.0-alpha.69";
  public static final String SCIENTIFIC_DIGEST = Phase2ScientificManifest.digest();

  private Phase2World() {}

  public static RegistrySnapshot baseScientificSnapshot() {
    return Phase2ScientificManifest.baseRegistry();
  }

  public static MaterialCatalogSnapshot materialCatalog() {
    return Phase2ScientificManifest.materials();
  }

  public static String scientificManifestJson() {
    return Phase2ScientificManifest.canonicalJson();
  }

  public static MaterialQueryEngine create(long worldSeed) {
    DimensionProfile profile = DimensionProfile.overworldPhase2();
    WorldIdentity geologyIdentity =
        new WorldIdentity(
            worldSeed, Phase1World.MODEL_VERSION, Phase1World.SCIENTIFIC_DIGEST, profile.id());
    GeologyQueryEngine geology = new GeologyQueryEngine(new GeologyAtlas(geologyIdentity, profile));
    WorldIdentity materialIdentity =
        new WorldIdentity(worldSeed, MODEL_VERSION, SCIENTIFIC_DIGEST, profile.id());
    return new MaterialQueryEngine(geology, materialCatalog(), materialIdentity);
  }
}
