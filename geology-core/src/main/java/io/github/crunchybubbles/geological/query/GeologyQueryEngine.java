package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.BoundedDescriptorCache;
import io.github.crunchybubbles.geological.atlas.DescriptorCache;
import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.mineral.MineralSystemProofs;
import io.github.crunchybubbles.geological.mineral.PorphyrySystemState;
import io.github.crunchybubbles.geological.mineral.VmsSystemState;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.spatial.ProvinceSpatialIndex;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import io.github.crunchybubbles.geological.surface.OverworldSurfaceModel;
import io.github.crunchybubbles.geological.surface.SurfaceFields;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Platform-neutral point/tile query facade over immutable atlas descriptors. */
public final class GeologyQueryEngine {
  private final GeologyAtlas atlas;
  private final MineralSystemProofs mineralProofs;
  private final OverworldSurfaceModel overworldSurface;
  private final DescriptorCache<StableId, ProvinceSpatialIndex> spatialIndexCache;

  public GeologyQueryEngine(GeologyAtlas atlas) {
    this(atlas, 4096, new BoundedDescriptorCache<>(256));
  }

  public GeologyQueryEngine(GeologyAtlas atlas, int noiseCacheSize) {
    this(atlas, noiseCacheSize, new BoundedDescriptorCache<>(256));
  }

  public GeologyQueryEngine(
      GeologyAtlas atlas,
      int noiseCacheSize,
      DescriptorCache<StableId, ProvinceSpatialIndex> spatialIndexCache) {
    this.atlas = atlas;
    this.mineralProofs = new MineralSystemProofs();
    this.spatialIndexCache = spatialIndexCache;
    this.overworldSurface =
        atlas.profile().surfaceTopology() == DimensionProfile.SurfaceTopology.SINGLE_VALUED_SURFACE
            ? new OverworldSurfaceModel(atlas.identity(), noiseCacheSize)
            : null;
  }

  public GeologyAtlas atlas() {
    return atlas;
  }

  public void clearCaches() {
    atlas.clearCaches();
    spatialIndexCache.clear();
    if (overworldSurface != null) {
      overworldSurface.clearCaches();
    }
  }

  public ColumnQueryResult column(ColumnRequest request) {
    return new ColumnQueryEngine(this, this::spatialIndex).query(request);
  }

  public List<SpatialCandidate> spatialCandidates(Bounds2D bounds) {
    Map<StableId, SpatialCandidate> unique = new TreeMap<>();
    for (Province province : atlas.provincesIntersecting(bounds)) {
      for (SpatialCandidate candidate : spatialIndex(province).intersecting(bounds)) {
        unique.put(candidate.id(), candidate);
      }
    }
    return List.copyOf(unique.values());
  }

  public int spatialIndexCacheSize() {
    return spatialIndexCache.size();
  }

  public GeologicalSample sample(Point3 worldPoint) {
    Point2 horizontal = new Point2(worldPoint.x(), worldPoint.z());
    Province province = atlas.provinceAt(horizontal);
    return sample(province, worldPoint);
  }

  public PointQueryTrace trace(Point3 worldPoint) {
    Point2 horizontal = new Point2(worldPoint.x(), worldPoint.z());
    Province province = atlas.provinceAt(horizontal);
    GeologicalSample sample = sample(province, worldPoint);
    Point3 presentLocal = province.frame().toLocal(worldPoint);
    Point3 afterFault = province.geometry().fault().pullBack(presentLocal, sample.formationAge());
    Point3 formation = province.geometry().fold().pullBack(afterFault, sample.formationAge());
    Point3 reconstructedLocal = province.geometry().pushForward(formation, sample.formationAge());
    Point3 reconstructedWorld = province.frame().toWorld(reconstructedLocal);
    double deltaX = reconstructedWorld.x() - worldPoint.x();
    double deltaY = reconstructedWorld.y() - worldPoint.y();
    double deltaZ = reconstructedWorld.z() - worldPoint.z();
    double residual = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    return new PointQueryTrace(
        sample,
        province.grammar(),
        spatialIndex(province).at(horizontal),
        presentLocal,
        afterFault,
        formation,
        reconstructedWorld,
        residual);
  }

  public GeologicalSample sample(Province province, Point3 worldPoint) {
    Point3 local = province.frame().toLocal(worldPoint);
    RiftArcGeometry geometry = province.geometry();

    AgeKey selectedAge = new AgeKey(1850.0, 0);
    StableId selectedBody = geometry.basementId();
    Lithology selectedLithology = Lithology.GRANITIC_GNEISS;

    Point3 basinPoint = pullBack(geometry, local, new AgeKey(250.0, 0));
    Lithology basinLithology =
        usesExplicitStratigraphy()
            ? geometry.stratigraphicPackage().lithologyAt(basinPoint)
            : geometry.basin().lithologyAt(basinPoint);
    if (basinLithology != null) {
      selectedAge = new AgeKey(250.0, 0);
      selectedBody = geometry.basin().packageId();
      selectedLithology = basinLithology;
    }

    Point3 vmsPoint = pullBack(geometry, local, new AgeKey(241.0, 0));
    boolean inVms = province.grammar().formsVms() && insideVms(geometry, vmsPoint);
    if (inVms) {
      selectedAge = new AgeKey(241.0, 0);
      selectedBody = province.proofIds().vmsDepositId();
      selectedLithology = Lithology.VMS_MASSIVE_SULFIDE;
    }

    for (RiftArcGeometry.PlutonPulse pulse : geometry.plutonPulses()) {
      Point3 pulsePoint = pullBack(geometry, local, pulse.birthAge());
      if (pulse.implicitValue(pulsePoint) <= 0.0) {
        selectedAge = pulse.birthAge();
        selectedBody = pulse.id();
        selectedLithology = pulse.lithology();
      }
    }

    Point3 unconformityPoint = pullBack(geometry, local, geometry.unconformity().age());
    Overprint overprint =
        usesExplicitStratigraphy()
                && geometry.unconformity().insideWeatheringProfile(unconformityPoint)
            ? Overprint.WEATHERED_UNCONFORMITY
            : Overprint.NONE;
    RiftArcGeometry.PlutonPulse youngestPulse = geometry.plutonPulses().getLast();
    Point3 aureolePoint = pullBack(geometry, local, new AgeKey(96.0, 0));
    double contactDistance = youngestPulse.approximateOutsideDistance(aureolePoint);
    if (contactDistance > 0.0 && contactDistance <= 128.0) {
      overprint = Overprint.CONTACT_HORNFELS;
    }

    Point3 porphyryPoint = pullBack(geometry, local, new AgeKey(92.0, 0));
    double porphyryDistance = distance(porphyryPoint, geometry.porphyryCenter());
    List<StableId> depositIds = new ArrayList<>();
    if (province.grammar().formsPorphyry() && porphyryDistance <= 205.0) {
      depositIds.add(province.proofIds().porphyryDepositId());
      if (porphyryDistance <= 65.0) {
        overprint = Overprint.POTASSIC_ALTERATION;
      } else if (porphyryDistance <= 125.0) {
        overprint = Overprint.PHYLLIC_ALTERATION;
      } else {
        overprint = Overprint.PROPYLITIC_ALTERATION;
      }
    }
    if (inVms) {
      depositIds.add(province.proofIds().vmsDepositId());
      if (overprint == Overprint.NONE) {
        overprint = Overprint.CHLORITIC_FOOTWALL;
      }
    }

    return new GeologicalSample(
        worldPoint,
        province.macroDomainId(),
        province.id(),
        selectedBody,
        selectedLithology,
        selectedAge,
        overprint,
        geometry.fault().intersectsDamageZone(local),
        depositIds);
  }

  public SurfaceSample surface(Point2 worldPoint) {
    if (overworldSurface == null) {
      throw new UnsupportedOperationException(
          "the selected dimension profile does not expose an Overworld-style surface");
    }
    Province province = atlas.provinceAt(worldPoint);
    SurfaceFields fields = overworldSurface.evaluate(province, worldPoint);
    GeologicalSample bedrock =
        sample(province, new Point3(worldPoint.x(), fields.elevation() - 0.5, worldPoint.z()));
    Lithology material;
    Overprint overprint;
    if (fields.drainage().sourceLinkedPlacer()) {
      material = Lithology.ALLUVIAL_GRAVEL;
      overprint = Overprint.OXIDIZED_GOSSAN;
    } else if (fields.outcrop()) {
      material = bedrock.lithology();
      overprint = bedrock.overprint();
    } else {
      material = bedrock.lithology();
      overprint = Overprint.WEATHERED_REGOLITH;
    }
    return new SurfaceSample(fields, bedrock, material, overprint);
  }

  public List<MineralSystemDecision> mineralDecisions(Province province) {
    return mineralProofs.compile(province);
  }

  /** Returns the linked Phase 3 porphyry intrusion/fluid/stockwork topology. */
  public PorphyrySystemState porphyrySystemState(Province province) {
    return mineralProofs.porphyryState(province);
  }

  /** Returns the linked Phase 3 VMS basin, stratiform lens, and feeder topology. */
  public VmsSystemState vmsSystemState(Province province) {
    return mineralProofs.vmsState(province);
  }

  private ProvinceSpatialIndex spatialIndex(Province province) {
    return spatialIndexCache.get(province.id(), ignored -> ProvinceSpatialIndex.compile(province));
  }

  private boolean usesExplicitStratigraphy() {
    return atlas.profile().chronicleGrammarId().equals("geological:varied_rift_to_arc_grammar_v1");
  }

  private static Point3 pullBack(RiftArcGeometry geometry, Point3 present, AgeKey bodyAge) {
    return geometry.pullBack(present, bodyAge);
  }

  private static boolean insideVms(RiftArcGeometry geometry, Point3 point) {
    Point3 center = geometry.vmsCenter();
    double u = (point.x() - center.x()) / 112.0;
    double y = (point.y() - center.y()) / 15.0;
    double v = (point.z() - center.z()) / 72.0;
    boolean lens = u * u + y * y + v * v <= 1.0;
    double feederU = (point.x() - center.x()) / 28.0;
    double feederV = (point.z() - center.z()) / 34.0;
    boolean feeder =
        feederU * feederU + feederV * feederV <= 1.0
            && point.y() <= center.y()
            && point.y() >= center.y() - 95.0;
    return lens || feeder;
  }

  private static double distance(Point3 first, Point3 second) {
    double deltaX = first.x() - second.x();
    double deltaY = first.y() - second.y();
    double deltaZ = first.z() - second.z();
    return StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
  }
}
