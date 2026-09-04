package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.query.MaterialState;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Transient, bounded evidence returned by a soil, stream-sediment, or heavy-mineral sample.
 *
 * <p>The constituent and indicator maps are observation-scale projections, not authoritative assay
 * results. Causal material and source bodies remain available without storing a sample in the
 * world.
 */
public record OverworldSedimentSample(
    StableId sampleId,
    long blockX,
    int blockY,
    long blockZ,
    ExplorationSampleKind kind,
    SurfaceMaterialKind surfaceKind,
    MaterialState material,
    Map<String, Long> reportedConstituentsPpm,
    Map<String, Long> heavyMineralModesPpm,
    Map<ChemicalElement, Long> indicatorSignalsPpm,
    List<StableId> provenanceBodyIds,
    double flowAccumulation,
    double hydraulicTrapScore,
    double channelDistance,
    int concentrationIndexPpm,
    int confidencePpm) {
  public OverworldSedimentSample {
    if (sampleId == null
        || kind == null
        || surfaceKind == null
        || material == null
        || reportedConstituentsPpm == null
        || heavyMineralModesPpm == null
        || indicatorSignalsPpm == null
        || provenanceBodyIds == null
        || provenanceBodyIds.isEmpty()
        || !Double.isFinite(flowAccumulation)
        || flowAccumulation < 0.0
        || flowAccumulation > 1.0
        || !Double.isFinite(hydraulicTrapScore)
        || hydraulicTrapScore < 0.0
        || hydraulicTrapScore > 1.0
        || !Double.isFinite(channelDistance)
        || channelDistance < 0.0
        || concentrationIndexPpm < 0
        || concentrationIndexPpm > 1_000_000
        || confidencePpm < 0
        || confidencePpm > 1_000_000) {
      throw new IllegalArgumentException("surface sample values are invalid");
    }
    reportedConstituentsPpm = boundedModes(reportedConstituentsPpm, "reported constituents");
    heavyMineralModesPpm = boundedModes(heavyMineralModesPpm, "heavy-mineral modes");
    indicatorSignalsPpm = boundedIndicators(indicatorSignalsPpm);
    provenanceBodyIds = List.copyOf(provenanceBodyIds).stream().sorted().toList();
    if (!provenanceBodyIds.contains(material.rockBodyId())) {
      throw new IllegalArgumentException(
          "surface sample provenance must include its material body");
    }
    if ((surfaceKind == SurfaceMaterialKind.COLLUVIAL_MANTLE
            && material.lithology() != Lithology.SOIL_COLLUVIUM)
        || (surfaceKind == SurfaceMaterialKind.ALLUVIAL_PLACER
            && material.lithology() != Lithology.ALLUVIAL_GRAVEL)) {
      throw new IllegalArgumentException("surface sample kind and material lithology disagree");
    }
    if (kind != ExplorationSampleKind.HEAVY_MINERAL && !heavyMineralModesPpm.isEmpty()) {
      throw new IllegalArgumentException(
          "only heavy-mineral samples may report a concentrate spectrum");
    }
  }

  /** Compact deterministic text suitable for a player-facing readout or notebook entry. */
  public String summary() {
    return "sample id=%s kind=%s at=(%d,%d,%d) surface=%s lithology=%s overprint=%s constituents=%s heavy=%s indicators=%s flow=%.3f trap=%.3f channelDistance=%.3f concentration=%d confidence=%d bodies=%d"
        .formatted(
            sampleId,
            kind,
            blockX,
            blockY,
            blockZ,
            surfaceKind,
            material.lithology(),
            material.overprint(),
            reportedConstituentsPpm,
            heavyMineralModesPpm,
            indicatorSignalsPpm,
            flowAccumulation,
            hydraulicTrapScore,
            channelDistance,
            concentrationIndexPpm,
            confidencePpm,
            provenanceBodyIds.size());
  }

  private static Map<String, Long> boundedModes(Map<String, Long> source, String name) {
    TreeMap<String, Long> sorted = new TreeMap<>();
    source.forEach(
        (id, amount) -> {
          if (id == null || id.isBlank() || amount == null || amount <= 0 || amount > 1_000_000) {
            throw new IllegalArgumentException(name + " must contain positive bounded values");
          }
          sorted.put(id, amount);
        });
    long sum = sorted.values().stream().mapToLong(Long::longValue).sum();
    if (sum > 1_000_000L) {
      throw new IllegalArgumentException(name + " cannot exceed the normalized sample scale");
    }
    return Collections.unmodifiableMap(sorted);
  }

  private static Map<ChemicalElement, Long> boundedIndicators(Map<ChemicalElement, Long> source) {
    TreeMap<ChemicalElement, Long> sorted = new TreeMap<>();
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount <= 0 || amount > 1_000_000) {
            throw new IllegalArgumentException("indicator signals must be positive bounded values");
          }
          sorted.put(element, amount);
        });
    return Collections.unmodifiableMap(sorted);
  }
}
