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
import io.github.crunchybubbles.geological.worldgen.SkarnHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated calc-silicate skarn proof around a younger intrusion and reactive host. */
public record SkarnSystemState(
    StableId systemId,
    FormationStatus status,
    StableId intrusionId,
    StableId fluidSystemId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    CommodityClass commodityClass,
    HostClass hostClass,
    IntrusionClass intrusionClass,
    FluidClass fluidClass,
    PermeabilityClass permeabilityClass,
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
  public SkarnSystemState {
    if (systemId == null
        || status == null
        || intrusionId == null
        || fluidSystemId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || commodityClass == null
        || hostClass == null
        || intrusionClass == null
        || fluidClass == null
        || permeabilityClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("skarn system state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException("skarn sources must retain the host body");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedFluidFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedFluidFixedUnits > sourceBudgetFixedUnits
        || releasedFluidFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("skarn fluid ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("skarn horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("skarn horizons must have unique kinds and bodies");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException("skarn horizon allocations must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (commodityClass == CommodityClass.NO_COMMODITY
          || hostClass == HostClass.NO_REACTIVE_HOST
          || intrusionClass == IntrusionClass.NO_YOUNGER_INTRUSION
          || fluidClass == FluidClass.NO_MAGMATIC_HYDROTHERMAL_FLUID
          || permeabilityClass == PermeabilityClass.NO_CONTACT_PERMEABILITY
          || preservationClass == PreservationClass.ERODED_OR_BURIED
          || !sourceBodyIds.contains(intrusionId)
          || sourceBudgetFixedUnits == 0L
          || releasedFluidFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed skarn requires host, intrusion, fluid, and contact proof");
      }
    } else if (failedGate.isEmpty()
        || !horizons.isEmpty()
        || sourceBudgetFixedUnits != 0L
        || releasedFluidFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L) {
      throw new IllegalArgumentException("barren skarn must retain a failed gate and no budget");
    }
  }

  /** Derives skarn evidence from the existing magmatic-hydrothermal engine and host policy. */
  public static SkarnSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      SkarnHostPolicy.HostEvidence host) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null
        || host == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, parent, and host evidence are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())) {
      throw new IllegalArgumentException("skarn parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("skarn surface point changed between stages");
    }
    RiftArcGeometry.PlutonPulse youngest = province.geometry().plutonPulses().getLast();
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId = identity.stream("geological", "skarn-system", cell, 0).stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                    surface.context().sourceBodyIds().stream(),
                    java.util.stream.Stream.of(parent.geology().rockBodyId())),
                java.util.stream.Stream.of(host.hostBodyId()))
            .distinct()
            .sorted()
            .toList();
    HostClass hostClass = hostClass(host.hostLithology());
    IntrusionClass intrusionClass = intrusionClass(parent, youngest);
    FluidClass fluidClass = fluidClass(province, parent);
    PermeabilityClass permeabilityClass =
        host.contactPermeability()
            ? PermeabilityClass.CONTACT_FRACTURE
            : PermeabilityClass.NO_CONTACT_PERMEABILITY;
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        hostClass == HostClass.NO_REACTIVE_HOST
            ? "reactive_carbonate_host"
            : intrusionClass == IntrusionClass.NO_YOUNGER_INTRUSION
                ? "younger_intrusion"
                : fluidClass == FluidClass.NO_MAGMATIC_HYDROTHERMAL_FLUID
                    ? "magmatic_fluid"
                    : permeabilityClass == PermeabilityClass.NO_CONTACT_PERMEABILITY
                        ? "contact_permeability"
                        : preservationClass == PreservationClass.ERODED_OR_BURIED
                            ? "preservation"
                            : null;
    if (failedGate != null) {
      return barren(
          systemId,
          youngest.id(),
          province.proofIds().porphyrySystemId(),
          host.hostBodyId(),
          sourceBodyIds,
          host.localCenter(),
          hostClass,
          intrusionClass,
          fluidClass,
          permeabilityClass,
          preservationClass,
          failedGate);
    }
    long sourceBudget =
        Math.min(
            400_000L,
            Math.addExact(residualFluidProxy(parent), host.reactiveInventoryFixedUnits()));
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          youngest.id(),
          province.proofIds().porphyrySystemId(),
          host.hostBodyId(),
          sourceBodyIds,
          host.localCenter(),
          hostClass,
          intrusionClass,
          FluidClass.NO_MAGMATIC_HYDROTHERMAL_FLUID,
          permeabilityClass,
          preservationClass,
          "source_inventory");
    }
    long released = Math.round(sourceBudget * 0.78);
    long deposit = Math.round(released * 0.68);
    long loss = released - deposit;
    CommodityClass commodity = commodityClass(parent);
    return new SkarnSystemState(
        systemId,
        FormationStatus.FORMED,
        youngest.id(),
        province.proofIds().porphyrySystemId(),
        host.hostBodyId(),
        sourceBodyIds,
        new AgeKey(96.0, 0),
        commodity,
        hostClass,
        intrusionClass,
        fluidClass,
        permeabilityClass,
        preservationClass,
        new Point3(host.localCenter().x(), host.localCenter().y() - 36.0, host.localCenter().z()),
        108.0,
        72.0,
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

  private static HostClass hostClass(Lithology lithology) {
    return switch (lithology) {
      case LIMESTONE -> HostClass.LIMESTONE;
      case DOLOSTONE -> HostClass.DOLOSTONE;
      case MARBLE -> HostClass.MARBLE;
      case CARBONATITIC -> HostClass.CARBONATITIC;
      default -> HostClass.NO_REACTIVE_HOST;
    };
  }

  private static IntrusionClass intrusionClass(
      PetrologicSample parent, RiftArcGeometry.PlutonPulse youngest) {
    return parent.geology().rockBodyId().equals(youngest.id())
            && (youngest.lithology() == Lithology.DIORITE_PULSE
                || youngest.lithology() == Lithology.GRANODIORITE_PULSE
                || youngest.lithology() == Lithology.FELSIC_STOCK)
        ? IntrusionClass.YOUNGER_ARC_INTRUSION
        : IntrusionClass.NO_YOUNGER_INTRUSION;
  }

  private static FluidClass fluidClass(Province province, PetrologicSample parent) {
    boolean fluid =
        province.grammar().formsPorphyry()
            && parent
                .magmaLineage()
                .map(
                    lineage ->
                        lineage.differentiationState().residualFluidPotential()
                            == MagmaDifferentiationState.ResidualFluidPotential.VERY_HIGH)
                .orElse(false)
            && residualFluidProxy(parent) > 0L;
    return fluid
        ? FluidClass.MAGMATIC_HYDROTHERMAL_FLUID
        : FluidClass.NO_MAGMATIC_HYDROTHERMAL_FLUID;
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_CONTACT
        : PreservationClass.ERODED_OR_BURIED;
  }

  private static CommodityClass commodityClass(PetrologicSample parent) {
    return parent
            .magmaLineage()
            .map(
                lineage ->
                    lineage.differentiationState().sulfurSaturationHistory()
                        == MagmaDifferentiationState.SulfurSaturationHistory.SATURATED)
            .orElse(false)
        ? CommodityClass.CU_AU_PROXY
        : CommodityClass.FE_CALC_SILICATE_PROXY;
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

  private static SkarnSystemState barren(
      StableId systemId,
      StableId intrusionId,
      StableId fluidSystemId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      Point3 localCenter,
      HostClass hostClass,
      IntrusionClass intrusionClass,
      FluidClass fluidClass,
      PermeabilityClass permeabilityClass,
      PreservationClass preservationClass,
      String failedGate) {
    return new SkarnSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        intrusionId,
        fluidSystemId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        CommodityClass.NO_COMMODITY,
        hostClass,
        intrusionClass,
        fluidClass,
        permeabilityClass,
        preservationClass,
        new Point3(localCenter.x(), localCenter.y() - 36.0, localCenter.z()),
        108.0,
        72.0,
        0L,
        0L,
        0L,
        0L,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, CellKey cell, long depositAllocation) {
    long prograde = Math.round(depositAllocation * 0.50);
    long retrograde = Math.round(depositAllocation * 0.30);
    long distal = depositAllocation - prograde - retrograde;
    return List.of(
        horizon(
            HorizonKind.PROGRADE_GARNET_PYROXENE,
            Overprint.CONTACT_HORNFELS,
            0.0,
            0.46,
            0.96,
            prograde,
            identity,
            cell,
            0),
        horizon(
            HorizonKind.RETROGRADE_AMPHIBOLE_EPIDOTE,
            Overprint.PROPYLITIC_ALTERATION,
            0.46,
            0.76,
            0.82,
            retrograde,
            identity,
            cell,
            1),
        horizon(
            HorizonKind.DISTAL_CARBONATE_REACTION,
            Overprint.CONTACT_HORNFELS,
            0.76,
            1.0,
            0.68,
            distal,
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
        identity.stream("geological", "skarn-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("skarn horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("skarn horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum CommodityClass {
    CU_AU_PROXY,
    FE_CALC_SILICATE_PROXY,
    NO_COMMODITY
  }

  public enum HostClass {
    LIMESTONE,
    DOLOSTONE,
    MARBLE,
    CARBONATITIC,
    NO_REACTIVE_HOST
  }

  public enum IntrusionClass {
    YOUNGER_ARC_INTRUSION,
    NO_YOUNGER_INTRUSION
  }

  public enum FluidClass {
    MAGMATIC_HYDROTHERMAL_FLUID,
    NO_MAGMATIC_HYDROTHERMAL_FLUID
  }

  public enum PermeabilityClass {
    CONTACT_FRACTURE,
    NO_CONTACT_PERMEABILITY
  }

  public enum PreservationClass {
    PRESERVED_CONTACT,
    ERODED_OR_BURIED
  }

  public enum HorizonKind {
    PROGRADE_GARNET_PYROXENE,
    RETROGRADE_AMPHIBOLE_EPIDOTE,
    DISTAL_CARBONATE_REACTION
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
        throw new IllegalArgumentException("skarn horizon identity is required");
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
        throw new IllegalArgumentException("skarn horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
