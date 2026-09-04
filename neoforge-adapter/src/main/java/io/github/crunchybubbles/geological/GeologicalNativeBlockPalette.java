package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.EndParentBodyState;
import io.github.crunchybubbles.geological.worldgen.NetherThermalProvinceState;
import java.util.Objects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Coarse vanilla presentations for the dimension-native Nether and End material states. */
public final class GeologicalNativeBlockPalette {
  private GeologicalNativeBlockPalette() {}

  /** Maps a Nether thermal province to a stable host-rock block. */
  public static BlockState netherHost(NetherThermalProvinceState.NetherProvinceKind kind) {
    Objects.requireNonNull(kind, "Nether province kind");
    return switch (kind) {
      case NETHERRACK_VOLCANIC_WASTE -> Blocks.NETHERRACK.defaultBlockState();
      case BASALT_DELTA_COMPLEX -> Blocks.BASALT.defaultBlockState();
      case SOUL_ASH_VALLEY -> Blocks.SOUL_SOIL.defaultBlockState();
      case VOLATILE_VENT_FIELD -> Blocks.BLACKSTONE.defaultBlockState();
    };
  }

  /** Maps a source-linked Nether resource horizon to its ordinary vanilla presentation. */
  public static BlockState netherResource(NetherResourceSystemState.ResourceFamily family) {
    Objects.requireNonNull(family, "Nether resource family");
    return switch (family) {
      case NETHER_QUARTZ -> Blocks.NETHER_QUARTZ_ORE.defaultBlockState();
      case NETHER_GOLD -> Blocks.NETHER_GOLD_ORE.defaultBlockState();
      case GLOWSTONE -> Blocks.GLOWSTONE.defaultBlockState();
      case ANCIENT_DEBRIS -> Blocks.ANCIENT_DEBRIS.defaultBlockState();
      case NONE -> Blocks.NETHERRACK.defaultBlockState();
    };
  }

  /** Maps an End parent family to a coarse host-rock presentation. */
  public static BlockState endHost(EndParentBodyState.ParentFamily family) {
    Objects.requireNonNull(family, "End parent family");
    return switch (family) {
      case PRIMITIVE -> Blocks.END_STONE.defaultBlockState();
      case SILICATE_DIFFERENTIATED -> Blocks.END_STONE_BRICKS.defaultBlockState();
      case METAL_SEPARATED -> Blocks.PURPUR_BLOCK.defaultBlockState();
      case PREVIOUSLY_MELTED -> Blocks.OBSIDIAN.defaultBlockState();
    };
  }

  /** Maps void-exposed End regolith to a coarse surface block. */
  public static BlockState endRegolith() {
    return Blocks.END_STONE_BRICKS.defaultBlockState();
  }

  /** Maps impact-melt intervals to a dense, visibly distinct vanilla block. */
  public static BlockState endImpactMelt() {
    return Blocks.OBSIDIAN.defaultBlockState();
  }
}
