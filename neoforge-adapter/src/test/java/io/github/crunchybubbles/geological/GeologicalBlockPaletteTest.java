package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class GeologicalBlockPaletteTest {
  private static final StableId ROCK_BODY = StableId.parse("00000000000000000000000000000001");

  @Test
  void mapsEveryCanonicalLithologyDeterministically() {
    Set<Lithology> seen = EnumSet.noneOf(Lithology.class);
    for (Lithology lithology : Lithology.values()) {
      MaterialState material = material(lithology);
      BlockState first = GeologicalBlockPalette.overworld(material);
      BlockState second = GeologicalBlockPalette.overworld(material);

      assertNotNull(first);
      assertEquals(first, second);
      seen.add(lithology);
    }
    assertEquals(EnumSet.allOf(Lithology.class), seen);
  }

  @Test
  void preservesTheSmallCoarsePaletteBoundaries() {
    assertEquals(Blocks.GRANITE.defaultBlockState(), palette(Lithology.GRANITIC_GNEISS));
    assertEquals(Blocks.DEEPSLATE.defaultBlockState(), palette(Lithology.MICA_SCHIST));
    assertEquals(Blocks.CALCITE.defaultBlockState(), palette(Lithology.LIMESTONE));
    assertEquals(Blocks.SANDSTONE.defaultBlockState(), palette(Lithology.BASIN_SANDSTONE));
    assertEquals(Blocks.TUFF.defaultBlockState(), palette(Lithology.PYROCLASTIC));
    assertEquals(Blocks.BASALT.defaultBlockState(), palette(Lithology.BASALTIC));
    assertEquals(Blocks.ANDESITE.defaultBlockState(), palette(Lithology.ANDESITIC));
    assertEquals(Blocks.DIORITE.defaultBlockState(), palette(Lithology.GABBROIC));
    assertEquals(Blocks.TERRACOTTA.defaultBlockState(), palette(Lithology.LATERITE_BAUXITE));
    assertEquals(Blocks.DIRT.defaultBlockState(), palette(Lithology.SOIL_COLLUVIUM));
    assertEquals(Blocks.GRAVEL.defaultBlockState(), palette(Lithology.ALLUVIAL_GRAVEL));
  }

  private static BlockState palette(Lithology lithology) {
    return GeologicalBlockPalette.overworld(material(lithology));
  }

  private static MaterialState material(Lithology lithology) {
    return new MaterialState(
        ROCK_BODY, lithology, new AgeKey(100.0, 0), Overprint.NONE, false, Arrays.asList());
  }
}
