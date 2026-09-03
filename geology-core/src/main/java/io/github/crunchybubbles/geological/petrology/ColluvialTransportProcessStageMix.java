package io.github.crunchybubbles.geological.petrology;

/** Process-class mixture snapshots at each normalized colluvial ledger stage. */
public record ColluvialTransportProcessStageMix(
    ColluvialTransportProcessMix capacity,
    ColluvialTransportProcessMix mobilized,
    ColluvialTransportProcessMix arrived,
    ColluvialTransportProcessMix deposited) {
  public ColluvialTransportProcessStageMix {
    if (capacity == null || mobilized == null || arrived == null || deposited == null) {
      throw new IllegalArgumentException("colluvial process-stage mixtures are required");
    }
  }

  /** Derives deterministic normalized process mixtures from the exact per-process ledgers. */
  public static ColluvialTransportProcessStageMix from(ColluvialSedimentBudget budget) {
    if (budget == null) {
      throw new IllegalArgumentException("colluvial sediment budget is required");
    }
    long[] capacity = new long[ColluvialTransportProcess.ProcessClass.values().length];
    long[] mobilized = new long[capacity.length];
    long[] arrived = new long[capacity.length];
    long[] deposited = new long[capacity.length];
    for (ColluvialTransportProcessUsage usage : budget.transportProcessUsages()) {
      int index = usage.processClass().ordinal();
      capacity[index] = usage.capacityFixedUnits();
      mobilized[index] = usage.mobilizedFixedUnits();
      arrived[index] =
          Math.subtractExact(usage.mobilizedFixedUnits(), usage.transportLossFixedUnits());
      deposited[index] = usage.depositedFixedUnits();
    }
    return new ColluvialTransportProcessStageMix(
        ColluvialTransportProcessMix.fromWeights(capacity),
        ColluvialTransportProcessMix.fromWeights(mobilized),
        ColluvialTransportProcessMix.fromWeights(arrived),
        ColluvialTransportProcessMix.fromWeights(deposited));
  }
}
