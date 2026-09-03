package io.github.crunchybubbles.geological.petrology;

/** Dimensionless hydraulic-response proxies derived from the resolved colluvial fabric. */
public record ColluvialHydraulicState(
    HydraulicClass hydraulicClass,
    double waterStorageIndex,
    double infiltrationIndex,
    double drainageIndex,
    double runoffPartitionIndex) {
  private static final double STORAGE_POROSITY_WEIGHT = 0.65;
  private static final double STORAGE_LOW_PERMEABILITY_WEIGHT = 0.35;
  private static final double INFILTRATION_POROSITY_WEIGHT = 0.45;
  private static final double DRAINAGE_STORAGE_WEIGHT = 0.35;
  private static final double RUNOFF_STORAGE_WEIGHT = 0.35;

  public ColluvialHydraulicState {
    if (hydraulicClass == null) {
      throw new IllegalArgumentException("colluvial hydraulic class is required");
    }
    requireUnit(waterStorageIndex, "water-storage index");
    requireUnit(infiltrationIndex, "infiltration index");
    requireUnit(drainageIndex, "drainage index");
    requireUnit(runoffPartitionIndex, "runoff-partition index");
  }

  /** Derives bounded hydraulic behavior without assigning physical conductivity units. */
  public static ColluvialHydraulicState from(ColluvialPhysicalState physicalState) {
    if (physicalState == null) {
      throw new IllegalArgumentException("colluvial physical state is required");
    }
    double porosity = physicalState.porosityFraction();
    double permeability = physicalState.permeabilityIndex();
    double waterStorageIndex =
        clamp(
            porosity
                * (STORAGE_POROSITY_WEIGHT
                    + STORAGE_LOW_PERMEABILITY_WEIGHT * (1.0 - permeability)));
    double infiltrationIndex =
        clamp(permeability * (1.0 - INFILTRATION_POROSITY_WEIGHT * (1.0 - porosity)));
    double drainageIndex =
        clamp(permeability * (1.0 - DRAINAGE_STORAGE_WEIGHT * waterStorageIndex));
    double runoffPartitionIndex =
        clamp(1.0 - infiltrationIndex * (1.0 - RUNOFF_STORAGE_WEIGHT * waterStorageIndex));
    HydraulicClass hydraulicClass =
        infiltrationIndex < 0.35
            ? HydraulicClass.LOW_INFILTRATION
            : drainageIndex >= 0.65
                ? HydraulicClass.RAPID_DRAINAGE
                : HydraulicClass.BALANCED_DRAINAGE;
    return new ColluvialHydraulicState(
        hydraulicClass, waterStorageIndex, infiltrationIndex, drainageIndex, runoffPartitionIndex);
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static void requireUnit(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must lie in [0, 1]");
    }
  }

  /** Coarse hydraulic response class, not a physical soil-hydrology classification. */
  public enum HydraulicClass {
    LOW_INFILTRATION,
    BALANCED_DRAINAGE,
    RAPID_DRAINAGE
  }
}
