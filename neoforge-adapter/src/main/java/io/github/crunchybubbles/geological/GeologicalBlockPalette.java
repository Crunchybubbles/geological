package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Objects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small, lossy Overworld presentation palette for the platform-neutral material state. */
public final class GeologicalBlockPalette {
  private GeologicalBlockPalette() {}

  /**
   * Maps every canonical lithology to a stable vanilla block state.
   *
   * <p>The mapping intentionally uses coarse lithology classes. Grade, deposit IDs, alteration,
   * formation age, and fault damage remain hidden query state and are not encoded as block
   * identity. The palette uses only existing vanilla blocks; registry-owned variants are a later
   * presentation increment.
   */
  public static BlockState overworld(MaterialState material) {
    Objects.requireNonNull(material, "material state");
    return switch (material.lithology()) {
      case GRANITIC_GNEISS,
          GRANULITE,
          RHYOLITIC,
          ALKALINE,
          CARBONATITIC,
          KIMBERLITIC,
          GRANODIORITE_PULSE,
          FELSIC_STOCK ->
          Blocks.GRANITE.defaultBlockState();
      case SLATE_PHYLLITE, MICA_SCHIST, GREENSCHIST, AMPHIBOLITE, QUARTZITE, SERPENTINITE, COAL ->
          Blocks.DEEPSLATE.defaultBlockState();
      case MARBLE, LIMESTONE, DOLOSTONE, GYPSUM_ANHYDRITE_EVAPORITE, HALITE_POTASH_EVAPORITE ->
          Blocks.CALCITE.defaultBlockState();
      case BASAL_CONGLOMERATE, BASIN_SANDSTONE, SILTSTONE -> Blocks.SANDSTONE.defaultBlockState();
      case MARINE_VOLCANICLASTIC,
          BASIN_SHALE,
          CHERT,
          BANDED_IRON_FORMATION,
          VMS_MASSIVE_SULFIDE,
          PYROCLASTIC ->
          Blocks.TUFF.defaultBlockState();
      case KOMATIITIC_ULTRAMAFIC, BASALTIC -> Blocks.BASALT.defaultBlockState();
      case ANDESITIC, DIORITE_PULSE -> Blocks.ANDESITE.defaultBlockState();
      case GABBROIC -> Blocks.DIORITE.defaultBlockState();
      case LATERITE_BAUXITE -> Blocks.TERRACOTTA.defaultBlockState();
      case SOIL_COLLUVIUM -> Blocks.DIRT.defaultBlockState();
      case ALLUVIAL_GRAVEL, GLACIAL_TILL -> Blocks.GRAVEL.defaultBlockState();
    };
  }

  /**
   * Maps a present-surface material to a coarse vanilla regolith presentation.
   *
   * <p>Weathered overprints become dirt or oxidized terracotta, while transported and primary
   * lithologies retain the ordinary coarse palette. Hidden grade, provenance, and clue metadata
   * remain query state rather than block identity.
   */
  public static BlockState regolith(MaterialState material) {
    Objects.requireNonNull(material, "material state");
    if (material.lithology() == Lithology.SOIL_COLLUVIUM) {
      return Blocks.DIRT.defaultBlockState();
    }
    if (material.lithology() == Lithology.ALLUVIAL_GRAVEL
        || material.lithology() == Lithology.GLACIAL_TILL) {
      return Blocks.GRAVEL.defaultBlockState();
    }
    if (material.lithology() == Lithology.LATERITE_BAUXITE) {
      return Blocks.TERRACOTTA.defaultBlockState();
    }
    return switch (material.overprint()) {
      case WEATHERED_UNCONFORMITY, WEATHERED_REGOLITH -> Blocks.DIRT.defaultBlockState();
      case OXIDIZED_GOSSAN -> Blocks.TERRACOTTA.defaultBlockState();
      default -> overworld(material);
    };
  }
}
