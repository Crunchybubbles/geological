package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.registry.Phase1ScientificRegistry;
import io.github.crunchybubbles.geological.registry.RegistrySnapshot;

/** Frozen identity and factory for the first Phase 1 platform-neutral query increment. */
public final class Phase1World {
  public static final String MODEL_VERSION = "phase1.0-alpha.2";
  public static final String SCIENTIFIC_DIGEST = Phase1ScientificRegistry.snapshot().digest();

  private Phase1World() {}

  public static RegistrySnapshot scientificSnapshot() {
    return Phase1ScientificRegistry.snapshot();
  }

  public static GeologyQueryEngine create(long worldSeed) {
    DimensionProfile profile = DimensionProfile.overworldPhase1();
    WorldIdentity identity =
        new WorldIdentity(worldSeed, MODEL_VERSION, SCIENTIFIC_DIGEST, profile.id());
    return new GeologyQueryEngine(new GeologyAtlas(identity, profile));
  }
}
