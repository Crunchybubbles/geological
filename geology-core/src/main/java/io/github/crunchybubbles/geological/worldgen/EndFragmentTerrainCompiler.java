package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Deterministic End island/void compiler with parent-fragment, impact, and regolith provenance. */
public final class EndFragmentTerrainCompiler {
  private static final DimensionGeologyProfile PROFILE =
      DimensionGeologyProfiles.require("minecraft:the_end");
  private static final int MIN_Y = PROFILE.verticalEnvelope().minimumY();
  private static final int MAX_Y_EXCLUSIVE = PROFILE.verticalEnvelope().maximumY() + 1;
  private static final int ISLAND_CELL_BLOCKS = 1024;
  private static final long OUTER_RING_CELL_RADIUS = 8L;
  private final WorldIdentity identity;
  private final EndParentBodyState centralBody;
  private final ConcurrentMap<CellKey, EndParentBodyState> bodyCache = new ConcurrentHashMap<>();

  private EndFragmentTerrainCompiler(WorldIdentity identity) {
    this.identity = Objects.requireNonNull(identity, "world identity");
    if (!PROFILE.profileId().equals(identity.dimensionProfileId())
        || !PROFILE.version().equals(identity.modelVersion())
        || !PROFILE.scientificDigest().equals(identity.scientificDigest())) {
      throw new IllegalArgumentException("End compiler identity does not match the End profile");
    }
    centralBody = EndParentBodyState.from(0L, 0L, identity);
  }

  public static EndFragmentTerrainCompiler from(WorldIdentity identity) {
    return new EndFragmentTerrainCompiler(identity);
  }

  public EndFragmentChunkPlan plan(long chunkX, long chunkZ) {
    long minX = Math.multiplyExact(chunkX, 16L);
    long minZ = Math.multiplyExact(chunkZ, 16L);
    ChunkBlockBounds bounds =
        new ChunkBlockBounds(minX, MIN_Y, minZ, minX + 16L, MAX_Y_EXCLUSIVE, minZ + 16L);
    List<EndFragmentColumnPlan> columns = new ArrayList<>(256);
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        columns.add(planColumn(blockX, blockZ));
      }
    }
    return new EndFragmentChunkPlan(chunkX, chunkZ, bounds, columns);
  }

  public EndFragmentColumnPlan planColumn(long blockX, long blockZ) {
    Optional<EndParentBodyState> body = parentBodyAt(blockX, blockZ);
    if (body.isEmpty()) {
      return new EndFragmentColumnPlan(
          blockX, blockZ, Optional.empty(), 0, -1, List.of(), List.of(), List.of());
    }
    EndParentBodyState parent = body.orElseThrow();
    double dx = (blockX + 0.5 - parent.center().x()) / parent.horizontalRadiusBlocks();
    double dz = (blockZ + 0.5 - parent.center().z()) / parent.horizontalRadiusBlocks();
    double radialSquared = Math.min(1.0, dx * dx + dz * dz);
    double halfHeight =
        Math.max(4.0, parent.verticalRadiusBlocks() * StrictMath.sqrt(1.0 - radialSquared));
    int baseY =
        clamp((int) StrictMath.floor(parent.center().y() - halfHeight), MIN_Y, MAX_Y_EXCLUSIVE - 1);
    int naturalTopY =
        clamp((int) StrictMath.ceil(parent.center().y() + halfHeight), baseY, MAX_Y_EXCLUSIVE - 1);
    int impactDepth = Math.min(parent.impactDepthAt(blockX, blockZ), naturalTopY - baseY - 1);
    int solidTopExclusive = naturalTopY + 1;
    List<EndTerrainInterval> impactMelt = List.of();
    if (impactDepth > 0) {
      int craterFloor = Math.max(baseY, naturalTopY - impactDepth);
      int meltStart = craterFloor;
      int meltEnd =
          Math.min(naturalTopY + 1, meltStart + Math.max(1, parent.impactMeltThicknessBlocks()));
      solidTopExclusive = Math.max(baseY + 1, meltEnd);
      impactMelt = List.of(new EndTerrainInterval(meltStart, solidTopExclusive));
    }
    List<EndTerrainInterval> solid = List.of(new EndTerrainInterval(baseY, solidTopExclusive));
    int regolithStart = Math.max(baseY, solidTopExclusive - (impactDepth > 0 ? 4 : 3));
    List<EndTerrainInterval> regolith =
        List.of(new EndTerrainInterval(regolithStart, solidTopExclusive));
    return new EndFragmentColumnPlan(
        blockX, blockZ, body, baseY, solidTopExclusive - 1, solid, regolith, impactMelt);
  }

  /** Returns the containing island body, or empty for the central gap, outer void, and open sky. */
  public Optional<EndParentBodyState> parentBodyAt(long blockX, long blockZ) {
    if (centralBody.containsHorizontal(blockX, blockZ)) {
      return Optional.of(centralBody);
    }
    long cellX = Math.floorDiv(blockX, ISLAND_CELL_BLOCKS);
    long cellZ = Math.floorDiv(blockZ, ISLAND_CELL_BLOCKS);
    Optional<EndParentBodyState> body = parentBodyAtCell(cellX, cellZ);
    return body.filter(value -> value.containsHorizontal(blockX, blockZ));
  }

  /** Returns the deterministic parent fragment for a lattice cell in the central/outer ring. */
  public Optional<EndParentBodyState> parentBodyAtCell(long cellX, long cellZ) {
    if (Math.max(Math.abs(cellX), Math.abs(cellZ)) > OUTER_RING_CELL_RADIUS) {
      return Optional.empty();
    }
    CellKey cell = new CellKey("end:parent", cellX, cellZ);
    return Optional.of(
        bodyCache.computeIfAbsent(
            cell, ignored -> EndParentBodyState.from(cellX, cellZ, identity)));
  }

  public DimensionGeologyProfile profile() {
    return PROFILE;
  }

  /** Returns the immutable identity used for every parent, fragment, and terrain field. */
  public WorldIdentity worldIdentity() {
    return identity;
  }

  private static int clamp(int value, int minimum, int maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }
}
