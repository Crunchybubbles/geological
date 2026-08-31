package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;

/** Frozen identity and factory for the first Phase 1 platform-neutral query increment. */
public final class Phase1World {
  public static final String MODEL_VERSION = "phase1.0-alpha.1";
  public static final String SCIENTIFIC_DIGEST = "geological:phase1-query-core-v1";

  private Phase1World() {}

  public static GeologyQueryEngine create(long worldSeed) {
    DimensionProfile profile = DimensionProfile.overworldPhase1();
    WorldIdentity identity =
        new WorldIdentity(worldSeed, MODEL_VERSION, SCIENTIFIC_DIGEST, profile.id());
    return new GeologyQueryEngine(new GeologyAtlas(identity, profile));
  }
}
