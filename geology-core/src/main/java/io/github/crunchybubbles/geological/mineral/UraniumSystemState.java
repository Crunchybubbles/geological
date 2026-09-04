package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentaryState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated unconformity and sandstone roll-front uranium proxy state. */
public record UraniumSystemState(
    StableId systemId,
    FormationStatus status,
    DepositFamily family,
    StableId basementSourceId,
    StableId fluidSourceId,
    StableId structureId,
    StableId hostBodyId,
    StableId unconformityId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    SourceClass sourceClass,
    HostClass hostClass,
    FluidSourceClass fluidSourceClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    SalinityClass fluidSalinity,
    RedoxClass hostRedox,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedFluidFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public UraniumSystemState {
    if (systemId == null
        || status == null
        || family == null
        || basementSourceId == null
        || fluidSourceId == null
        || structureId == null
        || hostBodyId == null
        || unconformityId == null
        || sourceBodyIds == null
        || formationAge == null
        || sourceClass == null
        || hostClass == null
        || fluidSourceClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || fluidSalinity == null
        || hostRedox == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("uranium system state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(basementSourceId)
        || !sourceBodyIds.contains(fluidSourceId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)
        || !sourceBodyIds.contains(unconformityId)) {
      throw new IllegalArgumentException(
          "uranium sources must retain basement, fluid, structure, host, and unconformity");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("uranium groundwater ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("uranium horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("uranium horizons must be unique");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException("uranium horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == DepositFamily.NONE
          || sourceClass == SourceClass.NO_URANIUM_SOURCE
          || hostClass == HostClass.NO_PERMEABLE_AQUIFER
          || fluidSourceClass == FluidSourceClass.NO_OXIDIZED_URANIUM_FLUID
          || pathwayClass == PathwayClass.NO_CONNECTED_PATH
          || trapClass == TrapClass.NO_REDUCING_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || fluidSalinity == SalinityClass.FRESH
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed uranium requires source, oxidized fluid, path, and reducing trap proof");
      }
    } else if (family != DepositFamily.NONE
        || sourceClass != SourceClass.NO_URANIUM_SOURCE
        || hostClass != HostClass.NO_PERMEABLE_AQUIFER
        || fluidSourceClass != FluidSourceClass.NO_OXIDIZED_URANIUM_FLUID
        || pathwayClass != PathwayClass.NO_CONNECTED_PATH
        || trapClass != TrapClass.NO_REDUCING_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || fluidSalinity != SalinityClass.FRESH
        || hostRedox != RedoxClass.BUFFERED
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren uranium systems must retain a failed hard gate");
    }
  }

  /** Derives a uranium proof from the authored unconformity, basin, and groundwater proxies. */
  public static UraniumSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample host,
      PetrologicSample basement) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || host == null
        || basement == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, host, and basement are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(host.geology().provinceId())
        || !province.id().equals(basement.geology().provinceId())) {
      throw new IllegalArgumentException("uranium parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("uranium surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    Point2 horizontal = new Point2(localSurface.x(), localSurface.z());
    double unconformityElevation = geometry.unconformity().elevation(horizontal);
    Point3 interfacePoint = new Point3(localSurface.x(), unconformityElevation, localSurface.z());
    boolean insideBasin = geometry.basin().footprintValue(horizontal) < 1.0;
    boolean insideUnconformity = geometry.unconformity().insideFootprint(horizontal);
    boolean faultFocused =
        geometry.fault().intersectsDamageZone(interfacePoint) || host.geology().faultDamageZone();
    boolean oldFertileBasement = oldFertileBasement(geometry, basement);
    boolean sandstoneHost = host.geology().lithology() == Lithology.BASIN_SANDSTONE;
    Optional<SedimentaryState> sedimentary = host.sedimentaryState();
    StableId systemId =
        identity.stream(
                "geological",
                "uranium-system",
                new CellKey("province", province.homeCell().x(), province.homeCell().z()),
                0)
            .stableId();
    StableId fluidSourceId =
        identity.stream(
                "geological",
                "uranium-groundwater-fluid",
                new CellKey("province", province.homeCell().x(), province.homeCell().z()),
                0)
            .stableId();
    StableId basementSourceId = geometry.basementId();
    StableId structureId = geometry.fault().id();
    StableId unconformityId = geometry.unconformity().id();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                    java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(
                            basementSourceId, fluidSourceId, structureId, unconformityId),
                        java.util.stream.Stream.of(
                            geometry.basin().id(), host.geology().rockBodyId())),
                    surface.context().sourceBodyIds().stream()),
                basement.geology().rockBodyId().equals(basementSourceId)
                    ? java.util.stream.Stream.empty()
                    : java.util.stream.Stream.of(basement.geology().rockBodyId()))
            .distinct()
            .sorted()
            .toList();
    DepositFamily family =
        oldFertileBasement && sandstoneHost && insideUnconformity && faultFocused
            ? DepositFamily.UNCONFORMITY_RELATED
            : sandstoneHost && insideBasin
                ? DepositFamily.SANDSTONE_ROLL_FRONT
                : DepositFamily.NONE;
    SourceClass sourceClass = sourceClass(family, oldFertileBasement, sedimentary);
    HostClass hostClass =
        sandstoneHost && sedimentary.isPresent() && host.permeabilityIndex() >= 0.20
            ? HostClass.PERMEABLE_BASIN_SANDSTONE
            : HostClass.NO_PERMEABLE_AQUIFER;
    SalinityClass salinity =
        sedimentary.map(state -> state.basinState().salinityClass()).orElse(SalinityClass.FRESH);
    RedoxClass redox =
        sedimentary.map(state -> state.basinState().redoxClass()).orElse(RedoxClass.BUFFERED);
    FluidSourceClass fluidSourceClass =
        hostClass != HostClass.NO_PERMEABLE_AQUIFER
                && salinity != SalinityClass.FRESH
                && sedimentary.isPresent()
            ? FluidSourceClass.OXIDIZED_URANIUM_GROUNDWATER_PROXY
            : FluidSourceClass.NO_OXIDIZED_URANIUM_FLUID;
    PathwayClass pathwayClass = pathwayClass(family, geometry, interfacePoint, host);
    TrapClass trapClass = trapClass(family, oldFertileBasement, basement, redox, host);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        sourceClass == SourceClass.NO_URANIUM_SOURCE
            ? "uranium_source"
            : hostClass == HostClass.NO_PERMEABLE_AQUIFER
                ? "permeable_sandstone_aquifer"
                : fluidSourceClass == FluidSourceClass.NO_OXIDIZED_URANIUM_FLUID
                    ? "oxidized_groundwater"
                    : pathwayClass == PathwayClass.NO_CONNECTED_PATH
                        ? "unconformity_or_aquifer_path"
                        : trapClass == TrapClass.NO_REDUCING_TRAP
                            ? "reducing_trap"
                            : preservationClass == PreservationClass.ERODED_OR_COVERED
                                ? "preservation"
                                : null;
    Point3 center =
        family == DepositFamily.UNCONFORMITY_RELATED
            ? new Point3(localSurface.x(), unconformityElevation - 4.0, localSurface.z())
            : new Point3(localSurface.x(), localSurface.y() - 52.0, localSurface.z());
    if (failedGate != null || family == DepositFamily.NONE) {
      return barren(
          systemId,
          basementSourceId,
          fluidSourceId,
          structureId,
          host.geology().rockBodyId(),
          unconformityId,
          sourceBodyIds,
          center,
          failedGate == null ? "uranium_source" : failedGate);
    }
    long sourceBudget = groundwaterProxy(family, host, basement, sedimentary);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          basementSourceId,
          fluidSourceId,
          structureId,
          host.geology().rockBodyId(),
          unconformityId,
          sourceBodyIds,
          center,
          "groundwater_inventory");
    }
    long released = Math.round(sourceBudget * 0.74);
    long deposit = Math.round(released * 0.57);
    long loss = released - deposit;
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    return new UraniumSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        basementSourceId,
        fluidSourceId,
        structureId,
        host.geology().rockBodyId(),
        unconformityId,
        sourceBodyIds,
        family == DepositFamily.UNCONFORMITY_RELATED
            ? geometry.unconformity().age()
            : new AgeKey(248.0, 0),
        sourceClass,
        hostClass,
        fluidSourceClass,
        pathwayClass,
        trapClass,
        preservationClass,
        salinity,
        redox,
        center,
        family == DepositFamily.UNCONFORMITY_RELATED ? 152.0 : 188.0,
        family == DepositFamily.UNCONFORMITY_RELATED ? 52.0 : 34.0,
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, cell, family, deposit),
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

  private static boolean oldFertileBasement(RiftArcGeometry geometry, PetrologicSample basement) {
    return basement.geology().rockBodyId().equals(geometry.basementId())
        && basement.geology().formationAge().ageMa() >= 1_000.0
        && basement.geology().lithology() == Lithology.GRANITIC_GNEISS;
  }

  private static SourceClass sourceClass(
      DepositFamily family, boolean oldFertileBasement, Optional<SedimentaryState> sedimentary) {
    if (family == DepositFamily.UNCONFORMITY_RELATED && oldFertileBasement) {
      return SourceClass.OLD_U_FERTILE_BASEMENT;
    }
    if (family == DepositFamily.SANDSTONE_ROLL_FRONT
        && (oldFertileBasement || sedimentary.isPresent())) {
      return SourceClass.BASIN_OR_BASEMENT_U_FERTILITY;
    }
    return SourceClass.NO_URANIUM_SOURCE;
  }

  private static PathwayClass pathwayClass(
      DepositFamily family,
      RiftArcGeometry geometry,
      Point3 interfacePoint,
      PetrologicSample host) {
    if (family == DepositFamily.UNCONFORMITY_RELATED
        && (geometry.fault().intersectsDamageZone(interfacePoint)
            || host.geology().faultDamageZone())) {
      return PathwayClass.UNCONFORMITY_FAULT_FLOW;
    }
    if (family == DepositFamily.SANDSTONE_ROLL_FRONT
        && (geometry.fault().intersectsDamageZone(interfacePoint)
            || host.permeabilityIndex() >= 0.28)) {
      return PathwayClass.AQUIFER_RECHARGE_FLOW;
    }
    return PathwayClass.NO_CONNECTED_PATH;
  }

  private static TrapClass trapClass(
      DepositFamily family,
      boolean oldFertileBasement,
      PetrologicSample basement,
      RedoxClass redox,
      PetrologicSample host) {
    if (family == DepositFamily.UNCONFORMITY_RELATED
        && oldFertileBasement
        && (basement.materialBufferState().ferrousIronCapacityPpm() >= 10_000L
            || basement.materialBufferState().reducedSulfurCapacityPpm() >= 10_000L)) {
      return TrapClass.REDUCING_BASEMENT_REACTION;
    }
    if (family == DepositFamily.SANDSTONE_ROLL_FRONT
        && (redox == RedoxClass.BUFFERED || redox == RedoxClass.REDUCING)
        && host.permeabilityIndex() >= 0.20) {
      return TrapClass.REDOX_FRONT_REACTION;
    }
    return TrapClass.NO_REDUCING_TRAP;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_BASIN_OR_UNCONFORMITY
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long groundwaterProxy(
      DepositFamily family,
      PetrologicSample host,
      PetrologicSample basement,
      Optional<SedimentaryState> sedimentary) {
    long water = sedimentary.map(state -> state.reservoirState().waterInventoryPpm()).orElse(0L);
    long volatileInventory =
        sedimentary.map(state -> state.reservoirState().volatileInventoryPpm()).orElse(0L);
    long basementWater = basement.materialBufferState().waterInventoryPpm();
    long sourceBonus =
        family == DepositFamily.UNCONFORMITY_RELATED
            ? basement.materialBufferState().ferrousIronCapacityPpm() / 8L + 24_000L
            : 18_000L;
    long permeabilityBonus = Math.round(host.permeabilityIndex() * 60_000.0);
    return Math.min(
        300_000L,
        Math.max(0L, water / 6L + volatileInventory / 15L + basementWater / 8L + sourceBonus)
            + permeabilityBonus);
  }

  private static UraniumSystemState barren(
      StableId systemId,
      StableId basementSourceId,
      StableId fluidSourceId,
      StableId structureId,
      StableId hostBodyId,
      StableId unconformityId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new UraniumSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        DepositFamily.NONE,
        basementSourceId,
        fluidSourceId,
        structureId,
        hostBodyId,
        unconformityId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        SourceClass.NO_URANIUM_SOURCE,
        HostClass.NO_PERMEABLE_AQUIFER,
        FluidSourceClass.NO_OXIDIZED_URANIUM_FLUID,
        PathwayClass.NO_CONNECTED_PATH,
        TrapClass.NO_REDUCING_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        SalinityClass.FRESH,
        RedoxClass.BUFFERED,
        center,
        188.0,
        52.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, CellKey cell, DepositFamily family, long deposit) {
    long inner = Math.round(deposit * 0.46);
    long middle = Math.round(deposit * 0.34);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        family == DepositFamily.UNCONFORMITY_RELATED
            ? new HorizonKind[] {
              HorizonKind.WEATHERED_SANDSTONE,
              HorizonKind.FAULT_BRECCIA_URANINITE,
              HorizonKind.REDUCING_BASEMENT_REACTION
            }
            : new HorizonKind[] {
              HorizonKind.OXIDATION_TONGUE,
              HorizonKind.ROLL_FRONT_REDUCTION,
              HorizonKind.TABULAR_REDUCTANT_HALO
            };
    return List.of(
        horizon(
            kinds[0],
            family == DepositFamily.UNCONFORMITY_RELATED
                ? Overprint.WEATHERED_UNCONFORMITY
                : Overprint.WEATHERED_REGOLITH,
            0.0,
            0.30,
            0.96,
            inner,
            identity,
            cell,
            0),
        horizon(
            kinds[1], Overprint.PHYLLIC_ALTERATION, 0.30, 0.68, 0.82, middle, identity, cell, 1),
        horizon(
            kinds[2], Overprint.PROPYLITIC_ALTERATION, 0.68, 1.0, 0.68, outer, identity, cell, 2));
  }

  private static Horizon horizon(
      HorizonKind kind,
      Overprint overprint,
      double top,
      double bottom,
      double radius,
      long allocation,
      WorldIdentity identity,
      CellKey cell,
      long index) {
    return new Horizon(
        kind,
        overprint,
        top,
        bottom,
        radius,
        allocation,
        identity.stream("geological", "uranium-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("uranium horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("uranium horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum DepositFamily {
    UNCONFORMITY_RELATED,
    SANDSTONE_ROLL_FRONT,
    NONE
  }

  public enum SourceClass {
    OLD_U_FERTILE_BASEMENT,
    BASIN_OR_BASEMENT_U_FERTILITY,
    NO_URANIUM_SOURCE
  }

  public enum HostClass {
    PERMEABLE_BASIN_SANDSTONE,
    NO_PERMEABLE_AQUIFER
  }

  public enum FluidSourceClass {
    OXIDIZED_URANIUM_GROUNDWATER_PROXY,
    NO_OXIDIZED_URANIUM_FLUID
  }

  public enum PathwayClass {
    UNCONFORMITY_FAULT_FLOW,
    AQUIFER_RECHARGE_FLOW,
    NO_CONNECTED_PATH
  }

  public enum TrapClass {
    REDUCING_BASEMENT_REACTION,
    REDOX_FRONT_REACTION,
    NO_REDUCING_TRAP
  }

  public enum PreservationClass {
    PRESERVED_BASIN_OR_UNCONFORMITY,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    WEATHERED_SANDSTONE,
    FAULT_BRECCIA_URANINITE,
    REDUCING_BASEMENT_REACTION,
    OXIDATION_TONGUE,
    ROLL_FRONT_REDUCTION,
    TABULAR_REDUCTANT_HALO
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
        throw new IllegalArgumentException("uranium horizon identity is required");
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
        throw new IllegalArgumentException("uranium horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
