package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import java.util.Optional;

/** One named debit from a coarse source reservoir to a trap, retained source, halo, or loss. */
public record ReservoirTransfer(
    String role,
    ReservoirSinkKind sinkKind,
    Optional<StableId> sinkId,
    long amount,
    Optional<StableId> processId,
    Optional<AgeKey> age,
    long confidencePpm) {
  public ReservoirTransfer(
      String role, ReservoirSinkKind sinkKind, Optional<StableId> sinkId, long amount) {
    this(role, sinkKind, sinkId, amount, Optional.empty(), Optional.empty(), 0L);
  }

  public ReservoirTransfer {
    if (role == null
        || role.isBlank()
        || sinkKind == null
        || sinkId == null
        || amount < 0
        || processId == null
        || age == null
        || confidencePpm < 0
        || confidencePpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("reservoir transfer must be complete and non-negative");
    }
    boolean requiresSink =
        sinkKind == ReservoirSinkKind.DEPOSIT || sinkKind == ReservoirSinkKind.RETAINED_SOURCE;
    if (requiresSink != sinkId.isPresent()) {
      throw new IllegalArgumentException("deposit and retained-source transfers require a sink ID");
    }
    if (processId.isPresent() != age.isPresent()) {
      throw new IllegalArgumentException("reservoir provenance requires both process and age");
    }
    if (confidencePpm > 0 && processId.isEmpty()) {
      throw new IllegalArgumentException(
          "reservoir confidence requires process and age provenance");
    }
  }
}
