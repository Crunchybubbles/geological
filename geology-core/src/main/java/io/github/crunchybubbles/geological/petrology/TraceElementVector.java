package io.github.crunchybubbles.geological.petrology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sparse trace-element abundance and log-concentration evidence for a resolved material parcel.
 *
 * <p>Abundances use the catalog's one-million-part normalized mass basis. The log values are
 * deterministic micro-log10 proxies for comparison across orders of magnitude; neither field is a
 * measured assay or a substitute for a reviewed distribution.
 */
public record TraceElementVector(
    Map<ChemicalElement, Long> concentrationPpm, Map<ChemicalElement, Long> log10PpmMicros) {
  private static final long LOG_SCALE = MaterialAssemblage.SCALE;
  private static final Set<ChemicalElement> TRACE_ELEMENTS =
      Collections.unmodifiableSet(
          EnumSet.of(
              ChemicalElement.P,
              ChemicalElement.S,
              ChemicalElement.F,
              ChemicalElement.CL,
              ChemicalElement.TI,
              ChemicalElement.CR,
              ChemicalElement.FE,
              ChemicalElement.CU,
              ChemicalElement.ZN,
              ChemicalElement.AU));

  public TraceElementVector {
    concentrationPpm = immutable(concentrationPpm, "trace concentrations", false);
    log10PpmMicros = immutable(log10PpmMicros, "trace log concentrations", true);
    if (!concentrationPpm.keySet().equals(log10PpmMicros.keySet())) {
      throw new IllegalArgumentException("trace concentration maps must have matching elements");
    }
    for (ChemicalElement element : concentrationPpm.keySet()) {
      long expected = log10Micros(concentrationPpm.get(element));
      if (log10PpmMicros.get(element) != expected) {
        throw new IllegalArgumentException("trace log concentration disagrees for " + element);
      }
    }
  }

  /** Derives the canonical sparse trace vector from a normalized bulk composition. */
  public static TraceElementVector from(BulkComposition composition) {
    if (composition == null) {
      throw new IllegalArgumentException("bulk composition is required for trace evidence");
    }
    EnumMap<ChemicalElement, Long> concentrations = new EnumMap<>(ChemicalElement.class);
    for (ChemicalElement element : TRACE_ELEMENTS) {
      long amount = composition.elementMassPpm().getOrDefault(element, 0L);
      if (amount > 0L) {
        concentrations.put(element, amount);
      }
    }
    EnumMap<ChemicalElement, Long> logs = new EnumMap<>(ChemicalElement.class);
    concentrations.forEach((element, amount) -> logs.put(element, log10Micros(amount)));
    return new TraceElementVector(concentrations, logs);
  }

  public long concentrationPpm(ChemicalElement element) {
    if (element == null) {
      throw new IllegalArgumentException("trace element is required");
    }
    return concentrationPpm.getOrDefault(element, 0L);
  }

  public long log10PpmMicros(ChemicalElement element) {
    if (element == null) {
      throw new IllegalArgumentException("trace element is required");
    }
    return log10PpmMicros.getOrDefault(element, 0L);
  }

  private static Map<ChemicalElement, Long> immutable(
      Map<ChemicalElement, Long> source, String name, boolean logValues) {
    if (source == null) {
      throw new IllegalArgumentException(name + " are required");
    }
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null
              || amount == null
              || !TRACE_ELEMENTS.contains(element)
              || amount < (logValues ? 0L : 1L)
              || amount > (logValues ? 6L * MaterialAssemblage.SCALE : MaterialAssemblage.SCALE)) {
            throw new IllegalArgumentException(name + " must contain positive supported values");
          }
          copied.put(element, amount);
        });
    return Collections.unmodifiableMap(copied);
  }

  private static long log10Micros(long concentrationPpm) {
    if (concentrationPpm <= 0L || concentrationPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("trace concentration must lie in [1, 1000000]");
    }
    return Math.round(StrictMath.log10(concentrationPpm) * LOG_SCALE);
  }
}
