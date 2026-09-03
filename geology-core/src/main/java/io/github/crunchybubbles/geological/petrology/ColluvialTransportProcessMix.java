package io.github.crunchybubbles.geological.petrology;

/** Exact normalized deposited mixture of coarse colluvial transport-process labels. */
public record ColluvialTransportProcessMix(
    ColluvialTransportProcess.ProcessClass dominantProcess,
    long hillslopeCreepFractionPpm,
    long sheetwashFractionPpm,
    long dryRavelFractionPpm) {
  public ColluvialTransportProcessMix {
    if (dominantProcess == null
        || hillslopeCreepFractionPpm < 0
        || sheetwashFractionPpm < 0
        || dryRavelFractionPpm < 0
        || Math.addExact(
                Math.addExact(hillslopeCreepFractionPpm, sheetwashFractionPpm), dryRavelFractionPpm)
            != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("colluvial transport-process mixture is invalid");
    }
    long maximum =
        Math.max(hillslopeCreepFractionPpm, Math.max(sheetwashFractionPpm, dryRavelFractionPpm));
    long selected =
        switch (dominantProcess) {
          case HILLSLOPE_CREEP -> hillslopeCreepFractionPpm;
          case SHEETWASH -> sheetwashFractionPpm;
          case DRY_RAVEL -> dryRavelFractionPpm;
        };
    if (selected != maximum) {
      throw new IllegalArgumentException(
          "colluvial dominant process must have the largest deposited fraction");
    }
  }

  /** Derives normalized deposited process fractions from every budget tranche. */
  public static ColluvialTransportProcessMix from(ColluvialSedimentBudget budget) {
    if (budget == null) {
      throw new IllegalArgumentException("colluvial sediment budget is required");
    }
    long[] depositedByProcess = new long[ColluvialTransportProcess.ProcessClass.values().length];
    add(depositedByProcess, budget.weatheredMatrixBalance());
    for (ColluvialSedimentBudget.SourceBalance source : budget.sourceBalances()) {
      add(depositedByProcess, source.balance());
    }
    long[] fractions = apportion(MaterialAssemblage.SCALE, depositedByProcess);
    ColluvialTransportProcess.ProcessClass dominant =
        ColluvialTransportProcess.ProcessClass.HILLSLOPE_CREEP;
    long selected = fractions[dominant.ordinal()];
    for (ColluvialTransportProcess.ProcessClass process :
        ColluvialTransportProcess.ProcessClass.values()) {
      if (fractions[process.ordinal()] > selected) {
        dominant = process;
        selected = fractions[process.ordinal()];
      }
    }
    return new ColluvialTransportProcessMix(dominant, fractions[0], fractions[1], fractions[2]);
  }

  private static void add(long[] depositedByProcess, ColluvialSedimentBudget.InputBalance balance) {
    depositedByProcess[balance.transportProcess().processClass().ordinal()] =
        Math.addExact(
            depositedByProcess[balance.transportProcess().processClass().ordinal()],
            balance.depositedFixedUnits());
  }

  private static long[] apportion(long allocation, long[] weights) {
    long weightTotal = 0;
    for (long weight : weights) {
      weightTotal = Math.addExact(weightTotal, weight);
    }
    if (weightTotal <= 0) {
      throw new IllegalArgumentException("positive deposited process weights are required");
    }
    long[] apportioned = new long[weights.length];
    long allocated = 0;
    long[] remainders = new long[weights.length];
    for (int index = 0; index < weights.length; index++) {
      long numerator = Math.multiplyExact(weights[index], allocation);
      apportioned[index] = numerator / weightTotal;
      allocated = Math.addExact(allocated, apportioned[index]);
      remainders[index] = numerator % weightTotal;
    }
    long missing = allocation - allocated;
    for (long rank = 0; rank < missing; rank++) {
      int best = 0;
      for (int index = 1; index < remainders.length; index++) {
        if (remainders[index] > remainders[best]) {
          best = index;
        }
      }
      apportioned[best]++;
      remainders[best] = -1;
    }
    return apportioned;
  }
}
