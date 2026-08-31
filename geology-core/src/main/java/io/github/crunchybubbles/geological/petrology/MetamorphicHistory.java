package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;

/** Compact P-T-t response attached to a resolved bulk-rock parcel. */
public record MetamorphicHistory(
    String protolithRockId,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double minimumPeakTemperatureCelsius,
    double maximumPeakTemperatureCelsius,
    double minimumPeakPressureMpa,
    double maximumPeakPressureMpa,
    List<StableId> eventIds) {
  public MetamorphicHistory {
    if (protolithRockId == null || protolithRockId.isBlank() || facies == null || path == null) {
      throw new IllegalArgumentException("metamorphic history identity must be complete");
    }
    if (!Double.isFinite(minimumPeakTemperatureCelsius)
        || !Double.isFinite(maximumPeakTemperatureCelsius)
        || !Double.isFinite(minimumPeakPressureMpa)
        || !Double.isFinite(maximumPeakPressureMpa)
        || minimumPeakTemperatureCelsius > maximumPeakTemperatureCelsius
        || minimumPeakPressureMpa > maximumPeakPressureMpa) {
      throw new IllegalArgumentException("metamorphic P-T interval must be finite and ordered");
    }
    eventIds = List.copyOf(eventIds).stream().sorted().toList();
  }
}
