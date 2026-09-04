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
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentaryBasinState;
import io.github.crunchybubbles.geological.petrology.SedimentaryState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.worldgen.BasinHydrothermalHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated MVT, SEDEX, and sediment-hosted copper basin-brine proxy state. */
public record BasinHydrothermalSystemState(
    StableId systemId,
    FormationStatus status,
    DepositFamily family,
    StableId basinId,
    StableId fluidSourceId,
    StableId structureId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    BasinSetting basinSetting,
    SalinityClass salinityClass,
    RedoxClass redoxClass,
    FluidSourceClass fluidSourceClass,
    HostClass hostClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedFluidFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public BasinHydrothermalSystemState {
    if (systemId == null
        || status == null
        || family == null
        || basinId == null
        || fluidSourceId == null
        || structureId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || basinSetting == null
        || salinityClass == null
        || redoxClass == null
        || fluidSourceClass == null
        || hostClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("basin hydrothermal state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(basinId)
        || !sourceBodyIds.contains(fluidSourceId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "basin hydrothermal sources must retain basin, fluid, structure, and host");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("basin hydrothermal fluid ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("basin hydrothermal horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("basin hydrothermal horizons must be unique");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "basin hydrothermal horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == DepositFamily.NONE
          || basinSetting == BasinSetting.NONE
          || salinityClass == SalinityClass.FRESH
          || fluidSourceClass == FluidSourceClass.NO_BASINAL_BRINE
          || hostClass == HostClass.NO_RECEPTIVE_HOST
          || pathwayClass == PathwayClass.NO_CONNECTED_AQUIFER
          || trapClass == TrapClass.NO_REACTIVE_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed basin hydrothermal systems require brine, path, and trap proof");
      }
    } else if (family != DepositFamily.NONE
        || basinSetting != BasinSetting.NONE
        || salinityClass != SalinityClass.FRESH
        || redoxClass != RedoxClass.BUFFERED
        || fluidSourceClass != FluidSourceClass.NO_BASINAL_BRINE
        || hostClass != HostClass.NO_RECEPTIVE_HOST
        || pathwayClass != PathwayClass.NO_CONNECTED_AQUIFER
        || trapClass != TrapClass.NO_REACTIVE_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "barren basin hydrothermal systems must retain the failed gate");
    }
  }

  /** Derives a basin-family proof from sedimentary context and an explicit host policy. */
  public static BasinHydrothermalSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      BasinHydrothermalHostPolicy.HostEvidence host) {
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
          "basin hydrothermal parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("basin hydrothermal surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Point3 localSurface = host.localCenter();
    Optional<SedimentaryState> sedimentary = parent.sedimentaryState();
    SedimentaryBasinState basinState =
        sedimentary
            .map(SedimentaryState::basinState)
            .orElseGet(
                () ->
                    host.fixture()
                        ? SedimentaryBasinState.proofFor(
                            "dolomitized_carbonate_platform", List.of(geometry.basementId()))
                        : null);
    DepositFamily family = family(host.hostLithology(), sedimentary, host.fixture());
    CellKeyKey ids = ids(identity, province);
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                    java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(geometry.basin().id(), ids.fluidSourceId()),
                        java.util.stream.Stream.of(geometry.fault().id(), host.hostBodyId())),
                    parent.geology().depositIds().stream()),
                parent.geology().rockBodyId().equals(host.hostBodyId())
                    ? java.util.stream.Stream.empty()
                    : java.util.stream.Stream.of(parent.geology().rockBodyId()))
            .distinct()
            .sorted()
            .toList();
    BasinSetting basinSetting = basinSetting(family);
    SalinityClass salinity = basinState == null ? SalinityClass.FRESH : basinState.salinityClass();
    RedoxClass redox = basinState == null ? RedoxClass.BUFFERED : basinState.redoxClass();
    FluidSourceClass fluidSourceClass = fluidSourceClass(family, basinState);
    HostClass hostClass = hostClass(host.hostLithology(), family);
    PathwayClass pathwayClass = pathwayClass(geometry, localSurface, parent);
    TrapClass trapClass = trapClass(family, host, basinState, parent);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        family == DepositFamily.NONE
            ? "basin_family_host"
            : fluidSourceClass == FluidSourceClass.NO_BASINAL_BRINE
                ? "basinal_brine"
                : hostClass == HostClass.NO_RECEPTIVE_HOST
                    ? "receptive_host"
                    : pathwayClass == PathwayClass.NO_CONNECTED_AQUIFER
                        ? "aquifer_or_fault_path"
                        : trapClass == TrapClass.NO_REACTIVE_TRAP
                            ? "redox_or_reactive_trap"
                            : preservationClass == PreservationClass.ERODED_OR_COVERED
                                ? "preservation"
                                : null;
    Point3 center = new Point3(localSurface.x(), localSurface.y() - 36.0, localSurface.z());
    if (failedGate != null) {
      return barren(
          ids.systemId(),
          geometry.basin().id(),
          ids.fluidSourceId(),
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          failedGate);
    }
    long sourceBudget = basinBrineProxy(family, basinState, parent, host.fixture());
    if (sourceBudget <= 0L) {
      return barren(
          ids.systemId(),
          geometry.basin().id(),
          ids.fluidSourceId(),
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          "basinal_brine_inventory");
    }
    long released = Math.round(sourceBudget * 0.72);
    long deposit = Math.round(released * 0.56);
    long loss = released - deposit;
    return new BasinHydrothermalSystemState(
        ids.systemId(),
        FormationStatus.FORMED,
        family,
        geometry.basin().id(),
        ids.fluidSourceId(),
        geometry.fault().id(),
        host.hostBodyId(),
        sourceBodyIds,
        formationAge(family, basinState),
        basinSetting,
        salinity,
        redox,
        fluidSourceClass,
        hostClass,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        180.0,
        72.0,
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

  private static CellKeyKey ids(WorldIdentity identity, Province province) {
    return new CellKeyKey(
        identity.stream("geological", "basin-hydrothermal-system", province.homeCell(), 0)
            .stableId(),
        identity.stream("geological", "basin-hydrothermal-fluid", province.homeCell(), 0)
            .stableId());
  }

  private static DepositFamily family(
      Lithology hostLithology, Optional<SedimentaryState> sedimentary, boolean fixture) {
    if (fixture && isCarbonate(hostLithology)) {
      return DepositFamily.MVT_PB_ZN;
    }
    if (isCarbonate(hostLithology) && sedimentary.isPresent()) {
      return DepositFamily.MVT_PB_ZN;
    }
    if (hostLithology == Lithology.BASIN_SHALE
        && sedimentary
            .map(state -> state.basinState().redoxClass() == RedoxClass.REDUCING)
            .orElse(false)) {
      return DepositFamily.SEDEX_ZN_PB_AG;
    }
    if ((hostLithology == Lithology.BASIN_SANDSTONE || hostLithology == Lithology.SILTSTONE)
        && sedimentary.isPresent()) {
      return DepositFamily.SEDIMENT_HOSTED_CU;
    }
    return DepositFamily.NONE;
  }

  private static BasinSetting basinSetting(DepositFamily family) {
    return switch (family) {
      case MVT_PB_ZN -> BasinSetting.CARBONATE_PLATFORM;
      case SEDEX_ZN_PB_AG -> BasinSetting.RIFT_REDUCED_BASIN;
      case SEDIMENT_HOSTED_CU -> BasinSetting.RED_BED_OR_REDUCED_BASIN;
      case NONE -> BasinSetting.NONE;
    };
  }

  private static FluidSourceClass fluidSourceClass(
      DepositFamily family, SedimentaryBasinState basinState) {
    return family != DepositFamily.NONE
            && basinState != null
            && basinState.salinityClass() != SalinityClass.FRESH
        ? FluidSourceClass.BASINAL_BRINE_METAL_LEACH_PROXY
        : FluidSourceClass.NO_BASINAL_BRINE;
  }

  private static HostClass hostClass(Lithology lithology, DepositFamily family) {
    return switch (family) {
      case MVT_PB_ZN -> HostClass.REACTIVE_CARBONATE;
      case SEDEX_ZN_PB_AG -> HostClass.REDUCED_BASINAL_SHALE;
      case SEDIMENT_HOSTED_CU -> HostClass.PERMEABLE_SANDSTONE_OR_SILTSTONE;
      case NONE -> HostClass.NO_RECEPTIVE_HOST;
    };
  }

  private static PathwayClass pathwayClass(
      RiftArcGeometry geometry, Point3 localPoint, PetrologicSample parent) {
    boolean insideBasin =
        geometry.basin().footprintValue(new Point2(localPoint.x(), localPoint.z())) < 1.0;
    return insideBasin
            && (geometry.fault().intersectsDamageZone(localPoint)
                || parent.permeabilityIndex() >= 0.35)
        ? PathwayClass.CONNECTED_AQUIFER_FAULT
        : PathwayClass.NO_CONNECTED_AQUIFER;
  }

  private static TrapClass trapClass(
      DepositFamily family,
      BasinHydrothermalHostPolicy.HostEvidence host,
      SedimentaryBasinState basinState,
      PetrologicSample parent) {
    if (family == DepositFamily.MVT_PB_ZN) {
      return host.fixture()
              || (basinState != null && basinState.redoxClass() != RedoxClass.STRONGLY_OXIDIZING)
          ? TrapClass.CARBONATE_REACTION_OR_BRECCIA
          : TrapClass.NO_REACTIVE_TRAP;
    }
    if (family == DepositFamily.SEDEX_ZN_PB_AG) {
      return basinState != null
              && (basinState.redoxClass() == RedoxClass.REDUCING
                  || basinState.redoxClass() == RedoxClass.STRONGLY_REDUCING)
          ? TrapClass.REDUCTION_SULFIDE_REACTION
          : TrapClass.NO_REACTIVE_TRAP;
    }
    return family == DepositFamily.SEDIMENT_HOSTED_CU
            && basinState != null
            && (basinState.redoxClass() == RedoxClass.BUFFERED
                || basinState.redoxClass() == RedoxClass.REDUCING)
            && parent.permeabilityIndex() >= 0.20
        ? TrapClass.REDOX_BOUNDARY_REACTION
        : TrapClass.NO_REACTIVE_TRAP;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_BASIN
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long basinBrineProxy(
      DepositFamily family,
      SedimentaryBasinState basinState,
      PetrologicSample parent,
      boolean fixture) {
    long water =
        parent
            .sedimentaryState()
            .map(state -> state.reservoirState().waterInventoryPpm())
            .orElse(fixture ? 650_000L : 0L);
    long volatileInventory =
        parent
            .sedimentaryState()
            .map(state -> state.reservoirState().volatileInventoryPpm())
            .orElse(fixture ? 800_000L : 0L);
    long redoxBonus =
        basinState == null
            ? 0L
            : switch (basinState.redoxClass()) {
              case STRONGLY_REDUCING -> 90_000L;
              case REDUCING -> 60_000L;
              case BUFFERED -> 30_000L;
              case OXIDIZING, STRONGLY_OXIDIZING -> 0L;
            };
    long familyBonus = family == DepositFamily.MVT_PB_ZN ? 30_000L : 0L;
    return Math.min(300_000L, water / 5L + volatileInventory / 12L + redoxBonus + familyBonus);
  }

  private static AgeKey formationAge(DepositFamily family, SedimentaryBasinState basinState) {
    return switch (family) {
      case MVT_PB_ZN -> new AgeKey(245.0, 0);
      case SEDEX_ZN_PB_AG -> new AgeKey(242.0, 0);
      case SEDIMENT_HOSTED_CU -> new AgeKey(238.0, 0);
      case NONE -> new AgeKey(0.0, 0);
    };
  }

  private static boolean isCarbonate(Lithology lithology) {
    return lithology == Lithology.LIMESTONE
        || lithology == Lithology.DOLOSTONE
        || lithology == Lithology.MARBLE
        || lithology == Lithology.CARBONATITIC;
  }

  private static BasinHydrothermalSystemState barren(
      StableId systemId,
      StableId basinId,
      StableId fluidSourceId,
      StableId structureId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new BasinHydrothermalSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        DepositFamily.NONE,
        basinId,
        fluidSourceId,
        structureId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        BasinSetting.NONE,
        SalinityClass.FRESH,
        RedoxClass.BUFFERED,
        FluidSourceClass.NO_BASINAL_BRINE,
        HostClass.NO_RECEPTIVE_HOST,
        PathwayClass.NO_CONNECTED_AQUIFER,
        TrapClass.NO_REACTIVE_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        center,
        180.0,
        72.0,
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
    long inner = Math.round(deposit * 0.45);
    long middle = Math.round(deposit * 0.33);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case MVT_PB_ZN ->
              new HorizonKind[] {
                HorizonKind.DOLOMITE_REPLACEMENT,
                HorizonKind.BRECCIA_OPEN_SPACE_FILL,
                HorizonKind.SULFIDE_GOSSAN_HALO
              };
          case SEDEX_ZN_PB_AG ->
              new HorizonKind[] {
                HorizonKind.LAMINATED_SULFIDE,
                HorizonKind.BARITE_SILICA_EXHALITE,
                HorizonKind.FEEDER_ALTERATION
              };
          case SEDIMENT_HOSTED_CU ->
              new HorizonKind[] {
                HorizonKind.RED_BED_DISSEMINATION,
                HorizonKind.REDOX_REPLACEMENT,
                HorizonKind.OXIDIZED_CU_HALO
              };
          case NONE -> throw new IllegalArgumentException("barren basin systems have no horizons");
        };
    return List.of(
        horizon(kinds[0], Overprint.PHYLLIC_ALTERATION, 0.0, 0.36, 0.96, inner, identity, cell, 0),
        horizon(
            kinds[1], Overprint.PHYLLIC_ALTERATION, 0.36, 0.70, 0.82, middle, identity, cell, 1),
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
        identity.stream("geological", "basin-hydrothermal-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("basin hydrothermal horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("basin hydrothermal horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  private record CellKeyKey(StableId systemId, StableId fluidSourceId) {}

  public enum DepositFamily {
    MVT_PB_ZN,
    SEDEX_ZN_PB_AG,
    SEDIMENT_HOSTED_CU,
    NONE
  }

  public enum BasinSetting {
    CARBONATE_PLATFORM,
    RIFT_REDUCED_BASIN,
    RED_BED_OR_REDUCED_BASIN,
    NONE
  }

  public enum FluidSourceClass {
    BASINAL_BRINE_METAL_LEACH_PROXY,
    NO_BASINAL_BRINE
  }

  public enum HostClass {
    REACTIVE_CARBONATE,
    REDUCED_BASINAL_SHALE,
    PERMEABLE_SANDSTONE_OR_SILTSTONE,
    NO_RECEPTIVE_HOST
  }

  public enum PathwayClass {
    CONNECTED_AQUIFER_FAULT,
    NO_CONNECTED_AQUIFER
  }

  public enum TrapClass {
    CARBONATE_REACTION_OR_BRECCIA,
    REDUCTION_SULFIDE_REACTION,
    REDOX_BOUNDARY_REACTION,
    NO_REACTIVE_TRAP
  }

  public enum PreservationClass {
    PRESERVED_BASIN,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    DOLOMITE_REPLACEMENT,
    BRECCIA_OPEN_SPACE_FILL,
    SULFIDE_GOSSAN_HALO,
    LAMINATED_SULFIDE,
    BARITE_SILICA_EXHALITE,
    FEEDER_ALTERATION,
    RED_BED_DISSEMINATION,
    REDOX_REPLACEMENT,
    OXIDIZED_CU_HALO
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
        throw new IllegalArgumentException("basin hydrothermal horizon identity is required");
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
        throw new IllegalArgumentException("basin hydrothermal horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
