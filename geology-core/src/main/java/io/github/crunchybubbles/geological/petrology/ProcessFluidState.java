package io.github.crunchybubbles.geological.petrology;

/** Compact, authored fluid conditions for rule-table lookup rather than equilibrium solving. */
public record ProcessFluidState(
    FluidMedium medium,
    RedoxClass redox,
    AcidityClass acidity,
    SalinityClass salinity,
    SulfurState sulfurState,
    LigandCapacities ligandCapacities,
    int integratedFluxClass) {
  public ProcessFluidState {
    if (medium == null
        || redox == null
        || acidity == null
        || salinity == null
        || sulfurState == null
        || ligandCapacities == null) {
      throw new IllegalArgumentException("process fluid state must be complete");
    }
    if (integratedFluxClass < 0 || integratedFluxClass > 3) {
      throw new IllegalArgumentException("integrated fluid-flux class must lie in [0, 3]");
    }
  }
}
