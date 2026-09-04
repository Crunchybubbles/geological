package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MantleCargoStatus;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.worldgen.CarbonatiteKimberliteHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated carbonatite/peralkaline REE and kimberlite/diamond proxy state. */
public record CarbonatiteKimberliteSystemState(
    StableId systemId,
    FormationStatus status,
    DepositFamily family,
    StableId intrusionId,
    StableId sourceBodyId,
    StableId structureId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    Setting setting,
    SourceClass sourceClass,
    HostClass hostClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Optional<MantleCargoState> mantleCargo,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedBudgetFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public CarbonatiteKimberliteSystemState {
    if (systemId == null
        || status == null
        || family == null
        || intrusionId == null
        || sourceBodyId == null
        || structureId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || setting == null
        || sourceClass == null
        || hostClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || mantleCargo == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("carbonatite/kimberlite state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(intrusionId)
        || !sourceBodyIds.contains(sourceBodyId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "alkaline complex sources must retain intrusion, source, structure, and host");
    }
    if (mantleCargo.isPresent() && !mantleCargo.orElseThrow().carrierBodyId().equals(hostBodyId)) {
      throw new IllegalArgumentException("mantle cargo carrier must match the host body");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedBudgetFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedBudgetFixedUnits > sourceBudgetFixedUnits
        || releasedBudgetFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("alkaline complex ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("alkaline complex horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("alkaline complex horizons must be unique");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException("alkaline complex horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == DepositFamily.NONE
          || setting == Setting.NONE
          || sourceClass == SourceClass.NO_FERTILE_SOURCE
          || hostClass == HostClass.NO_RECEPTIVE_HOST
          || pathwayClass == PathwayClass.NO_CONNECTED_PATH
          || trapClass == TrapClass.NO_REACTIVE_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedBudgetFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed alkaline complex systems require source, path, trap, and preservation proof");
      }
      if (family == DepositFamily.KIMBERLITE_DIAMOND
          && (mantleCargo.isEmpty()
              || mantleCargo.orElseThrow().status() != MantleCargoStatus.DIAMOND_BEARING)) {
        throw new IllegalArgumentException("formed kimberlite requires diamond-bearing cargo");
      }
      if (family != DepositFamily.KIMBERLITE_DIAMOND && mantleCargo.isPresent()) {
        throw new IllegalArgumentException("REE complex systems cannot carry diamond cargo");
      }
    } else if (family != DepositFamily.NONE
        || setting != Setting.NONE
        || sourceClass != SourceClass.NO_FERTILE_SOURCE
        || hostClass != HostClass.NO_RECEPTIVE_HOST
        || pathwayClass != PathwayClass.NO_CONNECTED_PATH
        || trapClass != TrapClass.NO_REACTIVE_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedBudgetFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || !formationAge.equals(new AgeKey(0.0, 0))
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "barren alkaline complex systems must retain the failed gate");
    }
  }

  /** Derives REE or diamond evidence while retaining carrier and mantle cargo identities. */
  public static CarbonatiteKimberliteSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      CarbonatiteKimberliteHostPolicy.HostEvidence host) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null
        || host == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, parent, and host are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())) {
      throw new IllegalArgumentException(
          "alkaline complex parent and surface must belong to province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("alkaline complex surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Point3 localSurface = host.localCenter();
    StableId systemId =
        identity.stream("geological", "carbonatite-kimberlite-system", province.homeCell(), 0)
            .stableId();
    DepositFamily family = host.family();
    Setting setting = setting(family);
    SourceClass sourceClass = sourceClass(family, host);
    HostClass hostClass = hostClass(family, host.hostLithology());
    PathwayClass pathwayClass = pathwayClass(family, geometry, localSurface, host);
    TrapClass trapClass = trapClass(family, host);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        family == DepositFamily.NONE
            ? "alkaline_or_carrier_host"
            : sourceClass == SourceClass.NO_FERTILE_SOURCE
                ? "enriched_mantle_source"
                : hostClass == HostClass.NO_RECEPTIVE_HOST
                    ? "complex_or_pipe_host"
                    : pathwayClass == PathwayClass.NO_CONNECTED_PATH
                        ? "deep_structure_path"
                        : trapClass == TrapClass.NO_REACTIVE_TRAP
                            ? "ree_or_diamond_trap"
                            : preservationClass == PreservationClass.ERODED_OR_COVERED
                                ? "preservation"
                                : null;
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    host.intrusionId(), host.sourceBodyId(), host.structureId(), host.hostBodyId()),
                java.util.stream.Stream.concat(
                    host.mantleCargo().flatMap(MantleCargoState::sourceReservoirId).stream(),
                    parent.geology().rockBodyId().equals(host.hostBodyId())
                        ? java.util.stream.Stream.empty()
                        : java.util.stream.Stream.of(parent.geology().rockBodyId())))
            .distinct()
            .sorted()
            .toList();
    Point3 center = new Point3(localSurface.x(), localSurface.y() - 50.0, localSurface.z());
    if (failedGate != null) {
      return barren(
          systemId,
          host.intrusionId(),
          host.sourceBodyId(),
          host.structureId(),
          host.hostBodyId(),
          sourceBodyIds,
          host.mantleCargo(),
          center,
          failedGate);
    }
    long sourceBudget = sourceBudget(family, host);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          host.intrusionId(),
          host.sourceBodyId(),
          host.structureId(),
          host.hostBodyId(),
          sourceBodyIds,
          host.mantleCargo(),
          center,
          "incompatible_or_cargo_budget");
    }
    long released =
        family == DepositFamily.KIMBERLITE_DIAMOND
            ? Math.round(sourceBudget * 0.90)
            : Math.round(sourceBudget * 0.75);
    long deposit =
        family == DepositFamily.KIMBERLITE_DIAMOND
            ? Math.round(released * 0.34)
            : Math.round(released * 0.56);
    long loss = released - deposit;
    return new CarbonatiteKimberliteSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        host.intrusionId(),
        host.sourceBodyId(),
        host.structureId(),
        host.hostBodyId(),
        sourceBodyIds,
        formationAge(family),
        setting,
        sourceClass,
        hostClass,
        pathwayClass,
        trapClass,
        preservationClass,
        host.mantleCargo(),
        center,
        family == DepositFamily.KIMBERLITE_DIAMOND ? 132.0 : 188.0,
        family == DepositFamily.KIMBERLITE_DIAMOND ? 112.0 : 84.0,
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, province.homeCell(), family, deposit),
        Optional.empty());
  }

  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > verticalExtentBlocks / 2.0) {
      return false;
    }
    double radial =
        StrictMath.hypot(localPoint.x() - localCenter.x(), localPoint.z() - localCenter.z());
    return radial <= lateralExtentBlocks;
  }

  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double radial =
        StrictMath.hypot(localPoint.x() - localCenter.x(), localPoint.z() - localCenter.z());
    double radialFraction = radial / lateralExtentBlocks;
    double top = localCenter.y() + verticalExtentBlocks / 2.0;
    double depth = (top - localPoint.y()) / verticalExtentBlocks;
    return horizons.stream()
        .filter(
            horizon ->
                horizon.containsDepth(depth) && radialFraction <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  private static Setting setting(DepositFamily family) {
    return switch (family) {
      case CARBONATITE_REE -> Setting.INTRACONTINENTAL_CARBONATITE_COMPLEX;
      case PERALKALINE_REE -> Setting.PERALKALINE_RIFT_COMPLEX;
      case KIMBERLITE_DIAMOND -> Setting.KIMBERLITE_CARRIER_PIPE;
      case NONE -> Setting.NONE;
    };
  }

  private static SourceClass sourceClass(
      DepositFamily family, CarbonatiteKimberliteHostPolicy.HostEvidence host) {
    if (family == DepositFamily.KIMBERLITE_DIAMOND) {
      return host.mantleCargo()
                  .map(MantleCargoState::status)
                  .orElse(MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED)
              == MantleCargoStatus.DIAMOND_BEARING
          ? SourceClass.DIAMOND_STABLE_MANTLE_CARGO
          : SourceClass.NO_FERTILE_SOURCE;
    }
    return family != DepositFamily.NONE && host.incompatibleInventoryFixedUnits() >= 50_000L
        ? SourceClass.ENRICHED_ALKALINE_MANTLE_PROXY
        : SourceClass.NO_FERTILE_SOURCE;
  }

  private static HostClass hostClass(DepositFamily family, Lithology hostLithology) {
    return switch (family) {
      case CARBONATITE_REE ->
          hostLithology == Lithology.CARBONATITIC
              ? HostClass.CARBONATITE_PLUG_OR_BRECCIA
              : HostClass.NO_RECEPTIVE_HOST;
      case PERALKALINE_REE ->
          hostLithology == Lithology.ALKALINE
              ? HostClass.PERALKALINE_INTRUSION
              : HostClass.NO_RECEPTIVE_HOST;
      case KIMBERLITE_DIAMOND ->
          hostLithology == Lithology.KIMBERLITIC
              ? HostClass.KIMBERLITE_CARRIER
              : HostClass.NO_RECEPTIVE_HOST;
      case NONE -> HostClass.NO_RECEPTIVE_HOST;
    };
  }

  private static PathwayClass pathwayClass(
      DepositFamily family,
      RiftArcGeometry geometry,
      Point3 localSurface,
      CarbonatiteKimberliteHostPolicy.HostEvidence host) {
    boolean connected =
        host.complexOrPipe()
            && (host.fixture()
                || host.connectivityIndex() >= 0.15
                || geometry.fault().intersectsDamageZone(localSurface));
    if (!connected || family == DepositFamily.NONE) {
      return PathwayClass.NO_CONNECTED_PATH;
    }
    return switch (family) {
      case CARBONATITE_REE -> PathwayClass.RIFT_FAULT_COMPLEX;
      case PERALKALINE_REE -> PathwayClass.LATE_ALKALINE_RESIDUAL_FLOW;
      case KIMBERLITE_DIAMOND -> PathwayClass.DEEP_STRUCTURE_RAPID_ASCENT;
      case NONE -> PathwayClass.NO_CONNECTED_PATH;
    };
  }

  private static TrapClass trapClass(
      DepositFamily family, CarbonatiteKimberliteHostPolicy.HostEvidence host) {
    return switch (family) {
      case CARBONATITE_REE ->
          host.incompatibleInventoryFixedUnits() >= 50_000L
              ? TrapClass.MAGMATIC_REE_NB_APATITE
              : TrapClass.NO_REACTIVE_TRAP;
      case PERALKALINE_REE ->
          host.incompatibleInventoryFixedUnits() >= 50_000L
              ? TrapClass.INCOMPATIBLE_RESIDUAL_ZONE
              : TrapClass.NO_REACTIVE_TRAP;
      case KIMBERLITE_DIAMOND ->
          host.mantleCargo()
                      .map(MantleCargoState::status)
                      .orElse(MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED)
                  == MantleCargoStatus.DIAMOND_BEARING
              ? TrapClass.DIAMONDIFEROUS_MANTLE_CARGO
              : TrapClass.NO_REACTIVE_TRAP;
      case NONE -> TrapClass.NO_REACTIVE_TRAP;
    };
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_COMPLEX_OR_PIPE
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long sourceBudget(
      DepositFamily family, CarbonatiteKimberliteHostPolicy.HostEvidence host) {
    long familyBudget =
        family == DepositFamily.KIMBERLITE_DIAMOND
            ? host.mantleCargo().map(MantleCargoState::diamondGradePpbByMass).orElse(0L) * 2L
            : host.incompatibleInventoryFixedUnits();
    return Math.min(300_000L, Math.min(host.sourceBudgetFixedUnits(), familyBudget));
  }

  private static AgeKey formationAge(DepositFamily family) {
    return switch (family) {
      case CARBONATITE_REE -> new AgeKey(175.0, 0);
      case PERALKALINE_REE -> new AgeKey(168.0, 0);
      case KIMBERLITE_DIAMOND -> new AgeKey(92.0, 0);
      case NONE -> new AgeKey(0.0, 0);
    };
  }

  private static CarbonatiteKimberliteSystemState barren(
      StableId systemId,
      StableId intrusionId,
      StableId sourceBodyId,
      StableId structureId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Optional<MantleCargoState> mantleCargo,
      Point3 center,
      String failedGate) {
    return new CarbonatiteKimberliteSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        DepositFamily.NONE,
        intrusionId,
        sourceBodyId,
        structureId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        Setting.NONE,
        SourceClass.NO_FERTILE_SOURCE,
        HostClass.NO_RECEPTIVE_HOST,
        PathwayClass.NO_CONNECTED_PATH,
        TrapClass.NO_REACTIVE_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        mantleCargo,
        center,
        188.0,
        84.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity,
      io.github.crunchybubbles.geological.model.CellKey cell,
      DepositFamily family,
      long deposit) {
    long inner = Math.round(deposit * 0.48);
    long middle = Math.round(deposit * 0.31);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case CARBONATITE_REE ->
              new HorizonKind[] {
                HorizonKind.PRIMARY_REE_CARBONATE,
                HorizonKind.APATITE_PYROCHLORE_CORE,
                HorizonKind.FENITE_MARGIN
              };
          case PERALKALINE_REE ->
              new HorizonKind[] {
                HorizonKind.PERALKALINE_REE_LAYER,
                HorizonKind.ZIRCON_NB_RESIDUAL_ZONE,
                HorizonKind.FENITE_ALKALI_MARGIN
              };
          case KIMBERLITE_DIAMOND ->
              new HorizonKind[] {
                HorizonKind.KIMBERLITE_PIPE,
                HorizonKind.DIAMONDIFEROUS_CARGO_ZONE,
                HorizonKind.INDICATOR_MINERAL_BRECCIA
              };
          case NONE ->
              throw new IllegalArgumentException("barren alkaline systems have no horizons");
        };
    return List.of(
        horizon(kinds[0], Overprint.NONE, 0.0, 0.36, 0.96, inner, identity, cell, 0),
        horizon(kinds[1], Overprint.CONTACT_HORNFELS, 0.36, 0.70, 0.82, middle, identity, cell, 1),
        horizon(
            kinds[2], Overprint.PROPYLITIC_ALTERATION, 0.70, 1.0, 0.68, outer, identity, cell, 2));
  }

  private static Horizon horizon(
      HorizonKind kind,
      Overprint overprint,
      double top,
      double bottom,
      double radius,
      long allocation,
      WorldIdentity identity,
      io.github.crunchybubbles.geological.model.CellKey cell,
      long index) {
    return new Horizon(
        kind,
        overprint,
        top,
        bottom,
        radius,
        allocation,
        identity.stream("geological", "carbonatite-kimberlite-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("alkaline complex horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("alkaline complex horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum DepositFamily {
    CARBONATITE_REE,
    PERALKALINE_REE,
    KIMBERLITE_DIAMOND,
    NONE
  }

  public enum Setting {
    INTRACONTINENTAL_CARBONATITE_COMPLEX,
    PERALKALINE_RIFT_COMPLEX,
    KIMBERLITE_CARRIER_PIPE,
    NONE
  }

  public enum SourceClass {
    ENRICHED_ALKALINE_MANTLE_PROXY,
    DIAMOND_STABLE_MANTLE_CARGO,
    NO_FERTILE_SOURCE
  }

  public enum HostClass {
    CARBONATITE_PLUG_OR_BRECCIA,
    PERALKALINE_INTRUSION,
    KIMBERLITE_CARRIER,
    NO_RECEPTIVE_HOST
  }

  public enum PathwayClass {
    RIFT_FAULT_COMPLEX,
    LATE_ALKALINE_RESIDUAL_FLOW,
    DEEP_STRUCTURE_RAPID_ASCENT,
    NO_CONNECTED_PATH
  }

  public enum TrapClass {
    MAGMATIC_REE_NB_APATITE,
    INCOMPATIBLE_RESIDUAL_ZONE,
    DIAMONDIFEROUS_MANTLE_CARGO,
    NO_REACTIVE_TRAP
  }

  public enum PreservationClass {
    PRESERVED_COMPLEX_OR_PIPE,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    PRIMARY_REE_CARBONATE,
    APATITE_PYROCHLORE_CORE,
    FENITE_MARGIN,
    PERALKALINE_REE_LAYER,
    ZIRCON_NB_RESIDUAL_ZONE,
    FENITE_ALKALI_MARGIN,
    KIMBERLITE_PIPE,
    DIAMONDIFEROUS_CARGO_ZONE,
    INDICATOR_MINERAL_BRECCIA
  }

  public record Horizon(
      HorizonKind kind,
      Overprint overprint,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      long allocationFixedUnits,
      StableId bodyId) {
    public Horizon {
      if (kind == null || overprint == null || bodyId == null) {
        throw new IllegalArgumentException("alkaline complex horizon identity is required");
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
        throw new IllegalArgumentException("alkaline complex horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || (bottomDepthFraction == 1.0 && depthFraction <= bottomDepthFraction));
    }
  }
}
