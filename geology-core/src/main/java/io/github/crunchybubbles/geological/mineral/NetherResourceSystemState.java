package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.NetherMaterialHistoryState;
import io.github.crunchybubbles.geological.worldgen.NetherThermalProvinceState;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source/path/trap-gated fictional Nether quartz, gold, glowstone, and ancient-debris state. */
public record NetherResourceSystemState(
    StableId systemId,
    FormationStatus status,
    ResourceFamily family,
    StableId provinceId,
    StableId historyId,
    StableId sourceBodyId,
    StableId pathwayId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    NetherMaterialHistoryState.MaterialFamily hostMaterial,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedResourceFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public NetherResourceSystemState {
    if (systemId == null
        || status == null
        || family == null
        || provinceId == null
        || historyId == null
        || sourceBodyId == null
        || pathwayId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || hostMaterial == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("Nether resource state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(provinceId)
        || !sourceBodyIds.contains(historyId)
        || !sourceBodyIds.contains(sourceBodyId)
        || !sourceBodyIds.contains(pathwayId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "Nether resource sources must retain province, history, source, path, and host IDs");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedResourceFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedResourceFixedUnits > sourceBudgetFixedUnits
        || releasedResourceFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("Nether resource ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("Nether resource horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("Nether resource horizons must be unique");
    }
    validateHorizonSequence(horizons);
    if (horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum()
        != depositAllocationFixedUnits) {
      throw new IllegalArgumentException("Nether resource horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == ResourceFamily.NONE
          || hostMaterial == NetherMaterialHistoryState.MaterialFamily.NONE
          || pathwayClass == PathwayClass.NO_CONNECTED_PATH
          || trapClass == TrapClass.NO_RESOURCE_TRAP
          || preservationClass == PreservationClass.NO_PRESERVATION
          || sourceBudgetFixedUnits == 0L
          || releasedResourceFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed Nether resources require material, path, trap, and preservation proof");
      }
    } else if (family != ResourceFamily.NONE
        || hostMaterial != NetherMaterialHistoryState.MaterialFamily.NONE
        || pathwayClass != PathwayClass.NO_CONNECTED_PATH
        || trapClass != TrapClass.NO_RESOURCE_TRAP
        || preservationClass != PreservationClass.NO_PRESERVATION
        || sourceBudgetFixedUnits != 0L
        || releasedResourceFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren Nether resources must retain the failed gate");
    }
  }

  /** Derives one source-linked resource body from the coherent Nether material history. */
  public static NetherResourceSystemState proofFor(
      NetherThermalProvinceState province,
      NetherMaterialHistoryState history,
      WorldIdentity identity) {
    if (province == null || history == null || identity == null) {
      throw new IllegalArgumentException("province, history, and identity are required");
    }
    requireNetherIdentity(identity);
    if (!province.provinceId().equals(history.provinceId())
        || !province
            .provinceId()
            .equals(
                history.sourceBodyIds().stream()
                    .filter(province.provinceId()::equals)
                    .findFirst()
                    .orElse(null))
        || province.kind() != history.provinceKind()) {
      throw new IllegalArgumentException("Nether resource province and material history diverged");
    }
    CellKey cell =
        new CellKey("nether:province", province.provinceCellX(), province.provinceCellZ());
    var stream = identity.stream("geological", "nether-resource-system", cell, 0);
    ResourceFamily family = familyFor(province.kind());
    StableId systemId = stream.stableId();
    StableId sourceBodyId =
        identity.stream("geological", "nether-resource-source:" + family.name(), cell, 0)
            .stableId();
    StableId pathwayId =
        identity.stream("geological", "nether-resource-pathway:" + family.name(), cell, 0)
            .stableId();
    StableId hostBodyId =
        identity.stream("geological", "nether-resource-host:" + family.name(), cell, 0).stableId();
    List<StableId> sourceIds =
        java.util.stream.Stream.concat(
                history.sourceBodyIds().stream(),
                java.util.stream.Stream.of(sourceBodyId, pathwayId, hostBodyId))
            .distinct()
            .sorted()
            .toList();
    NetherMaterialHistoryState.MaterialFamily hostMaterial = history.primaryMaterial();
    PathwayClass pathwayClass = pathwayClass(family, history);
    TrapClass trapClass = trapClass(family, history);
    PreservationClass preservationClass = PreservationClass.BOUNDED_CAVERN_HOST;
    String failedGate =
        !compatible(family, hostMaterial)
            ? "dimension_native_host"
            : pathwayClass == PathwayClass.NO_CONNECTED_PATH
                ? "resource_pathway"
                : trapClass == TrapClass.NO_RESOURCE_TRAP ? "resource_trap" : null;
    Point3 center = center(family, province, identity, cell);
    if (failedGate != null) {
      return barren(
          systemId,
          province.provinceId(),
          history.historyId(),
          sourceBodyId,
          pathwayId,
          hostBodyId,
          sourceIds,
          center,
          failedGate);
    }
    long sourceBudget = sourceBudget(family, province, stream);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          province.provinceId(),
          history.historyId(),
          sourceBodyId,
          pathwayId,
          hostBodyId,
          sourceIds,
          center,
          "resource_source_budget");
    }
    long released = Math.round(sourceBudget * releaseFraction(family));
    long deposit = Math.round(released * depositFraction(family));
    long loss = released - deposit;
    return new NetherResourceSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        province.provinceId(),
        history.historyId(),
        sourceBodyId,
        pathwayId,
        hostBodyId,
        sourceIds,
        hostMaterial,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        lateralExtent(family),
        verticalExtent(family),
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, cell, family, deposit),
        Optional.empty());
  }

  public boolean contains(Point3 point) {
    if (point == null) {
      throw new IllegalArgumentException("resource point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(point.y() - localCenter.y()) > verticalExtentBlocks / 2.0) {
      return false;
    }
    double radial = StrictMath.hypot(point.x() - localCenter.x(), point.z() - localCenter.z());
    return radial <= lateralExtentBlocks;
  }

  public Optional<Horizon> zoneAt(Point3 point) {
    if (!contains(point)) {
      return Optional.empty();
    }
    double radial = StrictMath.hypot(point.x() - localCenter.x(), point.z() - localCenter.z());
    double radialFraction = radial / lateralExtentBlocks;
    double top = localCenter.y() + verticalExtentBlocks / 2.0;
    double depth = (top - point.y()) / verticalExtentBlocks;
    return horizons.stream()
        .filter(
            horizon ->
                horizon.containsDepth(depth) && radialFraction <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  private static ResourceFamily familyFor(NetherThermalProvinceState.NetherProvinceKind kind) {
    return switch (kind) {
      case NETHERRACK_VOLCANIC_WASTE -> ResourceFamily.NETHER_QUARTZ;
      case BASALT_DELTA_COMPLEX -> ResourceFamily.NETHER_GOLD;
      case SOUL_ASH_VALLEY -> ResourceFamily.GLOWSTONE;
      case VOLATILE_VENT_FIELD -> ResourceFamily.ANCIENT_DEBRIS;
    };
  }

  private static boolean compatible(
      ResourceFamily family, NetherMaterialHistoryState.MaterialFamily material) {
    return switch (family) {
      case NETHER_QUARTZ -> material == NetherMaterialHistoryState.MaterialFamily.POROUS_NETHERRACK;
      case NETHER_GOLD -> material == NetherMaterialHistoryState.MaterialFamily.BASALT_LAVA;
      case GLOWSTONE -> material == NetherMaterialHistoryState.MaterialFamily.SOUL_ASH_PYROCLASTIC;
      case ANCIENT_DEBRIS ->
          material == NetherMaterialHistoryState.MaterialFamily.REFRACTORY_BRECCIA;
      case NONE -> false;
    };
  }

  private static PathwayClass pathwayClass(
      ResourceFamily family, NetherMaterialHistoryState history) {
    boolean connected =
        switch (family) {
          case NETHER_QUARTZ ->
              history.hasEvent(NetherMaterialHistoryState.EventKind.ROOF_FISSURE_CONDENSATION)
                  || history.hasEvent(NetherMaterialHistoryState.EventKind.PYROCLASTIC_PACKAGE);
          case NETHER_GOLD ->
              history.hasEvent(NetherMaterialHistoryState.EventKind.MAFIC_LAVA_PACKAGE);
          case GLOWSTONE ->
              history.hasEvent(NetherMaterialHistoryState.EventKind.ROOF_FISSURE_CONDENSATION)
                  && history.hasEvent(NetherMaterialHistoryState.EventKind.SOUL_ASH_ACCUMULATION);
          case ANCIENT_DEBRIS ->
              history.hasEvent(NetherMaterialHistoryState.EventKind.LAYERED_MAFIC_INTRUSION)
                  && history.hasEvent(NetherMaterialHistoryState.EventKind.COLLAPSE_BRECCIA);
          case NONE -> false;
        };
    if (!connected) {
      return PathwayClass.NO_CONNECTED_PATH;
    }
    return switch (family) {
      case NETHER_QUARTZ -> PathwayClass.LATE_MAGMATIC_SILICA_VEIN;
      case NETHER_GOLD -> PathwayClass.MAGMATIC_SEGREGATION;
      case GLOWSTONE -> PathwayClass.ROOF_FISSURE_VOLATILE_CONDENSATE;
      case ANCIENT_DEBRIS -> PathwayClass.DEEP_CUMULATE_CONDUIT;
      case NONE -> PathwayClass.NO_CONNECTED_PATH;
    };
  }

  private static TrapClass trapClass(ResourceFamily family, NetherMaterialHistoryState history) {
    if (!compatible(family, history.primaryMaterial())) {
      return TrapClass.NO_RESOURCE_TRAP;
    }
    return switch (family) {
      case NETHER_QUARTZ -> TrapClass.SILICA_VEIN_SEGREGATION;
      case NETHER_GOLD -> TrapClass.VOLATILE_STRUCTURAL_TRAP;
      case GLOWSTONE -> TrapClass.ROOF_CONDUIT_PRECIPITATE;
      case ANCIENT_DEBRIS -> TrapClass.REFRACTORY_CUMULATE_BRECCIA;
      case NONE -> TrapClass.NO_RESOURCE_TRAP;
    };
  }

  private static long sourceBudget(
      ResourceFamily family,
      NetherThermalProvinceState province,
      io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    long potential =
        family == ResourceFamily.GLOWSTONE || family == ResourceFamily.NETHER_GOLD
            ? province.volatilePotentialFixedUnits()
            : province.heatPotentialFixedUnits();
    long variation = 90_000L + Math.round(stream.unitDouble("resource-budget", 0) * 110_000L);
    return Math.min(360_000L, Math.min(potential, variation));
  }

  private static double releaseFraction(ResourceFamily family) {
    return switch (family) {
      case NETHER_QUARTZ -> 0.72;
      case NETHER_GOLD -> 0.68;
      case GLOWSTONE -> 0.80;
      case ANCIENT_DEBRIS -> 0.42;
      case NONE -> 0.0;
    };
  }

  private static double depositFraction(ResourceFamily family) {
    return switch (family) {
      case NETHER_QUARTZ -> 0.60;
      case NETHER_GOLD -> 0.48;
      case GLOWSTONE -> 0.54;
      case ANCIENT_DEBRIS -> 0.30;
      case NONE -> 0.0;
    };
  }

  private static Point3 center(
      ResourceFamily family,
      NetherThermalProvinceState province,
      WorldIdentity identity,
      CellKey cell) {
    var stream = identity.stream("geological", "nether-resource-center:" + family.name(), cell, 0);
    double x = province.provinceCellX() * 512.0 + 48.0 + stream.unitDouble("center-x", 0) * 416.0;
    double z = province.provinceCellZ() * 512.0 + 48.0 + stream.unitDouble("center-z", 0) * 416.0;
    double y =
        switch (family) {
          case NETHER_QUARTZ -> 4.0;
          case NETHER_GOLD -> -14.0;
          case GLOWSTONE -> 37.0;
          case ANCIENT_DEBRIS -> -43.0;
          case NONE -> -16.0;
        };
    return new Point3(x, y, z);
  }

  private static double lateralExtent(ResourceFamily family) {
    return switch (family) {
      case NETHER_QUARTZ -> 178.0;
      case NETHER_GOLD -> 166.0;
      case GLOWSTONE -> 154.0;
      case ANCIENT_DEBRIS -> 128.0;
      case NONE -> 128.0;
    };
  }

  private static double verticalExtent(ResourceFamily family) {
    return switch (family) {
      case NETHER_QUARTZ -> 132.0;
      case NETHER_GOLD -> 116.0;
      case GLOWSTONE -> 108.0;
      case ANCIENT_DEBRIS -> 76.0;
      case NONE -> 76.0;
    };
  }

  private static NetherResourceSystemState barren(
      StableId systemId,
      StableId provinceId,
      StableId historyId,
      StableId sourceBodyId,
      StableId pathwayId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new NetherResourceSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        ResourceFamily.NONE,
        provinceId,
        historyId,
        sourceBodyId,
        pathwayId,
        hostBodyId,
        sourceBodyIds,
        NetherMaterialHistoryState.MaterialFamily.NONE,
        PathwayClass.NO_CONNECTED_PATH,
        TrapClass.NO_RESOURCE_TRAP,
        PreservationClass.NO_PRESERVATION,
        center,
        128.0,
        76.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, CellKey cell, ResourceFamily family, long allocation) {
    long inner = Math.round(allocation * 0.48);
    long middle = Math.round(allocation * 0.31);
    long outer = allocation - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case NETHER_QUARTZ ->
              new HorizonKind[] {
                HorizonKind.QUARTZ_SILICA_SEGREGATION,
                HorizonKind.QUARTZ_ROOF_FISSURE_VEIN,
                HorizonKind.QUARTZ_ALTERED_NETHERRACK_MARGIN
              };
          case NETHER_GOLD ->
              new HorizonKind[] {
                HorizonKind.GOLD_MAGMATIC_SEGREGATION,
                HorizonKind.GOLD_VOLATILE_STRUCTURAL_VEIN,
                HorizonKind.GOLD_BASALT_ALTERATION_MARGIN
              };
          case GLOWSTONE ->
              new HorizonKind[] {
                HorizonKind.GLOWSTONE_ROOF_CONDENSATE,
                HorizonKind.GLOWSTONE_VOLATILE_CONDUIT,
                HorizonKind.GLOWSTONE_ASH_MARGIN
              };
          case ANCIENT_DEBRIS ->
              new HorizonKind[] {
                HorizonKind.ANCIENT_DEBRIS_REFRACTORY_RELIC,
                HorizonKind.ANCIENT_DEBRIS_CUMULATE_MARGIN,
                HorizonKind.ANCIENT_DEBRIS_BRECCIA_CAP
              };
          case NONE ->
              throw new IllegalArgumentException("barren Nether resources have no horizons");
        };
    return List.of(
        horizon(kinds[0], 0.0, 0.34, 0.96, inner, identity, cell, 0),
        horizon(kinds[1], 0.34, 0.70, 0.82, middle, identity, cell, 1),
        horizon(kinds[2], 0.70, 1.0, 0.68, outer, identity, cell, 2));
  }

  private static Horizon horizon(
      HorizonKind kind,
      double top,
      double bottom,
      double radius,
      long allocation,
      WorldIdentity identity,
      CellKey cell,
      long index) {
    return new Horizon(
        kind,
        top,
        bottom,
        radius,
        allocation,
        identity.stream("geological", "nether-resource-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("Nether resource horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("Nether resource horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  private static void requireNetherIdentity(WorldIdentity identity) {
    DimensionGeologyProfile profile = DimensionGeologyProfiles.require("minecraft:the_nether");
    if (!profile.profileId().equals(identity.dimensionProfileId())
        || !profile.version().equals(identity.modelVersion())
        || !profile.scientificDigest().equals(identity.scientificDigest())) {
      throw new IllegalArgumentException("Nether resource identity does not match profile");
    }
  }

  public enum ResourceFamily {
    NETHER_QUARTZ,
    NETHER_GOLD,
    GLOWSTONE,
    ANCIENT_DEBRIS,
    NONE
  }

  public enum PathwayClass {
    LATE_MAGMATIC_SILICA_VEIN,
    MAGMATIC_SEGREGATION,
    ROOF_FISSURE_VOLATILE_CONDENSATE,
    DEEP_CUMULATE_CONDUIT,
    NO_CONNECTED_PATH
  }

  public enum TrapClass {
    SILICA_VEIN_SEGREGATION,
    VOLATILE_STRUCTURAL_TRAP,
    ROOF_CONDUIT_PRECIPITATE,
    REFRACTORY_CUMULATE_BRECCIA,
    NO_RESOURCE_TRAP
  }

  public enum PreservationClass {
    BOUNDED_CAVERN_HOST,
    NO_PRESERVATION
  }

  public enum HorizonKind {
    QUARTZ_SILICA_SEGREGATION,
    QUARTZ_ROOF_FISSURE_VEIN,
    QUARTZ_ALTERED_NETHERRACK_MARGIN,
    GOLD_MAGMATIC_SEGREGATION,
    GOLD_VOLATILE_STRUCTURAL_VEIN,
    GOLD_BASALT_ALTERATION_MARGIN,
    GLOWSTONE_ROOF_CONDENSATE,
    GLOWSTONE_VOLATILE_CONDUIT,
    GLOWSTONE_ASH_MARGIN,
    ANCIENT_DEBRIS_REFRACTORY_RELIC,
    ANCIENT_DEBRIS_CUMULATE_MARGIN,
    ANCIENT_DEBRIS_BRECCIA_CAP
  }

  public record Horizon(
      HorizonKind kind,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      long allocationFixedUnits,
      StableId bodyId) {
    public Horizon {
      if (kind == null || bodyId == null) {
        throw new IllegalArgumentException("Nether resource horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0
          || allocationFixedUnits < 0L) {
        throw new IllegalArgumentException("Nether resource horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
