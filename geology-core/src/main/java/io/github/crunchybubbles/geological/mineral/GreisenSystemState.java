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
import io.github.crunchybubbles.geological.petrology.MagmaDifferentiationState;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Source-gated greisen refinement around an evolved felsic pulse.
 *
 * <p>This is a bounded mineral-system proof, not a new Phase 2 material definition. The current
 * catalog has no Sn/W greisen assay vocabulary, so the budget is an explicitly named residual-fluid
 * proxy and cannot be interpreted as grade or absolute tonnage.
 */
public record GreisenSystemState(
    StableId systemId,
    FormationStatus status,
    StableId intrusionId,
    StableId fluidSystemId,
    StableId parentBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    SourceBasis sourceBasis,
    FluidPathClass fluidPathClass,
    ParentClass parentClass,
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
  public GreisenSystemState {
    if (systemId == null
        || status == null
        || intrusionId == null
        || fluidSystemId == null
        || parentBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || sourceBasis == null
        || fluidPathClass == null
        || parentClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("greisen system state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(parentBodyId)) {
      throw new IllegalArgumentException("greisen sources must be non-empty and retain the parent");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("greisen fluid ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("greisen horizons cannot be null");
    }
    validateHorizonSequence(horizons);
    if (status == FormationStatus.FORMED) {
      if (sourceBasis == SourceBasis.NO_ELIGIBLE_RESIDUAL_FLUID
          || fluidPathClass == FluidPathClass.NO_RESIDUAL_FLUID
          || parentClass != ParentClass.EVOLVED_FELSIC_PULSE
          || preservationClass != PreservationClass.PRESERVED_CONTACT_ZONE
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.isEmpty()
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed greisen requires evolved fluid and contact proof");
      }
    } else if (sourceBasis != SourceBasis.NO_ELIGIBLE_RESIDUAL_FLUID
        || fluidPathClass != FluidPathClass.NO_RESIDUAL_FLUID
        || parentClass != ParentClass.NO_ELIGIBLE_PARENT
        || preservationClass != PreservationClass.STRIPPED_OR_COVERED
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren greisen must retain a failed hard gate");
    }
  }

  /** Derives the greisen proof from the resolved evolved pulse at one immutable surface parcel. */
  public static GreisenSystemState proofFor(
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
      throw new IllegalArgumentException("greisen parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("greisen surface point changed between stages");
    }
    RiftArcGeometry.PlutonPulse youngest = province.geometry().plutonPulses().getLast();
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId = identity.stream("geological", "greisen-system", cell, 0).stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                surface.context().sourceBodyIds().stream(),
                java.util.stream.Stream.of(parent.geology().rockBodyId()))
            .distinct()
            .sorted()
            .toList();
    ParentClass parentClass = parentClass(parent, youngest);
    FluidPathClass fluidPathClass = fluidPathClass(parent);
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        parentClass == ParentClass.NO_ELIGIBLE_PARENT
            ? "evolved_felsic_parent"
            : fluidPathClass == FluidPathClass.NO_RESIDUAL_FLUID
                ? "residual_fluid"
                : preservationClass == PreservationClass.STRIPPED_OR_COVERED
                    ? "preservation"
                    : null;
    if (failedGate != null) {
      return barren(
          systemId,
          youngest.id(),
          province.proofIds().magmaLineageId(),
          parent.geology().rockBodyId(),
          sourceBodyIds,
          localSurface,
          parentClass,
          fluidPathClass,
          preservationClass,
          failedGate);
    }
    long sourceBudget = residualFluidProxy(parent);
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          youngest.id(),
          province.proofIds().magmaLineageId(),
          parent.geology().rockBodyId(),
          sourceBodyIds,
          localSurface,
          parentClass,
          FluidPathClass.NO_RESIDUAL_FLUID,
          preservationClass,
          "residual_fluid_inventory");
    }
    long released = Math.round(sourceBudget * 0.72);
    long deposit = Math.round(released * 0.62);
    long loss = released - deposit;
    double thickness = 42.0;
    return new GreisenSystemState(
        systemId,
        FormationStatus.FORMED,
        youngest.id(),
        province.proofIds().magmaLineageId(),
        parent.geology().rockBodyId(),
        sourceBodyIds,
        new AgeKey(91.0, 0),
        SourceBasis.RESIDUAL_FELSIC_FLUID_PROXY,
        fluidPathClass,
        parentClass,
        preservationClass,
        new Point3(localSurface.x(), localSurface.y() - thickness / 2.0, localSurface.z()),
        92.0,
        58.0,
        sourceBudget,
        released,
        loss,
        deposit,
        formedHorizons(identity, cell),
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

  private static ParentClass parentClass(
      PetrologicSample parent, RiftArcGeometry.PlutonPulse youngest) {
    return parent.geology().rockBodyId().equals(youngest.id())
            && (parent.geology().lithology() == Lithology.FELSIC_STOCK
                || parent.geology().lithology() == Lithology.GRANODIORITE_PULSE)
        ? ParentClass.EVOLVED_FELSIC_PULSE
        : ParentClass.NO_ELIGIBLE_PARENT;
  }

  private static FluidPathClass fluidPathClass(PetrologicSample parent) {
    return parent
                .magmaLineage()
                .map(lineage -> lineage.differentiationState().residualFluidPotential())
                .orElse(MagmaDifferentiationState.ResidualFluidPotential.UNRESOLVED)
            == MagmaDifferentiationState.ResidualFluidPotential.VERY_HIGH
        ? FluidPathClass.VERY_HIGH_RESIDUAL_FLUID
        : FluidPathClass.NO_RESIDUAL_FLUID;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER
            && !fields.outcrop()
            && fields.slope() <= 0.48
        ? PreservationClass.PRESERVED_CONTACT_ZONE
        : PreservationClass.STRIPPED_OR_COVERED;
  }

  private static long residualFluidProxy(PetrologicSample parent) {
    return parent
        .magmaResidualInventoryState()
        .map(
            inventory ->
                Math.min(
                    250_000L,
                    inventory.residualFluidInventoryPpm().values().stream()
                        .mapToLong(Long::longValue)
                        .sum()))
        .orElse(0L);
  }

  private static GreisenSystemState barren(
      StableId systemId,
      StableId intrusionId,
      StableId fluidSystemId,
      StableId parentBodyId,
      List<StableId> sourceBodyIds,
      Point3 localSurface,
      ParentClass parentClass,
      FluidPathClass fluidPathClass,
      PreservationClass preservationClass,
      String failedGate) {
    return new GreisenSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        intrusionId,
        fluidSystemId,
        parentBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        SourceBasis.NO_ELIGIBLE_RESIDUAL_FLUID,
        FluidPathClass.NO_RESIDUAL_FLUID,
        ParentClass.NO_ELIGIBLE_PARENT,
        PreservationClass.STRIPPED_OR_COVERED,
        new Point3(localSurface.x(), localSurface.y() - 21.0, localSurface.z()),
        92.0,
        58.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(WorldIdentity identity, CellKey cell) {
    return List.of(
        horizon(
            HorizonKind.QUARTZ_MUSCOVITE_GREISEN,
            Overprint.PHYLLIC_ALTERATION,
            0.0,
            0.36,
            0.96,
            identity,
            cell,
            0),
        horizon(
            HorizonKind.TOURMALINE_PROXY,
            Overprint.PHYLLIC_ALTERATION,
            0.36,
            0.70,
            0.82,
            identity,
            cell,
            1),
        horizon(
            HorizonKind.KAOLINITIC_MARGIN,
            Overprint.PROPYLITIC_ALTERATION,
            0.70,
            1.0,
            0.70,
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
      WorldIdentity identity,
      CellKey cell,
      long index) {
    return new Horizon(
        kind,
        overprint,
        top,
        bottom,
        radius,
        identity.stream("geological", "greisen-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("greisen horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("greisen horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum SourceBasis {
    RESIDUAL_FELSIC_FLUID_PROXY,
    NO_ELIGIBLE_RESIDUAL_FLUID
  }

  public enum FluidPathClass {
    VERY_HIGH_RESIDUAL_FLUID,
    NO_RESIDUAL_FLUID
  }

  public enum ParentClass {
    EVOLVED_FELSIC_PULSE,
    NO_ELIGIBLE_PARENT
  }

  public enum PreservationClass {
    PRESERVED_CONTACT_ZONE,
    STRIPPED_OR_COVERED
  }

  public enum HorizonKind {
    QUARTZ_MUSCOVITE_GREISEN,
    TOURMALINE_PROXY,
    KAOLINITIC_MARGIN
  }

  public record Horizon(
      HorizonKind kind,
      Overprint overprint,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      StableId bodyId) {
    public Horizon {
      if (kind == null || overprint == null || bodyId == null) {
        throw new IllegalArgumentException("greisen horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("greisen horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
