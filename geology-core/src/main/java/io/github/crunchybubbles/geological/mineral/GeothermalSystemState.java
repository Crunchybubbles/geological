package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.ProcessFluidState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.worldgen.GeothermalHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated geothermal heat and porous-reservoir proxy state. */
public record GeothermalSystemState(
    StableId systemId,
    FormationStatus status,
    GeothermalType family,
    StableId heatSourceId,
    StableId fluidSourceId,
    StableId structureId,
    StableId reservoirBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    HeatClass heatClass,
    FluidClass fluidClass,
    ReservoirClass reservoirClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedHeatFixedUnits,
    long transportLossFixedUnits,
    long reservoirAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public GeothermalSystemState {
    if (systemId == null
        || status == null
        || family == null
        || heatSourceId == null
        || fluidSourceId == null
        || structureId == null
        || reservoirBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || heatClass == null
        || fluidClass == null
        || reservoirClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("geothermal state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(heatSourceId)
        || !sourceBodyIds.contains(fluidSourceId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(reservoirBodyId)) {
      throw new IllegalArgumentException(
          "geothermal sources must retain heat, fluid, structure, and reservoir identities");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedHeatFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || reservoirAllocationFixedUnits < 0L
        || releasedHeatFixedUnits > sourceBudgetFixedUnits
        || releasedHeatFixedUnits != transportLossFixedUnits + reservoirAllocationFixedUnits) {
      throw new IllegalArgumentException("geothermal heat ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("geothermal horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("geothermal horizons must be unique");
    }
    validateHorizonSequence(horizons);
    if (horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum()
        != reservoirAllocationFixedUnits) {
      throw new IllegalArgumentException("geothermal horizons must close to reservoir allocation");
    }
    if (status == FormationStatus.FORMED) {
      if (family == GeothermalType.NONE
          || heatClass == HeatClass.NO_SIGNIFICANT_HEAT
          || fluidClass == FluidClass.NO_RECHARGE_FLUID
          || reservoirClass == ReservoirClass.NO_PERMEABLE_RESERVOIR
          || pathwayClass == PathwayClass.NO_CONNECTED_CIRCULATION
          || trapClass == TrapClass.NO_RESERVOIR_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedHeatFixedUnits == 0L
          || reservoirAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed geothermal systems require heat, fluid, reservoir, path, and preservation proof");
      }
    } else if (family != GeothermalType.NONE
        || heatClass != HeatClass.NO_SIGNIFICANT_HEAT
        || fluidClass != FluidClass.NO_RECHARGE_FLUID
        || reservoirClass != ReservoirClass.NO_PERMEABLE_RESERVOIR
        || pathwayClass != PathwayClass.NO_CONNECTED_CIRCULATION
        || trapClass != TrapClass.NO_RESERVOIR_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedHeatFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || reservoirAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || !formationAge.equals(new AgeKey(0.0, 0))
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren geothermal systems must retain the failed gate");
    }
  }

  /** Derives a geothermal proof from heat, fluid, fracture, and reservoir evidence. */
  public static GeothermalSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      GeothermalHostPolicy.HostEvidence host) {
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
          "geothermal parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("geothermal surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    GeothermalType family = family(host, parent, geometry);
    StableId fluidSourceId =
        identity.stream("geological", "geothermal-fluid-source", province.homeCell(), 0).stableId();
    StableId systemId =
        identity.stream("geological", "geothermal-system", province.homeCell(), 0).stableId();
    HeatClass heatClass = heatClass(host.heatPotentialFixedUnits());
    FluidClass fluidClass = fluidClass(host, parent);
    ReservoirClass reservoirClass = reservoirClass(family, host);
    PathwayClass pathwayClass = pathwayClass(family, geometry, host, parent);
    TrapClass trapClass = trapClass(family, host, parent);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        family == GeothermalType.NONE
            ? "geothermal_type"
            : heatClass == HeatClass.NO_SIGNIFICANT_HEAT
                ? "heat_source"
                : fluidClass == FluidClass.NO_RECHARGE_FLUID
                    ? "fluid_or_recharge"
                    : reservoirClass == ReservoirClass.NO_PERMEABLE_RESERVOIR
                        ? "permeable_reservoir"
                        : pathwayClass == PathwayClass.NO_CONNECTED_CIRCULATION
                            ? "connected_circulation"
                            : trapClass == TrapClass.NO_RESERVOIR_TRAP
                                ? "reservoir_trap_or_cap"
                                : preservationClass == PreservationClass.ERODED_OR_COVERED
                                    ? "preservation"
                                    : null;
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    host.heatSourceId(), fluidSourceId, geometry.fault().id(), host.hostBodyId()),
                java.util.stream.Stream.concat(
                    parent.geology().depositIds().stream(),
                    parent.geology().rockBodyId().equals(host.hostBodyId())
                        ? java.util.stream.Stream.empty()
                        : java.util.stream.Stream.of(parent.geology().rockBodyId())))
            .distinct()
            .sorted()
            .toList();
    Point3 center =
        new Point3(host.localCenter().x(), host.localCenter().y() - 48.0, host.localCenter().z());
    if (failedGate != null) {
      return barren(
          systemId,
          host.heatSourceId(),
          fluidSourceId,
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          failedGate);
    }
    long sourceBudget =
        Math.min(
            300_000L,
            Math.min(
                host.heatPotentialFixedUnits(),
                Math.min(host.fluidInventoryFixedUnits(), host.permeabilityPotentialFixedUnits())));
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          host.heatSourceId(),
          fluidSourceId,
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          "geothermal_source_budget");
    }
    long released = Math.round(sourceBudget * 0.80);
    long allocation = Math.round(released * allocationFraction(family));
    long loss = released - allocation;
    return new GeothermalSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        host.heatSourceId(),
        fluidSourceId,
        geometry.fault().id(),
        host.hostBodyId(),
        sourceBodyIds,
        formationAge(family),
        heatClass,
        fluidClass,
        reservoirClass,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        lateralExtent(family),
        verticalExtent(family),
        sourceBudget,
        released,
        loss,
        allocation,
        formedHorizons(identity, province.homeCell(), family, allocation),
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

  private static GeothermalType family(
      GeothermalHostPolicy.HostEvidence host, PetrologicSample parent, RiftArcGeometry geometry) {
    if (host.fixture()) {
      return host.fixtureType();
    }
    if (parent.magmaThermalState().isPresent()
        && parent.fluidState().isPresent()
        && host.heatPotentialFixedUnits() >= 600_000L) {
      return GeothermalType.VOLCANIC_HIGH_ENTHALPY;
    }
    if (geometry.fault().intersectsDamageZone(host.localCenter())
        && host.heatPotentialFixedUnits() >= 280_000L) {
      return GeothermalType.FAULT_CONTROLLED;
    }
    if (parent.sedimentaryState().isPresent()
        && host.fluidInventoryFixedUnits() >= 200_000L
        && host.permeabilityPotentialFixedUnits() >= 180_000L) {
      return GeothermalType.SEDIMENTARY_AQUIFER;
    }
    if (host.heatPotentialFixedUnits() >= 450_000L
        && host.permeabilityPotentialFixedUnits() >= 300_000L) {
      return GeothermalType.HOT_DRY_ROCK;
    }
    return GeothermalType.NONE;
  }

  private static HeatClass heatClass(long heat) {
    if (heat >= 700_000L) {
      return HeatClass.MAGMATIC_HIGH_ENTHALPY;
    }
    if (heat >= 450_000L) {
      return HeatClass.ELEVATED_CRUSTAL_HEAT_FLOW;
    }
    if (heat >= 280_000L) {
      return HeatClass.DEEP_CIRCULATION_HEAT;
    }
    return HeatClass.NO_SIGNIFICANT_HEAT;
  }

  private static FluidClass fluidClass(
      GeothermalHostPolicy.HostEvidence host, PetrologicSample parent) {
    if (host.fluidInventoryFixedUnits() < 180_000L || host.rechargeIndex() < 0.20) {
      return FluidClass.NO_RECHARGE_FLUID;
    }
    if (parent.fluidState().isPresent()) {
      ProcessFluidState fluid = parent.fluidState().orElseThrow();
      return fluid.medium().name().contains("MAGMATIC")
          ? FluidClass.MAGMATIC_HYDROTHERMAL_FLUID
          : FluidClass.METEORIC_RECHARGE_FLUID;
    }
    return parent.sedimentaryState().isPresent()
        ? FluidClass.SEDIMENTARY_AQUIFER_FLUID
        : FluidClass.METEORIC_RECHARGE_FLUID;
  }

  private static ReservoirClass reservoirClass(
      GeothermalType family, GeothermalHostPolicy.HostEvidence host) {
    if (host.permeabilityPotentialFixedUnits() < 180_000L) {
      return ReservoirClass.NO_PERMEABLE_RESERVOIR;
    }
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> ReservoirClass.FRACTURED_VOLCANIC_RESERVOIR;
      case FAULT_CONTROLLED -> ReservoirClass.FAULT_DAMAGE_ZONE_RESERVOIR;
      case SEDIMENTARY_AQUIFER -> ReservoirClass.POROUS_SEDIMENTARY_RESERVOIR;
      case HOT_DRY_ROCK -> ReservoirClass.HOT_DRY_FRACTURE_VOLUME;
      case NONE -> ReservoirClass.NO_PERMEABLE_RESERVOIR;
    };
  }

  private static PathwayClass pathwayClass(
      GeothermalType family,
      RiftArcGeometry geometry,
      GeothermalHostPolicy.HostEvidence host,
      PetrologicSample parent) {
    boolean connected =
        host.fixture()
            || host.connectivityIndex() >= 0.20
            || geometry.fault().intersectsDamageZone(host.localCenter())
            || parent.permeabilityIndex() >= 0.20;
    if (!connected || family == GeothermalType.NONE) {
      return PathwayClass.NO_CONNECTED_CIRCULATION;
    }
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> PathwayClass.MAGMATIC_CONVECTION;
      case FAULT_CONTROLLED -> PathwayClass.FAULT_CIRCULATION;
      case SEDIMENTARY_AQUIFER -> PathwayClass.AQUIFER_RECHARGE;
      case HOT_DRY_ROCK -> PathwayClass.DEEP_FRACTURE_CONDUCTION;
      case NONE -> PathwayClass.NO_CONNECTED_CIRCULATION;
    };
  }

  private static TrapClass trapClass(
      GeothermalType family, GeothermalHostPolicy.HostEvidence host, PetrologicSample parent) {
    if (host.rechargeIndex() < 0.20 || host.connectivityIndex() < 0.15) {
      return TrapClass.NO_RESERVOIR_TRAP;
    }
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> TrapClass.HIGH_ENTHALPY_RESERVOIR;
      case FAULT_CONTROLLED -> TrapClass.FAULT_SEAL_RESERVOIR;
      case SEDIMENTARY_AQUIFER -> TrapClass.AQUIFER_CAPROCK;
      case HOT_DRY_ROCK -> TrapClass.HOT_DRY_FRACTURE_VOLUME;
      case NONE -> TrapClass.NO_RESERVOIR_TRAP;
    };
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.ACCESSIBLE_RESERVOIR
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static double allocationFraction(GeothermalType family) {
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> 0.62;
      case FAULT_CONTROLLED -> 0.56;
      case SEDIMENTARY_AQUIFER -> 0.52;
      case HOT_DRY_ROCK -> 0.44;
      case NONE -> 0.0;
    };
  }

  private static AgeKey formationAge(GeothermalType family) {
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> new AgeKey(154.0, 0);
      case FAULT_CONTROLLED -> new AgeKey(151.0, 0);
      case SEDIMENTARY_AQUIFER -> new AgeKey(148.0, 0);
      case HOT_DRY_ROCK -> new AgeKey(145.0, 0);
      case NONE -> new AgeKey(0.0, 0);
    };
  }

  private static double lateralExtent(GeothermalType family) {
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> 164.0;
      case FAULT_CONTROLLED -> 180.0;
      case SEDIMENTARY_AQUIFER -> 212.0;
      case HOT_DRY_ROCK -> 148.0;
      case NONE -> 180.0;
    };
  }

  private static double verticalExtent(GeothermalType family) {
    return switch (family) {
      case VOLCANIC_HIGH_ENTHALPY -> 132.0;
      case FAULT_CONTROLLED -> 116.0;
      case SEDIMENTARY_AQUIFER -> 92.0;
      case HOT_DRY_ROCK -> 156.0;
      case NONE -> 92.0;
    };
  }

  private static GeothermalSystemState barren(
      StableId systemId,
      StableId heatSourceId,
      StableId fluidSourceId,
      StableId structureId,
      StableId reservoirBodyId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new GeothermalSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        GeothermalType.NONE,
        heatSourceId,
        fluidSourceId,
        structureId,
        reservoirBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        HeatClass.NO_SIGNIFICANT_HEAT,
        FluidClass.NO_RECHARGE_FLUID,
        ReservoirClass.NO_PERMEABLE_RESERVOIR,
        PathwayClass.NO_CONNECTED_CIRCULATION,
        TrapClass.NO_RESERVOIR_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        center,
        180.0,
        92.0,
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
      GeothermalType family,
      long allocation) {
    long inner = Math.round(allocation * 0.46);
    long middle = Math.round(allocation * 0.32);
    long outer = allocation - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case VOLCANIC_HIGH_ENTHALPY ->
              new HorizonKind[] {
                HorizonKind.HIGH_ENTHALPY_CORE,
                HorizonKind.FRACTURED_VOLCANIC_RESERVOIR,
                HorizonKind.SINTER_ALTERATION_MARGIN
              };
          case FAULT_CONTROLLED ->
              new HorizonKind[] {
                HorizonKind.FAULT_DAMAGE_RESERVOIR,
                HorizonKind.CAPROCK_SEAL_ZONE,
                HorizonKind.RECHARGE_UPFLOW_MARGIN
              };
          case SEDIMENTARY_AQUIFER ->
              new HorizonKind[] {
                HorizonKind.POROUS_AQUIFER_RESERVOIR,
                HorizonKind.AQUIFER_HEAT_EXCHANGE,
                HorizonKind.SEDIMENTARY_CAPROCK_MARGIN
              };
          case HOT_DRY_ROCK ->
              new HorizonKind[] {
                HorizonKind.HOT_DRY_FRACTURE_CORE,
                HorizonKind.ENHANCED_CIRCULATION_ZONE,
                HorizonKind.CONDUCTIVE_HEAT_MARGIN
              };
          case NONE ->
              throw new IllegalArgumentException("barren geothermal systems have no horizons");
        };
    return List.of(
        horizon(kinds[0], Overprint.NONE, 0.0, 0.36, 0.96, inner, identity, cell, 0),
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
        identity.stream("geological", "geothermal-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("geothermal horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("geothermal horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum GeothermalType {
    VOLCANIC_HIGH_ENTHALPY,
    FAULT_CONTROLLED,
    SEDIMENTARY_AQUIFER,
    HOT_DRY_ROCK,
    NONE
  }

  public enum HeatClass {
    MAGMATIC_HIGH_ENTHALPY,
    ELEVATED_CRUSTAL_HEAT_FLOW,
    DEEP_CIRCULATION_HEAT,
    NO_SIGNIFICANT_HEAT
  }

  public enum FluidClass {
    MAGMATIC_HYDROTHERMAL_FLUID,
    METEORIC_RECHARGE_FLUID,
    SEDIMENTARY_AQUIFER_FLUID,
    NO_RECHARGE_FLUID
  }

  public enum ReservoirClass {
    FRACTURED_VOLCANIC_RESERVOIR,
    FAULT_DAMAGE_ZONE_RESERVOIR,
    POROUS_SEDIMENTARY_RESERVOIR,
    HOT_DRY_FRACTURE_VOLUME,
    NO_PERMEABLE_RESERVOIR
  }

  public enum PathwayClass {
    MAGMATIC_CONVECTION,
    FAULT_CIRCULATION,
    AQUIFER_RECHARGE,
    DEEP_FRACTURE_CONDUCTION,
    NO_CONNECTED_CIRCULATION
  }

  public enum TrapClass {
    HIGH_ENTHALPY_RESERVOIR,
    FAULT_SEAL_RESERVOIR,
    AQUIFER_CAPROCK,
    HOT_DRY_FRACTURE_VOLUME,
    NO_RESERVOIR_TRAP
  }

  public enum PreservationClass {
    ACCESSIBLE_RESERVOIR,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    HIGH_ENTHALPY_CORE,
    FRACTURED_VOLCANIC_RESERVOIR,
    SINTER_ALTERATION_MARGIN,
    FAULT_DAMAGE_RESERVOIR,
    CAPROCK_SEAL_ZONE,
    RECHARGE_UPFLOW_MARGIN,
    POROUS_AQUIFER_RESERVOIR,
    AQUIFER_HEAT_EXCHANGE,
    SEDIMENTARY_CAPROCK_MARGIN,
    HOT_DRY_FRACTURE_CORE,
    ENHANCED_CIRCULATION_ZONE,
    CONDUCTIVE_HEAT_MARGIN
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
        throw new IllegalArgumentException("geothermal horizon identity is required");
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
        throw new IllegalArgumentException("geothermal horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
