package io.github.crunchybubbles.geological;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.WorldgenChunkRequest;
import io.github.crunchybubbles.geological.worldgen.WorldgenStage;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** NeoForge generator binding for the dimension-native Nether thermal/resource compiler. */
public final class GeologicalNetherChunkGenerator extends NoiseBasedChunkGenerator {
  public static final MapCodec<GeologicalNetherChunkGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      BiomeSource.CODEC
                          .fieldOf("biome_source")
                          .forGetter(GeologicalNetherChunkGenerator::getBiomeSource),
                      NoiseGeneratorSettings.CODEC
                          .fieldOf("settings")
                          .forGetter(GeologicalNetherChunkGenerator::generatorSettings))
                  .apply(instance, GeologicalNetherChunkGenerator::new));

  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");

  private volatile boolean worldSeedBound;
  private volatile long worldSeed;

  public GeologicalNetherChunkGenerator(
      BiomeSource biomeSource, Holder<NoiseGeneratorSettings> generatorSettings) {
    super(
        Objects.requireNonNull(biomeSource, "biome source"),
        Objects.requireNonNull(generatorSettings, "noise generator settings"));
  }

  @Override
  protected MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public ChunkGeneratorStructureState createState(
      HolderLookup<StructureSet> structureSets, RandomState randomState, long seed) {
    bindWorldSeed(seed);
    return super.createState(structureSets, randomState, seed);
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender,
      RandomState randomState,
      StructureManager structureManager,
      ChunkAccess chunk) {
    WorldgenChunkRequest request =
        GeologicalWorldgenAdapter.request(
            requireWorldSeed(), Level.NETHER, chunk.getPos(), WorldgenStage.BASE_TERRAIN);
    GeologicalNativeChunkWriter.writeNetherTerrain(chunk, request);
    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public void buildSurface(
      net.minecraft.server.level.WorldGenRegion region,
      StructureManager structureManager,
      RandomState randomState,
      ChunkAccess chunk) {
    // Native Nether host/lava intervals are already complete; vanilla surface rules must not
    // reinterpret them as an Overworld surface.
  }

  @Override
  public void applyCarvers(
      net.minecraft.server.level.WorldGenRegion region,
      long seed,
      RandomState randomState,
      net.minecraft.world.level.biome.BiomeManager biomeManager,
      StructureManager structureManager,
      ChunkAccess chunk,
      net.minecraft.world.level.levelgen.GenerationStep.Carving carving) {
    // The dimension-native cavern compiler owns all voids and bridges for this generator.
  }

  public DimensionGeologyProfile profile() {
    return NETHER;
  }

  private void bindWorldSeed(long seed) {
    if (worldSeedBound && worldSeed != seed) {
      throw new IllegalStateException("one geological generator cannot serve multiple world seeds");
    }
    worldSeed = seed;
    worldSeedBound = true;
  }

  private long requireWorldSeed() {
    if (!worldSeedBound) {
      throw new IllegalStateException("geological Nether generator state has no world seed");
    }
    return worldSeed;
  }
}
