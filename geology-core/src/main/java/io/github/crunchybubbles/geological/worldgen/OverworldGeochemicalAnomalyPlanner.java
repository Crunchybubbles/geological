package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.ObjectRandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Applies deterministic detection limits and bounded uncertainty to surface indicator signals. */
public final class OverworldGeochemicalAnomalyPlanner {
  private static final long SCALE = MaterialAssemblage.SCALE;
  private final OverworldSedimentSampler sampler;
  private final OverworldRegolithPlanner regolith;

  private OverworldGeochemicalAnomalyPlanner(OverworldSedimentSampler sampler) {
    this.sampler = sampler;
    this.regolith = sampler.regolith();
  }

  public static OverworldGeochemicalAnomalyPlanner from(OverworldSedimentSampler sampler) {
    return new OverworldGeochemicalAnomalyPlanner(
        Objects.requireNonNull(sampler, "sediment sampler"));
  }

  public GeochemicalAnomalyEstimate estimateSoil(long blockX, long blockZ) {
    return estimate(sampler.sampleSoil(blockX, blockZ));
  }

  public GeochemicalAnomalyEstimate estimateStreamSediment(long blockX, long blockZ) {
    return estimate(sampler.sampleStreamSediment(blockX, blockZ));
  }

  public GeochemicalAnomalyEstimate estimateHeavyMineral(long blockX, long blockZ) {
    return estimate(sampler.sampleHeavyMineral(blockX, blockZ));
  }

  /** Estimates an already collected sample without re-querying the material state. */
  public GeochemicalAnomalyEstimate estimate(OverworldSedimentSample sample) {
    Objects.requireNonNull(sample, "sediment sample");
    ObjectRandomStream random =
        regolith
            .context()
            .request()
            .worldIdentity()
            .objectStream("geological:exploration", "geochemical-anomaly", sample.sampleId());
    List<GeochemicalIndicatorEstimate> indicators = new ArrayList<>();
    sample.indicatorSignalsPpm().entrySet().stream()
        .sorted(java.util.Map.Entry.comparingByKey())
        .forEach(
            entry ->
                indicators.add(
                    indicator(
                        sample,
                        entry.getKey(),
                        entry.getValue(),
                        random,
                        entry.getKey().ordinal())));
    StableId estimateId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "geochemical-anomaly",
                CellKey.containing("block", sample.blockX(), sample.blockZ(), 1),
                sample.blockY())
            .stableId();
    int confidence = Math.max(0, sample.confidencePpm() - 120_000);
    return new GeochemicalAnomalyEstimate(
        estimateId,
        sample.sampleId(),
        sample.blockX(),
        sample.blockY(),
        sample.blockZ(),
        sample.kind(),
        indicators,
        sample.provenanceBodyIds(),
        confidence);
  }

  private static GeochemicalIndicatorEstimate indicator(
      OverworldSedimentSample sample,
      ChemicalElement element,
      long signal,
      ObjectRandomStream random,
      int counter) {
    long baseLimit = baseDetectionLimit(sample.kind(), element);
    long limit =
        Math.max(
            1L,
            Math.round(baseLimit * (0.80 + 0.01 * random.boundedInt("limit-factor", counter, 61))));
    int uncertaintyPercent =
        switch (sample.kind()) {
          case SOIL -> 35 + random.boundedInt("uncertainty-percent", counter, 21);
          case STREAM_SEDIMENT -> 45 + random.boundedInt("uncertainty-percent", counter, 26);
          case HEAVY_MINERAL -> 30 + random.boundedInt("uncertainty-percent", counter, 21);
        };
    long width = Math.max(limit, Math.round(signal * uncertaintyPercent / 100.0));
    long lower = Math.max(0L, signal - width);
    long upper = Math.min(SCALE, signal + width);
    boolean detected = signal >= limit;
    long anomalyThreshold = Math.min(SCALE, Math.multiplyExact(limit, 3L));
    int anomalyScore =
        signal <= anomalyThreshold
            ? 0
            : (int)
                Math.min(
                    SCALE,
                    Math.round(
                        (signal - anomalyThreshold)
                            * (double) SCALE
                            / Math.max(1L, Math.multiplyExact(limit, 30L))));
    return new GeochemicalIndicatorEstimate(
        element, signal, limit, lower, upper, anomalyScore, detected, !detected);
  }

  private static long baseDetectionLimit(ExplorationSampleKind kind, ChemicalElement element) {
    long methodBase =
        switch (kind) {
          case SOIL -> 100L;
          case STREAM_SEDIMENT -> 70L;
          case HEAVY_MINERAL -> 35L;
        };
    long elementFactor =
        switch (element) {
          case AU -> 1L;
          case CU, ZN, CR, TI -> 2L;
          case P, S, F, CL -> 3L;
          default -> 4L;
        };
    return Math.min(SCALE, Math.multiplyExact(methodBase, elementFactor));
  }
}
