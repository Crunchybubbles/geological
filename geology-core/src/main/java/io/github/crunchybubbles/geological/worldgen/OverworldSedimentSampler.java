package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialConstituentDefinition;
import io.github.crunchybubbles.geological.petrology.MaterialConstituentKind;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.petrology.TraceElementVector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Produces deterministic, surface-only soil and sediment evidence from the Phase 2 material query.
 * No sample or upstream sediment inventory is persisted.
 */
public final class OverworldSedimentSampler {
  private static final long SCALE = MaterialAssemblage.SCALE;
  private static final long REPORTED_MODE_THRESHOLD_PPM = 20_000L;
  private static final int MAX_REPORTED_MODES = 8;
  private static final double HEAVY_MIN_DENSITY = 3.0;
  private static final double HEAVY_MIN_RESISTANCE = 0.45;
  private final OverworldRegolithPlanner regolith;

  private OverworldSedimentSampler(OverworldRegolithPlanner regolith) {
    this.regolith = regolith;
  }

  public static OverworldSedimentSampler from(OverworldRegolithPlanner regolith) {
    return new OverworldSedimentSampler(Objects.requireNonNull(regolith, "regolith planner"));
  }

  public OverworldSedimentSample sampleSoil(long blockX, long blockZ) {
    return sample(ExplorationSampleKind.SOIL, blockX, blockZ);
  }

  public OverworldSedimentSample sampleStreamSediment(long blockX, long blockZ) {
    return sample(ExplorationSampleKind.STREAM_SEDIMENT, blockX, blockZ);
  }

  public OverworldSedimentSample sampleHeavyMineral(long blockX, long blockZ) {
    return sample(ExplorationSampleKind.HEAVY_MINERAL, blockX, blockZ);
  }

  /** Samples the present surface using the rules for the requested field method. */
  public OverworldSedimentSample sample(ExplorationSampleKind kind, long blockX, long blockZ) {
    Objects.requireNonNull(kind, "sample kind");
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    if (!base.hasSolidTerrain()) {
      throw new IllegalArgumentException("surface sampling requires a solid terrain column");
    }
    SurfacePetrologicSample surface =
        regolith.material().surface(new Point2(blockX + 0.5, blockZ + 0.5));
    requireSamplingSetting(kind, surface);

    int blockY = base.solidMaxYExclusive() - 1;
    PetrologicSample material = surface.material();
    Map<String, Long> reported = reportedModes(material.resolvedAssemblage());
    Map<String, Long> heavy =
        kind == ExplorationSampleKind.HEAVY_MINERAL ? heavyMineralModes(material) : Map.of();
    var drainage = surface.surface().fields().drainage();
    Map<ChemicalElement, Long> indicators =
        indicatorSignals(
            material.traceElementVector(),
            kind,
            drainage.flowAccumulation(),
            drainage.hydraulicTrapScore());
    int concentration =
        concentrationIndex(
            kind,
            surface.surface().fields().weatheringDepth(),
            drainage.flowAccumulation(),
            drainage.hydraulicTrapScore());
    int confidence = confidence(kind, heavy);
    StableId sampleId =
        regolith.context().request().worldIdentity().stream(
                "geological:exploration",
                "sample:" + kind.name().toLowerCase(java.util.Locale.ROOT),
                CellKey.containing("block", blockX, blockZ, 1),
                blockY)
            .stableId();
    return new OverworldSedimentSample(
        sampleId,
        blockX,
        blockY,
        blockZ,
        kind,
        surface.context().kind(),
        io.github.crunchybubbles.geological.query.MaterialState.from(material.geology()),
        reported,
        heavy,
        indicators,
        provenance(surface),
        drainage.flowAccumulation(),
        drainage.hydraulicTrapScore(),
        drainage.channelDistance(),
        concentration,
        confidence);
  }

  private static void requireSamplingSetting(
      ExplorationSampleKind kind, SurfacePetrologicSample surface) {
    SurfaceMaterialKind surfaceKind = surface.context().kind();
    boolean channel = surface.surface().fields().drainage().channel();
    switch (kind) {
      case SOIL -> {
        if (surfaceKind == SurfaceMaterialKind.BEDROCK_OUTCROP
            || surfaceKind == SurfaceMaterialKind.ALLUVIAL_PLACER) {
          throw new IllegalArgumentException(
              "soil samples require exposed regolith or colluvial mantle");
        }
      }
      case STREAM_SEDIMENT, HEAVY_MINERAL -> {
        if (!channel) {
          throw new IllegalArgumentException(
              "stream and heavy-mineral samples require a channel reach");
        }
      }
    }
  }

  private static Map<String, Long> reportedModes(MaterialAssemblage assemblage) {
    List<Map.Entry<String, Long>> candidates =
        assemblage.modesPpm().entrySet().stream()
            .filter(entry -> entry.getValue() >= REPORTED_MODE_THRESHOLD_PPM)
            .sorted(modeOrder())
            .limit(MAX_REPORTED_MODES)
            .toList();
    if (candidates.isEmpty()) {
      candidates = assemblage.modesPpm().entrySet().stream().sorted(modeOrder()).limit(1).toList();
    }
    TreeMap<String, Long> result = new TreeMap<>();
    candidates.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return Collections.unmodifiableMap(result);
  }

  private Map<String, Long> heavyMineralModes(PetrologicSample material) {
    List<WeightedMode> candidates = new ArrayList<>();
    material
        .resolvedAssemblage()
        .modesPpm()
        .forEach(
            (id, mode) -> {
              MaterialConstituentDefinition definition =
                  regolith.material().catalog().requireConstituent(id);
              if (definition.kind() != MaterialConstituentKind.MINERAL
                  || definition.densityGramsPerCubicCentimeter() < HEAVY_MIN_DENSITY
                  || definition.weatheringResistance() < HEAVY_MIN_RESISTANCE) {
                return;
              }
              double densityFactor =
                  clamp((definition.densityGramsPerCubicCentimeter() - 2.5) / 4.0, 0.0, 1.0);
              double durabilityFactor = definition.weatheringResistance();
              double weight =
                  mode * (0.25 + 0.75 * densityFactor) * (0.35 + 0.65 * durabilityFactor);
              if (weight > 0.0 && Double.isFinite(weight)) {
                candidates.add(new WeightedMode(id, weight));
              }
            });
    candidates.sort(
        Comparator.comparingDouble(WeightedMode::weight)
            .reversed()
            .thenComparing(WeightedMode::id));
    return normalize(candidates.stream().limit(MAX_REPORTED_MODES).toList());
  }

  private static Map<ChemicalElement, Long> indicatorSignals(
      TraceElementVector trace, ExplorationSampleKind kind, double flow, double trap) {
    double response =
        switch (kind) {
          case SOIL -> 1.0;
          case STREAM_SEDIMENT -> 0.25 + 0.75 * flow;
          case HEAVY_MINERAL -> 0.4 + 0.6 * trap;
        };
    TreeMap<ChemicalElement, Long> result = new TreeMap<>();
    trace
        .concentrationPpm()
        .forEach((element, amount) -> result.put(element, coarseSignal(amount, response)));
    return Collections.unmodifiableMap(result);
  }

  private static long coarseSignal(long amount, double response) {
    long adjusted = Math.max(1L, Math.round(amount * response));
    if (adjusted <= 10L) {
      return 1L;
    }
    if (adjusted <= 100L) {
      return 10L;
    }
    if (adjusted <= 1_000L) {
      return 100L;
    }
    if (adjusted <= 10_000L) {
      return 1_000L;
    }
    if (adjusted <= 100_000L) {
      return 10_000L;
    }
    if (adjusted <= 500_000L) {
      return 100_000L;
    }
    return SCALE;
  }

  private static int concentrationIndex(
      ExplorationSampleKind kind, double weatheringDepth, double flow, double trap) {
    double value =
        switch (kind) {
          case SOIL -> 0.2 + 0.8 * clamp(weatheringDepth / 12.0, 0.0, 1.0);
          case STREAM_SEDIMENT -> (0.2 + 0.8 * flow) * (0.35 + 0.65 * trap);
          case HEAVY_MINERAL -> (0.3 + 0.7 * flow) * (0.25 + 0.75 * trap);
        };
    return (int) Math.round(clamp(value, 0.0, 1.0) * SCALE);
  }

  private static int confidence(ExplorationSampleKind kind, Map<String, Long> heavy) {
    if (kind == ExplorationSampleKind.HEAVY_MINERAL) {
      return heavy.isEmpty() ? 450_000 : 650_000;
    }
    return kind == ExplorationSampleKind.STREAM_SEDIMENT ? 680_000 : 720_000;
  }

  private static List<StableId> provenance(SurfacePetrologicSample surface) {
    List<StableId> ids = new ArrayList<>(surface.context().sourceBodyIds());
    ids.add(surface.context().materialBodyId());
    ids.add(surface.surface().bedrock().rockBodyId());
    return ids.stream().distinct().sorted().toList();
  }

  private static Map<String, Long> normalize(List<WeightedMode> candidates) {
    if (candidates.isEmpty()) {
      return Map.of();
    }
    double total = candidates.stream().mapToDouble(WeightedMode::weight).sum();
    TreeMap<String, Long> result = new TreeMap<>();
    List<Remainder> remainders = new ArrayList<>();
    long allocated = 0L;
    for (WeightedMode candidate : candidates) {
      double exact = candidate.weight() / total * SCALE;
      long whole = (long) StrictMath.floor(exact);
      result.put(candidate.id(), whole);
      allocated += whole;
      remainders.add(new Remainder(candidate.id(), exact - whole));
    }
    remainders.sort(
        Comparator.comparingDouble(Remainder::remainder).reversed().thenComparing(Remainder::id));
    long missing = SCALE - allocated;
    for (int index = 0; index < missing; index++) {
      result.merge(remainders.get(index % remainders.size()).id(), 1L, Long::sum);
    }
    return Collections.unmodifiableMap(result);
  }

  private static Comparator<Map.Entry<String, Long>> modeOrder() {
    return Map.Entry.<String, Long>comparingByValue()
        .reversed()
        .thenComparing(Map.Entry.comparingByKey());
  }

  private static double clamp(double value, double minimum, double maximum) {
    return StrictMath.max(minimum, StrictMath.min(maximum, value));
  }

  private record WeightedMode(String id, double weight) {}

  private record Remainder(String id, double remainder) {}
}
