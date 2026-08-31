package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.crunchybubbles.geological.atlas.AtlasSite;
import io.github.crunchybubbles.geological.atlas.DescriptorCache;
import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.MacroDomain;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.query.AtlasTile;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.TileKey;
import io.github.crunchybubbles.geological.query.TileQueryEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RandomAccessTileTest {
  @Test
  void repeatedAndArbitraryOrderQueriesAreIdentical() {
    List<TileKey> keys =
        List.of(
            new TileKey(-1024, -512, 12, 16),
            new TileKey(2048, -4096, 12, 16),
            new TileKey(-32_768, 65_536, 12, 16),
            new TileKey(1_000_000_000L, -1_000_000_000L, 12, 16));
    TileQueryEngine forward = new TileQueryEngine(queryWithCaches(144L));
    Map<TileKey, String> expected = new HashMap<>();
    keys.forEach(key -> expected.put(key, forward.query(key).digest()));
    assertEquals(expected.get(keys.getFirst()), forward.query(keys.getFirst()).digest());

    TileQueryEngine reverse = new TileQueryEngine(queryWithCaches(144L));
    for (TileKey key : keys.reversed()) {
      assertEquals(expected.get(key), reverse.query(key).digest());
    }
  }

  @Test
  void cacheBypassAndEvictionCannotChangeTiles() {
    TileKey key = new TileKey(-8192, 4096, 16, 16);
    String cached = new TileQueryEngine(queryWithCaches(991L)).query(key).digest();
    String uncached = new TileQueryEngine(queryWithoutCaches(991L)).query(key).digest();
    assertEquals(cached, uncached);

    GeologyQueryEngine evicting = queryWithCaches(991L);
    TileQueryEngine tiles = new TileQueryEngine(evicting);
    String before = tiles.query(key).digest();
    for (int index = 0; index < 400; index++) {
      evicting.atlas().province(new CellKey("province", index, -index));
    }
    assertEquals(before, tiles.query(key).digest());
  }

  @Test
  void adjacentTilesAgreeOnEverySharedBorderSample() {
    TileQueryEngine tiles = new TileQueryEngine(queryWithCaches(8_675_309L));
    TileKey leftKey = new TileKey(-256, -128, 16, 16);
    TileKey rightKey = new TileKey(0, -128, 16, 16);
    AtlasTile left = tiles.query(leftKey);
    AtlasTile right = tiles.query(rightKey);
    for (int z = 0; z < leftKey.samplesPerSide(); z++) {
      assertEquals(left.sample(leftKey.intervals(), z), right.sample(0, z));
    }
  }

  private static GeologyQueryEngine queryWithCaches(long seed) {
    DimensionProfile profile = DimensionProfile.overworldPhase0();
    WorldIdentity identity = new WorldIdentity(seed, "phase0.1", "digest", profile.id());
    return new GeologyQueryEngine(new GeologyAtlas(identity, profile));
  }

  private static GeologyQueryEngine queryWithoutCaches(long seed) {
    DimensionProfile profile = DimensionProfile.overworldPhase0();
    WorldIdentity identity = new WorldIdentity(seed, "phase0.1", "digest", profile.id());
    DescriptorCache<CellKey, AtlasSite> macroSites = DescriptorCache.none();
    DescriptorCache<CellKey, AtlasSite> provinceSites = DescriptorCache.none();
    DescriptorCache<CellKey, MacroDomain> macro = DescriptorCache.none();
    DescriptorCache<CellKey, Province> province = DescriptorCache.none();
    return new GeologyQueryEngine(
        new GeologyAtlas(identity, profile, macroSites, provinceSites, macro, province), 0);
  }
}
