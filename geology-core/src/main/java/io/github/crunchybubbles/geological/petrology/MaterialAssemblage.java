package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Exact constituent-volume recipe; one million parts represent the whole bulk-rock parcel. */
public record MaterialAssemblage(Map<String, Long> modesPpm) {
  public static final long SCALE = 1_000_000L;

  public MaterialAssemblage {
    TreeMap<String, Long> sorted = new TreeMap<>();
    modesPpm.forEach(
        (constituentId, amount) -> {
          if (constituentId == null || constituentId.isBlank() || amount == null || amount < 0) {
            throw new IllegalArgumentException("constituent modes must be named and non-negative");
          }
          if (amount > 0) {
            sorted.put(constituentId, amount);
          }
        });
    long sum = sorted.values().stream().mapToLong(Long::longValue).sum();
    if (sum != SCALE) {
      throw new IllegalArgumentException(
          "constituent modes must close to " + SCALE + ", found " + sum);
    }
    modesPpm = Collections.unmodifiableMap(sorted);
  }

  public static MaterialAssemblage blend(
      MaterialAssemblage original, MaterialAssemblage target, long replacementPpm) {
    if (replacementPpm < 0 || replacementPpm > SCALE) {
      throw new IllegalArgumentException("replacement fraction must lie in [0, 1000000]");
    }
    if (replacementPpm == 0) {
      return original;
    }
    return weightedBlend(
        replacementPpm == SCALE
            ? List.of(new Share(target, SCALE))
            : List.of(
                new Share(original, SCALE - replacementPpm), new Share(target, replacementPpm)));
  }

  public static MaterialAssemblage weightedBlend(List<Share> shares) {
    shares = List.copyOf(shares);
    if (shares.isEmpty()) {
      throw new IllegalArgumentException("weighted assemblage requires at least one share");
    }
    long fractionTotal = 0;
    TreeMap<String, Long> numerators = new TreeMap<>();
    for (Share share : shares) {
      fractionTotal = Math.addExact(fractionTotal, share.fractionPpm());
      share
          .assemblage()
          .modesPpm()
          .forEach(
              (id, mode) ->
                  numerators.merge(
                      id, Math.multiplyExact(mode, share.fractionPpm()), Math::addExact));
    }
    if (fractionTotal != SCALE) {
      throw new IllegalArgumentException("weighted assemblage fractions must close to " + SCALE);
    }

    TreeMap<String, Long> blended = new TreeMap<>();
    TreeMap<String, Long> remainders = new TreeMap<>();
    long allocated = 0;
    for (Map.Entry<String, Long> entry : numerators.entrySet()) {
      long whole = entry.getValue() / SCALE;
      blended.put(entry.getKey(), whole);
      remainders.put(entry.getKey(), entry.getValue() % SCALE);
      allocated += whole;
    }
    long missing = SCALE - allocated;
    remainders.entrySet().stream()
        .sorted(
            Map.Entry.<String, Long>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(missing)
        .forEach(entry -> blended.merge(entry.getKey(), 1L, Long::sum));
    return new MaterialAssemblage(blended);
  }

  public record Share(MaterialAssemblage assemblage, long fractionPpm) {
    public Share {
      if (assemblage == null || fractionPpm <= 0 || fractionPpm > SCALE) {
        throw new IllegalArgumentException(
            "assemblage share must have material and a fraction in (0, scale]");
      }
    }
  }
}
