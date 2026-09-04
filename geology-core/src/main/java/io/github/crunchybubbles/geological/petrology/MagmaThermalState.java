package io.github.crunchybubbles.geological.petrology;

/**
 * Reduced magma temperature, pressure, and depth context for one differentiated pulse.
 *
 * <p>Classes and the normalized thermal potential are deterministic response-table evidence, not
 * measured temperatures, lithostatic pressures, or a magma-chamber thermodynamic solve.
 */
public record MagmaThermalState(
    TemperatureClass temperatureClass,
    PressureClass pressureClass,
    DepthClass depthClass,
    long thermalPotentialPpm) {
  public MagmaThermalState {
    if (temperatureClass == null
        || pressureClass == null
        || depthClass == null
        || thermalPotentialPpm < 0L
        || thermalPotentialPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("magma thermal state is incomplete or out of bounds");
    }
  }

  /** Derives the canonical thermal context from pulse order and differentiation ledger. */
  public static MagmaThermalState proofFor(
      int pulseOrder, MagmaDifferentiationState differentiationState) {
    if (pulseOrder < 0 || differentiationState == null) {
      throw new IllegalArgumentException("magma thermal inputs are incomplete or out of bounds");
    }
    if (pulseOrder == 0) {
      return new MagmaThermalState(
          TemperatureClass.ULTRA_HOT, PressureClass.HIGH, DepthClass.DEEP_CRUSTAL, 850_000L);
    }
    if (pulseOrder == 1) {
      return new MagmaThermalState(
          TemperatureClass.HOT, PressureClass.MODERATE, DepthClass.MID_CRUSTAL, 700_000L);
    }
    return new MagmaThermalState(
        TemperatureClass.HOT,
        PressureClass.LOW_TO_MODERATE,
        DepthClass.SHALLOW_CRUSTAL,
        differentiationState.residualFluidPotential()
                == MagmaDifferentiationState.ResidualFluidPotential.VERY_HIGH
            ? 650_000L
            : 600_000L);
  }

  public enum TemperatureClass {
    HOT,
    ULTRA_HOT
  }

  public enum PressureClass {
    LOW_TO_MODERATE,
    MODERATE,
    HIGH
  }

  public enum DepthClass {
    SHALLOW_CRUSTAL,
    MID_CRUSTAL,
    DEEP_CRUSTAL
  }
}
