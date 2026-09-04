package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.EndParentBodyState;
import io.github.crunchybubbles.geological.worldgen.NetherThermalProvinceState;
import org.junit.jupiter.api.Test;

class GeologicalNativeBlockPaletteTest {
  @Test
  void mapsEveryNativeProvinceAndParentFamilyToAStableBlock() {
    for (NetherThermalProvinceState.NetherProvinceKind kind :
        NetherThermalProvinceState.NetherProvinceKind.values()) {
      assertNotNull(GeologicalNativeBlockPalette.netherHost(kind));
    }
    for (EndParentBodyState.ParentFamily family : EndParentBodyState.ParentFamily.values()) {
      assertNotNull(GeologicalNativeBlockPalette.endHost(family));
    }
    assertNotSame(
        GeologicalNativeBlockPalette.endRegolith(), GeologicalNativeBlockPalette.endImpactMelt());
  }

  @Test
  void mapsEverySourceLinkedNetherResourceFamily() {
    for (NetherResourceSystemState.ResourceFamily family :
        NetherResourceSystemState.ResourceFamily.values()) {
      assertNotNull(GeologicalNativeBlockPalette.netherResource(family));
    }
  }
}
