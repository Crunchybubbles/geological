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

/** Samples one bounded triangular modal deviation per rock body and closes it exactly. */
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
    TreeMap<String, Long> weights = new TreeMap<>();
    for (Map.Entry<String, Long> mode : rock.primaryAssemblage().modesPpm().entrySet()) {
      String purpose = "mode:" + mode.getKey();
      double triangular = stream.unitDouble(purpose, 0) + stream.unitDouble(purpose, 1) - 1.0;
      long delta = StrictMath.round(mode.getValue() * rock.modalSpreadFraction() * triangular);
      weights.put(mode.getKey(), StrictMath.max(1L, mode.getValue() + delta));
    }
    return normalize(weights, stream);
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
}
