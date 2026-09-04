package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.query.MaterialRun;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Derives bounded surface evidence from the existing Overworld trace without persisting hidden
 * geology or mutating a chunk.
 */
public final class OverworldExplorationObservationPlanner {
  private static final int MAX_CONTACT_OBSERVATIONS = 8;
  private final OverworldRegolithPlanner regolith;

  private OverworldExplorationObservationPlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldExplorationObservationPlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldExplorationObservationPlanner(
        Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Plans only observations plausibly exposed at or immediately below the present surface. */
  public List<OverworldExplorationObservation> plan(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(regolith.baseTerrain()).plan(blockX, blockZ);
    OverworldRegolithColumnPlan surface = regolith.plan(blockX, blockZ);
    OverworldColumnDebugTrace trace = OverworldColumnDebugTrace.from(base, air, surface);

    List<OverworldExplorationObservation> observations = new ArrayList<>();
    if (base.hasSolidTerrain()) {
      int surfaceY = base.solidMaxYExclusive() - 1;
      switch (trace.clueKind()) {
        case BEDROCK_OUTCROP ->
            observations.add(
                observation(
                    blockX,
                    surfaceY,
                    blockZ,
                    ExplorationObservationKind.OUTCROP,
                    trace.surfaceMaterial(),
                    null,
                    trace.sourceBodyIds(),
                    900_000,
                    1));
        case COLLUVIAL_MANTLE, ALLUVIAL_PLACER ->
            observations.add(
                observation(
                    blockX,
                    surfaceY,
                    blockZ,
                    ExplorationObservationKind.FLOAT,
                    trace.surfaceMaterial(),
                    null,
                    union(trace.surfaceMaterial().rockBodyId(), trace.sourceBodyIds()),
                    650_000,
                    4));
        case IN_SITU_REGOLITH -> {}
      }
    }

    int contactCount = 0;
    List<MaterialRun> runs = base.lithologyRuns();
    if (surface.hasRegolith() && !runs.isEmpty()) {
      MaterialRun bedrock = runs.getLast();
      if (!trace.surfaceMaterial().equals(bedrock.state())) {
        observations.add(
            observation(
                blockX,
                surface.regolithMinYInclusive(),
                blockZ,
                ExplorationObservationKind.CONTACT,
                trace.surfaceMaterial(),
                bedrock.state(),
                union(
                    trace.surfaceMaterial().rockBodyId(),
                    union(bedrock.state().rockBodyId(), trace.sourceBodyIds())),
                800_000,
                4));
        contactCount++;
      }
    }
    for (int index = 0; index + 1 < runs.size(); index++) {
      MaterialRun lower = runs.get(index);
      MaterialRun upper = runs.get(index + 1);
      int boundaryY = lower.maxYExclusive();
      if (boundaryY < surface.regolithMinYInclusive()
          || lower.state().equals(upper.state())
          || contactCount >= MAX_CONTACT_OBSERVATIONS) {
        continue;
      }
      observations.add(
          observation(
              blockX,
              boundaryY,
              blockZ,
              ExplorationObservationKind.CONTACT,
              lower.state(),
              upper.state(),
              union(lower.state().rockBodyId(), upper.state().rockBodyId()),
              750_000,
              16));
      contactCount++;
    }

    for (MaterialRun run : runs) {
      if (run.maxYExclusive() < surface.regolithMinYInclusive() || !run.state().faultDamageZone()) {
        continue;
      }
      observations.add(
          observation(
              blockX,
              Math.max(run.minYInclusive(), surface.regolithMinYInclusive()),
              blockZ,
              ExplorationObservationKind.STRUCTURAL,
              run.state(),
              null,
              List.of(run.state().rockBodyId()),
              700_000,
              8));
    }
    return observations.stream()
        .sorted(
            Comparator.comparingInt(OverworldExplorationObservation::blockY)
                .thenComparing(OverworldExplorationObservation::kind)
                .thenComparing(OverworldExplorationObservation::observationId))
        .toList();
  }

  /** Plans observations for exactly the target chunk in stable X-then-Z column order. */
  public List<OverworldExplorationObservation> planTargetChunk() {
    ChunkBlockBounds bounds = regolith.context().targetBounds();
    List<OverworldExplorationObservation> observations = new ArrayList<>();
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        observations.addAll(plan(blockX, blockZ));
      }
    }
    return List.copyOf(observations);
  }

  private OverworldExplorationObservation observation(
      long blockX,
      int blockY,
      long blockZ,
      ExplorationObservationKind kind,
      io.github.crunchybubbles.geological.query.MaterialState material,
      io.github.crunchybubbles.geological.query.MaterialState adjacentMaterial,
      List<StableId> provenanceBodyIds,
      int confidencePpm,
      int observationScaleBlocks) {
    StableId id =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "observation:" + kind.name().toLowerCase(java.util.Locale.ROOT),
                CellKey.containing("block", blockX, blockZ, 1),
                blockY)
            .stableId();
    return new OverworldExplorationObservation(
        id,
        blockX,
        blockY,
        blockZ,
        kind,
        material,
        adjacentMaterial,
        provenanceBodyIds,
        confidencePpm,
        observationScaleBlocks);
  }

  private static List<StableId> union(StableId first, StableId second) {
    return List.of(first, second);
  }

  private static List<StableId> union(StableId first, List<StableId> rest) {
    List<StableId> combined = new ArrayList<>(rest.size() + 1);
    combined.add(first);
    combined.addAll(rest);
    return combined;
  }
}
