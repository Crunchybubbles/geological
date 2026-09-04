package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.EndFragmentChunkPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentColumnPlan;
import io.github.crunchybubbles.geological.worldgen.EndFragmentTerrainCompiler;
import io.github.crunchybubbles.geological.worldgen.EndParentBodyState;
import org.junit.jupiter.api.Test;

class EndFragmentTerrainCompilerTest {
  private static final DimensionGeologyProfile END =
      DimensionGeologyProfiles.require("minecraft:the_end");

  @Test
  void compilesCentralIslandAndVoidBoundedChunk() {
    EndFragmentChunkPlan plan = compiler().plan(0, 0);

    assertEquals(256, plan.columns().size());
    assertEquals(-64, plan.bounds().minY());
    assertEquals(320, plan.bounds().maxYExclusive());
    assertTrue(plan.islandColumnCount() > 0);
    assertTrue(compiler().plan(20, 0).voidColumnCount() > 0);
    EndFragmentColumnPlan center = plan.at(0, 0);
    assertFalse(center.isVoid());
    assertEquals(
        EndParentBodyState.FragmentRole.CENTRAL_PROGRESSION, center.body().orElseThrow().role());
    assertTrue(center.isSolid(center.surfaceY()));
    assertTrue(center.regolithIntervals().size() == 1);
  }

  @Test
  void parentFamiliesAndDifferentiationAreStableAcrossFragments() {
    EndFragmentTerrainCompiler compiler = compiler();
    java.util.Set<EndParentBodyState.ParentFamily> families = new java.util.HashSet<>();
    java.util.Set<EndParentBodyState.DifferentiationClass> differentiation =
        new java.util.HashSet<>();
    for (long cellX = -8L; cellX <= 8L; cellX++) {
      for (long cellZ = -8L; cellZ <= 8L; cellZ++) {
        compiler
            .parentBodyAtCell(cellX, cellZ)
            .ifPresent(
                body -> {
                  families.add(body.parentFamily());
                  differentiation.add(body.differentiation());
                  assertTrue(body.sourceBodyIds().contains(body.parentBodyId()));
                  assertEquals(
                      body.parentMaterialBudgetFixedUnits(),
                      body.retainedParentMaterialFixedUnits() + body.impactLossFixedUnits());
                  assertEquals(
                      body.regolithBudgetFixedUnits(),
                      body.retainedRegolithFixedUnits() + body.voidExposureLossFixedUnits());
                });
      }
    }
    assertEquals(4, families.size());
    assertEquals(4, differentiation.size());
    assertTrue(compiler.parentBodyAtCell(9, 9).isEmpty());
  }

  @Test
  void impactMeltAndRegolithRemainInsideSolidBody() {
    EndFragmentTerrainCompiler compiler = compiler();
    EndFragmentColumnPlan impacted = null;
    for (long cellX = -2L; cellX <= 2L && impacted == null; cellX++) {
      for (long cellZ = -2L; cellZ <= 2L && impacted == null; cellZ++) {
        EndParentBodyState body = compiler.parentBodyAtCell(cellX, cellZ).orElseThrow();
        if (body.impactClass() == EndParentBodyState.ImpactClass.NONE) {
          continue;
        }
        long x = (long) StrictMath.floor(body.impactCenterX());
        long z = (long) StrictMath.floor(body.impactCenterZ());
        EndFragmentColumnPlan candidate = compiler.planColumn(x, z);
        if (!candidate.isVoid() && !candidate.impactMeltIntervals().isEmpty()) {
          impacted = candidate;
        }
      }
    }
    assertNotNull(impacted);
    EndFragmentColumnPlan finalImpacted = impacted;
    assertTrue(
        finalImpacted.impactMeltIntervals().stream()
            .allMatch(interval -> finalImpacted.isSolid(interval.minYInclusive())));
    assertTrue(
        finalImpacted.regolithIntervals().stream()
            .allMatch(interval -> finalImpacted.isSolid(interval.minYInclusive())));
  }

  @Test
  void directAndChunkAccessStayEqualAcrossVoidAndIslandSeams() {
    EndFragmentTerrainCompiler compiler = compiler();
    EndFragmentChunkPlan chunk = compiler.plan(-65, 2);
    assertEquals(chunk, compiler.plan(-65, 2));
    for (int offset = 0; offset < 16; offset++) {
      assertEquals(
          compiler.planColumn(chunk.bounds().minX(), chunk.bounds().minZ() + offset),
          chunk.at(chunk.bounds().minX(), chunk.bounds().minZ() + offset));
      assertEquals(
          compiler.planColumn(chunk.bounds().minX() + offset, chunk.bounds().minZ()),
          chunk.at(chunk.bounds().minX() + offset, chunk.bounds().minZ()));
    }
  }

  private static EndFragmentTerrainCompiler compiler() {
    WorldIdentity identity =
        new WorldIdentity(8_675_309L, END.version(), END.scientificDigest(), END.profileId());
    return EndFragmentTerrainCompiler.from(identity);
  }
}
