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
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated shallow magmatic-hydrothermal epithermal Au-Ag proxy system. */
public record EpithermalSystemState(
    StableId systemId,
    FormationStatus status,
    StableId sourceIntrusionId,
    StableId fluidSystemId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    SulfidationClass sulfidationClass,
    HostClass hostClass,
    FluidPathClass fluidPathClass,
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
  public EpithermalSystemState {
    if (systemId == null
        || status == null
        || sourceIntrusionId == null
        || fluidSystemId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || sulfidationClass == null
        || hostClass == null
        || fluidPathClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("epithermal system state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(sourceIntrusionId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException("epithermal sources must retain intrusion and host");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("epithermal fluid ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("epithermal horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("epithermal horizons must have unique kinds and bodies");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "epithermal horizon allocations must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (sulfidationClass == SulfidationClass.NONE
          || hostClass == HostClass.NO_RECEPTIVE_HOST
          || fluidPathClass == FluidPathClass.NO_MAGMATIC_FLUID
          || pathwayClass == PathwayClass.NO_PERMEABLE_PATH
          || trapClass == TrapClass.NO_DEPOSITION_TRAP
          || preservationClass == PreservationClass.ERODED_OR_BURIED
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed epithermal systems require shallow fluid and trap proof");
      }
    } else if (failedGate.isEmpty()
        || !horizons.isEmpty()
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L) {
      throw new IllegalArgumentException(
          "barren epithermal systems must retain a failed gate and no budget");
    }
  }

  /** Resolves a shallow epithermal proof from the existing porphyry fluid-phase state. */
  public static EpithermalSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, and parent are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())) {
      throw new IllegalArgumentException(
          "epithermal parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("epithermal surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    RiftArcGeometry.PlutonPulse intrusion = geometry.plutonPulses().getLast();
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId = identity.stream("geological", "epithermal-system", cell, 0).stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                    surface.context().sourceBodyIds().stream(),
                    java.util.stream.Stream.of(parent.geology().rockBodyId())),
                java.util.stream.Stream.of(intrusion.id()))
            .distinct()
            .sorted()
            .toList();
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    HostClass hostClass = hostClass(parent.geology().lithology());
    PorphyryFluidMetalState fluidState = porphyryFluidState(province);
    Optional<PorphyryFluidMetalState.FluidPulse> fluidPulse =
        province.grammar().formsPorphyry() ? fluidState.fluidAt(localSurface) : Optional.empty();
    FluidPathClass fluidPathClass = fluidPathClass(fluidPulse);
    PathwayClass pathwayClass = pathwayClass(geometry, localSurface);
    TrapClass trapClass = trapClass(fluidPulse, localSurface);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        !province.grammar().formsPorphyry()
            ? "fertile_intrusion"
            : fluidPathClass == FluidPathClass.NO_MAGMATIC_FLUID
                ? "residual_fluid"
                : hostClass == HostClass.NO_RECEPTIVE_HOST
                    ? "receptive_host"
                    : pathwayClass == PathwayClass.NO_PERMEABLE_PATH
                        ? "pathway"
                        : trapClass == TrapClass.NO_DEPOSITION_TRAP
                            ? "boiling_or_mixing_trap"
                            : preservationClass == PreservationClass.ERODED_OR_BURIED
                                ? "preservation"
                                : null;
    if (failedGate != null) {
      return barren(
          systemId,
          intrusion.id(),
          province.proofIds().porphyrySystemId(),
          parent.geology().rockBodyId(),
          sourceBodyIds,
          new Point3(
              geometry.porphyryCenter().x(),
              geometry.porphyryCenter().y() - 32.0,
              geometry.porphyryCenter().z()),
          hostClass,
          fluidPathClass,
          pathwayClass,
          trapClass,
          preservationClass,
          failedGate);
    }
    long sourceBudget = Math.min(250_000L, fluidState.sourceBudgetFixedUnits());
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          intrusion.id(),
          province.proofIds().porphyrySystemId(),
          parent.geology().rockBodyId(),
          sourceBodyIds,
          new Point3(
              geometry.porphyryCenter().x(),
              geometry.porphyryCenter().y() - 32.0,
              geometry.porphyryCenter().z()),
          hostClass,
          FluidPathClass.NO_MAGMATIC_FLUID,
          pathwayClass,
          trapClass,
          preservationClass,
          "source_inventory");
    }
    SulfidationClass sulfidation = sulfidationClass(fluidPulse.orElseThrow());
    long released = Math.round(sourceBudget * 0.74);
    long deposit = Math.round(released * 0.58);
    long loss = released - deposit;
    Point3 center =
        new Point3(
            geometry.porphyryCenter().x(),
            geometry.porphyryCenter().y() - 32.0,
            geometry.porphyryCenter().z());
    return new EpithermalSystemState(
        systemId,
        FormationStatus.FORMED,
        intrusion.id(),
        province.proofIds().porphyrySystemId(),
        parent.geology().rockBodyId(),
        sourceBodyIds,
        new AgeKey(90.5, 0),
        sulfidation,
        hostClass,
        fluidPathClass,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        220.0,
        64.0,
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, cell, sulfidation, deposit),
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

  private static PorphyryFluidMetalState porphyryFluidState(Province province) {
    return new MineralSystemProofs().porphyryFluidMetalState(province);
  }

  private static HostClass hostClass(Lithology lithology) {
    return switch (lithology) {
      case DIORITE_PULSE,
          GRANODIORITE_PULSE,
          FELSIC_STOCK,
          MARINE_VOLCANICLASTIC,
          BASAL_CONGLOMERATE,
          BASIN_SHALE,
          BASIN_SANDSTONE,
          VMS_MASSIVE_SULFIDE,
          GRANITIC_GNEISS ->
          HostClass.RECEPTIVE_VOLCANIC_OR_COUNTRY_ROCK;
      default -> HostClass.NO_RECEPTIVE_HOST;
    };
  }

  private static FluidPathClass fluidPathClass(Optional<PorphyryFluidMetalState.FluidPulse> pulse) {
    if (pulse.isEmpty()) {
      return FluidPathClass.NO_MAGMATIC_FLUID;
    }
    return pulse.orElseThrow().phase() == PorphyryFluidMetalState.FluidPhaseClass.METEORIC_MIXTURE
        ? FluidPathClass.MIXED_MAGMATIC_METEORIC
        : FluidPathClass.MAGMATIC_VAPOR_OR_BRINE;
  }

  private static PathwayClass pathwayClass(RiftArcGeometry geometry, Point3 localPoint) {
    double radial = distance(localPoint, geometry.porphyryCenter());
    return geometry.fault().intersectsDamageZone(localPoint) || radial <= 220.0
        ? PathwayClass.FAULT_OR_STOCKWORK
        : PathwayClass.NO_PERMEABLE_PATH;
  }

  private static TrapClass trapClass(
      Optional<PorphyryFluidMetalState.FluidPulse> pulse, Point3 localPoint) {
    return pulse.isPresent() && localPoint.y() <= 260.0
        ? TrapClass.BOILING_COOLING_MIXING
        : TrapClass.NO_DEPOSITION_TRAP;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_SHALLOW
        : PreservationClass.ERODED_OR_BURIED;
  }

  private static SulfidationClass sulfidationClass(PorphyryFluidMetalState.FluidPulse pulse) {
    return switch (pulse.phase()) {
      case VAPOR_RICH_SEPARATED -> SulfidationClass.HIGH;
      case MAGMATIC_BRINE -> SulfidationClass.INTERMEDIATE;
      case METEORIC_MIXTURE -> SulfidationClass.LOW;
    };
  }

  private static double distance(Point3 first, Point3 second) {
    double dx = first.x() - second.x();
    double dy = first.y() - second.y();
    double dz = first.z() - second.z();
    return StrictMath.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static EpithermalSystemState barren(
      StableId systemId,
      StableId sourceIntrusionId,
      StableId fluidSystemId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 localCenter,
      HostClass hostClass,
      FluidPathClass fluidPathClass,
      PathwayClass pathwayClass,
      TrapClass trapClass,
      PreservationClass preservationClass,
      String failedGate) {
    return new EpithermalSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        sourceIntrusionId,
        fluidSystemId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        SulfidationClass.NONE,
        hostClass,
        fluidPathClass,
        pathwayClass,
        trapClass,
        preservationClass,
        localCenter,
        220.0,
        64.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, CellKey cell, SulfidationClass sulfidation, long deposit) {
    long inner = Math.round(deposit * 0.46);
    long middle = Math.round(deposit * 0.32);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        switch (sulfidation) {
          case HIGH ->
              new HorizonKind[] {
                HorizonKind.VUGGY_SILICA, HorizonKind.ADVANCED_ARGILLIC, HorizonKind.PROPYLITIC_HALO
              };
          case INTERMEDIATE ->
              new HorizonKind[] {
                HorizonKind.BRECCIA_QUARTZ_SULFIDE,
                HorizonKind.ARGILLIC_SERICITIC,
                HorizonKind.PROPYLITIC_HALO
              };
          case LOW ->
              new HorizonKind[] {
                HorizonKind.CRUSTIFORM_QUARTZ_ADULARIA,
                HorizonKind.ARGILLIC_SERICITIC,
                HorizonKind.PROPYLITIC_HALO
              };
          case NONE ->
              throw new IllegalArgumentException("barren epithermal systems have no horizons");
        };
    return List.of(
        horizon(kinds[0], Overprint.PHYLLIC_ALTERATION, 0.0, 0.34, 0.96, inner, identity, cell, 0),
        horizon(
            kinds[1], Overprint.PHYLLIC_ALTERATION, 0.34, 0.70, 0.82, middle, identity, cell, 1),
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
      CellKey cell,
      long index) {
    return new Horizon(
        kind,
        overprint,
        top,
        bottom,
        radius,
        allocation,
        identity.stream("geological", "epithermal-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("epithermal horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("epithermal horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum SulfidationClass {
    HIGH,
    INTERMEDIATE,
    LOW,
    NONE
  }

  public enum HostClass {
    RECEPTIVE_VOLCANIC_OR_COUNTRY_ROCK,
    NO_RECEPTIVE_HOST
  }

  public enum FluidPathClass {
    MAGMATIC_VAPOR_OR_BRINE,
    MIXED_MAGMATIC_METEORIC,
    NO_MAGMATIC_FLUID
  }

  public enum PathwayClass {
    FAULT_OR_STOCKWORK,
    NO_PERMEABLE_PATH
  }

  public enum TrapClass {
    BOILING_COOLING_MIXING,
    NO_DEPOSITION_TRAP
  }

  public enum PreservationClass {
    PRESERVED_SHALLOW,
    ERODED_OR_BURIED
  }

  public enum HorizonKind {
    VUGGY_SILICA,
    ADVANCED_ARGILLIC,
    BRECCIA_QUARTZ_SULFIDE,
    ARGILLIC_SERICITIC,
    CRUSTIFORM_QUARTZ_ADULARIA,
    PROPYLITIC_HALO
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
        throw new IllegalArgumentException("epithermal horizon identity is required");
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
        throw new IllegalArgumentException("epithermal horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
