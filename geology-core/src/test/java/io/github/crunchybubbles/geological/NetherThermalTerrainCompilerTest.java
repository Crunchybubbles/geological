package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.NetherThermalChunkPlan;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import org.junit.jupiter.api.Test;

class NetherThermalTerrainCompilerTest {
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");

  @Test
  void compilesBoundedThreeDimensionalChunkWithLavaAndBridges() {
    NetherThermalChunkPlan plan = compiler().plan(0, 0);

    assertEquals(256, plan.columns().size());
    assertEquals(-64, plan.bounds().minY());
    assertEquals(128, plan.bounds().maxYExclusive());
    assertTrue(plan.lavaColumnCount() > 0);
    assertTrue(plan.bridgeColumnCount() > 0);
    assertTrue(plan.columns().stream().allMatch(column -> column.roofY() > column.floorY() + 8));
    assertTrue(
        plan.columns().stream()
            .allMatch(
                column ->
                    column.solidIntervals().stream()
                        .allMatch(interval -> interval.maxYExclusive() <= 128)));
  }

  @Test
  void randomAccessAndChunkSeamsAreStable() {
    NetherThermalTerrainCompiler compiler = compiler();
    assertEquals(compiler.plan(-4, 7), compiler.plan(-4, 7));
    NetherThermalChunkPlan east = compiler.plan(0, 0);
    NetherThermalChunkPlan south = compiler.plan(0, 0);
    for (int offset = 0; offset < 16; offset++) {
      assertEquals(compiler.planColumn(0, offset), east.at(0, offset));
      assertEquals(compiler.planColumn(offset, 0), south.at(offset, 0));
    }
  }

  @Test
  void provinceIdentityAndKindAreStableAcrossColumns() {
    NetherThermalTerrainCompiler compiler = compiler();
    var first = compiler.provinceAt(12, 17);
    var second = compiler.provinceAt(12, 17);

    assertEquals(first, second);
    assertEquals(first.provinceId(), compiler.plan(12 / 16, 17 / 16).at(12, 17).provinceId());
    assertTrue(first.heatPotentialFixedUnits() >= 620_000L);
    assertTrue(first.volatilePotentialFixedUnits() >= 420_000L);
  }

  private static NetherThermalTerrainCompiler compiler() {
    WorldIdentity identity =
        new WorldIdentity(
            8_675_309L, NETHER.version(), NETHER.scientificDigest(), NETHER.profileId());
    return NetherThermalTerrainCompiler.from(identity);
  }
}
