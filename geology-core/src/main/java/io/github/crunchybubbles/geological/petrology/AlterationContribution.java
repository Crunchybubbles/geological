package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.AgeKey;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Joined, reviewable contribution made by one authored alteration or metamorphic response. */
public record AlterationContribution(
    Optional<StableId> processId,
    MaterialProcessClass processClass,
    List<StableId> eventIds,
    List<AgeKey> eventAges,
    long reactionProgressPpm,
    long replacementPpm,
    Map<String, Long> mineralModeDeltaPpm,
    Map<ChemicalElement, Long> additionsPpm,
    Map<ChemicalElement, Long> removalsPpm,
    Optional<RockTexture> responseTexture,
    Optional<ProcessFluidState> fluidState,
    double porosityMultiplier,
    double erodibilityDelta) {
  public AlterationContribution {
    if (processId == null
        || processClass == null
        || eventIds == null
        || eventAges == null
        || mineralModeDeltaPpm == null
        || additionsPpm == null
        || removalsPpm == null
        || responseTexture == null
        || fluidState == null
        || reactionProgressPpm < 0
        || reactionProgressPpm > MaterialAssemblage.SCALE
        || replacementPpm < 0
        || replacementPpm > MaterialAssemblage.SCALE
        || !Double.isFinite(porosityMultiplier)
        || porosityMultiplier < 0.0
        || !Double.isFinite(erodibilityDelta)
        || erodibilityDelta < -1.0
        || erodibilityDelta > 1.0) {
      throw new IllegalArgumentException("alteration contribution is incomplete or out of bounds");
    }
    eventIds = List.copyOf(eventIds).stream().sorted().toList();
    eventAges = List.copyOf(eventAges).stream().sorted().toList();
    if (!eventAges.isEmpty() && eventAges.size() != eventIds.size()) {
      throw new IllegalArgumentException("alteration event IDs and ages must have matching counts");
    }
    mineralModeDeltaPpm = immutableModeDelta(mineralModeDeltaPpm);
    additionsPpm = immutablePositive(additionsPpm, "addition");
    removalsPpm = immutablePositive(removalsPpm, "removal");
    if (additionsPpm.keySet().stream().anyMatch(removalsPpm::containsKey)) {
      throw new IllegalArgumentException("an element cannot be both added and removed");
    }
    if (sum(additionsPpm) != sum(removalsPpm)) {
      throw new IllegalArgumentException("alteration element additions and removals must balance");
    }
    boolean requiresProcess = processClass != MaterialProcessClass.NONE;
    if (requiresProcess != processId.isPresent()) {
      throw new IllegalArgumentException("active alteration contributions require a process ID");
    }
    boolean requiresResponseTexture = processClass == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM;
    if (requiresResponseTexture != responseTexture.isPresent()) {
      throw new IllegalArgumentException(
          "isochemical alteration contributions require a response texture");
    }
    boolean requiresFluid =
        processClass == MaterialProcessClass.HYDROTHERMAL_METASOMATISM
            || processClass == MaterialProcessClass.WEATHERING;
    if (requiresFluid != fluidState.isPresent()) {
      throw new IllegalArgumentException(
          "mass-transfer alteration contributions require fluid evidence");
    }
  }

  public static AlterationContribution from(
      MaterialProcessLedger processLedger,
      MetamorphicProcessState processState,
      AlterationDefinition alteration,
      List<AgeKey> eventAges,
      MaterialAssemblage primary,
      MaterialAssemblage resolved) {
    if (processLedger == null
        || processState == null
        || alteration == null
        || eventAges == null
        || primary == null
        || resolved == null) {
      throw new IllegalArgumentException("alteration contribution inputs are required");
    }
    return new AlterationContribution(
        processLedger.processId(),
        processLedger.processClass(),
        processLedger.eventIds(),
        eventAges,
        processState.reactionProgressPpm(),
        alteration.replacementPpm(),
        modeDelta(primary, resolved),
        processLedger.additionsPpm(),
        processLedger.removalsPpm(),
        alteration.responseTexture(),
        alteration.fluidState(),
        alteration.porosityMultiplier(),
        alteration.erodibilityDelta());
  }

  private static Map<String, Long> modeDelta(
      MaterialAssemblage primary, MaterialAssemblage resolved) {
    TreeMap<String, Long> delta = new TreeMap<>();
    java.util.Set<String> ids = new java.util.TreeSet<>();
    ids.addAll(primary.modesPpm().keySet());
    ids.addAll(resolved.modesPpm().keySet());
    for (String id : ids) {
      long change =
          resolved.modesPpm().getOrDefault(id, 0L) - primary.modesPpm().getOrDefault(id, 0L);
      if (change != 0) {
        delta.put(id, change);
      }
    }
    if (sum(delta) != 0) {
      throw new IllegalArgumentException("alteration mineral mode deltas must close");
    }
    return delta;
  }

  private static Map<String, Long> immutableModeDelta(Map<String, Long> source) {
    TreeMap<String, Long> copied = new TreeMap<>();
    source.forEach(
        (id, amount) -> {
          if (id == null || id.isBlank() || amount == null || amount == 0) {
            throw new IllegalArgumentException(
                "alteration mineral mode deltas must be named and non-zero");
          }
          copied.put(id, amount);
        });
    if (sum(copied) != 0) {
      throw new IllegalArgumentException("alteration mineral mode deltas must close");
    }
    return Collections.unmodifiableMap(copied);
  }

  private static Map<ChemicalElement, Long> immutablePositive(
      Map<ChemicalElement, Long> source, String side) {
    EnumMap<ChemicalElement, Long> copied = new EnumMap<>(ChemicalElement.class);
    source.forEach(
        (element, amount) -> {
          if (element == null || amount == null || amount <= 0) {
            throw new IllegalArgumentException("alteration " + side + " entries must be positive");
          }
          copied.put(element, amount);
        });
    return Collections.unmodifiableMap(copied);
  }

  private static long sum(Map<?, Long> amounts) {
    long total = 0;
    for (long amount : amounts.values()) {
      total = Math.addExact(total, amount);
    }
    return total;
  }
}
