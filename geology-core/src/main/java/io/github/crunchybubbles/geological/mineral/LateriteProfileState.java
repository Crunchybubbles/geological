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
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Source-gated lateritic weathering profile for one bounded Overworld surface parcel.
 *
 * <p>This is a normalized process proof, not a grade or an absolute tonnage. Aluminum uses the
 * resolved parent composition directly. Ni-Co values are deliberately marked as an ultramafic proxy
 * because the initial Phase 2 element vocabulary does not yet include Ni or Co. No profile forms
 * without its required parent, weathering, drainage, and preservation evidence.
 */
public record LateriteProfileState(
    StableId systemId,
    FormationStatus status,
    ProfileKind profileKind,
    StableId parentBodyId,
    StableId weatheringProcessId,
    AgeKey formationAge,
    ParentClass parentClass,
    SourceBasis sourceBasis,
    ClimateClass climateClass,
    DrainageClass drainageClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double blanketHalfLengthBlocks,
    double blanketHalfWidthBlocks,
    double profileThicknessBlocks,
    List<Horizon> horizons,
    List<CommodityBudget> commodityBudgets,
    Optional<String> failedGate) {
  private static final long SCALE = 1_000_000L;

  public LateriteProfileState {
    if (systemId == null
        || status == null
        || profileKind == null
        || parentBodyId == null
        || weatheringProcessId == null
        || formationAge == null
        || parentClass == null
        || sourceBasis == null
        || climateClass == null
        || drainageClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || commodityBudgets == null
        || failedGate == null) {
      throw new IllegalArgumentException("laterite profile state must be complete");
    }
    requirePositive(blanketHalfLengthBlocks, "blanketHalfLengthBlocks");
    requirePositive(blanketHalfWidthBlocks, "blanketHalfWidthBlocks");
    requirePositive(profileThicknessBlocks, "profileThicknessBlocks");
    horizons = List.copyOf(horizons);
    commodityBudgets =
        List.copyOf(commodityBudgets).stream()
            .sorted(java.util.Comparator.comparing(CommodityBudget::commodity))
            .toList();
    if (horizons.stream().anyMatch(horizon -> horizon == null)
        || commodityBudgets.stream().anyMatch(budget -> budget == null)) {
      throw new IllegalArgumentException("laterite profile components cannot be null");
    }
    validateHorizonSequence(horizons);
    if (commodityBudgets.stream().map(CommodityBudget::commodity).distinct().count()
        != commodityBudgets.size()) {
      throw new IllegalArgumentException("laterite commodity budgets must be unique");
    }
    for (CommodityBudget budget : commodityBudgets) {
      long horizonAllocation =
          horizons.stream()
              .mapToLong(horizon -> horizon.allocationFixedUnits(budget.commodity()))
              .sum();
      if (horizonAllocation != budget.residualAllocationFixedUnits()) {
        throw new IllegalArgumentException(
            "laterite horizon allocation must close for " + budget.commodity());
      }
    }
    if (status == FormationStatus.FORMED) {
      if (profileKind == ProfileKind.NONE
          || parentClass == ParentClass.NO_ELIGIBLE_PARENT
          || sourceBasis == SourceBasis.NONE
          || climateClass != ClimateClass.WARM_HUMID_INTENSE
          || drainageClass == DrainageClass.NO_USABLE_PATH
          || preservationClass != PreservationClass.PRESERVED_LOW_RELIEF
          || commodityBudgets.isEmpty()
          || horizons.isEmpty()
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed laterite requires all source and preservation gates");
      }
    } else if (profileKind != ProfileKind.NONE && parentClass == ParentClass.NO_ELIGIBLE_PARENT
        || sourceBasis != SourceBasis.NONE
        || !horizons.isEmpty()
        || !commodityBudgets.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren laterite must retain a failed hard gate");
    }
  }

  /** Derives one profile from the parent exposed by the current surface parcel. */
  public static LateriteProfileState proofFor(
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
      throw new IllegalArgumentException("laterite parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("laterite surface point changed between stages");
    }

    Lithology lithology = parent.geology().lithology();
    ProfileKind kind = profileKind(lithology);
    ParentClass parentClass = parentClass(lithology);
    SourceBasis sourceBasis = sourceBasis(kind);
    long aluminumSource =
        parent.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.AL, 0L);
    List<CommodityBudget> sourceBudgets =
        switch (kind) {
          case BAUXITE ->
              aluminumSource >= 100_000L ? List.of(bauxiteBudget(aluminumSource)) : List.of();
          case NI_CO_LATERITE -> ultramaficBudgets(parent);
          case NONE -> List.of();
        };

    var fields = surface.surface().fields();
    ClimateClass climate =
        fields.weatheringDepth() >= 8.0
            ? ClimateClass.WARM_HUMID_INTENSE
            : ClimateClass.INSUFFICIENT_WEATHERING;
    DrainageClass drainage =
        fields.drainage().channel()
            ? DrainageClass.NO_USABLE_PATH
            : fields.drainage().flowAccumulation() <= 0.82
                ? DrainageClass.PERCOLATING_DRAINAGE
                : DrainageClass.NO_USABLE_PATH;
    double maximumSlope = kind == ProfileKind.BAUXITE ? 0.18 : 0.26;
    PreservationClass preservation =
        surface.context().kind() == SurfaceMaterialKind.IN_SITU_REGOLITH
                && !fields.outcrop()
                && fields.slope() <= maximumSlope
            ? PreservationClass.PRESERVED_LOW_RELIEF
            : PreservationClass.STRIPPED_OR_COVERED;
    String failedGate =
        sourceBudgets.isEmpty()
            ? (kind == ProfileKind.NONE ? "parent" : "source_inventory")
            : climate != ClimateClass.WARM_HUMID_INTENSE
                ? "climate"
                : drainage == DrainageClass.NO_USABLE_PATH
                    ? "drainage"
                    : preservation != PreservationClass.PRESERVED_LOW_RELIEF
                        ? "preservation"
                        : null;
    boolean formed = failedGate == null;
    if (formed) {
      long blockX = (long) StrictMath.floor(worldPoint.x());
      long blockZ = (long) StrictMath.floor(worldPoint.z());
      CellKey cell = CellKey.containing("block", blockX, blockZ, 1);
      StableId systemId =
          identity.stream(
                  "geological",
                  "laterite-system:" + kind.name().toLowerCase(java.util.Locale.ROOT),
                  cell,
                  0)
              .stableId();
      double thickness = kind == ProfileKind.BAUXITE ? 64.0 : 72.0;
      Point3 localSurface =
          province.frame().toLocal(new Point3(worldPoint.x(), fields.elevation(), worldPoint.z()));
      List<Horizon> horizons = formedHorizons(kind, identity, cell, sourceBudgets);
      return new LateriteProfileState(
          systemId,
          FormationStatus.FORMED,
          kind,
          parent.geology().rockBodyId(),
          province.proofIds().weatheringId(),
          new AgeKey(kind == ProfileKind.BAUXITE ? 3.0 : 2.0, 0),
          parentClass,
          sourceBasis,
          climate,
          drainage,
          preservation,
          new Point3(localSurface.x(), fields.elevation() - thickness / 2.0, localSurface.z()),
          kind == ProfileKind.BAUXITE ? 104.0 : 112.0,
          kind == ProfileKind.BAUXITE ? 84.0 : 92.0,
          thickness,
          horizons,
          sourceBudgets,
          Optional.empty());
    }

    long blockX = (long) StrictMath.floor(worldPoint.x());
    long blockZ = (long) StrictMath.floor(worldPoint.z());
    CellKey cell = CellKey.containing("block", blockX, blockZ, 1);
    StableId systemId =
        identity.stream(
                "geological",
                "laterite-system:" + kind.name().toLowerCase(java.util.Locale.ROOT),
                cell,
                0)
            .stableId();
    double thickness = kind == ProfileKind.BAUXITE ? 64.0 : 72.0;
    Point3 localSurface =
        province.frame().toLocal(new Point3(worldPoint.x(), fields.elevation(), worldPoint.z()));
    return new LateriteProfileState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        kind,
        parent.geology().rockBodyId(),
        province.proofIds().weatheringId(),
        new AgeKey(0.0, 0),
        parentClass,
        SourceBasis.NONE,
        climate,
        drainage,
        preservation,
        new Point3(localSurface.x(), fields.elevation() - thickness / 2.0, localSurface.z()),
        kind == ProfileKind.BAUXITE ? 104.0 : 112.0,
        kind == ProfileKind.BAUXITE ? 84.0 : 92.0,
        thickness,
        List.of(),
        List.of(),
        Optional.of(failedGate));
  }

  /** Returns whether a local point lies in this profile envelope. */
  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > profileThicknessBlocks / 2.0) {
      return false;
    }
    double along = (localPoint.z() - localCenter.z()) / blanketHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / blanketHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  /** Returns the laterite horizon containing a local point, if its radial gate passes. */
  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double along = (localPoint.z() - localCenter.z()) / blanketHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / blanketHalfWidthBlocks;
    double radial = StrictMath.sqrt(along * along + across * across);
    double top = localCenter.y() + profileThicknessBlocks / 2.0;
    double depth = (top - localPoint.y()) / profileThicknessBlocks;
    return horizons.stream()
        .filter(
            horizon -> horizon.containsDepth(depth) && radial <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  public long totalSourceFixedUnits() {
    return commodityBudgets.stream().mapToLong(CommodityBudget::sourceFixedUnits).sum();
  }

  public long totalResidualAllocationFixedUnits() {
    return commodityBudgets.stream().mapToLong(CommodityBudget::residualAllocationFixedUnits).sum();
  }

  public long totalDissolvedLossFixedUnits() {
    return commodityBudgets.stream().mapToLong(CommodityBudget::dissolvedLossFixedUnits).sum();
  }

  private static ProfileKind profileKind(Lithology lithology) {
    if (lithology == Lithology.KOMATIITIC_ULTRAMAFIC || lithology == Lithology.SERPENTINITE) {
      return ProfileKind.NI_CO_LATERITE;
    }
    return switch (lithology) {
      case GRANITIC_GNEISS,
          BASALTIC,
          ANDESITIC,
          RHYOLITIC,
          ALKALINE,
          PYROCLASTIC,
          DIORITE_PULSE,
          GRANODIORITE_PULSE,
          FELSIC_STOCK,
          BASIN_SHALE,
          MARINE_VOLCANICLASTIC,
          SILTSTONE ->
          ProfileKind.BAUXITE;
      default -> ProfileKind.NONE;
    };
  }

  private static ParentClass parentClass(Lithology lithology) {
    if (lithology == Lithology.KOMATIITIC_ULTRAMAFIC || lithology == Lithology.SERPENTINITE) {
      return ParentClass.ULTRAMAFIC_PARENT;
    }
    return profileKind(lithology) == ProfileKind.BAUXITE
        ? ParentClass.ALUMINOUS_SILICATE_PARENT
        : ParentClass.NO_ELIGIBLE_PARENT;
  }

  private static SourceBasis sourceBasis(ProfileKind kind) {
    return switch (kind) {
      case BAUXITE -> SourceBasis.PARENT_ALUMINUM_MASS_PPM;
      case NI_CO_LATERITE -> SourceBasis.ULTRAMAFIC_NI_CO_PROXY;
      case NONE -> SourceBasis.NONE;
    };
  }

  private static CommodityBudget bauxiteBudget(long source) {
    long residual = Math.multiplyExact(source, 650_000L) / SCALE;
    return new CommodityBudget(
        Commodity.ALUMINUM, source, residual, Math.subtractExact(source, residual));
  }

  private static List<CommodityBudget> ultramaficBudgets(PetrologicSample parent) {
    long magnesium =
        parent.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.MG, 0L);
    long iron = parent.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.FE, 0L);
    long chromium =
        parent.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.CR, 0L);
    long nickelSource = clamp(30_000L + magnesium / 4L + chromium / 2L, 20_000L, 400_000L);
    long cobaltSource = clamp(4_000L + iron / 20L + chromium / 15L, 5_000L, 100_000L);
    long nickelResidual = Math.multiplyExact(nickelSource, 560_000L) / SCALE;
    long cobaltResidual = Math.multiplyExact(cobaltSource, 620_000L) / SCALE;
    return List.of(
        new CommodityBudget(
            Commodity.NICKEL,
            nickelSource,
            nickelResidual,
            Math.subtractExact(nickelSource, nickelResidual)),
        new CommodityBudget(
            Commodity.COBALT,
            cobaltSource,
            cobaltResidual,
            Math.subtractExact(cobaltSource, cobaltResidual)));
  }

  private static List<Horizon> formedHorizons(
      ProfileKind kind, WorldIdentity identity, CellKey cell, List<CommodityBudget> budgets) {
    return switch (kind) {
      case BAUXITE ->
          List.of(
              horizon(
                  HorizonKind.FERRICRETE_CAP,
                  Overprint.WEATHERED_REGOLITH,
                  0.0,
                  0.18,
                  0.92,
                  Map.of(),
                  identity,
                  cell,
                  0),
              horizon(
                  HorizonKind.PISOLITIC_BAUXITE,
                  Overprint.WEATHERED_REGOLITH,
                  0.18,
                  0.58,
                  0.84,
                  Map.of(Commodity.ALUMINUM, budgets.getFirst().residualAllocationFixedUnits()),
                  identity,
                  cell,
                  1),
              horizon(
                  HorizonKind.KAOLINITIC_TRANSITION,
                  Overprint.WEATHERED_REGOLITH,
                  0.58,
                  1.0,
                  0.74,
                  Map.of(),
                  identity,
                  cell,
                  2));
      case NI_CO_LATERITE -> {
        CommodityBudget nickel = budgets.getFirst();
        CommodityBudget cobalt = budgets.getLast();
        long nickelLimonite = nickel.residualAllocationFixedUnits() * 350_000L / SCALE;
        long nickelSmectite = nickel.residualAllocationFixedUnits() * 250_000L / SCALE;
        long nickelSaprolite =
            nickel.residualAllocationFixedUnits() - nickelLimonite - nickelSmectite;
        long cobaltLimonite = cobalt.residualAllocationFixedUnits() * 550_000L / SCALE;
        long cobaltSmectite = cobalt.residualAllocationFixedUnits() * 100_000L / SCALE;
        long cobaltSaprolite =
            cobalt.residualAllocationFixedUnits() - cobaltLimonite - cobaltSmectite;
        yield List.of(
            horizon(
                HorizonKind.FERRICRETE_CAP,
                Overprint.WEATHERED_REGOLITH,
                0.0,
                0.15,
                0.95,
                Map.of(),
                identity,
                cell,
                0),
            horizon(
                HorizonKind.NI_CO_LIMONITE,
                Overprint.WEATHERED_REGOLITH,
                0.15,
                0.45,
                0.86,
                Map.of(Commodity.NICKEL, nickelLimonite, Commodity.COBALT, cobaltLimonite),
                identity,
                cell,
                1),
            horizon(
                HorizonKind.SMECTITE_TRANSITION,
                Overprint.WEATHERED_REGOLITH,
                0.45,
                0.65,
                0.78,
                Map.of(Commodity.NICKEL, nickelSmectite, Commodity.COBALT, cobaltSmectite),
                identity,
                cell,
                2),
            horizon(
                HorizonKind.SAPROLITE,
                Overprint.WEATHERED_REGOLITH,
                0.65,
                1.0,
                0.72,
                Map.of(Commodity.NICKEL, nickelSaprolite, Commodity.COBALT, cobaltSaprolite),
                identity,
                cell,
                3));
      }
      case NONE -> List.of();
    };
  }

  private static Horizon horizon(
      HorizonKind kind,
      Overprint overprint,
      double top,
      double bottom,
      double radius,
      Map<Commodity, Long> allocation,
      WorldIdentity identity,
      CellKey cell,
      long index) {
    StableId bodyId = identity.stream("geological", "laterite-horizon", cell, index).stableId();
    return new Horizon(kind, overprint, top, bottom, radius, allocation, bodyId);
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (horizon.topDepthFraction() < previousBottom - 1.0e-12
          || Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("laterite horizons must form a contiguous profile");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("laterite horizons must cover the normalized profile");
    }
  }

  private static long clamp(long value, long minimum, long maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum ProfileKind {
    BAUXITE,
    NI_CO_LATERITE,
    NONE
  }

  public enum ParentClass {
    ALUMINOUS_SILICATE_PARENT,
    ULTRAMAFIC_PARENT,
    NO_ELIGIBLE_PARENT
  }

  public enum SourceBasis {
    PARENT_ALUMINUM_MASS_PPM,
    ULTRAMAFIC_NI_CO_PROXY,
    NONE
  }

  public enum ClimateClass {
    WARM_HUMID_INTENSE,
    INSUFFICIENT_WEATHERING
  }

  public enum DrainageClass {
    PERCOLATING_DRAINAGE,
    NO_USABLE_PATH
  }

  public enum PreservationClass {
    PRESERVED_LOW_RELIEF,
    STRIPPED_OR_COVERED
  }

  public enum Commodity {
    ALUMINUM,
    NICKEL,
    COBALT
  }

  public enum HorizonKind {
    FERRICRETE_CAP,
    PISOLITIC_BAUXITE,
    KAOLINITIC_TRANSITION,
    NI_CO_LIMONITE,
    SMECTITE_TRANSITION,
    SAPROLITE
  }

  public record CommodityBudget(
      Commodity commodity,
      long sourceFixedUnits,
      long residualAllocationFixedUnits,
      long dissolvedLossFixedUnits) {
    public CommodityBudget {
      if (commodity == null
          || sourceFixedUnits <= 0L
          || sourceFixedUnits > SCALE
          || residualAllocationFixedUnits <= 0L
          || residualAllocationFixedUnits > sourceFixedUnits
          || dissolvedLossFixedUnits < 0L
          || Math.addExact(residualAllocationFixedUnits, dissolvedLossFixedUnits)
              != sourceFixedUnits) {
        throw new IllegalArgumentException("laterite commodity budget is inconsistent");
      }
    }
  }

  public record Horizon(
      HorizonKind kind,
      Overprint overprint,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      Map<Commodity, Long> allocationFixedUnits,
      StableId bodyId) {
    public Horizon {
      if (kind == null || overprint == null || allocationFixedUnits == null || bodyId == null) {
        throw new IllegalArgumentException("laterite horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("laterite horizon bounds are invalid");
      }
      EnumMap<Commodity, Long> sorted = new EnumMap<>(Commodity.class);
      allocationFixedUnits.forEach(
          (commodity, amount) -> {
            if (commodity == null || amount == null || amount < 0L || amount > SCALE) {
              throw new IllegalArgumentException("laterite horizon allocations are invalid");
            }
            if (amount > 0L) {
              sorted.put(commodity, amount);
            }
          });
      allocationFixedUnits = Collections.unmodifiableMap(sorted);
    }

    public long allocationFixedUnits(Commodity commodity) {
      if (commodity == null) {
        throw new IllegalArgumentException("laterite commodity is required");
      }
      return allocationFixedUnits.getOrDefault(commodity, 0L);
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
