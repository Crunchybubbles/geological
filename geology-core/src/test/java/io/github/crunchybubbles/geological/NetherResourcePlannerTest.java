package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.NetherResourceSystemState;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.NetherResourceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.NetherResourcePlanner;
import io.github.crunchybubbles.geological.worldgen.NetherThermalProvinceState;
import io.github.crunchybubbles.geological.worldgen.NetherThermalTerrainCompiler;
import org.junit.jupiter.api.Test;

class NetherResourcePlannerTest {
  private static final DimensionGeologyProfile NETHER =
      DimensionGeologyProfiles.require("minecraft:the_nether");

  @Test
  void derivesClosedMaterialHistoryAndResourceLedger() {
    NetherResourcePlanner planner = planner();
    NetherResourceColumnPlan column =
        findColumn(planner, NetherResourceSystemState.ResourceFamily.NETHER_QUARTZ);

    assertEquals(column.thermal().provinceId(), column.history().provinceId());
    assertTrue(column.history().events().size() >= 6);
    assertEquals(
        column.history().sourceBudgetFixedUnits(),
        column.history().retainedMaterialFixedUnits()
            + column.history().alterationLossFixedUnits());
    assertEquals(FormationStatus.FORMED, column.resource().status());
    assertEquals(
        column.resource().releasedResourceFixedUnits(),
        column.resource().transportLossFixedUnits()
            + column.resource().depositAllocationFixedUnits());
    assertEquals(
        column.resource().depositAllocationFixedUnits(),
        column.resource().horizons().stream()
            .mapToLong(NetherResourceSystemState.Horizon::allocationFixedUnits)
            .sum());
    assertTrue(column.hasResource());
  }

  @Test
  void exposesAllFourSourceLinkedFamiliesAcrossProvinceCells() {
    NetherResourcePlanner planner = planner();
    for (NetherResourceSystemState.ResourceFamily family :
        NetherResourceSystemState.ResourceFamily.values()) {
      if (family == NetherResourceSystemState.ResourceFamily.NONE) {
        continue;
      }
      NetherResourceColumnPlan column = findColumn(planner, family);
      assertEquals(family, column.resource().family());
      assertNotNull(column.resource().sourceBodyId());
      assertNotNull(column.resource().pathwayId());
      assertNotNull(column.resource().hostBodyId());
      assertEquals(3, column.resource().horizons().size());
    }
  }

  @Test
  void directAndChunkAccessStayEqualAtSeams() {
    NetherResourcePlanner planner = planner();
    var chunk = planner.plan(-2, 3);
    for (int offset = 0; offset < 16; offset++) {
      assertEquals(
          planner.planColumn(chunk.bounds().minX(), chunk.bounds().minZ() + offset),
          chunk.at(chunk.bounds().minX(), chunk.bounds().minZ() + offset));
      assertEquals(
          planner.planColumn(chunk.bounds().minX() + offset, chunk.bounds().minZ()),
          chunk.at(chunk.bounds().minX() + offset, chunk.bounds().minZ()));
    }
  }

  @Test
  void materialHistoryRejectsNonNetherIdentity() {
    DimensionGeologyProfile overworld = DimensionGeologyProfiles.require("minecraft:overworld");
    WorldIdentity identity =
        new WorldIdentity(
            8_675_309L, overworld.version(), overworld.scientificDigest(), overworld.profileId());
    NetherThermalProvinceState province =
        NetherThermalTerrainCompiler.from(netherIdentity()).provinceAt(0, 0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            io.github.crunchybubbles.geological.worldgen.NetherMaterialHistoryState.from(
                province, identity));
  }

  private static NetherResourceColumnPlan findColumn(
      NetherResourcePlanner planner, NetherResourceSystemState.ResourceFamily family) {
    for (long cellX = -16L; cellX <= 16L; cellX++) {
      for (long cellZ = -16L; cellZ <= 16L; cellZ++) {
        long blockX = cellX * 512L;
        long blockZ = cellZ * 512L;
        NetherResourceColumnPlan probe = planner.planColumn(blockX, blockZ);
        if (probe.resource().family() != family
            || probe.resource().status() != FormationStatus.FORMED) {
          continue;
        }
        long centerX = (long) StrictMath.floor(probe.resource().localCenter().x());
        long centerZ = (long) StrictMath.floor(probe.resource().localCenter().z());
        NetherResourceColumnPlan centered = planner.planColumn(centerX, centerZ);
        if (centered.resource().family() == family && centered.hasResource()) {
          return centered;
        }
      }
    }
    throw new AssertionError("no formed Nether resource sample for " + family);
  }

  private static NetherResourcePlanner planner() {
    return NetherResourcePlanner.from(NetherThermalTerrainCompiler.from(netherIdentity()));
  }

  private static WorldIdentity netherIdentity() {
    return new WorldIdentity(
        8_675_309L, NETHER.version(), NETHER.scientificDigest(), NETHER.profileId());
  }
}
