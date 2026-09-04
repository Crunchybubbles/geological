package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentaryBasinState;
import io.github.crunchybubbles.geological.petrology.SedimentaryReservoirState;
import io.github.crunchybubbles.geological.petrology.SedimentaryState;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import io.github.crunchybubbles.geological.worldgen.SedimentaryResourceHostPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Source-gated phosphorite, manganese, coal, brine, and helium-resource proxy state. */
public record SedimentaryResourceSystemState(
    StableId systemId,
    FormationStatus status,
    ResourceFamily family,
    StableId basinId,
    StableId sourceBodyId,
    StableId structureId,
    StableId hostBodyId,
    List<StableId> sourceBodyIds,
    AgeKey formationAge,
    String faciesClass,
    SalinityClass salinityClass,
    RedoxClass redoxClass,
    BasinSetting basinSetting,
    SourceClass sourceClass,
    HostClass hostClass,
    PathwayClass pathwayClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double lateralExtentBlocks,
    double verticalExtentBlocks,
    long sourceBudgetFixedUnits,
    long releasedResourceFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public SedimentaryResourceSystemState {
    if (systemId == null
        || status == null
        || family == null
        || basinId == null
        || sourceBodyId == null
        || structureId == null
        || hostBodyId == null
        || sourceBodyIds == null
        || formationAge == null
        || faciesClass == null
        || faciesClass.isBlank()
        || salinityClass == null
        || redoxClass == null
        || basinSetting == null
        || sourceClass == null
        || hostClass == null
        || pathwayClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("sedimentary resource state must be complete");
    }
    requirePositive(lateralExtentBlocks, "lateralExtentBlocks");
    requirePositive(verticalExtentBlocks, "verticalExtentBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(basinId)
        || !sourceBodyIds.contains(sourceBodyId)
        || !sourceBodyIds.contains(structureId)
        || !sourceBodyIds.contains(hostBodyId)) {
      throw new IllegalArgumentException(
          "sedimentary resource sources must retain basin, source, structure, and host");
    }
    if (sourceBudgetFixedUnits < 0L
        || releasedResourceFixedUnits < 0L
        || transportLossFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || releasedResourceFixedUnits > sourceBudgetFixedUnits
        || releasedResourceFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("sedimentary resource ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("sedimentary resource horizons cannot be null");
    }
    if (horizons.stream().map(Horizon::kind).distinct().count() != horizons.size()
        || horizons.stream().map(Horizon::bodyId).distinct().count() != horizons.size()) {
      throw new IllegalArgumentException("sedimentary resource horizons must be unique");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != depositAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "sedimentary resource horizons must close to deposit budget");
    }
    if (status == FormationStatus.FORMED) {
      if (family == ResourceFamily.NONE
          || basinSetting == BasinSetting.NONE
          || sourceClass == SourceClass.NO_RESOURCE_SOURCE
          || hostClass == HostClass.NO_RECEPTIVE_HOST
          || pathwayClass == PathwayClass.NO_CONNECTED_PATH
          || trapClass == TrapClass.NO_RESOURCE_TRAP
          || preservationClass == PreservationClass.ERODED_OR_COVERED
          || sourceBudgetFixedUnits == 0L
          || releasedResourceFixedUnits == 0L
          || depositAllocationFixedUnits == 0L
          || horizons.size() != 3
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed sedimentary resources require source, host, path, trap, and preservation proof");
      }
    } else if (family != ResourceFamily.NONE
        || basinSetting != BasinSetting.NONE
        || sourceClass != SourceClass.NO_RESOURCE_SOURCE
        || hostClass != HostClass.NO_RECEPTIVE_HOST
        || pathwayClass != PathwayClass.NO_CONNECTED_PATH
        || trapClass != TrapClass.NO_RESOURCE_TRAP
        || preservationClass != PreservationClass.ERODED_OR_COVERED
        || salinityClass != SalinityClass.FRESH
        || redoxClass != RedoxClass.BUFFERED
        || sourceBudgetFixedUnits != 0L
        || releasedResourceFixedUnits != 0L
        || transportLossFixedUnits != 0L
        || depositAllocationFixedUnits != 0L
        || !horizons.isEmpty()
        || !formationAge.equals(new AgeKey(0.0, 0))
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "barren sedimentary resources must retain the failed gate");
    }
  }

  /** Derives a deterministic resource proof from basin context and an explicit host policy. */
  public static SedimentaryResourceSystemState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      SedimentaryResourceHostPolicy.HostEvidence host) {
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
          "sedimentary resource parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException(
          "sedimentary resource surface point changed between stages");
    }
    RiftArcGeometry geometry = province.geometry();
    Optional<SedimentaryState> sedimentary = parent.sedimentaryState();
    SedimentaryBasinState basinState =
        sedimentary
            .map(SedimentaryState::basinState)
            .orElseGet(
                () ->
                    host.fixture()
                        ? SedimentaryBasinState.proofFor(
                            host.faciesClass(), List.of(geometry.basementId()))
                        : null);
    ResourceFamily family = family(host, sedimentary);
    StableId sourceBodyId =
        identity.stream("geological", "sedimentary-resource-source", province.homeCell(), 0)
            .stableId();
    StableId systemId =
        identity.stream("geological", "sedimentary-resource-system", province.homeCell(), 0)
            .stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    geometry.basin().id(), sourceBodyId, geometry.fault().id(), host.hostBodyId()),
                java.util.stream.Stream.concat(
                    parent.geology().depositIds().stream(),
                    parent.geology().rockBodyId().equals(host.hostBodyId())
                        ? java.util.stream.Stream.empty()
                        : java.util.stream.Stream.of(parent.geology().rockBodyId())))
            .distinct()
            .sorted()
            .toList();
    String facies = host.faciesClass();
    SalinityClass salinity = basinState == null ? SalinityClass.FRESH : basinState.salinityClass();
    RedoxClass redox = basinState == null ? RedoxClass.BUFFERED : basinState.redoxClass();
    BasinSetting basinSetting = basinSetting(family);
    SourceClass sourceClass = sourceClass(family, basinState, parent, host.fixture());
    HostClass hostClass = hostClass(family);
    PathwayClass pathwayClass =
        pathwayClass(family, geometry, host.localCenter(), parent, host.fixture());
    TrapClass trapClass = trapClass(family, basinState, parent, host.fixture());
    PreservationClass preservationClass = preservationClass(surface);
    String failedGate =
        family == ResourceFamily.NONE
            ? "sedimentary_resource_host"
            : basinState == null
                ? "basin_context"
                : sourceClass == SourceClass.NO_RESOURCE_SOURCE
                    ? "resource_source"
                    : hostClass == HostClass.NO_RECEPTIVE_HOST
                        ? "receptive_host"
                        : pathwayClass == PathwayClass.NO_CONNECTED_PATH
                            ? "basin_pathway"
                            : trapClass == TrapClass.NO_RESOURCE_TRAP
                                ? "resource_trap"
                                : preservationClass == PreservationClass.ERODED_OR_COVERED
                                    ? "preservation"
                                    : null;
    Point3 center =
        new Point3(host.localCenter().x(), host.localCenter().y() - 32.0, host.localCenter().z());
    if (failedGate != null) {
      return barren(
          systemId,
          geometry.basin().id(),
          sourceBodyId,
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          facies,
          center,
          failedGate);
    }
    long sourceBudget = sourceBudget(family, basinState, parent, host.fixture());
    if (sourceBudget <= 0L) {
      return barren(
          systemId,
          geometry.basin().id(),
          sourceBodyId,
          geometry.fault().id(),
          host.hostBodyId(),
          sourceBodyIds,
          facies,
          center,
          "resource_inventory");
    }
    long released = Math.round(sourceBudget * 0.74);
    long deposit = Math.round(released * depositFraction(family));
    long loss = released - deposit;
    return new SedimentaryResourceSystemState(
        systemId,
        FormationStatus.FORMED,
        family,
        geometry.basin().id(),
        sourceBodyId,
        geometry.fault().id(),
        host.hostBodyId(),
        sourceBodyIds,
        formationAge(family),
        facies,
        salinity,
        redox,
        basinSetting,
        sourceClass,
        hostClass,
        pathwayClass,
        trapClass,
        preservationClass,
        center,
        lateralExtent(family),
        verticalExtent(family),
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

  private static ResourceFamily family(
      SedimentaryResourceHostPolicy.HostEvidence host, Optional<SedimentaryState> sedimentary) {
    if (host.fixture()) {
      return host.fixtureFamily();
    }
    if (sedimentary.isEmpty()) {
      return ResourceFamily.NONE;
    }
    String facies = sedimentary.orElseThrow().faciesClass();
    return switch (host.hostLithology()) {
      case COAL -> facies.equals("buried_peat_mire") ? ResourceFamily.COAL : ResourceFamily.NONE;
      case HALITE_POTASH_EVAPORITE ->
          facies.equals("restricted_evaporite_basin_center")
              ? ResourceFamily.POTASH_BORATE_BRINE
              : ResourceFamily.NONE;
      case BASIN_SHALE ->
          facies.equals("offshore_low_energy")
                  && sedimentary.orElseThrow().basinState().redoxClass()
                      != RedoxClass.STRONGLY_OXIDIZING
              ? ResourceFamily.SEDIMENTARY_MANGANESE
              : ResourceFamily.NONE;
      case BASIN_SANDSTONE, LIMESTONE, DOLOSTONE ->
          facies.equals("shallow_marine_shoreface") || facies.equals("carbonate_platform")
              ? ResourceFamily.PHOSPHORITE
              : ResourceFamily.NONE;
      default -> ResourceFamily.NONE;
    };
  }

  private static BasinSetting basinSetting(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE -> BasinSetting.MARINE_UPWELLING_SHELF;
      case SEDIMENTARY_MANGANESE -> BasinSetting.ANOXIC_MARGIN_BASIN;
      case COAL -> BasinSetting.PEAT_MIRE_BASIN;
      case LITHIUM_BRINE -> BasinSetting.CLOSED_SALAR_BASIN;
      case POTASH_BORATE_BRINE -> BasinSetting.RESTRICTED_EVAPORITE_BASIN;
      case HELIUM_GAS -> BasinSetting.RADIOGENIC_GAS_BASIN;
      case NONE -> BasinSetting.NONE;
    };
  }

  private static SourceClass sourceClass(
      ResourceFamily family,
      SedimentaryBasinState basin,
      PetrologicSample parent,
      boolean fixture) {
    if (basin == null) {
      return SourceClass.NO_RESOURCE_SOURCE;
    }
    SedimentaryReservoirState reservoir =
        parent.sedimentaryState().map(SedimentaryState::reservoirState).orElse(null);
    return switch (family) {
      case PHOSPHORITE ->
          basin.salinityClass() == SalinityClass.SEAWATER
                  && basin.carbonateProductivityPpm() >= 80_000L
              ? SourceClass.MARINE_PHOSPHORUS_CYCLING
              : SourceClass.NO_RESOURCE_SOURCE;
      case SEDIMENTARY_MANGANESE ->
          basin.salinityClass() == SalinityClass.SEAWATER
                  && basin.redoxClass() != RedoxClass.STRONGLY_OXIDIZING
              ? SourceClass.DISSOLVED_MANGANESE_BASIN_SOURCE
              : SourceClass.NO_RESOURCE_SOURCE;
      case COAL ->
          basin.redoxClass() == RedoxClass.STRONGLY_REDUCING
                  && (fixture
                      || reservoir != null && reservoir.organicCarbonCapacityPpm() >= 300_000L)
              ? SourceClass.ORGANIC_PEAT_INPUT
              : SourceClass.NO_RESOURCE_SOURCE;
      case LITHIUM_BRINE ->
          fixture || basin.salinityClass() == SalinityClass.HYPERSALINE
              ? SourceClass.LITHIUM_RICH_VOLCANIC_WEATHERING
              : SourceClass.NO_RESOURCE_SOURCE;
      case POTASH_BORATE_BRINE ->
          basin.salinityClass() == SalinityClass.HYPERSALINE
              ? SourceClass.POTASH_BORATE_BRINE_EVAPORATION
              : SourceClass.NO_RESOURCE_SOURCE;
      case HELIUM_GAS ->
          fixture ? SourceClass.RADIOGENIC_HELIUM_RELEASE : SourceClass.NO_RESOURCE_SOURCE;
      case NONE -> SourceClass.NO_RESOURCE_SOURCE;
    };
  }

  private static HostClass hostClass(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE -> HostClass.PHOSPHATIC_SHELF_BED;
      case SEDIMENTARY_MANGANESE -> HostClass.CONDENSED_MARINE_SEDIMENT;
      case COAL -> HostClass.PEAT_BEARING_COAL_SEAM;
      case LITHIUM_BRINE -> HostClass.BRINE_RESERVOIR_AQUIFER;
      case POTASH_BORATE_BRINE -> HostClass.HALITE_POTASH_SEQUENCE;
      case HELIUM_GAS -> HostClass.POROUS_GAS_RESERVOIR;
      case NONE -> HostClass.NO_RECEPTIVE_HOST;
    };
  }

  private static PathwayClass pathwayClass(
      ResourceFamily family,
      RiftArcGeometry geometry,
      Point3 localPoint,
      PetrologicSample parent,
      boolean fixture) {
    boolean insideBasin =
        geometry.basin().footprintValue(new Point2(localPoint.x(), localPoint.z())) < 1.0;
    boolean connected =
        fixture
            || insideBasin
                && (geometry.fault().intersectsDamageZone(localPoint)
                    || parent.permeabilityIndex() >= 0.18);
    if (!connected || family == ResourceFamily.NONE) {
      return PathwayClass.NO_CONNECTED_PATH;
    }
    return switch (family) {
      case PHOSPHORITE -> PathwayClass.UPWELLING_REWORKING;
      case SEDIMENTARY_MANGANESE -> PathwayClass.REDOX_INTERFACE;
      case COAL -> PathwayClass.MIRE_ACCOMMODATION;
      case LITHIUM_BRINE -> PathwayClass.CLOSED_BASIN_GROUNDWATER;
      case POTASH_BORATE_BRINE -> PathwayClass.BRINE_EVAPORATION;
      case HELIUM_GAS -> PathwayClass.GAS_MIGRATION;
      case NONE -> PathwayClass.NO_CONNECTED_PATH;
    };
  }

  private static TrapClass trapClass(
      ResourceFamily family,
      SedimentaryBasinState basin,
      PetrologicSample parent,
      boolean fixture) {
    if (basin == null) {
      return TrapClass.NO_RESOURCE_TRAP;
    }
    SedimentaryReservoirState reservoir =
        parent.sedimentaryState().map(SedimentaryState::reservoirState).orElse(null);
    return switch (family) {
      case PHOSPHORITE ->
          fixture
                  || (basin.carbonateProductivityPpm() >= 80_000L
                      && basin.clasticDilutionPpm() <= 800_000L)
              ? TrapClass.PHOSPHATE_REWORKING
              : TrapClass.NO_RESOURCE_TRAP;
      case SEDIMENTARY_MANGANESE ->
          fixture
                  || (basin.redoxClass() == RedoxClass.REDUCING
                      && basin.clasticDilutionPpm() <= 800_000L)
              ? TrapClass.MANGANESE_REDOX_PRECIPITATION
              : TrapClass.NO_RESOURCE_TRAP;
      case COAL ->
          basin.redoxClass() == RedoxClass.STRONGLY_REDUCING
                  && (fixture
                      || reservoir != null && reservoir.organicCarbonCapacityPpm() >= 300_000L)
              ? TrapClass.ANOXIC_PEAT_PRESERVATION
              : TrapClass.NO_RESOURCE_TRAP;
      case LITHIUM_BRINE ->
          fixture || basin.salinityClass() == SalinityClass.HYPERSALINE
              ? TrapClass.RESIDUAL_LI_CONCENTRATION
              : TrapClass.NO_RESOURCE_TRAP;
      case POTASH_BORATE_BRINE ->
          basin.salinityClass() == SalinityClass.HYPERSALINE
              ? TrapClass.LATE_POTASH_BORATE_SALT
              : TrapClass.NO_RESOURCE_TRAP;
      case HELIUM_GAS -> fixture ? TrapClass.RESERVOIR_SEAL_CLOSURE : TrapClass.NO_RESOURCE_TRAP;
      case NONE -> TrapClass.NO_RESOURCE_TRAP;
    };
  }

  private static PreservationClass preservationClass(SurfacePetrologicSample surface) {
    var fields = surface.surface().fields();
    return surface.context().kind() != SurfaceMaterialKind.ALLUVIAL_PLACER && fields.slope() <= 0.72
        ? PreservationClass.PRESERVED_STRATIGRAPHIC_BASIN
        : PreservationClass.ERODED_OR_COVERED;
  }

  private static long sourceBudget(
      ResourceFamily family,
      SedimentaryBasinState basin,
      PetrologicSample parent,
      boolean fixture) {
    if (fixture) {
      return switch (family) {
        case PHOSPHORITE -> 280_000L;
        case SEDIMENTARY_MANGANESE -> 255_000L;
        case COAL -> 300_000L;
        case LITHIUM_BRINE -> 290_000L;
        case POTASH_BORATE_BRINE -> 270_000L;
        case HELIUM_GAS -> 220_000L;
        case NONE -> 0L;
      };
    }
    SedimentaryReservoirState reservoir =
        parent.sedimentaryState().map(SedimentaryState::reservoirState).orElse(null);
    if (reservoir == null || basin == null) {
      return 0L;
    }
    long potassium = reservoir.aggregateCompositionPpm().getOrDefault(ChemicalElement.K, 0L);
    long phosphorus = reservoir.aggregateCompositionPpm().getOrDefault(ChemicalElement.P, 0L);
    long iron = reservoir.aggregateCompositionPpm().getOrDefault(ChemicalElement.FE, 0L);
    return switch (family) {
      case PHOSPHORITE ->
          Math.min(300_000L, phosphorus * 2L + basin.carbonateProductivityPpm() / 4L);
      case SEDIMENTARY_MANGANESE ->
          Math.min(300_000L, iron / 3L + reservoir.waterInventoryPpm() / 8L);
      case COAL -> Math.min(300_000L, reservoir.organicCarbonCapacityPpm());
      case LITHIUM_BRINE ->
          Math.min(
              300_000L, reservoir.waterInventoryPpm() / 5L + reservoir.volatileInventoryPpm() / 6L);
      case POTASH_BORATE_BRINE ->
          Math.min(300_000L, reservoir.waterInventoryPpm() / 6L + potassium / 2L);
      case HELIUM_GAS, NONE -> 0L;
    };
  }

  private static double depositFraction(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE, SEDIMENTARY_MANGANESE -> 0.58;
      case COAL -> 0.64;
      case LITHIUM_BRINE, POTASH_BORATE_BRINE -> 0.48;
      case HELIUM_GAS -> 0.40;
      case NONE -> 0.0;
    };
  }

  private static AgeKey formationAge(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE -> new AgeKey(182.0, 0);
      case SEDIMENTARY_MANGANESE -> new AgeKey(180.0, 0);
      case COAL -> new AgeKey(176.0, 0);
      case LITHIUM_BRINE -> new AgeKey(172.0, 0);
      case POTASH_BORATE_BRINE -> new AgeKey(169.0, 0);
      case HELIUM_GAS -> new AgeKey(165.0, 0);
      case NONE -> new AgeKey(0.0, 0);
    };
  }

  private static double lateralExtent(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE, SEDIMENTARY_MANGANESE -> 220.0;
      case COAL -> 204.0;
      case LITHIUM_BRINE, POTASH_BORATE_BRINE -> 188.0;
      case HELIUM_GAS -> 176.0;
      case NONE -> 220.0;
    };
  }

  private static double verticalExtent(ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE, SEDIMENTARY_MANGANESE -> 62.0;
      case COAL -> 108.0;
      case LITHIUM_BRINE, POTASH_BORATE_BRINE -> 86.0;
      case HELIUM_GAS -> 124.0;
      case NONE -> 62.0;
    };
  }

  private static SedimentaryResourceSystemState barren(
      StableId systemId,
      StableId basinId,
      StableId sourceBodyId,
      StableId structureId,
      StableId hostBodyId,
      List<StableId> sourceBodyIds,
      String faciesClass,
      Point3 center,
      String failedGate) {
    return new SedimentaryResourceSystemState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        ResourceFamily.NONE,
        basinId,
        sourceBodyId,
        structureId,
        hostBodyId,
        sourceBodyIds,
        new AgeKey(0.0, 0),
        faciesClass,
        SalinityClass.FRESH,
        RedoxClass.BUFFERED,
        BasinSetting.NONE,
        SourceClass.NO_RESOURCE_SOURCE,
        HostClass.NO_RECEPTIVE_HOST,
        PathwayClass.NO_CONNECTED_PATH,
        TrapClass.NO_RESOURCE_TRAP,
        PreservationClass.ERODED_OR_COVERED,
        center,
        220.0,
        62.0,
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
      ResourceFamily family,
      long deposit) {
    long inner = Math.round(deposit * 0.44);
    long middle = Math.round(deposit * 0.33);
    long outer = deposit - inner - middle;
    HorizonKind[] kinds =
        switch (family) {
          case PHOSPHORITE ->
              new HorizonKind[] {
                HorizonKind.PHOSPHATIC_PELLET_BED,
                HorizonKind.PHOSPHATE_HARDGROUND,
                HorizonKind.PHOSPHATIC_REWORKED_MARGIN
              };
          case SEDIMENTARY_MANGANESE ->
              new HorizonKind[] {
                HorizonKind.MANGANESE_OXIDE_STRATUM,
                HorizonKind.MANGANESE_CARBONATE_REDUCED_ZONE,
                HorizonKind.MANGANESE_CONDENSED_HALO
              };
          case COAL ->
              new HorizonKind[] {
                HorizonKind.COAL_MAIN_SEAM,
                HorizonKind.COAL_SPLIT_OR_PARTING,
                HorizonKind.COAL_CONTACT_RANK_MARGIN
              };
          case LITHIUM_BRINE ->
              new HorizonKind[] {
                HorizonKind.LITHIUM_BRINE_AQUIFER,
                HorizonKind.RESIDUAL_BRINE_CONCENTRATION,
                HorizonKind.SALAR_MIXING_MARGIN
              };
          case POTASH_BORATE_BRINE ->
              new HorizonKind[] {
                HorizonKind.POTASH_HALITE_SEQUENCE,
                HorizonKind.POTASH_LATE_STAGE_LAYER,
                HorizonKind.BORATE_BRINE_MARGIN
              };
          case HELIUM_GAS ->
              new HorizonKind[] {
                HorizonKind.HELIUM_GAS_RESERVOIR,
                HorizonKind.HELIUM_MIGRATION_ZONE,
                HorizonKind.HELIUM_SEAL_CLOSURE
              };
          case NONE -> throw new IllegalArgumentException("barren resources have no horizons");
        };
    return List.of(
        horizon(kinds[0], Overprint.NONE, 0.0, 0.36, 0.96, inner, identity, cell, 0),
        horizon(kinds[1], Overprint.CONTACT_HORNFELS, 0.36, 0.70, 0.82, middle, identity, cell, 1),
        horizon(kinds[2], Overprint.WEATHERED_REGOLITH, 0.70, 1.0, 0.68, outer, identity, cell, 2));
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
        identity.stream("geological", "sedimentary-resource-horizon", cell, index).stableId());
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("sedimentary resource horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("sedimentary resource horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum ResourceFamily {
    PHOSPHORITE,
    SEDIMENTARY_MANGANESE,
    COAL,
    LITHIUM_BRINE,
    POTASH_BORATE_BRINE,
    HELIUM_GAS,
    NONE
  }

  public enum BasinSetting {
    MARINE_UPWELLING_SHELF,
    ANOXIC_MARGIN_BASIN,
    PEAT_MIRE_BASIN,
    CLOSED_SALAR_BASIN,
    RESTRICTED_EVAPORITE_BASIN,
    RADIOGENIC_GAS_BASIN,
    NONE
  }

  public enum SourceClass {
    MARINE_PHOSPHORUS_CYCLING,
    DISSOLVED_MANGANESE_BASIN_SOURCE,
    ORGANIC_PEAT_INPUT,
    LITHIUM_RICH_VOLCANIC_WEATHERING,
    POTASH_BORATE_BRINE_EVAPORATION,
    RADIOGENIC_HELIUM_RELEASE,
    NO_RESOURCE_SOURCE
  }

  public enum HostClass {
    PHOSPHATIC_SHELF_BED,
    CONDENSED_MARINE_SEDIMENT,
    PEAT_BEARING_COAL_SEAM,
    BRINE_RESERVOIR_AQUIFER,
    HALITE_POTASH_SEQUENCE,
    POROUS_GAS_RESERVOIR,
    NO_RECEPTIVE_HOST
  }

  public enum PathwayClass {
    UPWELLING_REWORKING,
    REDOX_INTERFACE,
    MIRE_ACCOMMODATION,
    CLOSED_BASIN_GROUNDWATER,
    BRINE_EVAPORATION,
    GAS_MIGRATION,
    NO_CONNECTED_PATH
  }

  public enum TrapClass {
    PHOSPHATE_REWORKING,
    MANGANESE_REDOX_PRECIPITATION,
    ANOXIC_PEAT_PRESERVATION,
    RESIDUAL_LI_CONCENTRATION,
    LATE_POTASH_BORATE_SALT,
    RESERVOIR_SEAL_CLOSURE,
    NO_RESOURCE_TRAP
  }

  public enum PreservationClass {
    PRESERVED_STRATIGRAPHIC_BASIN,
    ERODED_OR_COVERED
  }

  public enum HorizonKind {
    PHOSPHATIC_PELLET_BED,
    PHOSPHATE_HARDGROUND,
    PHOSPHATIC_REWORKED_MARGIN,
    MANGANESE_OXIDE_STRATUM,
    MANGANESE_CARBONATE_REDUCED_ZONE,
    MANGANESE_CONDENSED_HALO,
    COAL_MAIN_SEAM,
    COAL_SPLIT_OR_PARTING,
    COAL_CONTACT_RANK_MARGIN,
    LITHIUM_BRINE_AQUIFER,
    RESIDUAL_BRINE_CONCENTRATION,
    SALAR_MIXING_MARGIN,
    POTASH_HALITE_SEQUENCE,
    POTASH_LATE_STAGE_LAYER,
    BORATE_BRINE_MARGIN,
    HELIUM_GAS_RESERVOIR,
    HELIUM_MIGRATION_ZONE,
    HELIUM_SEAL_CLOSURE
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
        throw new IllegalArgumentException("sedimentary resource horizon identity is required");
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
        throw new IllegalArgumentException("sedimentary resource horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
