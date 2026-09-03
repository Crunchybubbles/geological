package io.github.crunchybubbles.geological.petrology;

/** Exact aggregate ledger for tranches assigned to one coarse transport process. */
public record ColluvialTransportProcessUsage(
    ColluvialTransportProcess.ProcessClass processClass,
    int trancheCount,
    long capacityFixedUnits,
    long mobilizedFixedUnits,
    long retainedFixedUnits,
    long transportLossFixedUnits,
    long bypassedFixedUnits,
    long depositedFixedUnits,
    ColluvialSedimentBudget.GrainMass capacityGrainMass,
    ColluvialSedimentBudget.GrainMass mobilizedGrainMass,
    ColluvialSedimentBudget.GrainMass retainedGrainMass,
    ColluvialSedimentBudget.GrainMass transportLossGrainMass,
    ColluvialSedimentBudget.GrainMass bypassedGrainMass,
    ColluvialSedimentBudget.GrainMass depositedGrainMass) {
  public ColluvialTransportProcessUsage {
    if (processClass == null
        || trancheCount < 0
        || capacityFixedUnits < 0
        || mobilizedFixedUnits < 0
        || retainedFixedUnits < 0
        || transportLossFixedUnits < 0
        || bypassedFixedUnits < 0
        || depositedFixedUnits < 0
        || capacityGrainMass == null
        || mobilizedGrainMass == null
        || retainedGrainMass == null
        || transportLossGrainMass == null
        || bypassedGrainMass == null
        || depositedGrainMass == null
        || capacityGrainMass.totalFixedUnits() != capacityFixedUnits
        || mobilizedGrainMass.totalFixedUnits() != mobilizedFixedUnits
        || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
        || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
        || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
        || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
        || capacityFixedUnits != Math.addExact(retainedFixedUnits, mobilizedFixedUnits)
        || mobilizedFixedUnits
            != Math.addExact(
                Math.addExact(transportLossFixedUnits, bypassedFixedUnits), depositedFixedUnits)
        || trancheCount == 0
            && (capacityFixedUnits != 0
                || mobilizedFixedUnits != 0
                || retainedFixedUnits != 0
                || transportLossFixedUnits != 0
                || bypassedFixedUnits != 0
                || depositedFixedUnits != 0)) {
      throw new IllegalArgumentException("colluvial transport-process usage does not close");
    }
  }

  public boolean hasDepositedMass() {
    return depositedFixedUnits > 0;
  }

  public SedimentGrainSize depositedGrainSize() {
    return depositedGrainMass.normalizedPpm();
  }
}
