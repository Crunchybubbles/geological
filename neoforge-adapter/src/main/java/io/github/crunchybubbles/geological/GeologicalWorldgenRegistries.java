package io.github.crunchybubbles.geological;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Static registry bindings for Geological's platform worldgen codecs. */
public final class GeologicalWorldgenRegistries {
  public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
      DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, GeologicalMod.MOD_ID);

  public static final Supplier<MapCodec<GeologicalOverworldChunkGenerator>> OVERWORLD_GENERATOR =
      CHUNK_GENERATORS.register("overworld", () -> GeologicalOverworldChunkGenerator.CODEC);

  private GeologicalWorldgenRegistries() {}

  public static void register(IEventBus modBus) {
    CHUNK_GENERATORS.register(modBus);
  }
}
