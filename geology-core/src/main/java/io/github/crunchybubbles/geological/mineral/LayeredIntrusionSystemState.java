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
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.worldgen.LayeredIntrusionHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated stratiform chromite and magmatic Ni-Cu-PGE proxy state. */
public record LayeredIntrusionSystemState(
    StableId systemId,
    FormationStatus status,
    DepositFamily family,
    StableId intrusionId,
    StableId magmaSourceId,
    StableId structureId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    MagmaSetting magmaSetting,
    HostClass hostClass,
    SaturationClass saturationClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedMeltFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public LayeredIntrusionSystemState {
    if (systemId == null
        || status == null
        || family == null
        || intrusionId == null
        || magmaSourceId == null
        || structureId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || magmaSetting == null
        || hostClass == null
        || saturationClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("layered intrusion state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(intrusionId)
        || !sourceBodyIds.contains(magmaSourceId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "layered intrusion sources must retain intrusion, magma, structure, and host");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedMeltFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedMeltFixedUnits > sourceBudgetFixedUnits
        || releasedMeltFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("layered intrusion melt ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("layered intrusion horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("layered intrusion horizons must be unique");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException("layered intrusion horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == DepositFamily.NONE
          || magmaSetting == MagmaSetting.NONE
          || hostClass == HostClass.NO_LAYERED_CUMULATE
          || saturationClass == SaturationClass.NO_SATURATION
          || pathwayClass == PathwayClass.NO_MAGMATIC_PATH
          || trapClass == TrapClass.NO_MAGMATIC_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedMeltFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed layered intrusion systems require chamber, saturation, path, and trap proof");
      }
    } else if (family != DepositFamily.NONE
        || magmaSetting != MagmaSetting.NONE
        || hostClass != HostClass.NO_LAYERED_CUMULATE
        || saturationClass != SaturationClass.NO_SATURATION
        || pathwayClass != PathwayClass.NO_MAGMATIC_PATH
        || trapClass != TrapClass.NO_MAGMATIC_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedMeltFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || !formationAge.equals(new AgeKey(0.0, 0))
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "barren layered intrusion systems must retain the failed gate");
    }
  }

  /** Derives a layered-chamber proof from explicit host policy and existing magma evidence. */
  public static LayeredIntrusionSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      LayeredIntrusionHostPolicy.HostEvidence host) {
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
          "layered intrusion parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("layered intrusion surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Point3 localSurface = host.localCenter();
    StableId systemId =
        identity.stream("geological", "layered-intrusion-system", province.homeCell(), 0)
            .stableId();
    DepositFamily family = family(host);
    MagmaSetting magmaSetting =
        host.layeredChamber() ? MagmaSetting.LAYERED_MAFIC_ULTRAMAFIC_CHAMBER : MagmaSetting.NONE;
    HostClass hostClass = hostClass(host);
    SaturationClass saturationClass = saturationClass(family, host);
    PathwayClass pathwayClass = pathwayClass(family, geometry, localSurface, host);
    TrapClass trapClass = trapClass(family, host);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        magmaSetting == MagmaSetting.NONE
            ? "layered_intrusion"
            : family == DepositFamily.NONE
                ? "cyclic_cumulate_interval"
                : hostClass == HostClass.NO_LAYERED_CUMULATE
                    ? "layered_cumulate_host"
                    : saturationClass == SaturationClass.NO_SATURATION
                        ? "chromite_or_sulfide_saturation"
                        : pathwayClass == PathwayClass.NO_MAGMATIC_PATH
                            ? "recharge_or_conduit_path"
                            : trapClass == TrapClass.NO_MAGMATIC_TRAP
                                ? "cumulate_trap"
                                : preservationClass == PreservationClass.ERODED_OR_COVERED
                                    ? "preservation"
                                    : null;
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    host.intrusionId(),
                    host.magmaSourceId(),
                    host.structureId(),
                    host.hostBodyId()),
                java.util.stream.Stream.concat(
                    parent.geology().depositIds().stream(),
                    parent.geology().rockBodyId().equals(host.hostBodyId())
                        ? java.util.stream.Stream.empty()
                        : java.util.stream.Stream.of(parent.geology().rockBodyId())))
            .distinct()
            .sorted()
            .toList();
    Point3 center = new Point3(localSurface.x(), localSurface.y() - 44.0, localSurface.z());
    if (failedGate != null) {
      return barren(
          systemId,
          host.intrusionId(),
          host.magmaSourceId(),
          host.structureId(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          failedGate);
    }
    long sourceBudget = sourceBudget(family, host);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          host.intrusionId(),
          host.magmaSourceId(),
          host.structureId(),
          host.hostBodyId(),
          sourceBodyIds,
          center,
          "magmatic_source_budget");
    }
    long released = Math.round(sourceBudget * 0.78);
    long deposit = Math.round(released * 0.61);
    long loss = released - deposit;
    return new LayeredIntrusionSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        host.intrusionId(),
        host.magmaSourceId(),
        host.structureId(),
        host.hostBodyId(),
        sourceBodyIds,
        formationAge(family),
        magmaSetting,
        hostClass,
        saturationClass,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        196.0,
        78.0,
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

  private static DepositFamily family(LayeredIntrusionHostPolicy.HostEvidence host) {
    if (!host.layeredChamber()) {
      return DepositFamily.NONE;
    }
    return switch (host.cyclicUnit()) {
      case 0 ->
          host.hostLithology() == Lithology.KOMATIITIC_ULTRAMAFIC
              ? DepositFamily.STRATIFORM_CHROMITE
              : DepositFamily.NONE;
      case 1 ->
          isMaficOrUltramafic(host.hostLithology())
              ? DepositFamily.NI_CU_PGE_SULFIDE
              : DepositFamily.NONE;
      case 2 ->
          isMaficOrUltramafic(host.hostLithology())
              ? DepositFamily.LAYERED_PGE_REEF
              : DepositFamily.NONE;
      default -> throw new IllegalArgumentException("unsupported layered cyclic unit");
    };
  }

  private static HostClass hostClass(LayeredIntrusionHostPolicy.HostEvidence host) {
    if (!host.layeredChamber()) {
      return HostClass.NO_LAYERED_CUMULATE;
    }
    return host.hostLithology() == Lithology.KOMATIITIC_ULTRAMAFIC
        ? HostClass.ULTRAMAFIC_CUMULATE
        : isMaficOrUltramafic(host.hostLithology())
            ? HostClass.MAFIC_CUMULATE
            : HostClass.NO_LAYERED_CUMULATE;
  }

  private static SaturationClass saturationClass(
      DepositFamily family, LayeredIntrusionHostPolicy.HostEvidence host) {
    return switch (family) {
      case STRATIFORM_CHROMITE ->
          host.chromiumInventoryFixedUnits() >= 100_000L
              ? SaturationClass.CHROMITE_SATURATION
              : SaturationClass.NO_SATURATION;
      case NI_CU_PGE_SULFIDE ->
          host.sulfurInventoryFixedUnits() >= 100_000L
              ? SaturationClass.SULFIDE_SATURATION
              : SaturationClass.NO_SATURATION;
      case LAYERED_PGE_REEF ->
          host.sulfurInventoryFixedUnits() >= 80_000L
              ? SaturationClass.PGE_SATURATION
              : SaturationClass.NO_SATURATION;
      case NONE -> SaturationClass.NO_SATURATION;
    };
  }

  private static PathwayClass pathwayClass(
      DepositFamily family,
      RiftArcGeometry geometry,
      Point3 localSurface,
      LayeredIntrusionHostPolicy.HostEvidence host) {
    boolean connected =
        host.fixture()
            || host.connectivityIndex() >= 0.15
            || geometry.fault().intersectsDamageZone(localSurface);
    if (!connected || family == DepositFamily.NONE) {
      return PathwayClass.NO_MAGMATIC_PATH;
    }
    return switch (family) {
      case STRATIFORM_CHROMITE -> PathwayClass.CUMULATE_RECHARGE;
      case NI_CU_PGE_SULFIDE -> PathwayClass.BASAL_CONDUIT_FLOW;
      case LAYERED_PGE_REEF -> PathwayClass.CYCLIC_REEF_FOCUS;
      case NONE -> PathwayClass.NO_MAGMATIC_PATH;
    };
  }

  private static TrapClass trapClass(
      DepositFamily family, LayeredIntrusionHostPolicy.HostEvidence host) {
    return switch (family) {
      case STRATIFORM_CHROMITE ->
          host.chromiumInventoryFixedUnits() >= 100_000L
              ? TrapClass.CHROMITITE_SEAM
              : TrapClass.NO_MAGMATIC_TRAP;
      case NI_CU_PGE_SULFIDE ->
          host.sulfurInventoryFixedUnits() >= 100_000L
              ? TrapClass.BASAL_SULFIDE_EMBAYMENT
              : TrapClass.NO_MAGMATIC_TRAP;
      case LAYERED_PGE_REEF ->
          host.sulfurInventoryFixedUnits() >= 80_000L
              ? TrapClass.PGE_REEF_HORIZON
              : TrapClass.NO_MAGMATIC_TRAP;
      case NONE -> TrapClass.NO_MAGMATIC_TRAP;
    };
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_LAYERED_CHAMBER
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long sourceBudget(
      DepositFamily family, LayeredIntrusionHostPolicy.HostEvidence host) {
    long familyInventory =
        switch (family) {
          case STRATIFORM_CHROMITE -> host.chromiumInventoryFixedUnits();
          case NI_CU_PGE_SULFIDE -> host.sulfurInventoryFixedUnits();
          case LAYERED_PGE_REEF -> Math.min(host.sulfurInventoryFixedUnits(), 480_000L);
          case NONE -> 0L;
        };
    return Math.min(300_000L, Math.min(host.sourceBudgetFixedUnits(), familyInventory));
  }

  private static AgeKey formationAge(DepositFamily family) {
    return switch (family) {
      case STRATIFORM_CHROMITE -> new AgeKey(106.0, 0);
      case NI_CU_PGE_SULFIDE -> new AgeKey(104.0, 0);
      case LAYERED_PGE_REEF -> new AgeKey(101.0, 0);
      case NONE -> new AgeKey(0.0, 0);
    };
  }

  private static LayeredIntrusionSystemState barren(
      StableId systemId,
      StableId intrusionId,
      StableId magmaSourceId,
      StableId structureId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 center,
      String failedGate) {
    return new LayeredIntrusionSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        DepositFamily.NONE,
        intrusionId,
        magmaSourceId,
        structureId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        MagmaSetting.NONE,
        HostClass.NO_LAYERED_CUMULATE,
        SaturationClass.NO_SATURATION,
        PathwayClass.NO_MAGMATIC_PATH,
        TrapClass.NO_MAGMATIC_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        center,
        196.0,
        78.0,
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
    long inner = Math.round(deposit * 0.46);
    long middle = Math.round(deposit * 0.32);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case STRATIFORM_CHROMITE ->
              new HorizonKind[] {
                HorizonKind.CHROMITITE_SEAM,
                HorizonKind.DISSEMINATED_CHROMITE_HALO,
                HorizonKind.ALTERED_ULTRAMAFIC_MARGIN
              };
          case NI_CU_PGE_SULFIDE ->
              new HorizonKind[] {
                HorizonKind.BASAL_MASSIVE_SULFIDE,
                HorizonKind.NET_TEXTURED_SULFIDE,
                HorizonKind.PGE_SULFIDE_DISSEMINATION
              };
          case LAYERED_PGE_REEF ->
              new HorizonKind[] {
                HorizonKind.PGE_REEF_SEAM,
                HorizonKind.CHROMITITE_ASSOCIATED_REEF,
                HorizonKind.SULFIDE_BEARING_CYCLIC_UNIT
              };
          case NONE ->
              throw new IllegalArgumentException("barren layered systems have no horizons");
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
        identity.stream("geological", "layered-intrusion-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("layered intrusion horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("layered intrusion horizons must cover the profile");
    }
  }

  private static boolean isMaficOrUltramafic(Lithology lithology) {
    return lithology == Lithology.KOMATIITIC_ULTRAMAFIC
        || lithology == Lithology.GABBROIC
        || lithology == Lithology.BASALTIC;
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum DepositFamily {
    STRATIFORM_CHROMITE,
    NI_CU_PGE_SULFIDE,
    LAYERED_PGE_REEF,
    NONE
  }

  public enum MagmaSetting {
    LAYERED_MAFIC_ULTRAMAFIC_CHAMBER,
    NONE
  }

  public enum HostClass {
    ULTRAMAFIC_CUMULATE,
    MAFIC_CUMULATE,
    NO_LAYERED_CUMULATE
  }

  public enum SaturationClass {
    CHROMITE_SATURATION,
    SULFIDE_SATURATION,
    PGE_SATURATION,
    NO_SATURATION
  }

  public enum PathwayClass {
    CUMULATE_RECHARGE,
    BASAL_CONDUIT_FLOW,
    CYCLIC_REEF_FOCUS,
    NO_MAGMATIC_PATH
  }

  public enum TrapClass {
    CHROMITITE_SEAM,
    BASAL_SULFIDE_EMBAYMENT,
    PGE_REEF_HORIZON,
    NO_MAGMATIC_TRAP
  }

  public enum PreservationClass {
    PRESERVED_LAYERED_CHAMBER,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    CHROMITITE_SEAM,
    DISSEMINATED_CHROMITE_HALO,
    ALTERED_ULTRAMAFIC_MARGIN,
    BASAL_MASSIVE_SULFIDE,
    NET_TEXTURED_SULFIDE,
    PGE_SULFIDE_DISSEMINATION,
    PGE_REEF_SEAM,
    CHROMITITE_ASSOCIATED_REEF,
    SULFIDE_BEARING_CYCLIC_UNIT
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
        throw new IllegalArgumentException("layered intrusion horizon identity is required");
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
        throw new IllegalArgumentException("layered intrusion horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || (bottomDepthFraction == 1.0 && depthFraction <= bottomDepthFraction));
    }
  }
}
