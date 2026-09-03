package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Exact normalized source-capacity, grain transport, and deposition ledger. */
public record ColluvialSedimentBudget(
    String unit,
    GrainTransportModel grainTransportModel,
    double depositionSlope,
    InputBalance weatheredMatrixBalance,
    List<SourceBalance> sourceBalances) {
  public static final String NORMALIZED_MASS_UNIT = "phase2_normalized_sediment_mass";

  private static final double WEATHERING_DEPTH_REFERENCE = 12.0;
  private static final double SLOPE_MOBILITY_REFERENCE = 0.24;
  private static final double MINIMUM_SLOPE_MOBILITY = 0.25;
  private static final double MINIMUM_RUNOFF_MOBILITY_RESPONSE = 0.65;
  private static final double MINIMUM_TRANSPORT_SLOPE_RESPONSE = 0.50;
  private static final double MINIMUM_TRANSPORT_ROUGHNESS_RESPONSE = 0.40;
  private static final double MINIMUM_TRANSPORT_PATH_RESPONSE = 0.50;
  private static final double MINIMUM_TRANSPORT_ROUTE_GRADE_RESPONSE = 0.75;
  private static final double MINIMUM_TRANSPORT_RUNOFF_RESPONSE = 0.70;
  private static final double PATH_REACH_TOLERANCE_BLOCKS = 1.0e-6;
  private static final double PATH_DIRECTION_TOLERANCE = 1.0e-9;
  private static final double GRAVEL_AND_COARSER_REFERENCE_E_FOLDING_DISTANCE_BLOCKS = 512.0;
  private static final double SAND_REFERENCE_E_FOLDING_DISTANCE_BLOCKS = 384.0;
  private static final double FINES_REFERENCE_E_FOLDING_DISTANCE_BLOCKS = 256.0;
  private static final double MAXIMUM_BYPASS_FRACTION = 0.50;

  public ColluvialSedimentBudget {
    if (!NORMALIZED_MASS_UNIT.equals(unit)
        || grainTransportModel
            != GrainTransportModel.SLOPE_ROUGHNESS_PATH_GRADE_RUNOFF_CONDITIONED_DRY_RAVEL_PROOF
        || !Double.isFinite(depositionSlope)
        || depositionSlope < 0.0
        || weatheredMatrixBalance == null
        || sourceBalances == null) {
      throw new IllegalArgumentException("colluvial sediment budget identity must be complete");
    }
    sourceBalances =
        List.copyOf(sourceBalances).stream()
            .sorted(
                Comparator.comparingInt(SourceBalance::upslopeDistanceBlocks)
                    .thenComparing(SourceBalance::sourceBodyId))
            .toList();
    if (sourceBalances.isEmpty()) {
      throw new IllegalArgumentException("colluvial sediment budget requires source balances");
    }
    if (!weatheredMatrixBalance.equals(
        deriveInputBalance(weatheredMatrixBalance.input(), 0, depositionSlope))) {
      throw new IllegalArgumentException("weathered-matrix balance does not match its inputs");
    }
    if (weatheredMatrixBalance.depositedFixedUnits() <= 0) {
      throw new IllegalArgumentException("weathered matrix must contribute deposited sediment");
    }
    Set<Integer> distances = new HashSet<>();
    long capacity = weatheredMatrixBalance.input().capacityFixedUnits();
    long deposited = weatheredMatrixBalance.depositedFixedUnits();
    TerrainPath previousPath = weatheredMatrixBalance.input().terrainPath();
    for (SourceBalance source : sourceBalances) {
      if (!distances.add(source.upslopeDistanceBlocks())) {
        throw new IllegalArgumentException("colluvial sediment source distances must be unique");
      }
      if (!source
          .balance()
          .equals(
              deriveInputBalance(
                  source.balance().input(), source.upslopeDistanceBlocks(), depositionSlope))) {
        throw new IllegalArgumentException("colluvial source balance does not match its inputs");
      }
      if (source.balance().depositedFixedUnits() <= 0) {
        throw new IllegalArgumentException("each colluvial source must contribute deposited mass");
      }
      TerrainPath sourcePath = source.balance().input().terrainPath();
      if (!extendsTerrainPath(sourcePath, previousPath)) {
        throw new IllegalArgumentException("colluvial source paths must extend one shared route");
      }
      previousPath = sourcePath;
      capacity = Math.addExact(capacity, source.balance().input().capacityFixedUnits());
      deposited = Math.addExact(deposited, source.balance().depositedFixedUnits());
    }
    if (capacity != MaterialAssemblage.SCALE || deposited <= 0) {
      throw new IllegalArgumentException(
          "colluvial sediment budget requires normalized capacity and positive deposition");
    }
  }

  public static ColluvialSedimentBudget derive(
      double depositionSlope,
      ProductionInput weatheredMatrixInput,
      List<SourceProductionInput> sourceInputs) {
    if (weatheredMatrixInput == null || sourceInputs == null || sourceInputs.isEmpty()) {
      throw new IllegalArgumentException("colluvial sediment production inputs are required");
    }
    InputBalance matrixBalance = deriveInputBalance(weatheredMatrixInput, 0, depositionSlope);
    List<SourceBalance> balances =
        List.copyOf(sourceInputs).stream()
            .map(
                source ->
                    new SourceBalance(
                        source.sourceBodyId(),
                        source.upslopeDistanceBlocks(),
                        deriveInputBalance(
                            source.input(), source.upslopeDistanceBlocks(), depositionSlope)))
            .toList();
    return new ColluvialSedimentBudget(
        NORMALIZED_MASS_UNIT,
        GrainTransportModel.SLOPE_ROUGHNESS_PATH_GRADE_RUNOFF_CONDITIONED_DRY_RAVEL_PROOF,
        depositionSlope,
        matrixBalance,
        balances);
  }

  public long sourceCapacityFixedUnits() {
    return total(balance -> balance.input().capacityFixedUnits());
  }

  public long mobilizedInventoryFixedUnits() {
    return total(InputBalance::mobilizedFixedUnits);
  }

  public long retainedInventoryFixedUnits() {
    return total(InputBalance::retainedFixedUnits);
  }

  public long transportLossFixedUnits() {
    return total(InputBalance::transportLossFixedUnits);
  }

  public long bypassedInventoryFixedUnits() {
    return total(InputBalance::bypassedFixedUnits);
  }

  public long depositedInventoryFixedUnits() {
    return total(InputBalance::depositedFixedUnits);
  }

  public GrainMass capacityGrainMass() {
    return totalGrainMass(InputBalance::capacityGrainMass);
  }

  public GrainMass mobilizedGrainMass() {
    return totalGrainMass(InputBalance::mobilizedGrainMass);
  }

  public GrainMass retainedGrainMass() {
    return totalGrainMass(InputBalance::retainedGrainMass);
  }

  public GrainMass transportLossGrainMass() {
    return totalGrainMass(InputBalance::transportLossGrainMass);
  }

  public GrainMass bypassedGrainMass() {
    return totalGrainMass(InputBalance::bypassedGrainMass);
  }

  public GrainMass depositedGrainMass() {
    return totalGrainMass(InputBalance::depositedGrainMass);
  }

  public SedimentGrainSize depositedGrainSize() {
    return depositedGrainMass().normalizedPpm();
  }

  public long weatheredMatrixFractionPpm() {
    return normalizedDepositFractions()[0];
  }

  public List<SourceDepositShare> sourceDepositShares() {
    long[] fractions = normalizedDepositFractions();
    List<SourceDepositShare> shares = new ArrayList<>(sourceBalances.size());
    for (int index = 0; index < sourceBalances.size(); index++) {
      SourceBalance source = sourceBalances.get(index);
      shares.add(
          new SourceDepositShare(
              source.sourceBodyId(), source.upslopeDistanceBlocks(), fractions[index + 1]));
    }
    return List.copyOf(shares);
  }

  /** Aggregates repeated source-body claims while preserving exact fixed-unit closure. */
  public List<ColluvialSourceUsage> sourceUsages() {
    Map<StableId, long[]> totals = new TreeMap<>();
    for (SourceBalance source : sourceBalances) {
      long[] total = totals.computeIfAbsent(source.sourceBodyId(), ignored -> new long[7]);
      InputBalance balance = source.balance();
      total[0] = Math.addExact(total[0], 1L);
      total[1] = Math.addExact(total[1], balance.input().capacityFixedUnits());
      total[2] = Math.addExact(total[2], balance.mobilizedFixedUnits());
      total[3] = Math.addExact(total[3], balance.retainedFixedUnits());
      total[4] = Math.addExact(total[4], balance.transportLossFixedUnits());
      total[5] = Math.addExact(total[5], balance.bypassedFixedUnits());
      total[6] = Math.addExact(total[6], balance.depositedFixedUnits());
    }
    return totals.entrySet().stream()
        .map(
            entry -> {
              long[] total = entry.getValue();
              return new ColluvialSourceUsage(
                  entry.getKey(),
                  Math.toIntExact(total[0]),
                  total[1],
                  total[2],
                  total[3],
                  total[4],
                  total[5],
                  total[6]);
            })
        .toList();
  }

  /** Returns each routed source tranche's exact deposited grain spectrum. */
  public List<ColluvialSourceGrainShare> sourceGrainShares() {
    return sourceBalances.stream()
        .map(
            source ->
                new ColluvialSourceGrainShare(
                    source.sourceBodyId(),
                    source.upslopeDistanceBlocks(),
                    source.balance().depositedFixedUnits(),
                    source.balance().depositedGrainMass()))
        .toList();
  }

  public long sourceFractionPpm(StableId sourceBodyId, int upslopeDistanceBlocks) {
    return sourceDepositShares().stream()
        .filter(
            share ->
                share.sourceBodyId().equals(sourceBodyId)
                    && share.upslopeDistanceBlocks() == upslopeDistanceBlocks)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown colluvial sediment source"))
        .fractionPpm();
  }

  public boolean matches(
      List<ColluvialSourceContribution> contributions, long weatheredMatrixFractionPpm) {
    if (contributions == null || weatheredMatrixFractionPpm != weatheredMatrixFractionPpm()) {
      return false;
    }
    List<ColluvialSourceContribution> sortedContributions =
        List.copyOf(contributions).stream()
            .sorted(
                Comparator.comparingInt(ColluvialSourceContribution::upslopeDistanceBlocks)
                    .thenComparing(ColluvialSourceContribution::sourceBodyId))
            .toList();
    List<SourceDepositShare> shares = sourceDepositShares();
    if (sortedContributions.size() != shares.size()) {
      return false;
    }
    for (int index = 0; index < shares.size(); index++) {
      SourceDepositShare share = shares.get(index);
      ColluvialSourceContribution contribution = sortedContributions.get(index);
      if (!share.sourceBodyId().equals(contribution.sourceBodyId())
          || share.upslopeDistanceBlocks() != contribution.upslopeDistanceBlocks()
          || share.fractionPpm() != contribution.assemblageFractionPpm()) {
        return false;
      }
    }
    return true;
  }

  private long total(ToLongFunction<InputBalance> value) {
    long result = value.applyAsLong(weatheredMatrixBalance);
    for (SourceBalance source : sourceBalances) {
      result = Math.addExact(result, value.applyAsLong(source.balance()));
    }
    return result;
  }

  private GrainMass totalGrainMass(Function<InputBalance, GrainMass> value) {
    GrainMass result = value.apply(weatheredMatrixBalance);
    for (SourceBalance source : sourceBalances) {
      result = result.add(value.apply(source.balance()));
    }
    return result;
  }

  private long[] normalizedDepositFractions() {
    long[] amounts = new long[sourceBalances.size() + 1];
    amounts[0] = weatheredMatrixBalance.depositedFixedUnits();
    for (int index = 0; index < sourceBalances.size(); index++) {
      amounts[index + 1] = sourceBalances.get(index).balance().depositedFixedUnits();
    }
    return apportion(MaterialAssemblage.SCALE, amounts);
  }

  private static InputBalance deriveInputBalance(
      ProductionInput input, int upslopeDistanceBlocks, double depositionSlope) {
    if (input == null
        || upslopeDistanceBlocks < 0
        || input.terrainPath().distanceBlocks() != upslopeDistanceBlocks
        || !Double.isFinite(depositionSlope)
        || depositionSlope < 0.0) {
      throw new IllegalArgumentException("valid colluvial transport inputs are required");
    }
    double weatheringAvailability = clamp(input.weatheringDepth() / WEATHERING_DEPTH_REFERENCE);
    double erodibilityResponse = 0.5 + 0.5 * input.erodibilityIndex();
    double slopeMobility =
        MINIMUM_SLOPE_MOBILITY
            + (1.0 - MINIMUM_SLOPE_MOBILITY) * clamp(input.slope() / SLOPE_MOBILITY_REFERENCE);
    double runoffMobility =
        MINIMUM_RUNOFF_MOBILITY_RESPONSE
            + (1.0 - MINIMUM_RUNOFF_MOBILITY_RESPONSE) * input.runoffIndex();
    long mobilizedTotal =
        roundedPortion(
            input.capacityFixedUnits(),
            weatheringAvailability * erodibilityResponse * slopeMobility * runoffMobility);
    GrainMass capacity = GrainMass.from(input.capacityFixedUnits(), input.sedimentYield());
    GrainMass mobilized = GrainMass.apportion(mobilizedTotal, capacity);
    GrainMass retained = capacity.subtract(mobilized);
    GrainTransportLengths transportLengths = grainTransportLengths(input);
    GrainMass arrived =
        new GrainMass(
            roundedPortion(
                mobilized.gravelAndCoarserFixedUnits(),
                transportSurvival(
                    upslopeDistanceBlocks, transportLengths.gravelAndCoarserBlocks())),
            roundedPortion(
                mobilized.sandFixedUnits(),
                transportSurvival(upslopeDistanceBlocks, transportLengths.sandBlocks())),
            roundedPortion(
                mobilized.finesFixedUnits(),
                transportSurvival(upslopeDistanceBlocks, transportLengths.finesBlocks())));
    GrainMass transportLoss = mobilized.subtract(arrived);
    double depositionFraction =
        1.0 - MAXIMUM_BYPASS_FRACTION * clamp(depositionSlope / SLOPE_MOBILITY_REFERENCE);
    long depositedTotal = roundedPortion(arrived.totalFixedUnits(), depositionFraction);
    GrainMass deposited = GrainMass.apportion(depositedTotal, arrived);
    GrainMass bypassed = arrived.subtract(deposited);
    return new InputBalance(
        input, capacity, mobilized, retained, transportLoss, bypassed, deposited);
  }

  private static double transportSurvival(int distanceBlocks, double eFoldingDistanceBlocks) {
    return StrictMath.exp(-distanceBlocks / eFoldingDistanceBlocks);
  }

  private static double transportDistanceScale(ProductionInput input) {
    double slopeResponse =
        MINIMUM_TRANSPORT_SLOPE_RESPONSE
            + (1.0 - MINIMUM_TRANSPORT_SLOPE_RESPONSE)
                * clamp(input.slope() / SLOPE_MOBILITY_REFERENCE);
    double roughnessResponse =
        1.0 - (1.0 - MINIMUM_TRANSPORT_ROUGHNESS_RESPONSE) * input.terrainRoughnessIndex();
    double runoffResponse =
        MINIMUM_TRANSPORT_RUNOFF_RESPONSE
            + (1.0 - MINIMUM_TRANSPORT_RUNOFF_RESPONSE) * input.runoffIndex();
    return slopeResponse * roughnessResponse * runoffResponse * transportPathResponse(input);
  }

  private static double transportPathResponse(ProductionInput input) {
    TerrainPath path = input.terrainPath();
    double routeGradeResponse =
        MINIMUM_TRANSPORT_ROUTE_GRADE_RESPONSE
            + (1.0 - MINIMUM_TRANSPORT_ROUTE_GRADE_RESPONSE) * path.routeGradeIndex();
    return MINIMUM_TRANSPORT_PATH_RESPONSE
        + (1.0 - MINIMUM_TRANSPORT_PATH_RESPONSE)
            * path.downslopeContinuityIndex()
            * path.routeDirectnessIndex()
            * routeGradeResponse;
  }

  private static GrainTransportLengths grainTransportLengths(ProductionInput input) {
    double scale = transportDistanceScale(input);
    return new GrainTransportLengths(
        GRAVEL_AND_COARSER_REFERENCE_E_FOLDING_DISTANCE_BLOCKS * scale,
        SAND_REFERENCE_E_FOLDING_DISTANCE_BLOCKS * scale,
        FINES_REFERENCE_E_FOLDING_DISTANCE_BLOCKS * scale);
  }

  private static long roundedPortion(long inventory, double fraction) {
    double bounded = clamp(fraction);
    return StrictMath.min(inventory, StrictMath.max(0L, StrictMath.round(inventory * bounded)));
  }

  private static long[] apportion(long allocation, long[] weights) {
    if (allocation < 0 || weights == null || weights.length == 0) {
      throw new IllegalArgumentException("valid fixed-unit apportionment is required");
    }
    long weightTotal = 0;
    for (long weight : weights) {
      if (weight < 0) {
        throw new IllegalArgumentException("fixed-unit weights must be non-negative");
      }
      weightTotal = Math.addExact(weightTotal, weight);
    }
    if (weightTotal <= 0) {
      if (allocation == 0) {
        return new long[weights.length];
      }
      throw new IllegalArgumentException("positive fixed-unit weights are required");
    }

    long[] apportioned = new long[weights.length];
    List<Remainder> remainders = new ArrayList<>(weights.length);
    long allocated = 0;
    for (int index = 0; index < weights.length; index++) {
      long numerator = Math.multiplyExact(weights[index], allocation);
      apportioned[index] = numerator / weightTotal;
      allocated = Math.addExact(allocated, apportioned[index]);
      remainders.add(new Remainder(index, numerator % weightTotal));
    }
    long missing = allocation - allocated;
    remainders.stream()
        .sorted(
            Comparator.comparingLong(Remainder::remainder)
                .reversed()
                .thenComparingInt(Remainder::index))
        .limit(missing)
        .forEach(remainder -> apportioned[remainder.index()]++);
    return apportioned;
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private static boolean extendsTerrainPath(TerrainPath path, TerrainPath prefix) {
    return path.reachLengthBlocks() == prefix.reachLengthBlocks()
        && path.samples().size() >= prefix.samples().size()
        && path.samples().subList(0, prefix.samples().size()).equals(prefix.samples())
        && path.reaches().size() >= prefix.reaches().size()
        && path.reaches().subList(0, prefix.reaches().size()).equals(prefix.reaches());
  }

  /** Inputs controlling production, runoff-conditioned transport, and initial grain spectrum. */
  public record ProductionInput(
      long capacityFixedUnits,
      double weatheringDepth,
      double slope,
      double erodibilityIndex,
      double terrainRoughnessIndex,
      double runoffIndex,
      TerrainPath terrainPath,
      SedimentGrainSize sedimentYield) {
    public ProductionInput(
        long capacityFixedUnits,
        double weatheringDepth,
        double slope,
        double erodibilityIndex,
        double terrainRoughnessIndex,
        TerrainPath terrainPath,
        SedimentGrainSize sedimentYield) {
      this(
          capacityFixedUnits,
          weatheringDepth,
          slope,
          erodibilityIndex,
          terrainRoughnessIndex,
          1.0,
          terrainPath,
          sedimentYield);
    }

    public ProductionInput {
      if (capacityFixedUnits <= 0
          || !Double.isFinite(weatheringDepth)
          || weatheringDepth < 0.0
          || !Double.isFinite(slope)
          || slope < 0.0
          || !Double.isFinite(erodibilityIndex)
          || erodibilityIndex < 0.0
          || erodibilityIndex > 1.0
          || !Double.isFinite(terrainRoughnessIndex)
          || terrainRoughnessIndex < 0.0
          || terrainRoughnessIndex > 1.0
          || !Double.isFinite(runoffIndex)
          || runoffIndex < 0.0
          || runoffIndex > 1.0
          || terrainPath == null
          || sedimentYield == null) {
        throw new IllegalArgumentException("colluvial sediment production input is invalid");
      }
    }
  }

  /** Explicit proof regime controlling the current grain-class survival ordering. */
  public enum GrainTransportModel {
    SLOPE_ROUGHNESS_PATH_GRADE_RUNOFF_CONDITIONED_DRY_RAVEL_PROOF
  }

  /** One positioned elevation observation along the bounded source route. */
  public record TerrainPathSample(int upslopeDistanceBlocks, Point2 point, double elevation) {
    public TerrainPathSample {
      if (upslopeDistanceBlocks < 0 || point == null || !Double.isFinite(elevation)) {
        throw new IllegalArgumentException("colluvial terrain-path sample is invalid");
      }
    }
  }

  /** One reach's raw-gradient decision and bounded bearing along the source route. */
  public record TerrainPathReach(
      int upslopeDistanceBlocks,
      Point2 startPoint,
      Point2 endPoint,
      Point2 rawUpslopeDirection,
      Point2 routedUpslopeDirection,
      boolean flatTerrainFallback,
      boolean deflectionClipped) {
    public TerrainPathReach {
      if (upslopeDistanceBlocks < 0
          || startPoint == null
          || endPoint == null
          || rawUpslopeDirection == null
          || routedUpslopeDirection == null) {
        throw new IllegalArgumentException("colluvial terrain-path reach is incomplete");
      }
      if (!isUnitDirection(rawUpslopeDirection) || !isUnitDirection(routedUpslopeDirection)) {
        throw new IllegalArgumentException("colluvial terrain-path reach directions are invalid");
      }
    }

    private static boolean isUnitDirection(Point2 direction) {
      return StrictMath.abs(StrictMath.hypot(direction.x(), direction.z()) - 1.0) <= 1.0e-9;
    }
  }

  /** Ordered terrain evidence and derived downhill continuity for one transport path. */
  public record TerrainPath(
      int reachLengthBlocks, List<TerrainPathSample> samples, List<TerrainPathReach> reaches) {
    public TerrainPath(int reachLengthBlocks, List<TerrainPathSample> samples) {
      this(reachLengthBlocks, samples, List.of());
    }

    public TerrainPath {
      if (reachLengthBlocks <= 0 || samples == null || samples.isEmpty()) {
        throw new IllegalArgumentException("colluvial terrain path is incomplete");
      }
      if (samples.stream().anyMatch(sample -> sample == null)) {
        throw new IllegalArgumentException("colluvial terrain-path samples must be complete");
      }
      samples = List.copyOf(samples);
      if (reaches == null) {
        throw new IllegalArgumentException("colluvial terrain-path decisions are incomplete");
      }
      if (reaches.stream().anyMatch(reach -> reach == null)) {
        throw new IllegalArgumentException("colluvial terrain-path decisions must be complete");
      }
      reaches = List.copyOf(reaches);
      if (reaches.isEmpty() && samples.size() > 1) {
        reaches = syntheticTerrainPathReaches(reachLengthBlocks, samples);
      }
      if (reaches.size() != samples.size() - 1) {
        throw new IllegalArgumentException(
            "colluvial terrain-path decision count must match reaches");
      }
      double cumulativeRelief = 0.0;
      for (int index = 0; index < samples.size(); index++) {
        TerrainPathSample sample = samples.get(index);
        long expectedDistance = (long) index * reachLengthBlocks;
        if (expectedDistance > Integer.MAX_VALUE
            || sample.upslopeDistanceBlocks() != (int) expectedDistance) {
          throw new IllegalArgumentException(
              "colluvial terrain-path samples must begin at zero and be contiguous");
        }
        if (index > 0) {
          double relief = sample.elevation() - samples.get(index - 1).elevation();
          cumulativeRelief += StrictMath.abs(relief);
          if (!Double.isFinite(relief) || !Double.isFinite(cumulativeRelief)) {
            throw new IllegalArgumentException("colluvial terrain-path relief must be finite");
          }
          Point2 previousPoint = samples.get(index - 1).point();
          double reachDistance =
              StrictMath.hypot(
                  sample.point().x() - previousPoint.x(), sample.point().z() - previousPoint.z());
          if (!Double.isFinite(reachDistance)
              || StrictMath.abs(reachDistance - reachLengthBlocks) > PATH_REACH_TOLERANCE_BLOCKS) {
            throw new IllegalArgumentException(
                "colluvial terrain-path geometry must match its reach length");
          }
          TerrainPathReach reach = reaches.get(index - 1);
          if (reach.upslopeDistanceBlocks() != (int) ((long) (index - 1) * reachLengthBlocks)
              || !reach.startPoint().equals(previousPoint)
              || !reach.endPoint().equals(sample.point())) {
            throw new IllegalArgumentException(
                "colluvial terrain-path decision geometry must match its samples");
          }
          double routedDirectionX = (sample.point().x() - previousPoint.x()) / reachLengthBlocks;
          double routedDirectionZ = (sample.point().z() - previousPoint.z()) / reachLengthBlocks;
          if (StrictMath.hypot(
                  routedDirectionX - reach.routedUpslopeDirection().x(),
                  routedDirectionZ - reach.routedUpslopeDirection().z())
              > PATH_DIRECTION_TOLERANCE) {
            throw new IllegalArgumentException(
                "colluvial terrain-path routed direction must match its geometry");
          }
        }
      }
    }

    private static List<TerrainPathReach> syntheticTerrainPathReaches(
        int reachLengthBlocks, List<TerrainPathSample> samples) {
      List<TerrainPathReach> synthetic = new ArrayList<>();
      for (int index = 1; index < samples.size(); index++) {
        Point2 start = samples.get(index - 1).point();
        Point2 end = samples.get(index).point();
        double reachDistance = StrictMath.hypot(end.x() - start.x(), end.z() - start.z());
        if (!Double.isFinite(reachDistance) || reachDistance <= 0.0) {
          throw new IllegalArgumentException(
              "colluvial terrain-path geometry must contain non-zero reaches");
        }
        double directionX = (end.x() - start.x()) / reachDistance;
        double directionZ = (end.z() - start.z()) / reachDistance;
        Point2 direction = new Point2(directionX, directionZ);
        synthetic.add(
            new TerrainPathReach(
                (index - 1) * reachLengthBlocks, start, end, direction, direction, false, false));
      }
      return List.copyOf(synthetic);
    }

    public int distanceBlocks() {
      return samples.getLast().upslopeDistanceBlocks();
    }

    public int reachCount() {
      return samples.size() - 1;
    }

    public Point2 originPoint() {
      return samples.getFirst().point();
    }

    public Point2 sourcePoint() {
      return samples.getLast().point();
    }

    public double straightLineDistanceBlocks() {
      return StrictMath.hypot(
          sourcePoint().x() - originPoint().x(), sourcePoint().z() - originPoint().z());
    }

    public double routedDistanceBlocks() {
      return (double) reachLengthBlocks * reachCount();
    }

    public double routeDirectnessIndex() {
      if (reachCount() == 0) {
        return 1.0;
      }
      return StrictMath.max(
          0.0, StrictMath.min(1.0, straightLineDistanceBlocks() / routedDistanceBlocks()));
    }

    /** Bounded net source-to-target elevation-grade index for this routed path. */
    public double routeGradeIndex() {
      if (reachCount() == 0) {
        return 1.0;
      }
      double netUpslopeRelief = sourceElevation() - originElevation();
      double routeGrade = netUpslopeRelief / routedDistanceBlocks();
      return clamp(routeGrade / SLOPE_MOBILITY_REFERENCE);
    }

    public double netUpslopeReliefBlocks() {
      return sourceElevation() - originElevation();
    }

    public double maximumDeflectionFromInitialDegrees() {
      if (reachCount() <= 1) {
        return 0.0;
      }
      Point2 origin = samples.getFirst().point();
      Point2 firstReach = samples.get(1).point();
      double initialX = (firstReach.x() - origin.x()) / reachLengthBlocks;
      double initialZ = (firstReach.z() - origin.z()) / reachLengthBlocks;
      double maximumRadians = 0.0;
      for (int index = 2; index < samples.size(); index++) {
        Point2 previous = samples.get(index - 1).point();
        Point2 current = samples.get(index).point();
        double directionX = (current.x() - previous.x()) / reachLengthBlocks;
        double directionZ = (current.z() - previous.z()) / reachLengthBlocks;
        double cosine = initialX * directionX + initialZ * directionZ;
        double deflection = StrictMath.acos(StrictMath.max(-1.0, StrictMath.min(1.0, cosine)));
        maximumRadians = StrictMath.max(maximumRadians, deflection);
      }
      return StrictMath.toDegrees(maximumRadians);
    }

    public double cumulativeDownslopeReliefBlocks() {
      double relief = 0.0;
      for (int index = 1; index < samples.size(); index++) {
        relief += StrictMath.max(0.0, elevationDifference(index));
      }
      return relief;
    }

    public double cumulativeBarrierReliefBlocks() {
      double relief = 0.0;
      for (int index = 1; index < samples.size(); index++) {
        relief += StrictMath.max(0.0, -elevationDifference(index));
      }
      return relief;
    }

    public double descendingReachFraction() {
      if (reachCount() == 0) {
        return 1.0;
      }
      int descending = 0;
      for (int index = 1; index < samples.size(); index++) {
        if (elevationDifference(index) > 0.0) {
          descending++;
        }
      }
      return (double) descending / reachCount();
    }

    public double downslopeContinuityIndex() {
      if (reachCount() == 0) {
        return 1.0;
      }
      double downslopeRelief = cumulativeDownslopeReliefBlocks();
      double totalRelief = downslopeRelief + cumulativeBarrierReliefBlocks();
      double reliefDirectionFraction = totalRelief > 0.0 ? downslopeRelief / totalRelief : 0.0;
      return clamp(0.5 * (descendingReachFraction() + reliefDirectionFraction));
    }

    private double elevationDifference(int upslopeSampleIndex) {
      return samples.get(upslopeSampleIndex).elevation()
          - samples.get(upslopeSampleIndex - 1).elevation();
    }

    private double originElevation() {
      return samples.getFirst().elevation();
    }

    private double sourceElevation() {
      return samples.getLast().elevation();
    }
  }

  /** Effective grain-class transport lengths after bounded slope, roughness, and path response. */
  public record GrainTransportLengths(
      double gravelAndCoarserBlocks, double sandBlocks, double finesBlocks) {
    public GrainTransportLengths {
      if (!Double.isFinite(gravelAndCoarserBlocks)
          || !Double.isFinite(sandBlocks)
          || !Double.isFinite(finesBlocks)
          || gravelAndCoarserBlocks <= 0.0
          || sandBlocks <= 0.0
          || finesBlocks <= 0.0
          || gravelAndCoarserBlocks <= sandBlocks
          || sandBlocks <= finesBlocks) {
        throw new IllegalArgumentException("colluvial grain transport lengths are invalid");
      }
    }
  }

  /** Production inputs tied to one bounded bedrock-source tranche. */
  public record SourceProductionInput(
      StableId sourceBodyId, int upslopeDistanceBlocks, ProductionInput input) {
    public SourceProductionInput {
      if (sourceBodyId == null
          || upslopeDistanceBlocks < 0
          || input == null
          || input.terrainPath().distanceBlocks() != upslopeDistanceBlocks) {
        throw new IllegalArgumentException("colluvial source production input is incomplete");
      }
    }
  }

  /** Exact fixed-unit grain inventory for one ledger stage. */
  public record GrainMass(
      long gravelAndCoarserFixedUnits, long sandFixedUnits, long finesFixedUnits) {
    public GrainMass {
      if (gravelAndCoarserFixedUnits < 0 || sandFixedUnits < 0 || finesFixedUnits < 0) {
        throw new IllegalArgumentException("colluvial grain mass must be non-negative");
      }
      total(gravelAndCoarserFixedUnits, sandFixedUnits, finesFixedUnits);
    }

    public long totalFixedUnits() {
      return total(gravelAndCoarserFixedUnits, sandFixedUnits, finesFixedUnits);
    }

    public GrainMass add(GrainMass other) {
      if (other == null) {
        throw new IllegalArgumentException("colluvial grain mass is required");
      }
      return new GrainMass(
          Math.addExact(gravelAndCoarserFixedUnits, other.gravelAndCoarserFixedUnits),
          Math.addExact(sandFixedUnits, other.sandFixedUnits),
          Math.addExact(finesFixedUnits, other.finesFixedUnits));
    }

    public GrainMass subtract(GrainMass other) {
      if (other == null) {
        throw new IllegalArgumentException("colluvial grain mass is required");
      }
      return new GrainMass(
          Math.subtractExact(gravelAndCoarserFixedUnits, other.gravelAndCoarserFixedUnits),
          Math.subtractExact(sandFixedUnits, other.sandFixedUnits),
          Math.subtractExact(finesFixedUnits, other.finesFixedUnits));
    }

    public SedimentGrainSize normalizedPpm() {
      if (totalFixedUnits() <= 0) {
        throw new IllegalArgumentException("positive colluvial grain mass is required");
      }
      long[] normalized =
          ColluvialSedimentBudget.apportion(
              MaterialAssemblage.SCALE,
              new long[] {gravelAndCoarserFixedUnits, sandFixedUnits, finesFixedUnits});
      return new SedimentGrainSize(normalized[0], normalized[1], normalized[2]);
    }

    private static GrainMass from(long inventory, SedimentGrainSize grainSize) {
      long[] apportioned =
          ColluvialSedimentBudget.apportion(
              inventory,
              new long[] {
                grainSize.gravelAndCoarserPpm(), grainSize.sandPpm(), grainSize.finesPpm()
              });
      return new GrainMass(apportioned[0], apportioned[1], apportioned[2]);
    }

    private static GrainMass apportion(long inventory, GrainMass weights) {
      long[] apportioned =
          ColluvialSedimentBudget.apportion(
              inventory,
              new long[] {
                weights.gravelAndCoarserFixedUnits, weights.sandFixedUnits, weights.finesFixedUnits
              });
      return new GrainMass(apportioned[0], apportioned[1], apportioned[2]);
    }

    private static long total(long gravel, long sand, long fines) {
      return Math.addExact(Math.addExact(gravel, sand), fines);
    }
  }

  /** Exact partition of one capacity tranche by both bulk amount and grain class. */
  public record InputBalance(
      ProductionInput input,
      GrainMass capacityGrainMass,
      GrainMass mobilizedGrainMass,
      GrainMass retainedGrainMass,
      GrainMass transportLossGrainMass,
      GrainMass bypassedGrainMass,
      GrainMass depositedGrainMass) {
    public InputBalance {
      if (input == null
          || capacityGrainMass == null
          || mobilizedGrainMass == null
          || retainedGrainMass == null
          || transportLossGrainMass == null
          || bypassedGrainMass == null
          || depositedGrainMass == null) {
        throw new IllegalArgumentException("colluvial sediment input balance is incomplete");
      }
      if (capacityGrainMass.totalFixedUnits() != input.capacityFixedUnits()
          || !capacityGrainMass.equals(
              GrainMass.from(input.capacityFixedUnits(), input.sedimentYield()))) {
        throw new IllegalArgumentException(
            "colluvial grain capacity must match its production input");
      }
      if (!capacityGrainMass.equals(retainedGrainMass.add(mobilizedGrainMass))
          || !mobilizedGrainMass.equals(
              transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
        throw new IllegalArgumentException("colluvial sediment input balance does not close");
      }
    }

    public long mobilizedFixedUnits() {
      return mobilizedGrainMass.totalFixedUnits();
    }

    public long retainedFixedUnits() {
      return retainedGrainMass.totalFixedUnits();
    }

    public long transportLossFixedUnits() {
      return transportLossGrainMass.totalFixedUnits();
    }

    public long bypassedFixedUnits() {
      return bypassedGrainMass.totalFixedUnits();
    }

    public long depositedFixedUnits() {
      return depositedGrainMass.totalFixedUnits();
    }

    public double transportDistanceScale() {
      return ColluvialSedimentBudget.transportDistanceScale(input);
    }

    public double transportPathResponse() {
      return ColluvialSedimentBudget.transportPathResponse(input);
    }

    public ColluvialTransportProcess transportProcess() {
      return ColluvialTransportProcess.from(input);
    }

    public ColluvialSinkState sinkState() {
      return ColluvialSinkState.from(this);
    }

    public GrainTransportLengths grainTransportLengths() {
      return ColluvialSedimentBudget.grainTransportLengths(input);
    }
  }

  /** Exact response for one named and distance-bounded bedrock source. */
  public record SourceBalance(
      StableId sourceBodyId, int upslopeDistanceBlocks, InputBalance balance) {
    public SourceBalance {
      if (sourceBodyId == null || upslopeDistanceBlocks < 0 || balance == null) {
        throw new IllegalArgumentException("colluvial sediment source balance is incomplete");
      }
    }
  }

  /** Normalized deposited share derived from one source's delivered fixed-unit mass. */
  public record SourceDepositShare(
      StableId sourceBodyId, int upslopeDistanceBlocks, long fractionPpm) {
    public SourceDepositShare {
      if (sourceBodyId == null
          || upslopeDistanceBlocks < 0
          || fractionPpm <= 0
          || fractionPpm >= MaterialAssemblage.SCALE) {
        throw new IllegalArgumentException("colluvial source deposit share is invalid");
      }
    }
  }

  private record Remainder(int index, long remainder) {}
}
