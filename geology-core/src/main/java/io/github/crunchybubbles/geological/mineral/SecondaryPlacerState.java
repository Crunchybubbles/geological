package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MantleCargoStatus;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Source-gated mechanical placer profile for one bounded Overworld channel parcel.
 *
 * <p>The state intentionally keeps the Phase 2 catalog boundary visible. Cassiterite and some
 * heavy-mineral phases are not catalog minerals yet, so their source evidence is typed as a
 * residual LCT or durable-mineral proxy rather than an invented Sn/Ti/Zr assay. Diamond placers
 * require the existing explicit diamondiferous mantle-cargo state; an unresolved kimberlite carrier
 * cannot form one.
 */
public record SecondaryPlacerState(
    StableId systemId,
    FormationStatus status,
    PlacerFamily family,
    SourceBasis sourceBasis,
    List<StableId> sourceBodyIds,
    StableId transportProcessId,
    StableId trapId,
    AgeKey formationAge,
    TransportClass transportClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double envelopeHalfLengthBlocks,
    double envelopeHalfWidthBlocks,
    double profileThicknessBlocks,
    long sourceBudgetFixedUnits,
    long releasedBudgetFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    Map<String, Long> sourceIndicatorModesPpm,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  private static final long SCALE = MaterialAssemblage.SCALE;

  public SecondaryPlacerState {
    if (systemId == null
        || status == null
        || family == null
        || sourceBasis == null
        || sourceBodyIds == null
        || transportProcessId == null
        || trapId == null
        || formationAge == null
        || transportClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || sourceIndicatorModesPpm == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("secondary placer state must be complete");
    }
    requirePositive(envelopeHalfLengthBlocks, "envelopeHalfLengthBlocks");
    requirePositive(envelopeHalfWidthBlocks, "envelopeHalfWidthBlocks");
    requirePositive(profileThicknessBlocks, "profileThicknessBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()) {
      throw new IllegalArgumentException(
          "secondary placer source bodies must be non-empty and unique");
    }
    TreeMap<String, Long> indicators = new TreeMap<>();
    sourceIndicatorModesPpm.forEach(
        (id, amount) -> {
          if (id == null || id.isBlank() || amount == null || amount < 0L || amount > SCALE) {
            throw new IllegalArgumentException("secondary placer source indicators are invalid");
          }
          if (amount > 0L) {
            indicators.put(id, amount);
          }
        });
    sourceIndicatorModesPpm = Collections.unmodifiableMap(indicators);
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("secondary placer horizons cannot be null");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "secondary placer horizons must close to the trap allocation");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedBudgetFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedBudgetFixedUnits > sourceBudgetFixedUnits
        || depositAllocationFixedUnits > releasedBudgetFixedUnits
        || transportLossFixedUnits > releasedBudgetFixedUnits
        || Math.addExact(transportLossFixedUnits, depositAllocationFixedUnits)
            != releasedBudgetFixedUnits) {
      throw new IllegalArgumentException(
          "secondary placer source and trap budgets are inconsistent");
    }
    if (status == FormationStatus.FORMED) {
      if (sourceBasis == SourceBasis.NO_ELIGIBLE_SOURCE
          || transportClass != TransportClass.CONNECTED_FLUVIAL_CHANNEL
          || trapClass != TrapClass.HYDRAULIC_GRADIENT_BREAK
          || preservationClass != PreservationClass.PRESERVED_ALLUVIAL_BAR
          || sourceBudgetFixedUnits <= 0L
          || releasedBudgetFixedUnits <= 0L
          || depositAllocationFixedUnits <= 0L
          || sourceIndicatorModesPpm.isEmpty()
          || horizons.isEmpty()
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed secondary placers require source, transport, and trap evidence");
      }
    } else if (sourceBasis != SourceBasis.NO_ELIGIBLE_SOURCE
        || transportClass != TransportClass.NO_CONNECTED_TRANSPORT
        || trapClass != TrapClass.NO_ALLOWABLE_TRAP
        || preservationClass != PreservationClass.ERODED_OR_UNPRESERVED
        || sourceBudgetFixedUnits != 0L
        || releasedBudgetFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !sourceIndicatorModesPpm.isEmpty()
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed secondary placers must retain a failed gate");
    }
  }

  /** Derives one placer family from the immutable parent, drainage, and trap evidence. */
  public static SecondaryPlacerState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      PlacerFamily family) {
    return proofFor(
        province,
        identity,
        worldPoint,
        surface,
        parent,
        parent,
        new MineralSystemProofs().placerState(province),
        family);
  }

  /**
   * Derives one family while retaining a separately resolved upstream source parent and transport
   * proof. This keeps a downstream basin or channel parcel from being mistaken for its source.
   */
  public static SecondaryPlacerState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      PetrologicSample sourceParent,
      PlacerSystemState transportProof,
      PlacerFamily family) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null
        || sourceParent == null
        || transportProof == null
        || family == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, parent, source parent, transport, and family are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())
        || !province.id().equals(sourceParent.geology().provinceId())) {
      throw new IllegalArgumentException(
          "secondary placer parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("secondary placer surface point changed between stages");
    }

    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId =
        identity.stream(
                "geological",
                "secondary-placer:" + family.name().toLowerCase(java.util.Locale.ROOT),
                cell,
                0)
            .stableId();
    SourceEvidence source = sourceEvidence(province, identity, sourceParent, family);
    var drainage = surface.surface().fields().drainage();
    TransportClass transportClass =
        source.available
                && transportProof.status() == FormationStatus.FORMED
                && drainage.channel()
                && drainage.flowAccumulation() >= 0.12
            ? TransportClass.CONNECTED_FLUVIAL_CHANNEL
            : TransportClass.NO_CONNECTED_TRANSPORT;
    TrapClass trapClass =
        transportClass == TransportClass.CONNECTED_FLUVIAL_CHANNEL
                && transportProof.trapClass()
                    == PlacerSystemState.TrapClass.HYDRAULIC_GRADIENT_BREAK
                && drainage.hydraulicTrapScore() >= 0.42
            ? TrapClass.HYDRAULIC_GRADIENT_BREAK
            : TrapClass.NO_ALLOWABLE_TRAP;
    PreservationClass preservationClass =
        surface.context().kind() == SurfaceMaterialKind.ALLUVIAL_PLACER
                && surface.surface().surfaceMaterial() == Lithology.ALLUVIAL_GRAVEL
            ? PreservationClass.PRESERVED_ALLUVIAL_BAR
            : PreservationClass.ERODED_OR_UNPRESERVED;
    String failedGate =
        !source.available
            ? source.failedGate
            : transportClass == TransportClass.NO_CONNECTED_TRANSPORT
                ? "transport"
                : trapClass == TrapClass.NO_ALLOWABLE_TRAP
                    ? "trap"
                    : preservationClass == PreservationClass.ERODED_OR_UNPRESERVED
                        ? "preservation"
                        : null;
    if (failedGate != null) {
      return barren(
          province,
          identity,
          systemId,
          family,
          source.sourceBodyIds,
          transportProof,
          transportClass,
          trapClass,
          preservationClass,
          surface,
          failedGate);
    }

    long sourceBudget = source.sourceBudgetFixedUnits;
    long released = Math.multiplyExact(sourceBudget, family.releaseFractionPpm) / SCALE;
    long deposit = Math.multiplyExact(released, family.trapFractionPpm) / SCALE;
    long loss = Math.subtractExact(released, deposit);
    double thickness = family.profileThicknessBlocks;
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    List<Horizon> horizons =
        formedHorizons(
            province,
            identity,
            family,
            deposit,
            new CellKey("province", province.homeCell().x(), province.homeCell().z()));
    return new SecondaryPlacerState(
        systemId,
        FormationStatus.FORMED,
        family,
        source.sourceBasis,
        source.sourceBodyIds,
        province.proofIds().weatheringId(),
        transportProof.trapId(),
        family.formationAge,
        transportClass,
        trapClass,
        preservationClass,
        new Point3(
            localSurface.x(),
            surface.surface().fields().elevation() - thickness / 2.0,
            localSurface.z()),
        family.envelopeHalfLengthBlocks,
        family.envelopeHalfWidthBlocks,
        thickness,
        sourceBudget,
        released,
        loss,
        deposit,
        source.indicatorModesPpm,
        horizons,
        Optional.empty());
  }

  /** Returns whether a local block point lies inside the bounded placer envelope. */
  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > profileThicknessBlocks / 2.0) {
      return false;
    }
    double along = (localPoint.z() - localCenter.z()) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / envelopeHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  /** Returns the horizon at a local point, if the radial and depth gates pass. */
  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double along = (localPoint.z() - localCenter.z()) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / envelopeHalfWidthBlocks;
    double radial = StrictMath.sqrt(along * along + across * across);
    double top = localCenter.y() + profileThicknessBlocks / 2.0;
    double depth = (top - localPoint.y()) / profileThicknessBlocks;
    return horizons.stream()
        .filter(
            horizon -> horizon.containsDepth(depth) && radial <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  public long retainedSourceBudgetFixedUnits() {
    return sourceBudgetFixedUnits - releasedBudgetFixedUnits;
  }

  public long totalProfileAllocationFixedUnits() {
    return horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
  }

  private static SecondaryPlacerState barren(
      Province province,
      WorldIdentity identity,
      StableId systemId,
      PlacerFamily family,
      List<StableId> sourceBodyIds,
      PlacerSystemState transportProof,
      TransportClass transportClass,
      TrapClass trapClass,
      PreservationClass preservationClass,
      SurfacePetrologicSample surface,
      String failedGate) {
    double thickness = family.profileThicknessBlocks;
    Point2 point = surface.surface().fields().point();
    Point3 localSurface =
        province
            .frame()
            .toLocal(new Point3(point.x(), surface.surface().fields().elevation(), point.z()));
    return new SecondaryPlacerState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        family,
        SourceBasis.NO_ELIGIBLE_SOURCE,
        sourceBodyIds,
        province.proofIds().weatheringId(),
        transportProof.trapId(),
        new AgeKey(0.0, 0),
        TransportClass.NO_CONNECTED_TRANSPORT,
        TrapClass.NO_ALLOWABLE_TRAP,
        PreservationClass.ERODED_OR_UNPRESERVED,
        new Point3(
            localSurface.x(),
            surface.surface().fields().elevation() - thickness / 2.0,
            localSurface.z()),
        family.envelopeHalfLengthBlocks,
        family.envelopeHalfWidthBlocks,
        thickness,
        0L,
        0L,
        0L,
        0L,
        Map.of(),
        List.of(),
        Optional.of(failedGate));
  }

  private static SourceEvidence sourceEvidence(
      Province province, WorldIdentity identity, PetrologicSample parent, PlacerFamily family) {
    return switch (family) {
      case CASSITERITE -> {
        LctPegmatiteState lct = LctPegmatiteState.proofFor(province, identity);
        if (lct.status() != FormationStatus.FORMED
            || lct.fertilityClass() != LctPegmatiteState.FertilityClass.LCT_RARE_ELEMENT
            || lct.childAllocationFixedUnits() <= 0L) {
          yield SourceEvidence.unavailable(List.of(lct.childBodyId()), "cassiterite_source");
        }
        yield new SourceEvidence(
            true,
            SourceBasis.LCT_RESIDUAL_CASSITERITE_PROXY,
            List.of(lct.childBodyId()),
            lct.childAllocationFixedUnits(),
            Map.of("lct_child_allocation_proxy", lct.childAllocationFixedUnits()),
            null);
      }
      case HEAVY_MINERAL_SAND -> {
        Map<String, Long> durable = durableModes(parent);
        long total = durable.values().stream().mapToLong(Long::longValue).sum();
        if (total < 20_000L) {
          yield SourceEvidence.unavailable(
              List.of(parent.geology().rockBodyId()), "durable_heavy_mineral_source");
        }
        yield new SourceEvidence(
            true,
            SourceBasis.DURABLE_HEAVY_MINERAL_VECTOR,
            List.of(parent.geology().rockBodyId()),
            total,
            durable,
            null);
      }
      case DIAMOND -> {
        Optional<MantleCargoState> cargo = parent.mantleCargo();
        if (parent.geology().lithology() != Lithology.KIMBERLITIC
            || cargo.isEmpty()
            || cargo.orElseThrow().status() != MantleCargoStatus.DIAMOND_BEARING
            || cargo.orElseThrow().diamondGradePpbByMass() <= 0L) {
          yield SourceEvidence.unavailable(
              List.of(parent.geology().rockBodyId()), "diamondiferous_source");
        }
        MantleCargoState resolved = cargo.orElseThrow();
        yield new SourceEvidence(
            true,
            SourceBasis.DIAMONDIFEROUS_KIMBERLITE_CARGO,
            List.of(parent.geology().rockBodyId()),
            resolved.diamondGradePpbByMass(),
            Map.of(resolved.diamondMineralId(), resolved.diamondGradePpbByMass()),
            null);
      }
    };
  }

  private static Map<String, Long> durableModes(PetrologicSample parent) {
    List<String> durableIds =
        List.of(
            "geological:mineral/ilmenite",
            "geological:mineral/magnetite",
            "geological:mineral/hematite",
            "geological:mineral/chromite",
            "geological:mineral/pyrope",
            "geological:mineral/almandine",
            "geological:mineral/diamond",
            "geological:mineral/perovskite");
    TreeMap<String, Long> result = new TreeMap<>();
    for (String id : durableIds) {
      long amount = parent.resolvedAssemblage().modesPpm().getOrDefault(id, 0L);
      if (amount > 0L) {
        result.put(id, amount);
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private static List<Horizon> formedHorizons(
      Province province, WorldIdentity identity, PlacerFamily family, long deposit, CellKey cell) {
    long first = deposit * family.firstHorizonFractionPpm / SCALE;
    long second = deposit * family.secondHorizonFractionPpm / SCALE;
    long third = deposit - first - second;
    return List.of(
        horizon(family.firstHorizon, 0.0, 0.28, 0.92, first, identity, family, cell, 0),
        horizon(family.secondHorizon, 0.28, 0.72, 0.84, second, identity, family, cell, 1),
        horizon(family.thirdHorizon, 0.72, 1.0, 0.76, third, identity, family, cell, 2));
  }

  private static Horizon horizon(
      HorizonKind kind,
      double top,
      double bottom,
      double radius,
      long allocation,
      WorldIdentity identity,
      PlacerFamily family,
      CellKey cell,
      long index) {
    StableId bodyId =
        identity.stream(
                "geological",
                "secondary-placer-horizon:" + family.name().toLowerCase(java.util.Locale.ROOT),
                cell,
                index)
            .stableId();
    return new Horizon(kind, Overprint.NONE, top, bottom, radius, allocation, bodyId);
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("secondary placer horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("secondary placer horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  private record SourceEvidence(
      boolean available,
      SourceBasis sourceBasis,
      List<StableId> sourceBodyIds,
      long sourceBudgetFixedUnits,
      Map<String, Long> indicatorModesPpm,
      String failedGate) {
    private static SourceEvidence unavailable(List<StableId> sourceBodyIds, String failedGate) {
      return new SourceEvidence(
          false, SourceBasis.NO_ELIGIBLE_SOURCE, sourceBodyIds, 0L, Map.of(), failedGate);
    }
  }

  public enum PlacerFamily {
    CASSITERITE(
        650_000L,
        700_000L,
        0.08,
        56.0,
        22.0,
        8.0,
        420_000L,
        360_000L,
        HorizonKind.ELUVIAL_RELEASE,
        HorizonKind.CASSITERITE_BAR,
        HorizonKind.BASAL_LAG),
    HEAVY_MINERAL_SAND(
        600_000L,
        620_000L,
        0.05,
        76.0,
        28.0,
        10.0,
        300_000L,
        500_000L,
        HorizonKind.HEAVY_MINERAL_BAR,
        HorizonKind.BEACH_OR_FLOODPLAIN_REWORK,
        HorizonKind.BASAL_LAG),
    DIAMOND(
        750_000L,
        500_000L,
        0.02,
        64.0,
        24.0,
        12.0,
        250_000L,
        450_000L,
        HorizonKind.DIAMOND_LAG,
        HorizonKind.HYDRAULIC_BAR,
        HorizonKind.TERRACE_REWORK);

    private final long releaseFractionPpm;
    private final long trapFractionPpm;
    private final AgeKey formationAge;
    private final double envelopeHalfLengthBlocks;
    private final double envelopeHalfWidthBlocks;
    private final double profileThicknessBlocks;
    private final long firstHorizonFractionPpm;
    private final long secondHorizonFractionPpm;
    private final HorizonKind firstHorizon;
    private final HorizonKind secondHorizon;
    private final HorizonKind thirdHorizon;

    PlacerFamily(
        long releaseFractionPpm,
        long trapFractionPpm,
        double formationAgeMa,
        double envelopeHalfLengthBlocks,
        double envelopeHalfWidthBlocks,
        double profileThicknessBlocks,
        long firstHorizonFractionPpm,
        long secondHorizonFractionPpm,
        HorizonKind firstHorizon,
        HorizonKind secondHorizon,
        HorizonKind thirdHorizon) {
      this.releaseFractionPpm = releaseFractionPpm;
      this.trapFractionPpm = trapFractionPpm;
      this.formationAge = new AgeKey(formationAgeMa, 0);
      this.envelopeHalfLengthBlocks = envelopeHalfLengthBlocks;
      this.envelopeHalfWidthBlocks = envelopeHalfWidthBlocks;
      this.profileThicknessBlocks = profileThicknessBlocks;
      this.firstHorizonFractionPpm = firstHorizonFractionPpm;
      this.secondHorizonFractionPpm = secondHorizonFractionPpm;
      this.firstHorizon = firstHorizon;
      this.secondHorizon = secondHorizon;
      this.thirdHorizon = thirdHorizon;
    }
  }

  public enum SourceBasis {
    LCT_RESIDUAL_CASSITERITE_PROXY,
    DURABLE_HEAVY_MINERAL_VECTOR,
    DIAMONDIFEROUS_KIMBERLITE_CARGO,
    NO_ELIGIBLE_SOURCE
  }

  public enum TransportClass {
    CONNECTED_FLUVIAL_CHANNEL,
    NO_CONNECTED_TRANSPORT
  }

  public enum TrapClass {
    HYDRAULIC_GRADIENT_BREAK,
    NO_ALLOWABLE_TRAP
  }

  public enum PreservationClass {
    PRESERVED_ALLUVIAL_BAR,
    ERODED_OR_UNPRESERVED
  }

  public enum HorizonKind {
    ELUVIAL_RELEASE,
    CASSITERITE_BAR,
    HEAVY_MINERAL_BAR,
    BEACH_OR_FLOODPLAIN_REWORK,
    DIAMOND_LAG,
    HYDRAULIC_BAR,
    BASAL_LAG,
    TERRACE_REWORK
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
        throw new IllegalArgumentException("secondary placer horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0
          || allocationFixedUnits < 0L
          || allocationFixedUnits > SCALE) {
        throw new IllegalArgumentException("secondary placer horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
