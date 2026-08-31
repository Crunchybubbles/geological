package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Exact local composition identity: initial inventory plus transfers equals resolved inventory. */
public record ElementTransferLedger(
    Map<ChemicalElement, Long> initialPpm,
    Map<ChemicalElement, Long> transferPpm,
    Map<ChemicalElement, Long> resolvedPpm) {
  public ElementTransferLedger {
    initialPpm = immutable(initialPpm);
    transferPpm = immutable(transferPpm);
    resolvedPpm = immutable(resolvedPpm);
    for (ChemicalElement element : ChemicalElement.values()) {
      long initial = initialPpm.getOrDefault(element, 0L);
      long transfer = transferPpm.getOrDefault(element, 0L);
      long resolved = resolvedPpm.getOrDefault(element, 0L);
      if (Math.addExact(initial, transfer) != resolved) {
        throw new IllegalArgumentException("element transfer ledger does not close for " + element);
      }
      if (initial < 0 || resolved < 0) {
        throw new IllegalArgumentException("element inventories must be non-negative");
      }
    }
  }

  public static ElementTransferLedger between(BulkComposition initial, BulkComposition resolved) {
    EnumMap<ChemicalElement, Long> transfers = new EnumMap<>(ChemicalElement.class);
    for (ChemicalElement element : ChemicalElement.values()) {
      long delta =
          resolved.elementMassPpm().getOrDefault(element, 0L)
              - initial.elementMassPpm().getOrDefault(element, 0L);
      if (delta != 0) {
        transfers.put(element, delta);
      }
    }
    return new ElementTransferLedger(
        initial.elementMassPpm(), transfers, resolved.elementMassPpm());
  }

  public boolean isIsochemical() {
    return transferPpm.isEmpty();
  }

  private static Map<ChemicalElement, Long> immutable(Map<ChemicalElement, Long> source) {
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null) {
            throw new IllegalArgumentException("element ledger entries must be complete");
          }
          if (amount != 0) {
            copied.put(element, amount);
          }
        });
    return Collections.unmodifiableMap(copied);
  }
}
