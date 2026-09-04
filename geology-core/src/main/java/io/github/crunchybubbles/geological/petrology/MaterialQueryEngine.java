package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.atlas.BoundedDescriptorCache;
import io.github.crunchybubbles.geological.atlas.DescriptorCache;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.BifSystemState;
import io.github.crunchybubbles.geological.mineral.DepositType;
import io.github.crunchybubbles.geological.mineral.EvaporitePotashState;
import io.github.crunchybubbles.geological.mineral.LctPegmatiteState;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.mineral.PlacerSystemState;
import io.github.crunchybubbles.geological.mineral.PorphyryFluidMetalState;
import io.github.crunchybubbles.geological.mineral.PorphyrySystemState;
import io.github.crunchybubbles.geological.mineral.VmsSystemState;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialRun;
import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Phase 2 facade deriving material composition and process state from immutable Phase 1 geology.
 */
public final class MaterialQueryEngine {
  private static final double TERRAIN_ROUGHNESS_REFERENCE_RESIDUAL_SLOPE = 0.05;
  private static final AgeKey COLLUVIUM_FORMATION_AGE = new AgeKey(0.02, 0);

  private final GeologyQueryEngine geology;
  private final MaterialCatalogSnapshot catalog;
  private final WorldIdentity materialIdentity;
  private final ColluvialRoutePolicy colluvialRoutePolicy;
  private final ColluvialTransportPolicy colluvialTransportPolicy;
  private final Map<RecipeKey, RecipeTemplate> recipeTemplates;
  private final BodyCompositionSampler compositionSampler;
  private final DescriptorCache<BodyRecipeKey, ResolvedRecipe> bodyRecipeCache;
  private final DescriptorCache<StableId, List<ElementReservoirLedger>> reservoirLedgerCache;

  public MaterialQueryEngine(GeologyQueryEngine geology, MaterialCatalogSnapshot catalog) {
    this(
        geology,
        catalog,
        geology == null ? null : geology.atlas().identity(),
        ColluvialRoutePolicy.DEFAULT,
        ColluvialTransportPolicy.DEFAULT);
  }

  public MaterialQueryEngine(
      GeologyQueryEngine geology, MaterialCatalogSnapshot catalog, WorldIdentity materialIdentity) {
    this(geology, catalog, materialIdentity, ColluvialRoutePolicy.DEFAULT);
  }

  public MaterialQueryEngine(
      GeologyQueryEngine geology,
      MaterialCatalogSnapshot catalog,
      WorldIdentity materialIdentity,
      ColluvialRoutePolicy colluvialRoutePolicy) {
    this(
        geology, catalog, materialIdentity, colluvialRoutePolicy, ColluvialTransportPolicy.DEFAULT);
  }

  public MaterialQueryEngine(
      GeologyQueryEngine geology,
      MaterialCatalogSnapshot catalog,
      WorldIdentity materialIdentity,
      ColluvialRoutePolicy colluvialRoutePolicy,
      ColluvialTransportPolicy colluvialTransportPolicy) {
    if (geology == null || catalog == null) {
      throw new IllegalArgumentException("geology query and material catalog are required");
    }
    if (materialIdentity == null) {
      throw new IllegalArgumentException("material identity is required");
    }
    if (colluvialRoutePolicy == null) {
      throw new IllegalArgumentException("colluvial route policy is required");
    }
    if (colluvialTransportPolicy == null) {
      throw new IllegalArgumentException("colluvial transport policy is required");
    }
    this.geology = geology;
    this.catalog = catalog;
    this.materialIdentity = materialIdentity;
    this.colluvialRoutePolicy = colluvialRoutePolicy;
    this.colluvialTransportPolicy = colluvialTransportPolicy;
    this.recipeTemplates = compileRecipeTemplates(catalog);
    this.compositionSampler = new BodyCompositionSampler(materialIdentity);
    this.bodyRecipeCache = new BoundedDescriptorCache<>(512);
    this.reservoirLedgerCache = new BoundedDescriptorCache<>(256);
  }

  public GeologyQueryEngine geology() {
    return geology;
  }

  public MaterialCatalogSnapshot catalog() {
    return catalog;
  }

  public WorldIdentity materialIdentity() {
    return materialIdentity;
  }

  public ColluvialRoutePolicy colluvialRoutePolicy() {
    return colluvialRoutePolicy;
  }

  public ColluvialTransportPolicy colluvialTransportPolicy() {
    return colluvialTransportPolicy;
  }

  public int resolvedRecipeCount() {
    return recipeTemplates.size();
  }

  public int bodyRecipeCacheSize() {
    return bodyRecipeCache.size();
  }

  public List<ElementReservoirLedger> elementReservoirLedgers(Province province) {
    return reservoirLedgerCache.get(province.id(), ignored -> compileReservoirLedgers(province));
  }

  /** Returns the linked Phase 3 porphyry intrusion/fluid/stockwork topology. */
  public PorphyrySystemState porphyrySystemState(Province province) {
    return geology.porphyrySystemState(province);
  }

  /** Returns the linked Phase 3 VMS basin, stratiform lens, and feeder topology. */
  public VmsSystemState vmsSystemState(Province province) {
    return geology.vmsSystemState(province);
  }

  /** Returns the linked Phase 3 LCT pegmatite child-body lineage. */
  public LctPegmatiteState lctPegmatiteState(Province province) {
    return geology.lctPegmatiteState(province);
  }

  /** Returns the linked Phase 3 BIF sheet, age, and ocean-redox state. */
  public BifSystemState bifSystemState(Province province) {
    return geology.bifSystemState(province);
  }

  /** Returns the linked Phase 3 restricted-basin evaporite and potash sequence. */
  public EvaporitePotashState evaporitePotashState(Province province) {
    return geology.evaporitePotashState(province);
  }

  /** Returns the linked Phase 3 source, transport, and hydraulic-trap placer state. */
  public PlacerSystemState placerSystemState(Province province) {
    return geology.placerSystemState(province);
  }

  /** Returns the richer Phase 3 porphyry fluid-phase and metal-distribution state. */
  public PorphyryFluidMetalState porphyryFluidMetalState(Province province) {
    return geology.porphyryFluidMetalState(province);
  }

  /** Builds a deterministic finite-query audit of source claims across colluvial parcels. */
  public ColluvialSourceClaimLedger colluvialSourceClaimLedger(List<Point2> parcelPoints) {
    if (parcelPoints == null) {
      throw new IllegalArgumentException("colluvial parcel points are required");
    }
    List<ColluvialSourceClaim> claims = new ArrayList<>();
    for (Point2 parcelPoint : List.copyOf(parcelPoints)) {
      if (parcelPoint == null) {
        throw new IllegalArgumentException("colluvial parcel points cannot be null");
      }
      SurfacePetrologicSample parcel = surface(parcelPoint);
      if (parcel.context().kind() != SurfaceMaterialKind.COLLUVIAL_MANTLE) {
        throw new IllegalArgumentException("every source-claim parcel must be colluvial mantle");
      }
      ColluvialSourceMix sourceMix = parcel.context().colluvialSourceMix().orElseThrow();
      ColluvialSedimentBudget budget = sourceMix.sedimentBudget();
      for (ColluvialSedimentBudget.SourceBalance source : budget.sourceBalances()) {
        ColluvialSedimentBudget.InputBalance balance = source.balance();
        claims.add(
            new ColluvialSourceClaim(
                parcelPoint,
                parcel.context().materialBodyId(),
                source.sourceBodyId(),
                source.upslopeDistanceBlocks(),
                balance.input().capacityFixedUnits(),
                balance.mobilizedFixedUnits(),
                balance.retainedFixedUnits(),
                balance.transportLossFixedUnits(),
                balance.bypassedFixedUnits(),
                balance.depositedFixedUnits(),
                balance.capacityGrainMass(),
                balance.mobilizedGrainMass(),
                balance.retainedGrainMass(),
                balance.transportLossGrainMass(),
                balance.bypassedGrainMass(),
                balance.depositedGrainMass()));
      }
    }
    return ColluvialSourceClaimLedger.from(claims);
  }

  /**
   * Builds a deterministic finite-query source-capacity reconciliation for colluvial parcels.
   *
   * <p>The capacity map is explicit audit input; this method never mutates random-access world
   * state or applies depletion to subsequent {@link #surface(Point2)} queries.
   */
  public ColluvialSourceCapacityLedger colluvialSourceCapacityLedger(
      List<Point2> parcelPoints, Map<StableId, Long> sourceCapacityFixedUnits) {
    return colluvialSourceClaimLedger(parcelPoints)
        .reconcileSourceCapacity(sourceCapacityFixedUnits);
  }

  public void clearCaches() {
    geology.clearCaches();
    bodyRecipeCache.clear();
    reservoirLedgerCache.clear();
  }

  public PetrologicSample sample(Point3 worldPoint) {
    Province province = geology.atlas().provinceAt(new Point2(worldPoint.x(), worldPoint.z()));
    GeologicalSample geological = geology.sample(province, worldPoint);
    return resolve(province, geological);
  }

  public PetrologicColumnResult column(ColumnRequest request) {
    ColumnQueryResult geologicalColumn = geology.column(request);
    Province province = geology.atlas().provinceAt(request.horizontalPoint());
    if (!province.id().equals(geologicalColumn.provinceId())) {
      throw new IllegalStateException("column owner changed between geological query stages");
    }
    List<PetrologicRun> runs = new ArrayList<>();
    for (MaterialRun geologicalRun : geologicalColumn.runs()) {
      GeologicalSample representative =
          geologicalSample(
              province,
              new Point3(request.x(), geologicalRun.minYInclusive() + 0.5, request.z()),
              geologicalRun.state());
      PetrologicState state = PetrologicState.from(resolve(province, representative));
      if (!runs.isEmpty() && runs.getLast().state().equals(state)) {
        PetrologicRun previous = runs.removeLast();
        runs.add(new PetrologicRun(previous.minYInclusive(), geologicalRun.maxYExclusive(), state));
      } else {
        runs.add(
            new PetrologicRun(geologicalRun.minYInclusive(), geologicalRun.maxYExclusive(), state));
      }
    }
    return new PetrologicColumnResult(geologicalColumn, runs, geologicalColumn.runs().size());
  }

  public SurfacePetrologicSample surface(Point2 worldPoint) {
    Province province = geology.atlas().provinceAt(worldPoint);
    SurfaceSample surface = geology.surface(worldPoint);
    GeologicalSample bedrock = surface.bedrock();
    if (!province.id().equals(bedrock.provinceId())) {
      throw new IllegalStateException("surface owner changed between geological query stages");
    }

    GeologicalSample surfaceGeology;
    SurfaceMaterialContext context;
    PetrologicSample material;
    if (surface.fields().drainage().sourceLinkedPlacer()) {
      MineralSystemDecision placer = formedPlacer(province);
      long trapped = placer.ledger().allocations().getOrDefault("placer_trap", 0L);
      surfaceGeology =
          new GeologicalSample(
              bedrock.point(),
              bedrock.macroDomainId(),
              bedrock.provinceId(),
              placer.deposit().id(),
              surface.surfaceMaterial(),
              placer.deposit().formationAge(),
              surface.surfaceOverprint(),
              bedrock.faultDamageZone(),
              List.of(placer.deposit().id()));
      context =
          new SurfaceMaterialContext(
              SurfaceMaterialKind.ALLUVIAL_PLACER,
              placer.deposit().id(),
              placer.deposit().sourceIds(),
              Optional.empty(),
              Optional.of(placer.deposit().id()),
              Optional.of(placer.ledger().element()),
              Optional.of(placer.ledger().unit()),
              placer.ledger().sourceAmount(),
              trapped);
      material = resolve(province, surfaceGeology);
    } else if (formsColluvialMantle(surface)) {
      Point2 initialUpslopeDirection = terrainUpslopeDirection(surface, null).direction();
      RockDefinition colluviumRock = catalog.requireRock(Lithology.SOIL_COLLUVIUM);
      List<ColluvialSourceCandidate> sourceCandidates =
          resolveColluvialSourceCandidates(surface, initialUpslopeDirection);
      ColluvialSedimentBudget sedimentBudget =
          resolveColluvialSedimentBudget(surface, colluviumRock, sourceCandidates);
      List<ResolvedColluvialSource> sources =
          resolveColluvialSourceContributions(sourceCandidates, sedimentBudget);
      ColluvialTextureState textureState = resolveColluvialTexture(sedimentBudget);
      ColluvialPhysicalState physicalState =
          ColluvialPhysicalState.derive(
              textureState,
              colluviumRock.porosityDistribution(),
              colluviumRock.permeabilityDistribution(),
              colluviumRock.erodibilityDistribution());
      List<ColluvialSourceContribution> sourceContributions =
          sources.stream().map(ResolvedColluvialSource::contribution).toList();
      ColluvialSourceMix sourceMix =
          new ColluvialSourceMix(
              initialUpslopeDirection,
              sourceContributions,
              sedimentBudget.weatheredMatrixFractionPpm(),
              textureState,
              physicalState,
              sedimentBudget,
              ColluvialHorizonState.from(sedimentBudget),
              colluvialRoutePolicy,
              resolveColluvialSinkDestinations(sedimentBudget));
      StableId colluvialBodyId = colluvialBodyId(sourceMix);
      surface =
          new SurfaceSample(surface.fields(), bedrock, Lithology.SOIL_COLLUVIUM, Overprint.NONE);
      surfaceGeology =
          new GeologicalSample(
              bedrock.point(),
              bedrock.macroDomainId(),
              bedrock.provinceId(),
              colluvialBodyId,
              surface.surfaceMaterial(),
              COLLUVIUM_FORMATION_AGE,
              surface.surfaceOverprint(),
              false,
              List.of());
      context =
          new SurfaceMaterialContext(
              SurfaceMaterialKind.COLLUVIAL_MANTLE,
              colluvialBodyId,
              sourceMix.sourceBodyIds(),
              Optional.of(sourceMix),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              0,
              0);
      material = resolveColluvialMaterial(province, surfaceGeology, sourceMix, sources);
    } else {
      SurfaceMaterialKind kind =
          surface.fields().outcrop()
              ? SurfaceMaterialKind.BEDROCK_OUTCROP
              : SurfaceMaterialKind.IN_SITU_REGOLITH;
      surfaceGeology =
          new GeologicalSample(
              bedrock.point(),
              bedrock.macroDomainId(),
              bedrock.provinceId(),
              bedrock.rockBodyId(),
              surface.surfaceMaterial(),
              bedrock.formationAge(),
              surface.surfaceOverprint(),
              bedrock.faultDamageZone(),
              bedrock.depositIds());
      context =
          new SurfaceMaterialContext(
              kind,
              bedrock.rockBodyId(),
              List.of(bedrock.rockBodyId()),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              0,
              0);
      material = resolve(province, surfaceGeology);
    }
    return new SurfacePetrologicSample(surface, material, context);
  }

  private boolean formsColluvialMantle(SurfaceSample surface) {
    return !surface.fields().outcrop()
        && !surface.fields().drainage().channel()
        && surface.fields().slope() >= colluvialRoutePolicy.minimumSlope()
        && surface.fields().weatheringDepth() >= colluvialRoutePolicy.minimumWeatheringDepth()
        && surface.fields().drainage().channelDistance()
            >= colluvialRoutePolicy.minimumChannelDistance();
  }

  private List<ColluvialSourceCandidate> resolveColluvialSourceCandidates(
      SurfaceSample surface, Point2 initialUpslopeDirection) {
    List<SurfaceSample> pathSurfaces = new ArrayList<>();
    List<ColluvialSedimentBudget.TerrainPathReach> pathReaches = new ArrayList<>();
    SurfaceSample currentSurface = surface;
    Point2 reachDirection = initialUpslopeDirection;
    for (int distance = 0;
        distance < colluvialRoutePolicy.farSourceDistanceBlocks();
        distance += colluvialRoutePolicy.pathReachLengthBlocks()) {
      pathSurfaces.add(currentSurface);
      TerrainDirection terrainDirection =
          distance == 0
              ? new TerrainDirection(initialUpslopeDirection, false)
              : terrainUpslopeDirection(currentSurface, reachDirection);
      RouteDirectionDecision routeDecision =
          routeDirection(initialUpslopeDirection, terrainDirection.direction());
      Point2 nextPoint =
          upslopePoint(
              currentSurface.fields().point(),
              routeDecision.direction(),
              colluvialRoutePolicy.pathReachLengthBlocks());
      pathReaches.add(
          new ColluvialSedimentBudget.TerrainPathReach(
              distance,
              currentSurface.fields().point(),
              nextPoint,
              terrainDirection.direction(),
              routeDecision.direction(),
              terrainDirection.flatTerrainFallback(),
              routeDecision.deflectionClipped()));
      currentSurface = geology.surface(nextPoint);
      reachDirection = routeDecision.direction();
    }
    pathSurfaces.add(currentSurface);
    List<ColluvialSourceCandidate> sources = new ArrayList<>();
    sources.add(
        resolveColluvialSourceCandidate(
            pathSurfaces.getFirst(),
            terrainPath(pathSurfaces, pathReaches, 0),
            colluvialRoutePolicy.localSourceCapacityFixedUnits()));
    sources.add(
        resolveColluvialSourceCandidate(
            pathSurfaces.get(
                colluvialRoutePolicy.nearSourceDistanceBlocks()
                    / colluvialRoutePolicy.pathReachLengthBlocks()),
            terrainPath(pathSurfaces, pathReaches, colluvialRoutePolicy.nearSourceDistanceBlocks()),
            colluvialRoutePolicy.nearSourceCapacityFixedUnits()));
    sources.add(
        resolveColluvialSourceCandidate(
            pathSurfaces.getLast(),
            terrainPath(pathSurfaces, pathReaches, colluvialRoutePolicy.farSourceDistanceBlocks()),
            colluvialRoutePolicy.farSourceCapacityFixedUnits()));
    return List.copyOf(sources);
  }

  private ColluvialSedimentBudget.TerrainPath terrainPath(
      List<SurfaceSample> pathSurfaces,
      List<ColluvialSedimentBudget.TerrainPathReach> pathReaches,
      int sourceDistanceBlocks) {
    int sourceIndex = sourceDistanceBlocks / colluvialRoutePolicy.pathReachLengthBlocks();
    List<ColluvialSedimentBudget.TerrainPathSample> samples = new ArrayList<>();
    for (int index = 0; index <= sourceIndex; index++) {
      samples.add(
          new ColluvialSedimentBudget.TerrainPathSample(
              index * colluvialRoutePolicy.pathReachLengthBlocks(),
              pathSurfaces.get(index).fields().point(),
              pathSurfaces.get(index).fields().elevation()));
    }
    return new ColluvialSedimentBudget.TerrainPath(
        colluvialRoutePolicy.pathReachLengthBlocks(), samples, pathReaches.subList(0, sourceIndex));
  }

  private TerrainDirection terrainUpslopeDirection(
      SurfaceSample surface, Point2 flatTerrainFallback) {
    Point2 point = surface.fields().point();
    double step = colluvialRoutePolicy.gradientStepBlocks();
    double gradientX =
        (surfaceElevation(point.add(step, 0.0)) - surfaceElevation(point.add(-step, 0.0)))
            / (2.0 * step);
    double gradientZ =
        (surfaceElevation(point.add(0.0, step)) - surfaceElevation(point.add(0.0, -step)))
            / (2.0 * step);
    double length = StrictMath.hypot(gradientX, gradientZ);
    if (length <= 1.0e-12) {
      if (flatTerrainFallback != null) {
        return new TerrainDirection(flatTerrainFallback, true);
      }
      throw new IllegalStateException("initial colluvial terrain gradient must be non-zero");
    }
    return new TerrainDirection(new Point2(gradientX / length, gradientZ / length), false);
  }

  private double surfaceElevation(Point2 point) {
    return geology.surface(point).fields().elevation();
  }

  private RouteDirectionDecision routeDirection(Point2 initialDirection, Point2 localDirection) {
    double maximumDeflectionRadians =
        StrictMath.toRadians(colluvialRoutePolicy.maximumDeflectionDegrees());
    double signedDeflection =
        StrictMath.atan2(
            initialDirection.x() * localDirection.z() - initialDirection.z() * localDirection.x(),
            initialDirection.x() * localDirection.x() + initialDirection.z() * localDirection.z());
    double boundedDeflection =
        StrictMath.max(
            -maximumDeflectionRadians, StrictMath.min(maximumDeflectionRadians, signedDeflection));
    double cosine = StrictMath.cos(boundedDeflection);
    double sine = StrictMath.sin(boundedDeflection);
    return new RouteDirectionDecision(
        new Point2(
            initialDirection.x() * cosine - initialDirection.z() * sine,
            initialDirection.x() * sine + initialDirection.z() * cosine),
        StrictMath.abs(signedDeflection) > maximumDeflectionRadians + 1.0e-12);
  }

  private double terrainRoughnessIndex(SurfaceSample centerSurface) {
    Point2 center = centerSurface.fields().point();
    double centerElevation = centerSurface.fields().elevation();
    double radius = colluvialRoutePolicy.roughnessStencilRadiusBlocks();
    double east = surfaceElevation(center.add(radius, 0.0));
    double west = surfaceElevation(center.add(-radius, 0.0));
    double south = surfaceElevation(center.add(0.0, radius));
    double north = surfaceElevation(center.add(0.0, -radius));
    double gradientX = (east - west) / (2.0 * radius);
    double gradientZ = (south - north) / (2.0 * radius);
    double squaredResiduals =
        squaredTerrainResidual(east, centerElevation, gradientX, gradientZ, radius, 0.0)
            + squaredTerrainResidual(west, centerElevation, gradientX, gradientZ, -radius, 0.0)
            + squaredTerrainResidual(south, centerElevation, gradientX, gradientZ, 0.0, radius)
            + squaredTerrainResidual(north, centerElevation, gradientX, gradientZ, 0.0, -radius)
            + squaredTerrainResidual(
                surfaceElevation(center.add(radius, radius)),
                centerElevation,
                gradientX,
                gradientZ,
                radius,
                radius)
            + squaredTerrainResidual(
                surfaceElevation(center.add(radius, -radius)),
                centerElevation,
                gradientX,
                gradientZ,
                radius,
                -radius)
            + squaredTerrainResidual(
                surfaceElevation(center.add(-radius, radius)),
                centerElevation,
                gradientX,
                gradientZ,
                -radius,
                radius)
            + squaredTerrainResidual(
                surfaceElevation(center.add(-radius, -radius)),
                centerElevation,
                gradientX,
                gradientZ,
                -radius,
                -radius);
    double rmsRelief = StrictMath.sqrt(squaredResiduals / 8.0);
    double rmsResidualSlope = rmsRelief / radius;
    return clamp(rmsResidualSlope / TERRAIN_ROUGHNESS_REFERENCE_RESIDUAL_SLOPE);
  }

  private static double squaredTerrainResidual(
      double elevation,
      double centerElevation,
      double gradientX,
      double gradientZ,
      double offsetX,
      double offsetZ) {
    double residual = elevation - (centerElevation + gradientX * offsetX + gradientZ * offsetZ);
    return residual * residual;
  }

  private static Point2 upslopePoint(Point2 point, Point2 direction, int distanceBlocks) {
    return point.add(direction.x() * distanceBlocks, direction.z() * distanceBlocks);
  }

  private ColluvialSourceCandidate resolveColluvialSourceCandidate(
      SurfaceSample sourceSurface,
      ColluvialSedimentBudget.TerrainPath terrainPath,
      long capacityFixedUnits) {
    Point2 sourcePoint = sourceSurface.fields().point();
    GeologicalSample source = sourceSurface.bedrock();
    Province sourceProvince = geology.atlas().provinceAt(sourcePoint);
    if (!sourceProvince.id().equals(source.provinceId())) {
      throw new IllegalStateException("colluvial source owner changed between query stages");
    }
    return new ColluvialSourceCandidate(
        sourceSurface,
        sourceProvince.id(),
        resolve(sourceProvince, source),
        terrainPath.distanceBlocks(),
        capacityFixedUnits,
        terrainRoughnessIndex(sourceSurface),
        terrainPath);
  }

  private ColluvialSedimentBudget resolveColluvialSedimentBudget(
      SurfaceSample depositionSurface,
      RockDefinition colluviumRock,
      List<ColluvialSourceCandidate> sources) {
    ColluvialSedimentBudget.ProductionInput weatheredMatrixInput =
        new ColluvialSedimentBudget.ProductionInput(
            colluvialRoutePolicy.weatheredMatrixCapacityFixedUnits(),
            depositionSurface.fields().weatheringDepth(),
            depositionSurface.fields().slope(),
            colluviumRock.erodibilityIndex(),
            sources.getFirst().terrainRoughnessIndex(),
            depositionSurface.fields().drainage().flowAccumulation(),
            sources.getFirst().terrainPath(),
            colluviumRock.sedimentYield());
    List<ColluvialSedimentBudget.SourceProductionInput> sourceInputs =
        sources.stream()
            .map(
                source ->
                    new ColluvialSedimentBudget.SourceProductionInput(
                        source.surface().bedrock().rockBodyId(),
                        source.upslopeDistanceBlocks(),
                        new ColluvialSedimentBudget.ProductionInput(
                            source.capacityFixedUnits(),
                            source.surface().fields().weatheringDepth(),
                            source.surface().fields().slope(),
                            source.material().erodibilityIndex(),
                            source.terrainRoughnessIndex(),
                            source.surface().fields().drainage().flowAccumulation(),
                            source.terrainPath(),
                            source.material().rock().sedimentYield())))
            .toList();
    return ColluvialSedimentBudget.derive(
        depositionSurface.fields().slope(),
        weatheredMatrixInput,
        sourceInputs,
        colluvialTransportPolicy);
  }

  private static List<ResolvedColluvialSource> resolveColluvialSourceContributions(
      List<ColluvialSourceCandidate> candidates, ColluvialSedimentBudget budget) {
    return candidates.stream()
        .map(
            candidate -> {
              GeologicalSample source = candidate.surface().bedrock();
              long fractionPpm =
                  budget.sourceFractionPpm(source.rockBodyId(), candidate.upslopeDistanceBlocks());
              ColluvialSourceContribution contribution =
                  new ColluvialSourceContribution(
                      candidate.surface().fields().point(),
                      candidate.sourceProvinceId(),
                      source.rockBodyId(),
                      source.lithology(),
                      source.overprint(),
                      candidate.upslopeDistanceBlocks(),
                      fractionPpm);
              return new ResolvedColluvialSource(contribution, candidate.material());
            })
        .toList();
  }

  private List<ColluvialSinkDestination> resolveColluvialSinkDestinations(
      ColluvialSedimentBudget budget) {
    List<ColluvialSinkDestination> destinations = new ArrayList<>();
    addColluvialSinkDestinations(
        destinations, Optional.empty(), 0, budget.weatheredMatrixBalance());
    for (ColluvialSedimentBudget.SourceBalance source : budget.sourceBalances()) {
      addColluvialSinkDestinations(
          destinations,
          Optional.of(source.sourceBodyId()),
          source.upslopeDistanceBlocks(),
          source.balance());
    }
    return List.copyOf(destinations);
  }

  private void addColluvialSinkDestinations(
      List<ColluvialSinkDestination> destinations,
      Optional<StableId> sourceBodyId,
      int distance,
      ColluvialSedimentBudget.InputBalance balance) {
    ColluvialSinkAllocation allocation = balance.sinkAllocation();
    if (allocation.hasTransportLoss()) {
      destinations.add(
          resolveColluvialSinkDestination(
              ColluvialSinkState.SinkRole.INTERMEDIATE_ROUTE_STORAGE,
              sourceBodyId,
              distance,
              allocation.transportLossPoint(),
              balance.transportLossFixedUnits()));
    }
    if (allocation.hasBypass()) {
      destinations.add(
          resolveColluvialSinkDestination(
              ColluvialSinkState.SinkRole.DOWNSTREAM_CONTINUATION,
              sourceBodyId,
              distance,
              allocation.bypassPoint(),
              balance.bypassedFixedUnits()));
    }
  }

  private ColluvialSinkDestination resolveColluvialSinkDestination(
      ColluvialSinkState.SinkRole sinkRole,
      Optional<StableId> sourceBodyId,
      int distance,
      Point2 point,
      long fixedUnits) {
    SurfaceSample receivingSurface = geology.surface(point);
    GeologicalSample receivingBedrock = receivingSurface.bedrock();
    Province receivingProvince = geology.atlas().provinceAt(point);
    if (!receivingProvince.id().equals(receivingBedrock.provinceId())) {
      throw new IllegalStateException(
          "colluvial sink destination owner changed between query stages");
    }
    return new ColluvialSinkDestination(
        sinkRole,
        sourceBodyId,
        distance,
        point,
        receivingProvince.id(),
        receivingBedrock.rockBodyId(),
        receivingSurface.surfaceMaterial(),
        receivingSurface.surfaceOverprint(),
        receivingBedrock.lithology(),
        receivingBedrock.overprint(),
        fixedUnits);
  }

  private StableId colluvialBodyId(ColluvialSourceMix sourceMix) {
    StringBuilder purpose = new StringBuilder("material-body-id");
    for (ColluvialSourceContribution contribution : sourceMix.sourceContributions()) {
      purpose
          .append(':')
          .append(contribution.upslopeDistanceBlocks())
          .append(':')
          .append(contribution.sourceProvinceId())
          .append(':')
          .append(contribution.sourceBodyId())
          .append(':')
          .append(contribution.sourceLithology().name())
          .append(':')
          .append(contribution.sourceOverprint().name())
          .append(':')
          .append(contribution.assemblageFractionPpm());
    }
    purpose.append(":matrix:").append(sourceMix.weatheredMatrixFractionPpm());
    SedimentGrainSize grainSize = sourceMix.textureState().grainSize();
    purpose
        .append(":grain:")
        .append(grainSize.gravelAndCoarserPpm())
        .append(':')
        .append(grainSize.sandPpm())
        .append(':')
        .append(grainSize.finesPpm())
        .append(':')
        .append(sourceMix.textureState().sorting().name())
        .append(":sorting-dominance:")
        .append(sourceMix.textureState().sortingDominanceIndex())
        .append(':')
        .append(sourceMix.textureState().support().name())
        .append(':')
        .append(sourceMix.textureState().clastShape().name());
    ColluvialGrainDispersionState dispersionState = sourceMix.textureState().dispersionState();
    purpose
        .append(":dispersion:")
        .append(dispersionState.dispersionClass().name())
        .append(':')
        .append(dispersionState.coarseSpreadIndex())
        .append(':')
        .append(dispersionState.sandSpreadIndex())
        .append(':')
        .append(dispersionState.finesSpreadIndex())
        .append(':')
        .append(dispersionState.weightedSpreadIndex());
    ColluvialPhysicalState physicalState = sourceMix.physicalState();
    purpose
        .append(":physical:")
        .append(physicalState.porosityQuantile())
        .append(':')
        .append(physicalState.permeabilityQuantile())
        .append(':')
        .append(physicalState.erodibilityQuantile())
        .append(':')
        .append(physicalState.porosityFraction())
        .append(':')
        .append(physicalState.permeabilityIndex())
        .append(':')
        .append(physicalState.erodibilityIndex());
    ColluvialCohesionState cohesionState = physicalState.cohesionState();
    purpose
        .append(":cohesion:")
        .append(cohesionState.cohesionClass().name())
        .append(':')
        .append(cohesionState.finesFraction())
        .append(':')
        .append(cohesionState.cohesionIndex())
        .append(':')
        .append(cohesionState.cohesionAdjustedErodibilityIndex());
    ColluvialHydraulicState hydraulicState = physicalState.hydraulicState();
    purpose
        .append(":hydraulic:")
        .append(hydraulicState.hydraulicClass().name())
        .append(':')
        .append(hydraulicState.waterStorageIndex())
        .append(':')
        .append(hydraulicState.infiltrationIndex())
        .append(':')
        .append(hydraulicState.drainageIndex())
        .append(':')
        .append(hydraulicState.runoffPartitionIndex());
    ColluvialHorizonState horizonState = sourceMix.horizonState();
    purpose
        .append(":horizon:")
        .append(horizonState.profileClass().name())
        .append(':')
        .append(horizonState.weatheringIndex())
        .append(':')
        .append(horizonState.weatheredMatrixFractionPpm())
        .append(':')
        .append(horizonState.transportedSourceFractionPpm());
    ColluvialSedimentBudget sedimentBudget = sourceMix.sedimentBudget();
    purpose
        .append(":sediment-budget:")
        .append(sedimentBudget.unit())
        .append(':')
        .append(sedimentBudget.grainTransportModel().name())
        .append(':')
        .append(sedimentBudget.depositionSlope())
        .append(':')
        .append(sedimentBudget.sourceCapacityFixedUnits())
        .append(':')
        .append(sedimentBudget.mobilizedInventoryFixedUnits())
        .append(':')
        .append(sedimentBudget.retainedInventoryFixedUnits())
        .append(':')
        .append(sedimentBudget.transportLossFixedUnits())
        .append(':')
        .append(sedimentBudget.bypassedInventoryFixedUnits())
        .append(':')
        .append(sedimentBudget.depositedInventoryFixedUnits())
        .append(":matrix:");
    appendTransportPolicy(purpose, sedimentBudget.transportPolicy());
    appendColluvialInputBalance(purpose, sedimentBudget.weatheredMatrixBalance());
    for (ColluvialSedimentBudget.SourceBalance source : sedimentBudget.sourceBalances()) {
      purpose
          .append(":source:")
          .append(source.upslopeDistanceBlocks())
          .append(':')
          .append(source.sourceBodyId())
          .append(':');
      appendColluvialInputBalance(purpose, source.balance());
    }
    for (ColluvialSourceUsage usage : sedimentBudget.sourceUsages()) {
      purpose
          .append(":source-usage:")
          .append(usage.sourceBodyId())
          .append(':')
          .append(usage.trancheCount())
          .append(':')
          .append(usage.claimedCapacityFixedUnits())
          .append(':')
          .append(usage.mobilizedFixedUnits())
          .append(':')
          .append(usage.retainedFixedUnits())
          .append(':')
          .append(usage.transportLossFixedUnits())
          .append(':')
          .append(usage.bypassedFixedUnits())
          .append(':')
          .append(usage.depositedFixedUnits());
    }
    for (ColluvialSourceGrainShare grainShare : sedimentBudget.sourceGrainShares()) {
      purpose
          .append(":source-grain:")
          .append(grainShare.sourceBodyId())
          .append(':')
          .append(grainShare.upslopeDistanceBlocks())
          .append(':')
          .append(grainShare.depositedFixedUnits())
          .append(':')
          .append(grainShare.depositedGrainMass().gravelAndCoarserFixedUnits())
          .append(':')
          .append(grainShare.depositedGrainMass().sandFixedUnits())
          .append(':')
          .append(grainShare.depositedGrainMass().finesFixedUnits());
    }
    for (ColluvialGrainSourceShare grainShare : sedimentBudget.grainSourceShares()) {
      purpose
          .append(":grain-source:")
          .append(grainShare.sourceRole())
          .append(':')
          .append(grainShare.sourceBodyId().map(StableId::toString).orElse("weathered-matrix"))
          .append(':')
          .append(grainShare.upslopeDistanceBlocks())
          .append(':')
          .append(grainShare.gravelAndCoarserFractionPpm())
          .append(':')
          .append(grainShare.sandFractionPpm())
          .append(':')
          .append(grainShare.finesFractionPpm());
    }
    appendRoutePolicy(purpose, sourceMix.routePolicy());
    ColluvialTransportProcessMix processMix = sedimentBudget.transportProcessMix();
    purpose
        .append(":process-mix:")
        .append(processMix.dominantProcess())
        .append(':')
        .append(processMix.hillslopeCreepFractionPpm())
        .append(':')
        .append(processMix.sheetwashFractionPpm())
        .append(':')
        .append(processMix.dryRavelFractionPpm());
    for (ColluvialTransportProcessUsage usage : sedimentBudget.transportProcessUsages()) {
      purpose
          .append(":process-usage:")
          .append(usage.processClass())
          .append(':')
          .append(usage.trancheCount())
          .append(':')
          .append(usage.capacityFixedUnits())
          .append(':')
          .append(usage.mobilizedFixedUnits())
          .append(':')
          .append(usage.retainedFixedUnits())
          .append(':')
          .append(usage.transportLossFixedUnits())
          .append(':')
          .append(usage.bypassedFixedUnits())
          .append(':')
          .append(usage.depositedFixedUnits())
          .append(':')
          .append(usage.depositedGrainMass().gravelAndCoarserFixedUnits())
          .append(':')
          .append(usage.depositedGrainMass().sandFixedUnits())
          .append(':')
          .append(usage.depositedGrainMass().finesFixedUnits());
    }
    ColluvialTransportProcessStageMix processStages = sedimentBudget.transportProcessStageMix();
    appendProcessMix(purpose, ":process-stage-capacity:", processStages.capacity());
    appendProcessMix(purpose, ":process-stage-mobilized:", processStages.mobilized());
    appendProcessMix(purpose, ":process-stage-arrived:", processStages.arrived());
    appendProcessMix(purpose, ":process-stage-deposited:", processStages.deposited());
    return StableId.first128(
        materialIdentity
            .objectStream(
                "surface-material", "colluvial-mantle", sourceMix.localSource().sourceBodyId())
            .bytes(purpose.toString(), 0));
  }

  private static void appendColluvialInputBalance(
      StringBuilder purpose, ColluvialSedimentBudget.InputBalance balance) {
    ColluvialSedimentBudget.ProductionInput input = balance.input();
    ColluvialTransportProcess transportProcess = balance.transportProcess();
    ColluvialProductionState productionState = balance.productionState();
    ColluvialSinkState sinkState = balance.sinkState();
    ColluvialSinkAllocation sinkAllocation = balance.sinkAllocation();
    purpose
        .append(input.capacityFixedUnits())
        .append(':')
        .append(input.weatheringDepth())
        .append(':')
        .append(input.slope())
        .append(':')
        .append(input.erodibilityIndex())
        .append(':')
        .append(input.terrainRoughnessIndex())
        .append(':')
        .append(input.runoffIndex())
        .append(":transport-process:")
        .append(transportProcess.processClass().name())
        .append(':')
        .append(transportProcess.hillslopeCreepScore())
        .append(':')
        .append(transportProcess.sheetwashScore())
        .append(':')
        .append(transportProcess.dryRavelScore())
        .append(":production-state:")
        .append(productionState.weatheringAvailability())
        .append(':')
        .append(productionState.erodibilityResponse())
        .append(':')
        .append(productionState.slopeMobilityResponse())
        .append(':')
        .append(productionState.runoffMobilityResponse())
        .append(':')
        .append(productionState.mobilizationPotential())
        .append(':')
        .append(productionState.processResponse())
        .append(':')
        .append(productionState.mobilizedFraction())
        .append(':')
        .append(productionState.retainedFraction())
        .append(':')
        .append(productionState.transportArrivalFraction())
        .append(':')
        .append(productionState.depositionFraction())
        .append(':')
        .append(productionState.netDepositionFraction())
        .append(":sink-state:")
        .append(sinkState.transportLossSink().name())
        .append(':')
        .append(sinkState.bypassSink().name())
        .append(':')
        .append(sinkState.transportLossFraction())
        .append(':')
        .append(sinkState.bypassFraction())
        .append(":sink-allocation:")
        .append(sinkAllocation.transportLossDistanceBlocks())
        .append(':')
        .append(sinkAllocation.transportLossPoint().x())
        .append(':')
        .append(sinkAllocation.transportLossPoint().z())
        .append(':')
        .append(sinkAllocation.bypassDistanceBlocks())
        .append(':')
        .append(sinkAllocation.bypassPoint().x())
        .append(':')
        .append(sinkAllocation.bypassPoint().z())
        .append(':')
        .append(input.terrainPath().reachLengthBlocks())
        .append(':')
        .append(input.terrainPath().downslopeContinuityIndex())
        .append(':')
        .append(input.terrainPath().straightLineDistanceBlocks())
        .append(':')
        .append(input.terrainPath().routedDistanceBlocks())
        .append(':')
        .append(input.terrainPath().routeDirectnessIndex())
        .append(':')
        .append(input.terrainPath().netUpslopeReliefBlocks())
        .append(':')
        .append(
            input
                .terrainPath()
                .routeGradeIndex(balance.transportPolicy().slopeMobilityReference()));
    for (ColluvialSedimentBudget.TerrainPathSample sample : input.terrainPath().samples()) {
      purpose
          .append(':')
          .append(sample.upslopeDistanceBlocks())
          .append(':')
          .append(sample.elevation());
    }
    for (ColluvialSedimentBudget.TerrainPathReach reach : input.terrainPath().reaches()) {
      purpose
          .append(":reach:")
          .append(reach.upslopeDistanceBlocks())
          .append(':')
          .append(reach.rawUpslopeDirection().x())
          .append(':')
          .append(reach.rawUpslopeDirection().z())
          .append(':')
          .append(reach.routedUpslopeDirection().x())
          .append(':')
          .append(reach.routedUpslopeDirection().z())
          .append(':')
          .append(reach.flatTerrainFallback())
          .append(':')
          .append(reach.deflectionClipped());
    }
    purpose
        .append(':')
        .append(input.sedimentYield().gravelAndCoarserPpm())
        .append(':')
        .append(input.sedimentYield().sandPpm())
        .append(':')
        .append(input.sedimentYield().finesPpm())
        .append(':')
        .append(balance.mobilizedFixedUnits())
        .append(':')
        .append(balance.retainedFixedUnits())
        .append(':')
        .append(balance.transportLossFixedUnits())
        .append(':')
        .append(balance.bypassedFixedUnits())
        .append(':')
        .append(balance.depositedFixedUnits());
    appendColluvialGrainMass(purpose, balance.capacityGrainMass());
    appendColluvialGrainMass(purpose, balance.mobilizedGrainMass());
    appendColluvialGrainMass(purpose, balance.retainedGrainMass());
    appendColluvialGrainMass(purpose, balance.transportLossGrainMass());
    appendColluvialGrainMass(purpose, balance.bypassedGrainMass());
    appendColluvialGrainMass(purpose, balance.depositedGrainMass());
  }

  private static void appendProcessMix(
      StringBuilder purpose, String prefix, ColluvialTransportProcessMix processMix) {
    purpose
        .append(prefix)
        .append(processMix.dominantProcess())
        .append(':')
        .append(processMix.hillslopeCreepFractionPpm())
        .append(':')
        .append(processMix.sheetwashFractionPpm())
        .append(':')
        .append(processMix.dryRavelFractionPpm());
  }

  private static void appendRoutePolicy(StringBuilder purpose, ColluvialRoutePolicy policy) {
    purpose
        .append(":route-policy:")
        .append(policy.minimumSlope())
        .append(':')
        .append(policy.minimumWeatheringDepth())
        .append(':')
        .append(policy.minimumChannelDistance())
        .append(':')
        .append(policy.gradientStepBlocks())
        .append(':')
        .append(policy.roughnessStencilRadiusBlocks())
        .append(':')
        .append(policy.pathReachLengthBlocks())
        .append(':')
        .append(policy.nearSourceDistanceBlocks())
        .append(':')
        .append(policy.farSourceDistanceBlocks())
        .append(':')
        .append(policy.maximumDeflectionDegrees())
        .append(':')
        .append(policy.weatheredMatrixCapacityFixedUnits())
        .append(':')
        .append(policy.localSourceCapacityFixedUnits())
        .append(':')
        .append(policy.nearSourceCapacityFixedUnits())
        .append(':')
        .append(policy.farSourceCapacityFixedUnits());
  }

  private static void appendTransportPolicy(
      StringBuilder purpose, ColluvialTransportPolicy policy) {
    purpose
        .append(":transport-policy:")
        .append(policy.weatheringDepthReference())
        .append(':')
        .append(policy.slopeMobilityReference())
        .append(':')
        .append(policy.minimumSlopeMobility())
        .append(':')
        .append(policy.minimumRunoffMobilityResponse())
        .append(':')
        .append(policy.minimumTransportSlopeResponse())
        .append(':')
        .append(policy.minimumTransportRoughnessResponse())
        .append(':')
        .append(policy.minimumTransportPathResponse())
        .append(':')
        .append(policy.minimumTransportRouteGradeResponse())
        .append(':')
        .append(policy.minimumTransportRunoffResponse())
        .append(':')
        .append(policy.gravelAndCoarserReferenceEFoldingDistanceBlocks())
        .append(':')
        .append(policy.sandReferenceEFoldingDistanceBlocks())
        .append(':')
        .append(policy.finesReferenceEFoldingDistanceBlocks())
        .append(':')
        .append(policy.maximumBypassFraction())
        .append(':')
        .append(policy.hillslopeCreepResponse())
        .append(':')
        .append(policy.sheetwashResponse())
        .append(':')
        .append(policy.dryRavelResponse());
  }

  private static void appendColluvialGrainMass(
      StringBuilder purpose, ColluvialSedimentBudget.GrainMass grainMass) {
    purpose
        .append(':')
        .append(grainMass.gravelAndCoarserFixedUnits())
        .append(':')
        .append(grainMass.sandFixedUnits())
        .append(':')
        .append(grainMass.finesFixedUnits());
  }

  private static ColluvialTextureState resolveColluvialTexture(
      ColluvialSedimentBudget sedimentBudget) {
    return ColluvialTextureState.from(sedimentBudget.depositedGrainSize());
  }

  private PetrologicSample resolveColluvialMaterial(
      Province province,
      GeologicalSample geological,
      ColluvialSourceMix sourceMix,
      List<ResolvedColluvialSource> sources) {
    PetrologicSample generic = resolve(province, geological);
    List<MaterialAssemblage.Share> shares = new ArrayList<>();
    shares.add(
        new MaterialAssemblage.Share(
            generic.primaryAssemblage(), sourceMix.weatheredMatrixFractionPpm()));
    sources.forEach(
        source ->
            shares.add(
                new MaterialAssemblage.Share(
                    source.material().resolvedAssemblage(),
                    source.contribution().assemblageFractionPpm())));
    MaterialAssemblage mixed = MaterialAssemblage.weightedBlend(shares);
    BulkComposition composition = catalog.composition(mixed);
    List<SolidSolutionState> solidSolutions = catalog.solidSolutionStates(mixed);
    ElementTransferLedger elementLedger = ElementTransferLedger.between(composition, composition);
    return new PetrologicSample(
        geological,
        generic.rock(),
        generic.resolvedTexture(),
        mixed,
        mixed,
        solidSolutions,
        solidSolutions,
        composition,
        composition,
        elementLedger,
        generic.materialProcessLedger(),
        generic.alterationContribution(),
        generic.metamorphism(),
        generic.processClass(),
        generic.fluidState(),
        sourceMix.physicalState().porosityFraction(),
        sourceMix.physicalState().permeabilityIndex(),
        sourceMix.physicalState().erodibilityIndex(),
        generic.magmaLineage(),
        generic.mantleCargo(),
        generic.sedimentaryState(),
        generic.reservoirLedgers());
  }

  public PetrologicSample resolve(Province province, GeologicalSample geological) {
    if (!province.id().equals(geological.provinceId())) {
      throw new IllegalArgumentException("sample does not belong to the supplied province");
    }
    BodyRecipeKey key =
        new BodyRecipeKey(geological.rockBodyId(), geological.lithology(), geological.overprint());
    ResolvedRecipe recipe = bodyRecipeCache.get(key, this::compileBodyRecipe);
    RockDefinition rock = recipe.rock();
    AlterationDefinition alteration = catalog.requireAlteration(geological.overprint());
    MaterialProcessLedger processLedger =
        materialProcessLedger(province, geological, alteration, recipe.elementLedger());
    MetamorphicHistory metamorphism =
        metamorphicHistory(province, geological.point(), rock, alteration);
    AlterationContribution alterationContribution =
        AlterationContribution.from(
            processLedger,
            metamorphism.processState(),
            alteration,
            processEventAges(province, alteration.processClass()),
            recipe.primaryAssemblage(),
            recipe.resolvedAssemblage());

    return new PetrologicSample(
        geological,
        rock,
        alteration.responseTexture().orElse(rock.texture()),
        recipe.primaryAssemblage(),
        recipe.resolvedAssemblage(),
        recipe.primarySolidSolutions(),
        recipe.resolvedSolidSolutions(),
        recipe.primaryComposition(),
        recipe.resolvedComposition(),
        recipe.elementLedger(),
        processLedger,
        alterationContribution,
        metamorphism,
        alteration.processClass(),
        alteration.fluidState(),
        recipe.porosityFraction(),
        recipe.permeabilityIndex(),
        recipe.erodibilityIndex(),
        magmaLineage(province, geological),
        mantleCargo(geological),
        sedimentaryState(province, rock),
        ledgersForSample(province, geological));
  }

  private static MaterialProcessLedger materialProcessLedger(
      Province province,
      GeologicalSample geological,
      AlterationDefinition alteration,
      ElementTransferLedger elementLedger) {
    Optional<StableId> processId =
        switch (geological.overprint()) {
          case NONE -> Optional.empty();
          case CONTACT_HORNFELS -> Optional.of(province.geometry().aureoleId());
          case POTASSIC_ALTERATION, PHYLLIC_ALTERATION, PROPYLITIC_ALTERATION ->
              Optional.of(province.proofIds().porphyrySystemId());
          case CHLORITIC_FOOTWALL -> Optional.of(province.proofIds().vmsSystemId());
          case WEATHERED_UNCONFORMITY, OXIDIZED_GOSSAN, WEATHERED_REGOLITH ->
              Optional.of(province.proofIds().weatheringId());
        };
    return MaterialProcessLedger.from(
        processId,
        alteration.processClass(),
        processEvents(province, alteration.processClass()),
        elementLedger);
  }

  private static MetamorphicHistory metamorphicHistory(
      Province province, Point3 worldPoint, RockDefinition rock, AlterationDefinition alteration) {
    if (alteration.facies() != MetamorphicFacies.NONE) {
      return new MetamorphicHistory(
          rock.id(),
          MetamorphicGrade.HIGH,
          alteration.facies(),
          alteration.path(),
          alteration.minimumTemperatureCelsius(),
          alteration.maximumTemperatureCelsius(),
          alteration.minimumPressureMpa(),
          alteration.maximumPressureMpa(),
          events(province, EventType.CONTACT_METAMORPHISM),
          eventAges(province, EventType.CONTACT_METAMORPHISM),
          MetamorphicProcessState.proofFor(
              MetamorphicGrade.HIGH,
              alteration.facies(),
              alteration.path(),
              alteration.processClass(),
              alteration.replacementPpm(),
              alteration.fluidState(),
              Optional.of(rock.lithology())),
          RegionalMetamorphicState.proofFor(province, worldPoint));
    }
    if (rock.primaryMetamorphism().isPresent()) {
      PrimaryMetamorphicDefinition primary = rock.primaryMetamorphism().orElseThrow();
      return new MetamorphicHistory(
          primary.protolithRockId(),
          primary.grade(),
          primary.facies(),
          primary.path(),
          primary.minimumTemperatureCelsius(),
          primary.maximumTemperatureCelsius(),
          primary.minimumPressureMpa(),
          primary.maximumPressureMpa(),
          events(province, EventType.ESTABLISH_BASEMENT),
          eventAges(province, EventType.ESTABLISH_BASEMENT),
          MetamorphicProcessState.proofFor(
              primary.grade(),
              primary.facies(),
              primary.path(),
              MaterialProcessClass.NONE,
              0L,
              Optional.empty(),
              Optional.of(rock.lithology())),
          RegionalMetamorphicState.proofFor(province, worldPoint));
    }
    return new MetamorphicHistory(
        rock.id(),
        MetamorphicGrade.NONE,
        MetamorphicFacies.NONE,
        MetamorphicPath.NONE,
        alteration.minimumTemperatureCelsius(),
        alteration.maximumTemperatureCelsius(),
        alteration.minimumPressureMpa(),
        alteration.maximumPressureMpa(),
        processEvents(province, alteration.processClass()),
        processEventAges(province, alteration.processClass()),
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.NONE,
            MetamorphicFacies.NONE,
            MetamorphicPath.NONE,
            alteration.processClass(),
            alteration.replacementPpm(),
            alteration.fluidState(),
            Optional.of(rock.lithology())),
        RegionalMetamorphicState.proofFor(province, worldPoint));
  }

  private static Optional<MagmaLineageState> magmaLineage(
      Province province, GeologicalSample sample) {
    List<RiftArcGeometry.PlutonPulse> pulses = province.geometry().plutonPulses();
    for (int index = 0; index < pulses.size(); index++) {
      RiftArcGeometry.PlutonPulse pulse = pulses.get(index);
      if (pulse.id().equals(sample.rockBodyId())) {
        MagmaDifferentiationState differentiationState =
            MagmaDifferentiationState.arcProofFor(index, List.of(province.geometry().basementId()));
        double progress =
            differentiationState.cumulativeCrystalFractionPpm() / (double) MaterialAssemblage.SCALE;
        String fluidPotential = differentiationState.residualFluidPotential().wireValue();
        return Optional.of(
            new MagmaLineageState(
                province.proofIds().magmaLineageId(),
                pulse.id(),
                index,
                progress,
                "hydrated_mantle_wedge_plus_lower_crust",
                "water_rich",
                "oxidized",
                fluidPotential,
                differentiationState));
      }
    }
    return Optional.empty();
  }

  private Optional<MantleCargoState> mantleCargo(GeologicalSample sample) {
    if (sample.lithology() != Lithology.KIMBERLITIC) {
      return Optional.empty();
    }
    String diamondMineralId = "geological:mineral/diamond";
    List<String> indicatorMineralIds =
        List.of(
            "geological:mineral/chromite",
            "geological:mineral/diopside",
            "geological:mineral/ilmenite",
            "geological:mineral/pyrope");
    catalog.requireMineral(diamondMineralId);
    indicatorMineralIds.forEach(catalog::requireMineral);
    return Optional.of(
        new MantleCargoState(
            sample.rockBodyId(),
            Optional.empty(),
            MantleCargoStatus.SOURCE_CONTEXT_UNRESOLVED,
            diamondMineralId,
            0L,
            indicatorMineralIds));
  }

  private static Optional<SedimentaryState> sedimentaryState(
      Province province, RockDefinition rock) {
    if (rock.geneticFamily() != GeneticFamily.SEDIMENTARY) {
      return Optional.empty();
    }
    List<StableId> sources = new ArrayList<>();
    sources.add(province.geometry().basementId());
    if (rock.lithology() == Lithology.MARINE_VOLCANICLASTIC) {
      sources.add(province.proofIds().magmaLineageId());
    }
    return Optional.of(
        switch (rock.lithology()) {
          case BASAL_CONGLOMERATE ->
              new SedimentaryState(
                  "rift_margin_alluvial_fan",
                  "gravel",
                  "compositionally_immature",
                  "compacted_and_locally_cemented",
                  sources);
          case MARINE_VOLCANICLASTIC ->
              new SedimentaryState(
                  "submarine_volcanic_apron",
                  "sand_to_tuff",
                  "volcanogenic_immature",
                  "burial_chlorite_calcite",
                  sources);
          case BASIN_SHALE ->
              new SedimentaryState(
                  "offshore_low_energy",
                  "mud",
                  "clay_rich_mature",
                  "compacted_low_permeability",
                  sources);
          case BASIN_SANDSTONE ->
              new SedimentaryState(
                  "shallow_marine_shoreface",
                  "sand",
                  "submature",
                  "quartz_calcite_cemented",
                  sources);
          case SILTSTONE ->
              new SedimentaryState(
                  "delta_front_to_offshore_transition",
                  "silt",
                  "submature_micaceous",
                  "compacted_clay_matrix_and_calcite_cement",
                  sources);
          case LIMESTONE ->
              new SedimentaryState(
                  "carbonate_platform",
                  "carbonate_mud_to_sand",
                  "biochemical_chemical",
                  "calcite_lithified",
                  sources);
          case DOLOSTONE ->
              new SedimentaryState(
                  "dolomitized_carbonate_platform",
                  "carbonate_crystalline",
                  "replacement_modified",
                  "magnesium_replacement_and_recrystallization",
                  sources);
          case CHERT ->
              new SedimentaryState(
                  "marine_bedded_silica",
                  "microcrystalline",
                  "silica_rich",
                  "silica_precipitation_and_recrystallization",
                  sources);
          case BANDED_IRON_FORMATION ->
              new SedimentaryState(
                  "ancient_iron_silica_precipitation_basin",
                  "microcrystalline_banded",
                  "chemical_precipitate_redox_controlled",
                  "iron_oxide_carbonate_silica_recrystallization",
                  sources);
          case GYPSUM_ANHYDRITE_EVAPORITE ->
              new SedimentaryState(
                  "restricted_evaporite_margin",
                  "crystalline_sulfate",
                  "chemical_precipitate",
                  "gypsum_anhydrite_hydration_recrystallization",
                  sources);
          case HALITE_POTASH_EVAPORITE ->
              new SedimentaryState(
                  "restricted_evaporite_basin_center",
                  "crystalline_salt",
                  "late_stage_brine_precipitate",
                  "salt_recrystallization_dissolution_and_halokinesis",
                  sources);
          case COAL ->
              new SedimentaryState(
                  "buried_peat_mire",
                  "organic_bedded_with_clastic_partings",
                  "peat_derived_rank_unresolved",
                  "compaction_dewatering_and_burial_maturation",
                  sources);
          default ->
              throw new IllegalStateException("unmapped sedimentary lithology " + rock.lithology());
        });
  }

  private static List<StableId> processEvents(
      Province province, MaterialProcessClass processClass) {
    return switch (processClass) {
      case NONE -> List.of();
      case ISOCHEMICAL_METAMORPHISM -> events(province, EventType.CONTACT_METAMORPHISM);
      case HYDROTHERMAL_METASOMATISM -> events(province, EventType.MINERALIZE);
      case WEATHERING -> events(province, EventType.WEATHER);
    };
  }

  private static List<AgeKey> processEventAges(
      Province province, MaterialProcessClass processClass) {
    return switch (processClass) {
      case NONE -> List.of();
      case ISOCHEMICAL_METAMORPHISM -> eventAges(province, EventType.CONTACT_METAMORPHISM);
      case HYDROTHERMAL_METASOMATISM -> eventAges(province, EventType.MINERALIZE);
      case WEATHERING -> eventAges(province, EventType.WEATHER);
    };
  }

  private static List<StableId> events(Province province, EventType type) {
    return eventRecords(province, type).stream().map(GeologicalEvent::id).toList();
  }

  private static List<AgeKey> eventAges(Province province, EventType type) {
    return eventRecords(province, type).stream().map(GeologicalEvent::age).toList();
  }

  private static List<GeologicalEvent> eventRecords(Province province, EventType type) {
    return province.chronicle().events().stream()
        .filter(event -> event.type() == type)
        .sorted(Comparator.comparing(GeologicalEvent::age).thenComparing(GeologicalEvent::id))
        .toList();
  }

  private static Map<RecipeKey, RecipeTemplate> compileRecipeTemplates(
      MaterialCatalogSnapshot catalog) {
    Map<RecipeKey, RecipeTemplate> result = new HashMap<>();
    for (Lithology lithology : Lithology.values()) {
      RockDefinition rock = catalog.requireRock(lithology);
      for (Overprint overprint : Overprint.values()) {
        AlterationDefinition alteration = catalog.requireAlteration(overprint);
        if (result.put(new RecipeKey(lithology, overprint), new RecipeTemplate(rock, alteration))
            != null) {
          throw new IllegalStateException("duplicate material recipe");
        }
      }
    }
    return Map.copyOf(result);
  }

  private ResolvedRecipe compileBodyRecipe(BodyRecipeKey key) {
    RecipeTemplate template = recipeTemplates.get(new RecipeKey(key.lithology(), key.overprint()));
    if (template == null) {
      throw new IllegalStateException("material recipe matrix is incomplete");
    }
    RockDefinition rock = template.rock();
    AlterationDefinition alteration = template.alteration();
    MaterialAssemblage primary = compositionSampler.sample(rock, key.bodyId());
    MaterialAssemblage resolved =
        alteration.replacementPpm() == 0
            ? primary
            : MaterialAssemblage.blend(
                primary,
                alteration.targetAssemblage(rock.geneticFamily()),
                alteration.replacementPpm());
    List<SolidSolutionState> primarySolidSolutions = catalog.solidSolutionStates(primary);
    List<SolidSolutionState> resolvedSolidSolutions = catalog.solidSolutionStates(resolved);
    BulkComposition primaryComposition = catalog.composition(primary);
    BulkComposition resolvedComposition = catalog.composition(resolved);
    ElementTransferLedger ledger =
        ElementTransferLedger.between(primaryComposition, resolvedComposition);
    if (alteration.processClass() == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM
        && !ledger.isIsochemical()) {
      throw new IllegalStateException("isochemical response changed bulk composition");
    }
    return new ResolvedRecipe(
        rock,
        primary,
        resolved,
        primarySolidSolutions,
        resolvedSolidSolutions,
        primaryComposition,
        resolvedComposition,
        ledger,
        clamp(
            compositionSampler.sample(
                    rock.porosityDistribution(), key.bodyId(), "porosity_fraction")
                * alteration.porosityMultiplier()),
        clamp(
            compositionSampler.sample(
                    rock.permeabilityDistribution(), key.bodyId(), "permeability_index")
                * StrictMath.sqrt(StrictMath.max(0.0, alteration.porosityMultiplier()))),
        clamp(
            compositionSampler.sample(
                    rock.erodibilityDistribution(), key.bodyId(), "erodibility_index")
                + alteration.erodibilityDelta()));
  }

  private List<ElementReservoirLedger> ledgersForSample(
      Province province, GeologicalSample geological) {
    if (geological.depositIds().isEmpty()) {
      return List.of();
    }
    Set<StableId> deposits = Set.copyOf(geological.depositIds());
    return elementReservoirLedgers(province).stream()
        .filter(ledger -> ledger.depositId().filter(deposits::contains).isPresent())
        .toList();
  }

  private List<ElementReservoirLedger> compileReservoirLedgers(Province province) {
    return geology.mineralDecisions(province).stream()
        .filter(decision -> decision.deposit() != null && decision.ledger() != null)
        .map(MaterialQueryEngine::reservoirLedger)
        .sorted(java.util.Comparator.comparing(ElementReservoirLedger::systemId))
        .toList();
  }

  private static ElementReservoirLedger reservoirLedger(MineralSystemDecision decision) {
    StableId source = decision.deposit().sourceIds().getFirst();
    StableId processId = decision.deposit().mineralSystemId();
    AgeKey age = decision.deposit().formationAge();
    List<ReservoirTransfer> transfers =
        decision.ledger().allocations().entrySet().stream()
            .map(
                allocation -> {
                  String role = allocation.getKey();
                  if (role.equals("deposit") || role.equals("placer_trap")) {
                    return new ReservoirTransfer(
                        role,
                        ReservoirSinkKind.DEPOSIT,
                        Optional.of(decision.deposit().id()),
                        allocation.getValue(),
                        Optional.of(processId),
                        Optional.of(age),
                        950_000L);
                  }
                  if (role.startsWith("retained_")) {
                    return new ReservoirTransfer(
                        role,
                        ReservoirSinkKind.RETAINED_SOURCE,
                        Optional.of(source),
                        allocation.getValue(),
                        Optional.of(processId),
                        Optional.of(age),
                        900_000L);
                  }
                  ReservoirSinkKind kind =
                      role.contains("transport")
                          ? ReservoirSinkKind.TRANSPORT_LOSS
                          : ReservoirSinkKind.DIFFUSE_HALO_OR_LOSS;
                  return new ReservoirTransfer(
                      role,
                      kind,
                      Optional.empty(),
                      allocation.getValue(),
                      Optional.of(processId),
                      Optional.of(age),
                      kind == ReservoirSinkKind.TRANSPORT_LOSS ? 750_000L : 700_000L);
                })
            .toList();
    return new ElementReservoirLedger(
        decision.candidateId(),
        source,
        Optional.of(decision.deposit().id()),
        decision.ledger().element(),
        decision.ledger().unit(),
        decision.ledger().sourceAmount(),
        transfers);
  }

  private MineralSystemDecision formedPlacer(Province province) {
    return geology.mineralDecisions(province).stream()
        .filter(decision -> decision.deposit() != null)
        .filter(decision -> decision.deposit().type() == DepositType.ALLUVIAL_PLACER_AU)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "surface field selected placer material without a formed placer decision"));
  }

  private static GeologicalSample geologicalSample(
      Province province, Point3 point, MaterialState state) {
    return new GeologicalSample(
        point,
        province.macroDomainId(),
        province.id(),
        state.rockBodyId(),
        state.lithology(),
        state.formationAge(),
        state.overprint(),
        state.faultDamageZone(),
        state.depositIds());
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private record RecipeKey(Lithology lithology, Overprint overprint) {}

  private record RecipeTemplate(RockDefinition rock, AlterationDefinition alteration) {}

  private record BodyRecipeKey(StableId bodyId, Lithology lithology, Overprint overprint) {}

  private record TerrainDirection(Point2 direction, boolean flatTerrainFallback) {}

  private record RouteDirectionDecision(Point2 direction, boolean deflectionClipped) {}

  private record ColluvialSourceCandidate(
      SurfaceSample surface,
      StableId sourceProvinceId,
      PetrologicSample material,
      int upslopeDistanceBlocks,
      long capacityFixedUnits,
      double terrainRoughnessIndex,
      ColluvialSedimentBudget.TerrainPath terrainPath) {}

  private record ResolvedColluvialSource(
      ColluvialSourceContribution contribution, PetrologicSample material) {}

  private record ResolvedRecipe(
      RockDefinition rock,
      MaterialAssemblage primaryAssemblage,
      MaterialAssemblage resolvedAssemblage,
      List<SolidSolutionState> primarySolidSolutions,
      List<SolidSolutionState> resolvedSolidSolutions,
      BulkComposition primaryComposition,
      BulkComposition resolvedComposition,
      ElementTransferLedger elementLedger,
      double porosityFraction,
      double permeabilityIndex,
      double erodibilityIndex) {}
}
