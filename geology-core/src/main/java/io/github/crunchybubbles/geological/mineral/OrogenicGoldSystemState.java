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
import io.github.crunchybubbles.geological.petrology.MetamorphicGrade;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.RegionalMetamorphicState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated quartz-carbonate orogenic-gold proxy tied to deformation and metamorphic fluid. */
public record OrogenicGoldSystemState(
    StableId systemId,
    FormationStatus status,
    StableId metamorphicDriverId,
    StableId fluidSystemId,
    StableId structureId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    DepthClass depthClass,
    FluidSourceClass fluidSourceClass,
    HostClass hostClass,
    StructureClass structureClass,
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
  public OrogenicGoldSystemState {
    if (systemId == null
        || status == null
        || metamorphicDriverId == null
        || fluidSystemId == null
        || structureId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || depthClass == null
        || fluidSourceClass == null
        || hostClass == null
        || structureClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("orogenic-gold system state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(metamorphicDriverId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "orogenic sources must retain driver, structure, and host");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("orogenic fluid ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("orogenic horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("orogenic horizons must have unique kinds and bodies");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "orogenic horizon allocations must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (depthClass == DepthClass.UNKNOWN
          || fluidSourceClass == FluidSourceClass.NO_METAMORPHIC_FLUID
          || hostClass == HostClass.NO_RECEPTIVE_HOST
          || structureClass == StructureClass.NO_CONNECTED_STRUCTURE
          || trapClass == TrapClass.NO_SUITABLE_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed orogenic gold requires metamorphic fluid and shear proof");
      }
    } else if (depthClass != DepthClass.UNKNOWN
        || fluidSourceClass != FluidSourceClass.NO_METAMORPHIC_FLUID
        || hostClass != HostClass.NO_RECEPTIVE_HOST
        || structureClass != StructureClass.NO_CONNECTED_STRUCTURE
        || trapClass != TrapClass.NO_SUITABLE_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren orogenic gold must retain a failed hard gate");
    }
  }

  /** Derives an orogenic proof from the authored regional metamorphic field and fault. */
  public static OrogenicGoldSystemState proofFor(
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
      throw new IllegalArgumentException("orogenic parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("orogenic surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId = identity.stream("geological", "orogenic-gold-system", cell, 0).stableId();
    StableId driverId =
        province.chronicle().events().stream()
            .filter(
                event -> event.type() == io.github.crunchybubbles.geological.model.EventType.FOLD)
            .findFirst()
            .map(io.github.crunchybubbles.geological.model.GeologicalEvent::id)
            .orElse(geometry.fold().id());
    StableId structureId = geometry.fault().id();
    StableId fluidSystemId =
        identity.stream("geological", "orogenic-gold-fluid", cell, 0).stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                    surface.context().sourceBodyIds().stream(),
                    java.util.stream.Stream.of(parent.geology().rockBodyId())),
                java.util.stream.Stream.of(driverId, structureId))
            .distinct()
            .sorted()
            .toList();
    Point3 regionalPoint =
        new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z());
    Optional<RegionalMetamorphicState> regional =
        RegionalMetamorphicState.proofFor(province, regionalPoint);
    DepthClass depthClass =
        regional.map(state -> depthClass(state.grade())).orElse(DepthClass.UNKNOWN);
    FluidSourceClass fluidSourceClass =
        regional
            .map(OrogenicGoldSystemState::fluidSourceClass)
            .orElse(FluidSourceClass.NO_METAMORPHIC_FLUID);
    HostClass hostClass = hostClass(parent.geology().lithology());
    StructureClass structureClass = structureClass(geometry, localSurface);
    TrapClass trapClass = trapClass(parent, regional, structureClass);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        depthClass == DepthClass.UNKNOWN
            ? "orogenic_deformation"
            : fluidSourceClass == FluidSourceClass.NO_METAMORPHIC_FLUID
                ? "metamorphic_fluid"
                : hostClass == HostClass.NO_RECEPTIVE_HOST
                    ? "receptive_host"
                    : structureClass == StructureClass.NO_CONNECTED_STRUCTURE
                        ? "crustal_shear_zone"
                        : trapClass == TrapClass.NO_SUITABLE_TRAP
                            ? "dilational_or_reactive_trap"
                            : preservationClass == PreservationClass.ERODED_OR_COVERED
                                ? "preservation"
                                : null;
    Point3 barrenCenter = new Point3(localSurface.x(), localSurface.y() - 44.0, localSurface.z());
    if (failedGate != null) {
      return barren(
          systemId,
          driverId,
          fluidSystemId,
          structureId,
          parent.geology().rockBodyId(),
          sourceBodyIds,
          barrenCenter,
          failedGate);
    }
    long sourceBudget = metamorphicFluidProxy(regional.orElseThrow(), parent);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          driverId,
          fluidSystemId,
          structureId,
          parent.geology().rockBodyId(),
          sourceBodyIds,
          barrenCenter,
          "metamorphic_fluid_inventory");
    }
    long released = Math.round(sourceBudget * 0.70);
    long deposit = Math.round(released * 0.58);
    long loss = released - deposit;
    Point3 center = new Point3(localSurface.x(), localSurface.y() - 44.0, localSurface.z());
    return new OrogenicGoldSystemState(
        systemId,
        FormationStatus.FORMED,
        driverId,
        fluidSystemId,
        structureId,
        parent.geology().rockBodyId(),
        sourceBodyIds,
        regional.orElseThrow().eventAge(),
        depthClass,
        fluidSourceClass,
        hostClass,
        structureClass,
        trapClass,
        preservationClass,
        center,
        112.0,
        88.0,
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, cell, deposit),
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

  private static DepthClass depthClass(MetamorphicGrade grade) {
    return switch (grade) {
      case LOW -> DepthClass.EPIZONAL;
      case MEDIUM -> DepthClass.MESOZONAL;
      case HIGH -> DepthClass.HYPOZONAL;
      case NONE -> DepthClass.UNKNOWN;
    };
  }

  private static FluidSourceClass fluidSourceClass(RegionalMetamorphicState regional) {
    return regional.path() == MetamorphicPath.COLLISION_CLOCKWISE
            && regional.grade() != MetamorphicGrade.NONE
            && regional.strainClass()
                != io.github.crunchybubbles.geological.petrology.MetamorphicProcessState.StrainClass
                    .NONE
        ? FluidSourceClass.METAMORPHIC_AQUEOUS_CARBONIC_PROXY
        : FluidSourceClass.NO_METAMORPHIC_FLUID;
  }

  private static HostClass hostClass(Lithology lithology) {
    return switch (lithology) {
      case SLATE_PHYLLITE,
          MICA_SCHIST,
          GREENSCHIST,
          AMPHIBOLITE,
          GRANULITE,
          QUARTZITE,
          MARBLE,
          SERPENTINITE,
          MARINE_VOLCANICLASTIC,
          BASIN_SHALE,
          BASIN_SANDSTONE,
          SILTSTONE,
          CHERT,
          BANDED_IRON_FORMATION,
          BASALTIC,
          ANDESITIC,
          VMS_MASSIVE_SULFIDE,
          GRANITIC_GNEISS ->
          HostClass.RECEPTIVE_METASEDIMENTARY_OR_VOLCANIC;
      default -> HostClass.NO_RECEPTIVE_HOST;
    };
  }

  private static StructureClass structureClass(RiftArcGeometry geometry, Point3 localPoint) {
    return geometry.fault().intersectsDamageZone(localPoint)
        ? StructureClass.CRUSTAL_SHEAR_ZONE
        : StructureClass.NO_CONNECTED_STRUCTURE;
  }

  private static TrapClass trapClass(
      PetrologicSample parent,
      Optional<RegionalMetamorphicState> regional,
      StructureClass structureClass) {
    boolean competencyContrast =
        parent.rock().lithology().strength() >= 0.50 && parent.permeabilityIndex() >= 0.20;
    boolean strained = regional.map(state -> state.intensityPpm() >= 250_000L).orElse(false);
    return structureClass == StructureClass.CRUSTAL_SHEAR_ZONE && (competencyContrast || strained)
        ? TrapClass.DILATIONAL_OR_REACTIVE_SITE
        : TrapClass.NO_SUITABLE_TRAP;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_OROGEN
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long metamorphicFluidProxy(
      RegionalMetamorphicState regional, PetrologicSample parent) {
    long gradeBonus =
        switch (regional.grade()) {
          case LOW -> 35_000L;
          case MEDIUM -> 80_000L;
          case HIGH -> 125_000L;
          case NONE -> 0L;
        };
    long strainProxy = Math.round(regional.intensityPpm() * 0.22);
    long reactionFluid =
        parent.metamorphism().processState().reactionState().dehydrationPpm()
            + parent.metamorphism().processState().reactionState().decarbonationPpm();
    return Math.min(
        300_000L, Math.max(0L, 35_000L + gradeBonus + strainProxy + reactionFluid / 4L));
  }

  private static OrogenicGoldSystemState barren(
      StableId systemId,
      StableId driverId,
      StableId fluidSystemId,
      StableId structureId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new OrogenicGoldSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        driverId,
        fluidSystemId,
        structureId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        DepthClass.UNKNOWN,
        FluidSourceClass.NO_METAMORPHIC_FLUID,
        HostClass.NO_RECEPTIVE_HOST,
        StructureClass.NO_CONNECTED_STRUCTURE,
        TrapClass.NO_SUITABLE_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        center,
        112.0,
        88.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(WorldIdentity identity, CellKey cell, long deposit) {
    long inner = Math.round(deposit * 0.48);
    long middle = Math.round(deposit * 0.31);
    long outer = deposit - inner - middle;
    return List.of(
        horizon(
            HorizonKind.QUARTZ_CARBONATE_VEIN,
            Overprint.PHYLLIC_ALTERATION,
            0.0,
            0.36,
            0.96,
            inner,
            identity,
            cell,
            0),
        horizon(
            HorizonKind.LAMINATED_SHEAR_VEIN,
            Overprint.PHYLLIC_ALTERATION,
            0.36,
            0.70,
            0.82,
            middle,
            identity,
            cell,
            1),
        horizon(
            HorizonKind.SULFIDATION_CARBONATE_HALO,
            Overprint.PROPYLITIC_ALTERATION,
            0.70,
            1.0,
            0.68,
            outer,
            identity,
            cell,
            2));
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
        identity.stream("geological", "orogenic-gold-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("orogenic horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("orogenic horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum DepthClass {
    EPIZONAL,
    MESOZONAL,
    HYPOZONAL,
    UNKNOWN
  }

  public enum FluidSourceClass {
    METAMORPHIC_AQUEOUS_CARBONIC_PROXY,
    NO_METAMORPHIC_FLUID
  }

  public enum HostClass {
    RECEPTIVE_METASEDIMENTARY_OR_VOLCANIC,
    NO_RECEPTIVE_HOST
  }

  public enum StructureClass {
    CRUSTAL_SHEAR_ZONE,
    NO_CONNECTED_STRUCTURE
  }

  public enum TrapClass {
    DILATIONAL_OR_REACTIVE_SITE,
    NO_SUITABLE_TRAP
  }

  public enum PreservationClass {
    PRESERVED_OROGEN,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    QUARTZ_CARBONATE_VEIN,
    LAMINATED_SHEAR_VEIN,
    SULFIDATION_CARBONATE_HALO
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
        throw new IllegalArgumentException("orogenic horizon identity is required");
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
        throw new IllegalArgumentException("orogenic horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
