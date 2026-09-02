package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Sparse deep-mantle cargo kept separate from the carrier-rock assemblage and chemistry. */
public record MantleCargoState(
    StableId carrierBodyId,
    Optional<StableId> sourceReservoirId,
    MantleCargoStatus status,
    String diamondMineralId,
    long diamondGradePpbByMass,
    List<String> candidateIndicatorMineralIds) {
  private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

  public MantleCargoState {
    if (carrierBodyId == null
        || sourceReservoirId == null
        || status == null
        || diamondMineralId == null
        || !NAMESPACED_ID.matcher(diamondMineralId).matches()
        || candidateIndicatorMineralIds == null) {
      throw new IllegalArgumentException("mantle cargo state must be complete");
    }
    if (diamondGradePpbByMass < 0 || diamondGradePpbByMass > 1_000_000L) {
      throw new IllegalArgumentException("diamond grade must lie in [0, 1000000] ppb by mass");
    }
    boolean resolved = status != MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED;
    if (resolved != sourceReservoirId.isPresent()) {
      throw new IllegalArgumentException("resolved mantle cargo requires a source reservoir");
    }
    if ((status == MantleCargoStatus.DIAMOND_BEARING) != (diamondGradePpbByMass > 0)) {
      throw new IllegalArgumentException("diamond-bearing status and positive grade must agree");
    }
    if (candidateIndicatorMineralIds.isEmpty()
        || candidateIndicatorMineralIds.contains(diamondMineralId)
        || candidateIndicatorMineralIds.stream()
            .anyMatch(id -> id == null || !NAMESPACED_ID.matcher(id).matches())
        || candidateIndicatorMineralIds.stream().distinct().count()
            != candidateIndicatorMineralIds.size()) {
      throw new IllegalArgumentException(
          "mantle cargo indicator minerals must be named, unique, and non-empty");
    }
    candidateIndicatorMineralIds = candidateIndicatorMineralIds.stream().sorted().toList();
  }
}
