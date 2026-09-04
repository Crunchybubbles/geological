package io.github.crunchybubbles.geological;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
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

/**
 * Overworld generator hook that replaces vanilla base terrain with the frozen geological plan.
 *
 * <p>Vanilla's biome source and noise settings remain codec inputs so biome/structure contracts can
 * be preserved. This hook writes solid geological runs plus explicit surface water and air;
 * groundwater, caves, decoration, and Nether/End generators are separate increments.
 */
public final class GeologicalOverworldChunkGenerator extends NoiseBasedChunkGenerator {
  public static final MapCodec<GeologicalOverworldChunkGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      BiomeSource.CODEC
                          .fieldOf("biome_source")
                          .forGetter(GeologicalOverworldChunkGenerator::getBiomeSource),
                      NoiseGeneratorSettings.CODEC
                          .fieldOf("settings")
                          .forGetter(GeologicalOverworldChunkGenerator::generatorSettings))
                  .apply(instance, GeologicalOverworldChunkGenerator::new));

  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private static final WorldgenSnapshot SNAPSHOT = WorldgenSnapshot.forProfile(OVERWORLD);

  private volatile boolean worldSeedBound;
  private volatile long worldSeed;

  public GeologicalOverworldChunkGenerator(
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
    if (worldSeedBound && worldSeed != seed) {
      throw new IllegalStateException("one geological generator cannot serve multiple world seeds");
    }
    worldSeed = seed;
    worldSeedBound = true;
    return super.createState(structureSets, randomState, seed);
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender,
      RandomState randomState,
      StructureManager structureManager,
      ChunkAccess chunk) {
    long seed = requireWorldSeed();
    WorldgenExecutionContext context =
        GeologicalWorldgenAdapter.baseTerrainContext(
            seed, Level.OVERWORLD, chunk.getPos(), SNAPSHOT, Runnable::run);
    GeologicalChunkWriter.writeBaseTerrain(chunk, context, GeologicalBlockPalette::overworld);
    GeologicalChunkWriter.writeAirAndSurfaceWater(chunk, context);
    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public void buildSurface(
      net.minecraft.server.level.WorldGenRegion region,
      StructureManager structureManager,
      RandomState randomState,
      ChunkAccess chunk) {
    // Vanilla surface rules would overwrite the geology-owned base/lithology palette. Apply the
    // bounded geological regolith projection instead; cave/groundwater fluids remain separate.
    long seed = requireWorldSeed();
    WorldgenExecutionContext context =
        GeologicalWorldgenAdapter.regolithSurfaceContext(
            seed, Level.OVERWORLD, chunk.getPos(), SNAPSHOT, Runnable::run);
    GeologicalChunkWriter.writeRegolithSurface(chunk, context, GeologicalBlockPalette::regolith);
  }

  private long requireWorldSeed() {
    if (!worldSeedBound) {
      throw new IllegalStateException(
          "geological generator state was not created with a world seed");
    }
    return worldSeed;
  }
}
