package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Causal process identity and the positive/negative sides of a normalized element delta. */
public record MaterialProcessLedger(
    Optional<StableId> processId,
    MaterialProcessClass processClass,
    List<StableId> eventIds,
    Map<ChemicalElement, Long> additionsPpm,
    Map<ChemicalElement, Long> removalsPpm) {
  public MaterialProcessLedger {
    if (processId == null
        || processClass == null
        || eventIds == null
        || additionsPpm == null
        || removalsPpm == null) {
      throw new IllegalArgumentException("material process ledger must be complete");
    }
    eventIds = List.copyOf(eventIds).stream().sorted().toList();
    additionsPpm = immutablePositive(additionsPpm, "addition");
    removalsPpm = immutablePositive(removalsPpm, "removal");
    boolean inactive = processClass == MaterialProcessClass.NONE;
    if (inactive != processId.isEmpty()) {
      throw new IllegalArgumentException("only the NONE process class may omit a process ID");
    }
    if (inactive && !eventIds.isEmpty()) {
      throw new IllegalArgumentException("an inactive material process cannot name events");
    }
    if (additionsPpm.keySet().stream().anyMatch(removalsPpm::containsKey)) {
      throw new IllegalArgumentException("an element cannot be both added and removed");
    }
    long additions = sumExact(additionsPpm);
    long removals = sumExact(removalsPpm);
    if (additions != removals) {
      throw new IllegalArgumentException(
          "normalized process contributions must balance: additions="
              + additions
              + ", removals="
              + removals);
    }
    boolean compositionChanged = additions > 0;
    if (processClass == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM && compositionChanged) {
      throw new IllegalArgumentException("isochemical metamorphism cannot transfer elements");
    }
    if (inactive && compositionChanged) {
      throw new IllegalArgumentException("an inactive material process cannot transfer elements");
    }
  }

  public static MaterialProcessLedger from(
      Optional<StableId> processId,
      MaterialProcessClass processClass,
      List<StableId> eventIds,
      ElementTransferLedger elementLedger) {
    if (elementLedger == null) {
      throw new IllegalArgumentException("element transfer ledger is required");
    }
    EnumMap<ChemicalElement, Long> additions = new EnumMap<>(ChemicalElement.class);
    EnumMap<ChemicalElement, Long> removals = new EnumMap<>(ChemicalElement.class);
    elementLedger
        .transferPpm()
        .forEach(
            (element, delta) -> {
              if (delta > 0) {
                additions.put(element, delta);
              } else if (delta < 0) {
                removals.put(element, Math.negateExact(delta));
              }
            });
    return new MaterialProcessLedger(processId, processClass, eventIds, additions, removals);
  }

  public long exchangeMagnitudePpm() {
    return sumExact(additionsPpm);
  }

  public long netTransferPpm(ChemicalElement element) {
    if (element == null) {
      throw new IllegalArgumentException("element is required");
    }
    return additionsPpm.getOrDefault(element, 0L) - removalsPpm.getOrDefault(element, 0L);
  }

  private static Map<ChemicalElement, Long> immutablePositive(
      Map<ChemicalElement, Long> source, String side) {
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount <= 0) {
            throw new IllegalArgumentException(
                "material process " + side + " entries must be positive");
          }
          copied.put(element, amount);
        });
    return Collections.unmodifiableMap(copied);
  }

  private static long sumExact(Map<ChemicalElement, Long> amounts) {
    long total = 0;
    for (long amount : amounts.values()) {
      total = Math.addExact(total, amount);
    }
    return total;
  }
}
