package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import java.util.ArrayList;
import java.util.Comparator;
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
    MetamorphicProcessState processState,
    Optional<RegionalMetamorphicState> regionalState) {
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
            grade, facies, path, MaterialProcessClass.NONE, 0L, Optional.empty()),
        Optional.empty());
  }

  /** Compatibility constructor for callers that provide process state but no regional field. */
  public MetamorphicHistory(
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
        eventAges,
        processState,
        Optional.empty());
  }

  public MetamorphicHistory {
    if (protolithRockId == null
        || protolithRockId.isBlank()
        || grade == null
        || facies == null
        || path == null
        || processState == null
        || regionalState == null) {
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
    List<StableId> suppliedEventIds = List.copyOf(eventIds);
    List<AgeKey> suppliedEventAges = List.copyOf(eventAges);
    if (suppliedEventIds.size() == suppliedEventAges.size()) {
      List<MetamorphicEventTiming> timing = new ArrayList<>(suppliedEventIds.size());
      for (int index = 0; index < suppliedEventIds.size(); index++) {
        timing.add(
            new MetamorphicEventTiming(suppliedEventIds.get(index), suppliedEventAges.get(index)));
      }
      timing.sort(
          Comparator.comparing(MetamorphicEventTiming::age)
              .thenComparing(MetamorphicEventTiming::eventId));
      eventIds = timing.stream().map(MetamorphicEventTiming::eventId).toList();
      eventAges = timing.stream().map(MetamorphicEventTiming::age).toList();
    } else {
      // Legacy construction may provide IDs without timing; retain its canonical independent lists.
      eventIds = suppliedEventIds.stream().sorted().toList();
      eventAges = suppliedEventAges.stream().sorted().toList();
    }
  }

  /** Returns canonical event/age pairs when this history carries complete timing evidence. */
  public List<MetamorphicEventTiming> eventTimeline() {
    if (eventIds.size() != eventAges.size()) {
      return List.of();
    }
    List<MetamorphicEventTiming> result = new ArrayList<>(eventIds.size());
    for (int index = 0; index < eventIds.size(); index++) {
      result.add(new MetamorphicEventTiming(eventIds.get(index), eventAges.get(index)));
    }
    return List.copyOf(result);
  }
}
