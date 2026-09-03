package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import java.util.List;
import java.util.Optional;

/** Compact P-T-t response attached to a resolved bulk-rock parcel. */
public record MetamorphicHistory(
    String protolithRockId,
    MetamorphicGrade grade,
    MetamorphicFacies facies,
    MetamorphicPath path,
    double minimumPeakTemperatureCelsius,
    double maximumPeakTemperatureCelsius,
    double minimumPeakPressureMpa,
    double maximumPeakPressureMpa,
    List<StableId> eventIds,
    List<AgeKey> eventAges,
    MetamorphicProcessState processState) {
  public MetamorphicHistory(
      String protolithRockId,
      MetamorphicGrade grade,
      MetamorphicFacies facies,
      MetamorphicPath path,
      double minimumPeakTemperatureCelsius,
      double maximumPeakTemperatureCelsius,
      double minimumPeakPressureMpa,
      double maximumPeakPressureMpa,
      List<StableId> eventIds) {
    this(
        protolithRockId,
        grade,
        facies,
        path,
        minimumPeakTemperatureCelsius,
        maximumPeakTemperatureCelsius,
        minimumPeakPressureMpa,
        maximumPeakPressureMpa,
        eventIds,
        List.of(),
        MetamorphicProcessState.proofFor(
            grade, facies, path, MaterialProcessClass.NONE, 0L, Optional.empty()));
  }

  public MetamorphicHistory {
    if (protolithRockId == null
        || protolithRockId.isBlank()
        || grade == null
        || facies == null
        || path == null
        || processState == null) {
      throw new IllegalArgumentException("metamorphic history identity must be complete");
    }
    boolean inactive =
        grade == MetamorphicGrade.NONE
            && facies == MetamorphicFacies.NONE
            && path == MetamorphicPath.NONE;
    boolean active =
        grade != MetamorphicGrade.NONE
            && facies != MetamorphicFacies.NONE
            && path != MetamorphicPath.NONE;
    if (!inactive && !active) {
      throw new IllegalArgumentException("metamorphic grade, facies, and path must agree");
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
    eventAges = List.copyOf(eventAges).stream().sorted().toList();
  }
}
