package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Derives coarse hand-sample identifications without exposing the full hidden material state. */
public final class OverworldHandSamplePlanner {
  private static final long VISIBLE_MODE_THRESHOLD_PPM = 25_000L;
  private static final int MAX_VISIBLE_CONSTITUENTS = 8;
  private final OverworldRegolithPlanner regolith;

  private OverworldHandSamplePlanner(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldHandSamplePlanner from(OverworldRegolithPlanner regolith) {
    return new OverworldHandSamplePlanner(Objects.requireNonNull(regolith, "regolith planner"));
  }

  /** Identifies the material at the exposed solid surface of one column. */
  public HandSampleIdentification identifySurface(long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    if (!base.hasSolidTerrain()) {
      throw new IllegalArgumentException("cannot collect a hand sample from an empty column");
    }
    int blockY = base.solidMaxYExclusive() - 1;
    return identifySurface(blockX, blockY, blockZ);
  }

  /** Identifies a solid block explicitly; air and fluid intervals are rejected. */
  public HandSampleIdentification identify(long blockX, int blockY, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    if (blockY < base.minYInclusive() || blockY >= base.solidMaxYExclusive()) {
      throw new IllegalArgumentException("hand samples must name a solid block in the column");
    }
    OverworldRegolithColumnPlan surface = regolith.plan(blockX, blockZ);
    if (blockY >= surface.regolithMinYInclusive()) {
      return identifySurface(blockX, blockY, blockZ);
    }
    PetrologicSample sample =
        regolith.material().sample(new Point3(blockX + 0.5, blockY + 0.5, blockZ + 0.5));
    return identification(
        blockX,
        blockY,
        blockZ,
        "SUBSURFACE",
        sample,
        List.of(sample.geology().rockBodyId()),
        700_000);
  }

  private HandSampleIdentification identifySurface(long blockX, int blockY, long blockZ) {
    SurfacePetrologicSample sample =
        regolith.material().surface(new Point2(blockX + 0.5, blockZ + 0.5));
    SurfaceMaterialKind kind = sample.context().kind();
    int confidence =
        switch (kind) {
          case BEDROCK_OUTCROP -> 900_000;
          case IN_SITU_REGOLITH -> 800_000;
          case COLLUVIAL_MANTLE -> 650_000;
          case ALLUVIAL_PLACER -> 600_000;
        };
    List<StableId> provenance = new ArrayList<>(sample.context().sourceBodyIds());
    provenance.add(sample.material().geology().rockBodyId());
    return identification(
        blockX, blockY, blockZ, kind.name(), sample.material(), provenance, confidence);
  }

  private HandSampleIdentification identification(
      long blockX,
      int blockY,
      long blockZ,
      String samplingContext,
      PetrologicSample sample,
      List<StableId> provenance,
      int confidencePpm) {
    MaterialAssemblage assemblage = sample.resolvedAssemblage();
    Map<String, Long> visible = visibleModes(assemblage);
    long visibleTotal = visible.values().stream().mapToLong(Long::longValue).sum();
    StableId sampleId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "hand-sample",
                CellKey.containing("block", blockX, blockZ, 1),
                blockY)
            .stableId();
    return new HandSampleIdentification(
        sampleId,
        blockX,
        blockY,
        blockZ,
        samplingContext,
        MaterialState.from(sample.geology()),
        sample.rock().id(),
        sample.resolvedTexture(),
        sample.geology().overprint(),
        visible,
        visibleTotal < MaterialAssemblage.SCALE,
        provenance,
        confidencePpm);
  }

  private static Map<String, Long> visibleModes(MaterialAssemblage assemblage) {
    List<Map.Entry<String, Long>> candidates =
        assemblage.modesPpm().entrySet().stream()
            .filter(entry -> entry.getValue() >= VISIBLE_MODE_THRESHOLD_PPM)
            .sorted(
                Map.Entry.<String, Long>comparingByValue()
                    .reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
            .limit(MAX_VISIBLE_CONSTITUENTS)
            .toList();
    if (candidates.isEmpty()) {
      candidates =
          assemblage.modesPpm().entrySet().stream()
              .sorted(
                  Map.Entry.<String, Long>comparingByValue()
                      .reversed()
                      .thenComparing(Map.Entry.comparingByKey()))
              .limit(1)
              .toList();
    }
    TreeMap<String, Long> visible = new TreeMap<>();
    candidates.forEach(entry -> visible.put(entry.getKey(), entry.getValue()));
    return Collections.unmodifiableMap(visible);
  }
}
