package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.RandomStream;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Chronicle;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure compiler for locally owned macro-domain and rift-to-arc province descriptors. */
public final class AtlasCompiler {
  public static final long MACRO_CELL_SIZE = 8192;
  public static final long PROVINCE_CELL_SIZE = 2048;

  private final WorldIdentity identity;
  private final DimensionProfile profile;

  public AtlasCompiler(WorldIdentity identity, DimensionProfile profile) {
    this.identity = identity;
    this.profile = profile;
    if (!identity.dimensionProfileId().equals(profile.id())) {
      throw new IllegalArgumentException("world identity and dimension profile do not match");
    }
  }

  public MacroDomain compileMacroDomain(CellKey homeCell) {
    requireLevel(homeCell, "macro");
    RandomStream stream = identity.stream("geological", "macro_domain", homeCell, 0);
    StableId id = stream.stableId();
    Point2 site = site(homeCell, MACRO_CELL_SIZE, stream);
    CrustClass crustClass =
        CrustClass.values()[stream.boundedInt("crust-class", 0, CrustClass.values().length)];
    double basementAgeMa = 950.0 + 1_650.0 * stream.unitDouble("basement-age-ma", 0);
    List<StableId> neighbors = new ArrayList<>();
    for (long offsetX = -1; offsetX <= 1; offsetX++) {
      for (long offsetZ = -1; offsetZ <= 1; offsetZ++) {
        if (offsetX == 0 && offsetZ == 0) {
          continue;
        }
        neighbors.add(
            macroId(new CellKey("macro", homeCell.x() + offsetX, homeCell.z() + offsetZ)));
      }
    }
    neighbors.sort(Comparator.naturalOrder());
    return new MacroDomain(id, homeCell, site, crustClass, basementAgeMa, neighbors);
  }

  public Province compileProvince(CellKey homeCell) {
    requireLevel(homeCell, "province");
    RandomStream provinceStream = identity.stream("geological", "province", homeCell, 0);
    StableId provinceId = provinceStream.stableId();
    Point2 site = site(homeCell, PROVINCE_CELL_SIZE, provinceStream);
    ProvinceGrammar grammar = selectGrammar(provinceStream);
    StableId macroDomainId =
        macroId(resolveNearestCell(site, "macro", MACRO_CELL_SIZE, "macro_domain"));
    double orientation =
        provinceStream.symmetricDouble("structural-orientation", 0) * StrictMath.PI;
    LocalFrame frame = new LocalFrame(site, orientation);

    ProvinceIds ids = createIds(homeCell);
    RiftArcGeometry geometry = createGeometry(provinceStream, ids);
    Chronicle chronicle = createChronicle(homeCell, ids, geometry, grammar);
    List<ProvinceAdjacency> adjacency = createAdjacency(homeCell, provinceId);
    ProvinceProofIds proofIds =
        new ProvinceProofIds(
            ids.magmaLineage,
            ids.vmsSystem,
            ids.vmsDeposit,
            ids.porphyrySystem,
            ids.porphyryDeposit,
            ids.placerSystem,
            ids.placerDeposit,
            ids.rejectedPorphyry,
            ids.rejectedVms,
            ids.rejectedPlacer,
            ids.uplift,
            ids.weathering);

    return new Province(
        provinceId,
        homeCell,
        macroDomainId,
        site,
        PROVINCE_CELL_SIZE,
        frame,
        grammar,
        chronicle,
        geometry,
        proofIds,
        adjacency);
  }

  public Point2 provinceSite(CellKey cell) {
    return compileProvinceSite(cell).point();
  }

  public StableId provinceId(CellKey cell) {
    return compileProvinceSite(cell).id();
  }

  public Point2 macroSite(CellKey cell) {
    return compileMacroSite(cell).point();
  }

  public StableId macroId(CellKey cell) {
    return compileMacroSite(cell).id();
  }

  public AtlasSite compileProvinceSite(CellKey cell) {
    requireLevel(cell, "province");
    RandomStream stream = identity.stream("geological", "province", cell, 0);
    return new AtlasSite(stream.stableId(), site(cell, PROVINCE_CELL_SIZE, stream));
  }

  public AtlasSite compileMacroSite(CellKey cell) {
    requireLevel(cell, "macro");
    RandomStream stream = identity.stream("geological", "macro_domain", cell, 0);
    return new AtlasSite(stream.stableId(), site(cell, MACRO_CELL_SIZE, stream));
  }

  private RiftArcGeometry createGeometry(RandomStream stream, ProvinceIds ids) {
    double scale = PROVINCE_CELL_SIZE;
    RiftArcGeometry.Basin basin =
        new RiftArcGeometry.Basin(
            ids.basin,
            ids.packageId,
            new Point2(-0.08 * scale, 0.03 * scale),
            0.44 * scale,
            0.39 * scale,
            -42.0,
            205.0,
            new AgeKey(260.0, 0));

    double plutonU = 0.17 * scale;
    double plutonV = -0.23 * scale;
    List<RiftArcGeometry.PlutonPulse> pulses =
        List.of(
            new RiftArcGeometry.PlutonPulse(
                ids.pulse1,
                new Point3(plutonU - 55.0, 32.0, plutonV + 45.0),
                285.0,
                165.0,
                245.0,
                new AgeKey(108.0, 0),
                Lithology.DIORITE_PULSE),
            new RiftArcGeometry.PlutonPulse(
                ids.pulse2,
                new Point3(plutonU + 28.0, 58.0, plutonV - 20.0),
                225.0,
                150.0,
                205.0,
                new AgeKey(103.0, 0),
                Lithology.GRANODIORITE_PULSE),
            new RiftArcGeometry.PlutonPulse(
                ids.pulse3,
                new Point3(plutonU + 75.0, 82.0, plutonV + 18.0),
                145.0,
                130.0,
                128.0,
                new AgeKey(98.0, 0),
                Lithology.FELSIC_STOCK));

    RiftArcGeometry.Fault fault =
        new RiftArcGeometry.Fault(
            ids.fault,
            0.015 * scale,
            0.47 * scale,
            300.0,
            52.0,
            38.0,
            new AgeKey(270.0, 0),
            new AgeKey(60.0, 0));
    RiftArcGeometry.Fold fold =
        new RiftArcGeometry.Fold(ids.fold, 0.48 * scale, 29.0, 410.0, new AgeKey(80.0, 0));

    Point3 porphyryCenter = pulses.getLast().center().withY(116.0);
    Point2 vmsHorizontal = new Point2(-0.23 * scale, -0.12 * scale);
    double vmsTop = basin.topElevation(vmsHorizontal);
    Point3 vmsCenter =
        new Point3(
            vmsHorizontal.x(),
            basin.baseElevation() + 0.53 * (vmsTop - basin.baseElevation()),
            vmsHorizontal.z());
    double drainagePhase = stream.unitDouble("drainage-phase", 0) * 2.0 * StrictMath.PI;
    double placerV = porphyryCenter.z() + 0.37 * scale;
    double placerU = trunkU(porphyryCenter.x(), placerV, scale, drainagePhase);

    return new RiftArcGeometry(
        ids.basement,
        basin,
        pulses,
        fault,
        fold,
        ids.aureole,
        porphyryCenter,
        vmsCenter,
        new Point2(placerU, placerV),
        drainagePhase);
  }

  static double trunkU(double sourceU, double localV, double scale, double phase) {
    return sourceU + 0.075 * scale * StrictMath.sin(2.0 * StrictMath.PI * localV / scale + phase);
  }

  private Chronicle createChronicle(
      CellKey cell, ProvinceIds ids, RiftArcGeometry geometry, ProvinceGrammar grammar) {
    List<GeologicalEvent> events = new ArrayList<>();
    int ordinal = 0;
    events.add(
        event(
            cell,
            ordinal++,
            EventType.ESTABLISH_BASEMENT,
            1850.0,
            List.of(),
            List.of(ids.basement),
            "Establish inherited granitic-gneiss basement and source reservoirs."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.RIFT,
            270.0,
            List.of(ids.basement),
            List.of(ids.fault),
            "Open inherited finite normal-fault permeability during rifting."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.OPEN_BASIN,
            260.0,
            List.of(ids.basement, ids.fault),
            List.of(ids.basin),
            "Create fault-influenced marine rift accommodation."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.DEPOSIT_SEQUENCE,
            250.0,
            List.of(ids.basin),
            List.of(ids.packageId),
            "Deposit basal clastics, submarine volcaniclastics, shale, and sandstone."));
    if (grammar.formsVms()) {
      events.add(
          event(
              cell,
              ordinal++,
              EventType.VOLCANIC_HYDROTHERMAL_PULSE,
              242.0,
              List.of(ids.packageId, ids.fault),
              List.of(ids.vmsSystem),
              "Drive seawater convection through the synvolcanic pile and inherited fault."));
      events.add(
          event(
              cell,
              ordinal++,
              EventType.MINERALIZE,
              241.0,
              List.of(ids.vmsSystem),
              List.of(ids.vmsDeposit),
              "Precipitate a VMS lens and chloritic footwall at the active seafloor horizon."));
    }
    events.add(
        event(
            cell,
            ordinal++,
            EventType.ARC_EPISODE,
            120.0,
            List.of(ids.basement),
            List.of(ids.magmaLineage),
            grammar.formsPorphyry()
                ? "Establish a hydrous calc-alkaline arc magma lineage."
                : "Establish a comparatively volatile-poor calc-alkaline arc magma lineage."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.INTRUDE_MAGMA,
            108.0,
            List.of(ids.magmaLineage),
            List.of(ids.pulse1),
            "Emplace the dioritic first pluton pulse."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.INTRUDE_MAGMA,
            103.0,
            List.of(ids.magmaLineage, ids.pulse1),
            List.of(ids.pulse2),
            "Cross-cut pulse one with a granodioritic pulse."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.INTRUDE_MAGMA,
            98.0,
            List.of(ids.magmaLineage, ids.pulse2),
            List.of(ids.pulse3),
            "Emplace a shallow felsic stock and residual volatile source."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.CONTACT_METAMORPHISM,
            96.0,
            List.of(ids.pulse3, ids.packageId),
            List.of(ids.aureole),
            "Bake older country rock in a finite contact aureole."));
    if (grammar.formsPorphyry()) {
      events.add(
          event(
              cell,
              ordinal++,
              EventType.MINERALIZE,
              92.0,
              List.of(ids.magmaLineage, ids.pulse3, ids.fault),
              List.of(ids.porphyrySystem, ids.porphyryDeposit),
              "Focus magmatic-hydrothermal fluid into a porphyry Cu-Au stockwork."));
    }
    events.add(
        event(
            cell,
            ordinal++,
            EventType.FOLD,
            80.0,
            grammar.formsVms() ? List.of(ids.packageId, ids.vmsDeposit) : List.of(ids.packageId),
            List.of(ids.fold),
            grammar.formsVms()
                ? "Fold older basin strata and the VMS horizon in a finite warp."
                : "Fold older barren basin strata in a finite warp."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.FAULT_REACTIVATION,
            60.0,
            List.of(ids.fault, ids.fold),
            List.of(ids.faultReactivation),
            "Reactivate the inherited fault and displace all older bodies."));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.UPLIFT_EXHUME,
            20.0,
            upliftInputs(ids, grammar),
            List.of(ids.uplift),
            upliftDescription(grammar)));
    events.add(
        event(
            cell,
            ordinal++,
            EventType.WEATHER,
            1.0,
            grammar.formsPorphyry()
                ? List.of(ids.uplift, ids.porphyryDeposit)
                : List.of(ids.uplift),
            List.of(ids.weathering),
            grammar.formsPlacer()
                ? "Weather exposed bedrock and release a bounded fraction of durable gold."
                : "Weather exposed bedrock without establishing a connected placer source budget."));
    if (grammar.formsPlacer()) {
      events.add(
          event(
              cell,
              ordinal,
              EventType.ERODE_TRANSPORT_DEPOSIT,
              0.1,
              List.of(ids.weathering, ids.porphyryDeposit),
              List.of(ids.placerSystem, ids.placerDeposit),
              "Route released gold through trunk drainage into a downstream hydraulic trap."));
    } else {
      events.add(
          event(
              cell,
              ordinal,
              EventType.ERODE_TRANSPORT,
              0.1,
              List.of(ids.weathering),
              List.of(ids.placerSystem),
              "Route ordinary sediment without satisfying the source-linked placer gates."));
    }
    return new Chronicle(events);
  }

  private ProvinceGrammar selectGrammar(RandomStream provinceStream) {
    return switch (profile.chronicleGrammarId()) {
      case "geological:fixed_rift_to_arc_proof_v1" -> ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC;
      case "geological:varied_rift_to_arc_grammar_v1" ->
          switch (provinceStream.boundedInt("chronicle-grammar", 0, 3)) {
            case 0 -> ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC;
            case 1 -> ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC;
            case 2 -> ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC;
            default ->
                throw new IllegalStateException("bounded grammar selector escaped its range");
          };
      default ->
          throw new IllegalArgumentException(
              "unsupported chronicle grammar " + profile.chronicleGrammarId());
    };
  }

  private static List<StableId> upliftInputs(ProvinceIds ids, ProvinceGrammar grammar) {
    if (grammar.formsPorphyry() && grammar.formsVms()) {
      return List.of(ids.faultReactivation, ids.porphyryDeposit, ids.vmsDeposit);
    }
    if (grammar.formsPorphyry()) {
      return List.of(ids.faultReactivation, ids.porphyryDeposit);
    }
    if (grammar.formsVms()) {
      return List.of(ids.faultReactivation, ids.vmsDeposit);
    }
    return List.of(ids.faultReactivation);
  }

  private static String upliftDescription(ProvinceGrammar grammar) {
    return switch (grammar) {
      case EXHUMED_FERTILE_RIFT_TO_ARC ->
          "Uplift, erode, and partly expose the mineralized province.";
      case BURIED_FERTILE_RIFT_TO_ARC ->
          "Uplift the mineralized province without exposing a placer-capable primary source.";
      case BARREN_DRY_RIFT_TO_ARC ->
          "Uplift and erode the structurally mature but barren province.";
    };
  }

  private GeologicalEvent event(
      CellKey cell,
      int ordinal,
      EventType type,
      double ageMa,
      List<StableId> inputs,
      List<StableId> outputs,
      String description) {
    StableId eventId = identity.stream("geological", "event", cell, ordinal).stableId();
    return new GeologicalEvent(
        eventId, type, new AgeKey(ageMa, ordinal), inputs, outputs, description);
  }

  private ProvinceIds createIds(CellKey cell) {
    return new ProvinceIds(
        objectId(cell, "basement", 0),
        objectId(cell, "fault", 0),
        objectId(cell, "basin", 0),
        objectId(cell, "stratigraphic_package", 0),
        objectId(cell, "vms_system", 0),
        objectId(cell, "vms_deposit", 0),
        objectId(cell, "magma_lineage", 0),
        objectId(cell, "pluton_pulse", 0),
        objectId(cell, "pluton_pulse", 1),
        objectId(cell, "pluton_pulse", 2),
        objectId(cell, "contact_aureole", 0),
        objectId(cell, "porphyry_system", 0),
        objectId(cell, "porphyry_deposit", 0),
        objectId(cell, "fold", 0),
        objectId(cell, "fault_reactivation", 0),
        objectId(cell, "uplift", 0),
        objectId(cell, "weathering", 0),
        objectId(cell, "placer_system", 0),
        objectId(cell, "placer_deposit", 0),
        objectId(cell, "porphyry_candidate", 1),
        objectId(cell, "vms_candidate", 1),
        objectId(cell, "placer_candidate", 1));
  }

  private StableId objectId(CellKey cell, String objectType, long index) {
    return identity.stream("geological", objectType, cell, index).stableId();
  }

  private List<ProvinceAdjacency> createAdjacency(CellKey homeCell, StableId provinceId) {
    List<ProvinceAdjacency> adjacency = new ArrayList<>();
    for (long offsetX = -1; offsetX <= 1; offsetX++) {
      for (long offsetZ = -1; offsetZ <= 1; offsetZ++) {
        if (offsetX == 0 && offsetZ == 0) {
          continue;
        }
        CellKey neighborCell =
            new CellKey("province", homeCell.x() + offsetX, homeCell.z() + offsetZ);
        StableId neighborId = provinceId(neighborCell);
        StableId first = provinceId.compareTo(neighborId) <= 0 ? provinceId : neighborId;
        StableId second = provinceId.compareTo(neighborId) <= 0 ? neighborId : provinceId;
        CellKey owner = homeCell.compareTo(neighborCell) <= 0 ? homeCell : neighborCell;
        RandomStream boundaryStream =
            identity.stream("geological", "boundary/" + first + "/" + second, owner, 0);
        BoundaryType boundary =
            BoundaryType.values()[
                boundaryStream.boundedInt("classification", 0, BoundaryType.values().length)];
        adjacency.add(new ProvinceAdjacency(neighborId, boundary));
      }
    }
    return adjacency;
  }

  private CellKey resolveNearestCell(Point2 point, String level, long cellSize, String objectType) {
    CellKey containing = containing(level, point, cellSize);
    CellKey best = null;
    double bestDistance = Double.POSITIVE_INFINITY;
    StableId bestId = null;
    for (long offsetX = -1; offsetX <= 1; offsetX++) {
      for (long offsetZ = -1; offsetZ <= 1; offsetZ++) {
        CellKey candidate = new CellKey(level, containing.x() + offsetX, containing.z() + offsetZ);
        RandomStream stream = identity.stream("geological", objectType, candidate, 0);
        Point2 candidateSite = site(candidate, cellSize, stream);
        double distance = point.squaredDistance(candidateSite);
        StableId candidateId = stream.stableId();
        if (distance < bestDistance
            || (distance == bestDistance
                && (bestId == null || candidateId.compareTo(bestId) < 0))) {
          best = candidate;
          bestDistance = distance;
          bestId = candidateId;
        }
      }
    }
    return best;
  }

  static CellKey containing(String level, Point2 point, long cellSize) {
    long blockX = floorToLong(point.x());
    long blockZ = floorToLong(point.z());
    return CellKey.containing(level, blockX, blockZ, cellSize);
  }

  private static Point2 site(CellKey cell, long cellSize, RandomStream stream) {
    double jitterX = stream.symmetricDouble("site-jitter-x", 0) * 0.28 * cellSize;
    double jitterZ = stream.symmetricDouble("site-jitter-z", 0) * 0.28 * cellSize;
    double centerX = ((double) cell.x() + 0.5) * cellSize;
    double centerZ = ((double) cell.z() + 0.5) * cellSize;
    return new Point2(centerX + jitterX, centerZ + jitterZ);
  }

  private static long floorToLong(double value) {
    if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
      throw new IllegalArgumentException("coordinate is outside the supported atlas range");
    }
    return (long) StrictMath.floor(value);
  }

  private static void requireLevel(CellKey cell, String expected) {
    if (!cell.level().equals(expected)) {
      throw new IllegalArgumentException("expected a " + expected + " cell, got " + cell.level());
    }
  }

  private record ProvinceIds(
      StableId basement,
      StableId fault,
      StableId basin,
      StableId packageId,
      StableId vmsSystem,
      StableId vmsDeposit,
      StableId magmaLineage,
      StableId pulse1,
      StableId pulse2,
      StableId pulse3,
      StableId aureole,
      StableId porphyrySystem,
      StableId porphyryDeposit,
      StableId fold,
      StableId faultReactivation,
      StableId uplift,
      StableId weathering,
      StableId placerSystem,
      StableId placerDeposit,
      StableId rejectedPorphyry,
      StableId rejectedVms,
      StableId rejectedPlacer) {}
}
