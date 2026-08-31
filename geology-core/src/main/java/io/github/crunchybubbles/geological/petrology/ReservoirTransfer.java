package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Optional;

/** One named debit from a coarse source reservoir to a trap, retained source, halo, or loss. */
public record ReservoirTransfer(
    String role, ReservoirSinkKind sinkKind, Optional<StableId> sinkId, long amount) {
  public ReservoirTransfer {
    if (role == null || role.isBlank() || sinkKind == null || sinkId == null || amount < 0) {
      throw new IllegalArgumentException("reservoir transfer must be complete and non-negative");
    }
    boolean requiresSink =
        sinkKind == ReservoirSinkKind.DEPOSIT || sinkKind == ReservoirSinkKind.RETAINED_SOURCE;
    if (requiresSink != sinkId.isPresent()) {
      throw new IllegalArgumentException("deposit and retained-source transfers require a sink ID");
    }
  }
}
