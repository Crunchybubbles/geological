package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.ObjectRandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Samples body-scale mineral modes and bounded physical properties without mutable RNG state. */
public final class BodyCompositionSampler {
  private final WorldIdentity identity;

  public BodyCompositionSampler(WorldIdentity identity) {
    if (identity == null) {
      throw new IllegalArgumentException("world identity is required");
    }
    this.identity = identity;
  }

  public MineralAssemblage sample(RockDefinition rock, StableId bodyId) {
    if (rock == null || bodyId == null) {
      throw new IllegalArgumentException("rock and body identity are required");
    }
    ObjectRandomStream stream =
        identity.objectStream("geological", "body_modal_composition", bodyId);
    TreeMap<String, Long> weights = new TreeMap<>(rock.primaryAssemblage().modesPpm());
    for (ModalVariationAxis axis : rock.modalVariationAxes()) {
      String purpose = "axis:" + axis.id();
      double score = stream.unitDouble(purpose, 0) + stream.unitDouble(purpose, 1) - 1.0;
      applyAxis(weights, axis, score, stream);
    }
    return normalize(weights, stream);
  }

  public double sample(
      UnitIntervalDistribution distribution, StableId bodyId, String propertyName) {
    if (distribution == null || bodyId == null || propertyName == null || propertyName.isBlank()) {
      throw new IllegalArgumentException("property distribution, body ID, and name are required");
    }
    ObjectRandomStream stream =
        identity.objectStream("geological", "body_material_properties", bodyId);
    return distribution.sample(stream.unitDouble("property:" + propertyName, 0));
  }

  private static void applyAxis(
      Map<String, Long> weights, ModalVariationAxis axis, double score, ObjectRandomStream stream) {
    TreeMap<String, Long> deltas = new TreeMap<>();
    List<AxisRemainder> remainders = new ArrayList<>();
    long allocated = 0;
    for (Map.Entry<String, Long> loading : axis.loadingsPpm().entrySet()) {
      double exact = loading.getValue() * score;
      long whole = (long) StrictMath.floor(exact);
      deltas.put(loading.getKey(), whole);
      allocated += whole;
      long tieRank =
          ByteBuffer.wrap(
                  stream.bytes("axis-rounding-tie:" + axis.id() + ":" + loading.getKey(), 0))
              .getLong();
      remainders.add(new AxisRemainder(loading.getKey(), exact - whole, tieRank));
    }
    long correction = -allocated;
    if (StrictMath.abs(correction) > remainders.size()) {
      throw new IllegalStateException("modal variation rounding exceeded its conservation bound");
    }
    if (correction > 0) {
      remainders.sort(
          Comparator.comparingDouble(AxisRemainder::remainder)
              .reversed()
              .thenComparing(
                  AxisRemainder::tieRank, (first, second) -> Long.compareUnsigned(first, second)));
      for (int index = 0; index < correction; index++) {
        deltas.merge(remainders.get(index).mineralId(), 1L, Math::addExact);
      }
    } else if (correction < 0) {
      remainders.sort(
          Comparator.comparingDouble(AxisRemainder::remainder)
              .thenComparing(
                  AxisRemainder::tieRank, (first, second) -> Long.compareUnsigned(first, second)));
      for (int index = 0; index < -correction; index++) {
        deltas.merge(remainders.get(index).mineralId(), -1L, Math::addExact);
      }
    }
    deltas.forEach((mineralId, delta) -> weights.merge(mineralId, delta, Math::addExact));
  }

  private static MineralAssemblage normalize(Map<String, Long> weights, ObjectRandomStream stream) {
    long total = weights.values().stream().mapToLong(Long::longValue).sum();
    if (total <= 0) {
      throw new IllegalStateException("body modal distribution produced no positive weight");
    }
    TreeMap<String, Long> modes = new TreeMap<>();
    List<Remainder> remainders = new ArrayList<>();
    long allocated = 0;
    for (Map.Entry<String, Long> entry : weights.entrySet()) {
      long numerator = Math.multiplyExact(entry.getValue(), MineralAssemblage.SCALE);
      long whole = numerator / total;
      modes.put(entry.getKey(), whole);
      allocated += whole;
      long tieRank = ByteBuffer.wrap(stream.bytes("rounding-tie:" + entry.getKey(), 0)).getLong();
      remainders.add(new Remainder(entry.getKey(), numerator % total, tieRank));
    }
    remainders.sort(
        Comparator.comparingLong(Remainder::remainder)
            .reversed()
            .thenComparing(
                Remainder::tieRank, (first, second) -> Long.compareUnsigned(first, second)));
    long missing = MineralAssemblage.SCALE - allocated;
    for (int index = 0; index < missing; index++) {
      modes.merge(remainders.get(index).mineralId(), 1L, Long::sum);
    }
    return new MineralAssemblage(modes);
  }

  private record Remainder(String mineralId, long remainder, long tieRank) {}

  private record AxisRemainder(String mineralId, double remainder, long tieRank) {}
}
