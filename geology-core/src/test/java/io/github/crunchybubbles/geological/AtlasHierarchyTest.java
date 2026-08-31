package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.MacroDomain;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceAdjacency;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Point2;
import org.junit.jupiter.api.Test;

class AtlasHierarchyTest {
  @Test
  void stableIdsRepeatAndChangeWithWorldIdentity() {
    GeologyAtlas first = atlas(91L);
    GeologyAtlas second = atlas(91L);
    GeologyAtlas otherSeed = atlas(92L);
    CellKey key = new CellKey("province", -13, 29);

    assertEquals(first.province(key).id(), second.province(key).id());
    assertNotEquals(first.province(key).id(), otherSeed.province(key).id());
    assertEquals(key, first.province(key).homeCell());
  }

  @Test
  void negativeCoordinatesUseFloorDivision() {
    assertEquals(new CellKey("province", -1, -1), CellKey.containing("province", -1, -1, 2048));
    assertEquals(new CellKey("province", -1, 0), CellKey.containing("province", -2048, 0, 2048));
    assertEquals(new CellKey("province", -2, 0), CellKey.containing("province", -2049, 0, 2048));
  }

  @Test
  void provinceAndMacroDomainResolveAsAStableHierarchy() {
    GeologyAtlas atlas = atlas(718L);
    Province province = atlas.provinceAt(new Point2(-32_000.25, 99_999.75));
    MacroDomain macro = atlas.macroDomainAt(province.site());
    assertEquals(province.macroDomainId(), macro.id());
    assertEquals(8, province.adjacency().size());
    assertEquals(8, macro.adjacentDomainIds().size());
  }

  @Test
  void adjacencyClassificationAgreesFromBothSides() {
    GeologyAtlas atlas = atlas(444L);
    Province first = atlas.province(new CellKey("province", -2, 7));
    Province second = atlas.province(new CellKey("province", -1, 7));
    ProvinceAdjacency fromFirst =
        first.adjacency().stream()
            .filter(adjacency -> adjacency.neighborId().equals(second.id()))
            .findFirst()
            .orElseThrow();
    ProvinceAdjacency fromSecond =
        second.adjacency().stream()
            .filter(adjacency -> adjacency.neighborId().equals(first.id()))
            .findFirst()
            .orElseThrow();
    assertEquals(fromFirst.boundaryType(), fromSecond.boundaryType());
    assertTrue(
        first.adjacency().stream().map(ProvinceAdjacency::neighborId).distinct().count() == 8);
  }

  private static GeologyAtlas atlas(long seed) {
    DimensionProfile profile = DimensionProfile.overworldPhase0();
    WorldIdentity identity = new WorldIdentity(seed, "phase0.1", "digest", profile.id());
    return new GeologyAtlas(identity, profile);
  }
}
