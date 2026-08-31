package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Typed, exact system-scale element inventory compiled from a formed mineral-system proof. */
public record ElementReservoirLedger(
    StableId systemId,
    StableId sourceReservoirId,
    Optional<StableId> depositId,
    String element,
    String unit,
    long initialInventory,
    List<ReservoirTransfer> transfers) {
  public ElementReservoirLedger {
    if (systemId == null
        || sourceReservoirId == null
        || depositId == null
        || element == null
        || element.isBlank()
        || unit == null
        || unit.isBlank()
        || initialInventory < 0) {
      throw new IllegalArgumentException("element reservoir ledger identity must be complete");
    }
    transfers =
        List.copyOf(transfers).stream()
            .sorted(java.util.Comparator.comparing(ReservoirTransfer::role))
            .toList();
    if (transfers.isEmpty()) {
      throw new IllegalArgumentException("element reservoir ledger requires transfers");
    }
    Set<String> roles = new HashSet<>();
    long allocated = 0;
    for (ReservoirTransfer transfer : transfers) {
      if (!roles.add(transfer.role())) {
        throw new IllegalArgumentException("element reservoir transfer roles must be unique");
      }
      allocated = Math.addExact(allocated, transfer.amount());
    }
    if (allocated != initialInventory) {
      throw new IllegalArgumentException(
          "element reservoir does not close: source="
              + initialInventory
              + ", allocated="
              + allocated);
    }
    if (depositId.isPresent()
        && transfers.stream()
            .filter(transfer -> transfer.sinkKind() == ReservoirSinkKind.DEPOSIT)
            .noneMatch(transfer -> transfer.sinkId().equals(depositId))) {
      throw new IllegalArgumentException("formed deposit is not credited by its reservoir ledger");
    }
  }

  public long allocation(String role) {
    return transfers.stream()
        .filter(transfer -> transfer.role().equals(role))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown reservoir transfer " + role))
        .amount();
  }
}
