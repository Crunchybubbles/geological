package io.github.crunchybubbles.geological.mineral;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Exact integer allocation across named sinks; units are model-specific fixed mass proxies. */
public final class FixedPointLedger {
  private final String element;
  private final String unit;
  private final long sourceAmount;
  private final Map<String, Long> allocations;

  public FixedPointLedger(
      String element, String unit, long sourceAmount, Map<String, Long> allocations) {
    if (element == null || element.isBlank() || unit == null || unit.isBlank()) {
      throw new IllegalArgumentException("ledger element and unit are required");
    }
    if (sourceAmount < 0) {
      throw new IllegalArgumentException("source amount must be non-negative");
    }
    TreeMap<String, Long> sorted = new TreeMap<>();
    allocations.forEach(
        (name, amount) -> {
          if (name == null || name.isBlank() || amount == null || amount < 0) {
            throw new IllegalArgumentException("ledger allocations must be named and non-negative");
          }
          sorted.put(name, amount);
        });
    long sum = 0;
    for (long amount : sorted.values()) {
      sum = Math.addExact(sum, amount);
    }
    if (sum != sourceAmount) {
      throw new IllegalArgumentException(
          "ledger does not close: source=" + sourceAmount + ", allocated=" + sum);
    }
    this.element = element;
    this.unit = unit;
    this.sourceAmount = sourceAmount;
    this.allocations = Collections.unmodifiableMap(sorted);
  }

  public String element() {
    return element;
  }

  public String unit() {
    return unit;
  }

  public long sourceAmount() {
    return sourceAmount;
  }

  public Map<String, Long> allocations() {
    return allocations;
  }
}
