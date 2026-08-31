package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.DimensionProfile;

/** Frozen identity and convenience factory for the first standalone proof. */
public final class Phase0World {
  public static final String MODEL_VERSION = "phase0.1";
  public static final String SCIENTIFIC_DIGEST = "geological:phase0-scientific-v1";

  private Phase0World() {}

  public static GeologyQueryEngine create(long worldSeed) {
    DimensionProfile profile = DimensionProfile.overworldPhase0();
    WorldIdentity identity =
        new WorldIdentity(worldSeed, MODEL_VERSION, SCIENTIFIC_DIGEST, profile.id());
    return new GeologyQueryEngine(new GeologyAtlas(identity, profile));
  }
}
