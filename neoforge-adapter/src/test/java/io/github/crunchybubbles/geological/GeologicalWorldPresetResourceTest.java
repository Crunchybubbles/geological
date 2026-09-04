package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GeologicalWorldPresetResourceTest {
  @Test
  void presetUsesRegisteredGeologicalGeneratorsForAllCanonicalDimensions() throws IOException {
    String preset = read("data/geological/worldgen/world_preset/geological.json");

    assertTrue(preset.contains("\"type\": \"geological:overworld\""));
    assertTrue(preset.contains("\"type\": \"geological:nether\""));
    assertTrue(preset.contains("\"type\": \"geological:end\""));
    assertTrue(preset.contains("\"minecraft:the_nether\""));
    assertTrue(preset.contains("\"minecraft:the_end\""));
  }

  @Test
  void generatorCodecIsRegisteredUnderThePresetType() {
    assertEquals(
        GeologicalOverworldChunkGenerator.CODEC,
        BuiltInRegistries.CHUNK_GENERATOR.get(
            ResourceLocation.fromNamespaceAndPath(GeologicalMod.MOD_ID, "overworld")));
    assertEquals(
        GeologicalNetherChunkGenerator.CODEC,
        BuiltInRegistries.CHUNK_GENERATOR.get(
            ResourceLocation.fromNamespaceAndPath(GeologicalMod.MOD_ID, "nether")));
    assertEquals(
        GeologicalEndChunkGenerator.CODEC,
        BuiltInRegistries.CHUNK_GENERATOR.get(
            ResourceLocation.fromNamespaceAndPath(GeologicalMod.MOD_ID, "end")));
  }

  private static String read(String resource) throws IOException {
    try (InputStream input =
        GeologicalWorldPresetResourceTest.class.getClassLoader().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IOException("missing resource: " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
