package io.github.crunchybubbles.geological.petrology;

/**
 * Reduced transport context derived from typed process-fluid conditions.
 *
 * <p>These classes are bounded response-table axes. They describe comparative fluid behavior and do
 * not claim measured temperature, pressure, water/rock ratio, or multiphase thermodynamics.
 */
public record FluidTransportState(
    TemperatureClass temperatureClass,
    PressureClass pressureClass,
    WaterRockRatioClass waterRockRatioClass,
    PhaseBehaviorClass phaseBehaviorClass,
    int integratedFluxClass) {
  public FluidTransportState {
    if (temperatureClass == null
        || pressureClass == null
        || waterRockRatioClass == null
        || phaseBehaviorClass == null
        || integratedFluxClass < 0
        || integratedFluxClass > 3) {
      throw new IllegalArgumentException("fluid transport state is incomplete or out of bounds");
    }
  }

  /** Derives the canonical transport axes from the existing fluid condition record. */
  public static FluidTransportState proofFor(ProcessFluidState fluidState) {
    if (fluidState == null) {
      throw new IllegalArgumentException("process fluid state is required");
    }
    Axes axes =
        switch (fluidState.medium()) {
          case MAGMATIC_HYDROTHERMAL ->
              new Axes(TemperatureClass.HOT, PressureClass.HIGH, WaterRockRatioClass.HIGH);
          case SEAWATER_HYDROTHERMAL ->
              new Axes(TemperatureClass.WARM, PressureClass.MODERATE, WaterRockRatioClass.HIGH);
          case MIXED_HYDROTHERMAL ->
              new Axes(TemperatureClass.WARM, PressureClass.MODERATE, WaterRockRatioClass.MODERATE);
          case METEORIC_WATER ->
              new Axes(TemperatureClass.COOL, PressureClass.LOW, WaterRockRatioClass.VERY_HIGH);
        };
    PhaseBehaviorClass phaseBehavior =
        switch (fluidState.medium()) {
          case MIXED_HYDROTHERMAL, METEORIC_WATER -> PhaseBehaviorClass.MIXING;
          case MAGMATIC_HYDROTHERMAL, SEAWATER_HYDROTHERMAL ->
              fluidState.salinity() == SalinityClass.CONCENTRATED_BRINE
                      || fluidState.salinity() == SalinityClass.HYPERSALINE
                  ? PhaseBehaviorClass.SEPARATION
                  : fluidState.integratedFluxClass() >= 3
                      ? PhaseBehaviorClass.BOILING
                      : PhaseBehaviorClass.SINGLE_PHASE;
        };
    return new FluidTransportState(
        axes.temperatureClass(),
        axes.pressureClass(),
        axes.waterRockRatioClass(),
        phaseBehavior,
        fluidState.integratedFluxClass());
  }

  private record Axes(
      TemperatureClass temperatureClass,
      PressureClass pressureClass,
      WaterRockRatioClass waterRockRatioClass) {}

  public enum TemperatureClass {
    COOL,
    WARM,
    HOT
  }

  public enum PressureClass {
    LOW,
    MODERATE,
    HIGH
  }

  public enum WaterRockRatioClass {
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH
  }

  public enum PhaseBehaviorClass {
    SINGLE_PHASE,
    BOILING,
    MIXING,
    SEPARATION
  }
}
