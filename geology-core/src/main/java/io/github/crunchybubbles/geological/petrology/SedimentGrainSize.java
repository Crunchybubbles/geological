package io.github.crunchybubbles.geological.petrology;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exact coarse grain-size spectrum for mechanically released or deposited sediment. */
public record SedimentGrainSize(long gravelAndCoarserPpm, long sandPpm, long finesPpm) {
  public SedimentGrainSize {
    if (gravelAndCoarserPpm < 0 || sandPpm < 0 || finesPpm < 0) {
      throw new IllegalArgumentException("sediment grain-size fractions must be non-negative");
    }
    long total = Math.addExact(Math.addExact(gravelAndCoarserPpm, sandPpm), finesPpm);
    if (total != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sediment grain-size fractions must close to " + MaterialAssemblage.SCALE);
    }
  }

  public static SedimentGrainSize weightedBlend(List<Share> shares) {
    if (shares == null || shares.isEmpty()) {
      throw new IllegalArgumentException("sediment blend requires at least one share");
    }
    shares = List.copyOf(shares);
    long fractionTotal = 0;
    long[] numerators = new long[3];
    for (Share share : shares) {
      fractionTotal = Math.addExact(fractionTotal, share.fractionPpm());
      numerators[0] =
          Math.addExact(
              numerators[0],
              Math.multiplyExact(share.grainSize().gravelAndCoarserPpm(), share.fractionPpm()));
      numerators[1] =
          Math.addExact(
              numerators[1], Math.multiplyExact(share.grainSize().sandPpm(), share.fractionPpm()));
      numerators[2] =
          Math.addExact(
              numerators[2], Math.multiplyExact(share.grainSize().finesPpm(), share.fractionPpm()));
    }
    if (fractionTotal != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sediment blend fractions must close to " + MaterialAssemblage.SCALE);
    }

    long[] blended = new long[3];
    List<Remainder> remainders = new ArrayList<>(3);
    long allocated = 0;
    for (int index = 0; index < numerators.length; index++) {
      blended[index] = numerators[index] / MaterialAssemblage.SCALE;
      allocated += blended[index];
      remainders.add(new Remainder(index, numerators[index] % MaterialAssemblage.SCALE));
    }
    long missing = MaterialAssemblage.SCALE - allocated;
    remainders.stream()
        .sorted(
            Comparator.comparingLong(Remainder::remainder)
                .reversed()
                .thenComparingInt(Remainder::index))
        .limit(missing)
        .forEach(remainder -> blended[remainder.index()]++);
    return new SedimentGrainSize(blended[0], blended[1], blended[2]);
  }

  public record Share(SedimentGrainSize grainSize, long fractionPpm) {
    public Share {
      if (grainSize == null || fractionPpm <= 0 || fractionPpm > MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException(
            "sediment share must have grain sizes and a fraction in (0, scale]");
      }
    }
  }

  private record Remainder(int index, long remainder) {}
}
