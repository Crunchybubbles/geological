package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.HashSet;
import java.util.List;

/**
 * Dimension-native Nether material history derived from a thermal province.
 *
 * <p>The units in this descriptor are bounded model proxies. They do not assert terrestrial
 * chemistry or an absolute Nether mass.
 */
public record NetherMaterialHistoryState(
    StableId historyId,
    StableId provinceId,
    StableId refractoryBasementId,
    StableId magmaProvinceId,
    List<StableId> sourceBodyIds,
    NetherThermalProvinceState.NetherProvinceKind provinceKind,
    MaterialFamily primaryMaterial,
    List<Event> events,
    long sourceBudgetFixedUnits,
    long retainedMaterialFixedUnits,
    long alterationLossFixedUnits) {
  public NetherMaterialHistoryState {
    if (historyId == null
        || provinceId == null
        || refractoryBasementId == null
        || magmaProvinceId == null
        || sourceBodyIds == null
        || provinceKind == null
        || primaryMaterial == null
        || events == null) {
      throw new IllegalArgumentException("Nether material history identities are required");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(historyId)
        || !sourceBodyIds.contains(provinceId)
        || !sourceBodyIds.contains(refractoryBasementId)
        || !sourceBodyIds.contains(magmaProvinceId)) {
      throw new IllegalArgumentException(
          "Nether material history must retain province, basement, magma, and history IDs");
    }
    events = List.copyOf(events);
    if (events.isEmpty() || events.stream().anyMatch(event -> event == null)) {
      throw new IllegalArgumentException("Nether material history must contain events");
    }
    HashSet<StableId> eventIds = new HashSet<>();
    int expectedSequence = 0;
    for (Event event : events) {
      if (event.sequence() != expectedSequence++ || !eventIds.add(event.eventId())) {
        throw new IllegalArgumentException("Nether material events must be ordered and unique");
      }
    }
    if (sourceBudgetFixedUnits < 0L
        || retainedMaterialFixedUnits < 0L
        || alterationLossFixedUnits < 0L
        || retainedMaterialFixedUnits + alterationLossFixedUnits != sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("Nether material history ledger is not closed");
    }
    if (primaryMaterial == MaterialFamily.NONE) {
      throw new IllegalArgumentException("formed Nether material history needs a primary material");
    }
  }

  /** Builds a deterministic event chronicle for one frozen thermal province. */
  public static NetherMaterialHistoryState from(
      NetherThermalProvinceState province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and identity are required");
    }
    requireNetherIdentity(identity);
    CellKey cell =
        new CellKey("nether:province", province.provinceCellX(), province.provinceCellZ());
    var stream = identity.stream("geological", "nether-material-history", cell, 0);
    StableId historyId = stream.stableId();
    MaterialFamily material = primaryMaterial(province.kind());
    List<StableId> sourceIds =
        List.of(
            historyId,
            province.provinceId(),
            province.refractoryBasementId(),
            province.magmaProvinceId());
    EventKind[] chronicle = chronicle(province.kind());
    List<Event> events =
        java.util.stream.IntStream.range(0, chronicle.length)
            .mapToObj(
                sequence ->
                    new Event(
                        identity.stream("geological", "nether-material-event", cell, sequence)
                            .stableId(),
                        chronicle[sequence],
                        sequence,
                        36_000L
                            + Math.round(
                                stream.unitDouble("event-" + sequence, sequence) * 92_000L)))
            .toList();
    long sourceBudget = 520_000L + Math.round(stream.unitDouble("material-budget", 0) * 380_000L);
    long alterationLoss = Math.round(sourceBudget * alterationLossFraction(material));
    long retained = sourceBudget - alterationLoss;
    return new NetherMaterialHistoryState(
        historyId,
        province.provinceId(),
        province.refractoryBasementId(),
        province.magmaProvinceId(),
        sourceIds,
        province.kind(),
        material,
        events,
        sourceBudget,
        retained,
        alterationLoss);
  }

  public boolean hasEvent(EventKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("event kind is required");
    }
    return events.stream().anyMatch(event -> event.kind() == kind);
  }

  private static MaterialFamily primaryMaterial(
      NetherThermalProvinceState.NetherProvinceKind kind) {
    return switch (kind) {
      case NETHERRACK_VOLCANIC_WASTE -> MaterialFamily.POROUS_NETHERRACK;
      case BASALT_DELTA_COMPLEX -> MaterialFamily.BASALT_LAVA;
      case SOUL_ASH_VALLEY -> MaterialFamily.SOUL_ASH_PYROCLASTIC;
      case VOLATILE_VENT_FIELD -> MaterialFamily.REFRACTORY_BRECCIA;
    };
  }

  private static EventKind[] chronicle(NetherThermalProvinceState.NetherProvinceKind kind) {
    return switch (kind) {
      case NETHERRACK_VOLCANIC_WASTE ->
          new EventKind[] {
            EventKind.THERMAL_PROVINCE_SEED,
            EventKind.REFRACTORY_BASEMENT,
            EventKind.PYROCLASTIC_PACKAGE,
            EventKind.MAFIC_LAVA_PACKAGE,
            EventKind.COLLAPSE_BRECCIA,
            EventKind.VOLATILE_ALTERATION,
            EventKind.ROOF_FISSURE_CONDENSATION
          };
      case BASALT_DELTA_COMPLEX ->
          new EventKind[] {
            EventKind.THERMAL_PROVINCE_SEED,
            EventKind.REFRACTORY_BASEMENT,
            EventKind.MAFIC_LAVA_PACKAGE,
            EventKind.LAYERED_MAFIC_INTRUSION,
            EventKind.DIKE_OR_SILL_EMPLACEMENT,
            EventKind.COOLING_CONTRACTION,
            EventKind.VOLATILE_ALTERATION
          };
      case SOUL_ASH_VALLEY ->
          new EventKind[] {
            EventKind.THERMAL_PROVINCE_SEED,
            EventKind.REFRACTORY_BASEMENT,
            EventKind.PYROCLASTIC_PACKAGE,
            EventKind.SOUL_ASH_ACCUMULATION,
            EventKind.COLLAPSE_BRECCIA,
            EventKind.COOLING_CONTRACTION,
            EventKind.ROOF_FISSURE_CONDENSATION
          };
      case VOLATILE_VENT_FIELD ->
          new EventKind[] {
            EventKind.THERMAL_PROVINCE_SEED,
            EventKind.REFRACTORY_BASEMENT,
            EventKind.LAYERED_MAFIC_INTRUSION,
            EventKind.DIKE_OR_SILL_EMPLACEMENT,
            EventKind.COLLAPSE_BRECCIA,
            EventKind.VOLATILE_ALTERATION,
            EventKind.ROOF_FISSURE_CONDENSATION
          };
    };
  }

  private static double alterationLossFraction(MaterialFamily material) {
    return switch (material) {
      case POROUS_NETHERRACK -> 0.18;
      case BASALT_LAVA -> 0.11;
      case SOUL_ASH_PYROCLASTIC -> 0.24;
      case REFRACTORY_BRECCIA -> 0.07;
      case NONE -> 0.0;
    };
  }

  private static void requireNetherIdentity(WorldIdentity identity) {
    DimensionGeologyProfile profile = DimensionGeologyProfiles.require("minecraft:the_nether");
    if (!profile.profileId().equals(identity.dimensionProfileId())
        || !profile.version().equals(identity.modelVersion())
        || !profile.scientificDigest().equals(identity.scientificDigest())) {
      throw new IllegalArgumentException("Nether material history identity does not match profile");
    }
  }

  public enum MaterialFamily {
    POROUS_NETHERRACK,
    BASALT_LAVA,
    SOUL_ASH_PYROCLASTIC,
    REFRACTORY_BRECCIA,
    NONE
  }

  public enum EventKind {
    THERMAL_PROVINCE_SEED,
    REFRACTORY_BASEMENT,
    MAFIC_LAVA_PACKAGE,
    PYROCLASTIC_PACKAGE,
    SOUL_ASH_ACCUMULATION,
    LAYERED_MAFIC_INTRUSION,
    DIKE_OR_SILL_EMPLACEMENT,
    COLLAPSE_BRECCIA,
    COOLING_CONTRACTION,
    VOLATILE_ALTERATION,
    ROOF_FISSURE_CONDENSATION
  }

  public record Event(StableId eventId, EventKind kind, int sequence, long contributionFixedUnits) {
    public Event {
      if (eventId == null || kind == null || sequence < 0 || contributionFixedUnits <= 0L) {
        throw new IllegalArgumentException("Nether material events are invalid");
      }
    }
  }
}
