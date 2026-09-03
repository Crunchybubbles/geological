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

/**
 * Deterministic, read-only reconciliation of finite source capacity across a queried parcel set.
 *
 * <p>The supplied source capacities are a finite audit input and cap mobilized inventory only. They
 * never mutate the random-access material query or introduce world-global depletion state.
 */
public record ColluvialSourceCapacityLedger(
    List<ReconciledClaim> claims, List<SourceCapacityAggregate> sourceAggregates) {
  public ColluvialSourceCapacityLedger {
    if (claims == null || sourceAggregates == null) {
      throw new IllegalArgumentException("colluvial source capacity ledger is incomplete");
    }
    claims = canonicalClaims(claims);
    sourceAggregates = canonicalAggregates(sourceAggregates);
    validateClaims(claims);
    validateAggregates(claims, sourceAggregates);
  }

  public static ColluvialSourceCapacityLedger from(
      ColluvialSourceClaimLedger claimLedger, Map<StableId, Long> sourceCapacityFixedUnits) {
    if (claimLedger == null || sourceCapacityFixedUnits == null) {
      throw new IllegalArgumentException("source claims and capacities are required");
    }
    TreeMap<StableId, Long> capacities = canonicalCapacities(sourceCapacityFixedUnits);
    Set<StableId> sourceIds =
        claimLedger.sourceAggregates().stream()
            .map(ColluvialSourceClaimLedger.SourceAggregate::sourceBodyId)
            .collect(java.util.stream.Collectors.toSet());
    if (!sourceIds.equals(capacities.keySet())) {
      throw new IllegalArgumentException(
          "source capacities must cover exactly the queried source bodies");
    }

    List<ColluvialSourceClaim> requested = claimLedger.claims();
    List<ReconciledClaim> reconciled = new ArrayList<>(requested.size());
    List<SourceCapacityAggregate> aggregates = new ArrayList<>(capacities.size());
    int index = 0;
    while (index < requested.size()) {
      StableId sourceBodyId = requested.get(index).sourceBodyId();
      int end = index + 1;
      while (end < requested.size() && requested.get(end).sourceBodyId().equals(sourceBodyId)) {
        end++;
      }
      List<ColluvialSourceClaim> sourceClaims = requested.subList(index, end);
      long requestedMobilized =
          sourceClaims.stream()
              .mapToLong(ColluvialSourceClaim::mobilizedFixedUnits)
              .reduce(0L, Math::addExact);
      long available = capacities.get(sourceBodyId);
      long target = Math.min(available, requestedMobilized);
      long[] requestedWeights =
          sourceClaims.stream().mapToLong(ColluvialSourceClaim::mobilizedFixedUnits).toArray();
      long[] allocated = apportion(target, requestedWeights);
      for (int sourceIndex = 0; sourceIndex < sourceClaims.size(); sourceIndex++) {
        ColluvialSourceClaim claim = sourceClaims.get(sourceIndex);
        long allocatedMobilized = allocated[sourceIndex];
        long unallocatedMobilized =
            Math.subtractExact(claim.mobilizedFixedUnits(), allocatedMobilized);
        long retained = Math.addExact(claim.retainedFixedUnits(), unallocatedMobilized);
        ColluvialSedimentBudget.GrainMass allocatedMobilizedGrainMass =
            apportionGrain(allocatedMobilized, claim.mobilizedGrainMass());
        ColluvialSedimentBudget.GrainMass unallocatedMobilizedGrainMass =
            claim.mobilizedGrainMass().subtract(allocatedMobilizedGrainMass);
        ColluvialSedimentBudget.GrainMass retainedGrainMass =
            claim.retainedGrainMass().add(unallocatedMobilizedGrainMass);
        long[] stageAllocation =
            apportion(
                allocatedMobilized,
                new long[] {
                  claim.transportLossFixedUnits(),
                  claim.bypassedFixedUnits(),
                  claim.depositedFixedUnits()
                });
        ColluvialSedimentBudget.GrainMass[] stageGrainMasses =
            apportionStageGrainMasses(
                stageAllocation,
                allocatedMobilizedGrainMass,
                claim.transportLossGrainMass(),
                claim.bypassedGrainMass(),
                claim.depositedGrainMass());
        reconciled.add(
            new ReconciledClaim(
                claim.parcelPoint(),
                claim.parcelBodyId(),
                claim.sourceBodyId(),
                claim.upslopeDistanceBlocks(),
                claim.claimedCapacityFixedUnits(),
                claim.mobilizedFixedUnits(),
                allocatedMobilized,
                unallocatedMobilized,
                retained,
                stageGrainMasses[0].totalFixedUnits(),
                stageGrainMasses[1].totalFixedUnits(),
                stageGrainMasses[2].totalFixedUnits(),
                claim.capacityGrainMass(),
                claim.mobilizedGrainMass(),
                allocatedMobilizedGrainMass,
                unallocatedMobilizedGrainMass,
                retainedGrainMass,
                stageGrainMasses[0],
                stageGrainMasses[1],
                stageGrainMasses[2]));
      }
      SourceTotals totals = new SourceTotals(sourceBodyId, available);
      for (int sourceIndex = 0; sourceIndex < sourceClaims.size(); sourceIndex++) {
        totals.add(reconciled.get(index + sourceIndex));
      }
      aggregates.add(totals.toAggregate());
      index = end;
    }
    return new ColluvialSourceCapacityLedger(reconciled, aggregates);
  }

  public static ColluvialSourceCapacityLedger from(
      List<ColluvialSourceClaim> claims, Map<StableId, Long> sourceCapacityFixedUnits) {
    return from(ColluvialSourceClaimLedger.from(claims), sourceCapacityFixedUnits);
  }

  public long sourceCapacityFixedUnits() {
    return sourceAggregates.stream()
        .mapToLong(SourceCapacityAggregate::sourceCapacityFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long claimedCapacityFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::claimedCapacityFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long requestedMobilizedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::requestedMobilizedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long allocatedMobilizedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::allocatedMobilizedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long unallocatedMobilizedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::unallocatedMobilizedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long retainedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::retainedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long transportLossFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::transportLossFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long bypassedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::bypassedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public long depositedFixedUnits() {
    return claims.stream()
        .mapToLong(ReconciledClaim::depositedFixedUnits)
        .reduce(0L, Math::addExact);
  }

  public ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass() {
    return totalGrainMass(ReconciledClaim::claimedCapacityGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass requestedMobilizedGrainMass() {
    return totalGrainMass(ReconciledClaim::requestedMobilizedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass allocatedMobilizedGrainMass() {
    return totalGrainMass(ReconciledClaim::allocatedMobilizedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass unallocatedMobilizedGrainMass() {
    return totalGrainMass(ReconciledClaim::unallocatedMobilizedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass retainedGrainMass() {
    return totalGrainMass(ReconciledClaim::retainedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass transportLossGrainMass() {
    return totalGrainMass(ReconciledClaim::transportLossGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass bypassedGrainMass() {
    return totalGrainMass(ReconciledClaim::bypassedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass depositedGrainMass() {
    return totalGrainMass(ReconciledClaim::depositedGrainMass);
  }

  public long remainingSourceCapacityFixedUnits() {
    return Math.subtractExact(sourceCapacityFixedUnits(), allocatedMobilizedFixedUnits());
  }

  public boolean hasDepletion() {
    return unallocatedMobilizedFixedUnits() > 0;
  }

  private ColluvialSedimentBudget.GrainMass totalGrainMass(
      java.util.function.Function<ReconciledClaim, ColluvialSedimentBudget.GrainMass> selector) {
    ColluvialSedimentBudget.GrainMass total = new ColluvialSedimentBudget.GrainMass(0, 0, 0);
    for (ReconciledClaim claim : claims) {
      total = total.add(selector.apply(claim));
    }
    return total;
  }

  private static TreeMap<StableId, Long> canonicalCapacities(
      Map<StableId, Long> sourceCapacityFixedUnits) {
    TreeMap<StableId, Long> capacities = new TreeMap<>();
    for (Map.Entry<StableId, Long> entry : sourceCapacityFixedUnits.entrySet()) {
      if (entry.getKey() == null
          || entry.getValue() == null
          || entry.getValue() < 0
          || capacities.put(entry.getKey(), entry.getValue()) != null) {
        throw new IllegalArgumentException("source capacities must be unique and non-negative");
      }
    }
    return capacities;
  }

  private static List<ReconciledClaim> canonicalClaims(List<ReconciledClaim> claims) {
    return List.copyOf(claims).stream()
        .sorted(
            Comparator.comparing(ReconciledClaim::sourceBodyId)
                .thenComparing(ReconciledClaim::parcelBodyId)
                .thenComparingDouble(claim -> claim.parcelPoint().x())
                .thenComparingDouble(claim -> claim.parcelPoint().z())
                .thenComparingInt(ReconciledClaim::upslopeDistanceBlocks))
        .toList();
  }

  private static List<SourceCapacityAggregate> canonicalAggregates(
      List<SourceCapacityAggregate> aggregates) {
    return List.copyOf(aggregates).stream()
        .sorted(Comparator.comparing(SourceCapacityAggregate::sourceBodyId))
        .toList();
  }

  private static void validateClaims(List<ReconciledClaim> claims) {
    Set<String> keys = new HashSet<>();
    for (ReconciledClaim claim : claims) {
      String key =
          claim.parcelPoint() + ":" + claim.sourceBodyId() + ":" + claim.upslopeDistanceBlocks();
      if (!keys.add(key)) {
        throw new IllegalArgumentException("reconciled source claim is duplicated");
      }
    }
  }

  private static void validateAggregates(
      List<ReconciledClaim> claims, List<SourceCapacityAggregate> aggregates) {
    TreeMap<StableId, SourceTotals> expected = new TreeMap<>();
    for (ReconciledClaim claim : claims) {
      expected
          .computeIfAbsent(
              claim.sourceBodyId(), ignored -> new SourceTotals(claim.sourceBodyId(), -1L))
          .add(claim);
    }
    if (expected.size() != aggregates.size()) {
      throw new IllegalArgumentException("reconciled source aggregates do not cover claims");
    }
    for (SourceCapacityAggregate aggregate : aggregates) {
      SourceTotals total = expected.get(aggregate.sourceBodyId());
      if (total == null || !total.matches(aggregate)) {
        throw new IllegalArgumentException("reconciled source aggregate does not match claims");
      }
    }
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
    if (weightTotal == 0) {
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
    long missing = Math.subtractExact(allocation, allocated);
    remainders.stream()
        .sorted(
            Comparator.comparingLong(Remainder::remainder)
                .reversed()
                .thenComparingInt(Remainder::index))
        .limit(missing)
        .forEach(remainder -> apportioned[remainder.index()]++);
    return apportioned;
  }

  private static ColluvialSedimentBudget.GrainMass apportionGrain(
      long allocation, ColluvialSedimentBudget.GrainMass weights) {
    if (weights == null) {
      throw new IllegalArgumentException("colluvial grain weights are required");
    }
    long[] apportioned =
        apportion(
            allocation,
            new long[] {
              weights.gravelAndCoarserFixedUnits(),
              weights.sandFixedUnits(),
              weights.finesFixedUnits()
            });
    return new ColluvialSedimentBudget.GrainMass(apportioned[0], apportioned[1], apportioned[2]);
  }

  private static ColluvialSedimentBudget.GrainMass[] apportionStageGrainMasses(
      long[] stageAllocation,
      ColluvialSedimentBudget.GrainMass allocatedMobilized,
      ColluvialSedimentBudget.GrainMass transportLoss,
      ColluvialSedimentBudget.GrainMass bypassed,
      ColluvialSedimentBudget.GrainMass deposited) {
    if (stageAllocation == null
        || stageAllocation.length != 3
        || allocatedMobilized == null
        || transportLoss == null
        || bypassed == null
        || deposited == null) {
      throw new IllegalArgumentException("colluvial grain stage inputs are incomplete");
    }
    long[] capacities =
        new long[] {
          allocatedMobilized.gravelAndCoarserFixedUnits(),
          allocatedMobilized.sandFixedUnits(),
          allocatedMobilized.finesFixedUnits()
        };
    long[] loss =
        apportionBounded(
            stageAllocation[0],
            new long[] {
              transportLoss.gravelAndCoarserFixedUnits(),
              transportLoss.sandFixedUnits(),
              transportLoss.finesFixedUnits()
            },
            capacities);
    subtractInPlace(capacities, loss);
    long[] bypass =
        apportionBounded(
            stageAllocation[1],
            new long[] {
              bypassed.gravelAndCoarserFixedUnits(),
              bypassed.sandFixedUnits(),
              bypassed.finesFixedUnits()
            },
            capacities);
    subtractInPlace(capacities, bypass);
    if (sum(capacities) != stageAllocation[2]) {
      throw new IllegalArgumentException("colluvial grain stage allocation does not close");
    }
    return new ColluvialSedimentBudget.GrainMass[] {
      new ColluvialSedimentBudget.GrainMass(loss[0], loss[1], loss[2]),
      new ColluvialSedimentBudget.GrainMass(bypass[0], bypass[1], bypass[2]),
      new ColluvialSedimentBudget.GrainMass(capacities[0], capacities[1], capacities[2])
    };
  }

  private static long[] apportionBounded(long allocation, long[] weights, long[] capacities) {
    if (allocation < 0
        || weights == null
        || capacities == null
        || weights.length != capacities.length
        || weights.length == 0) {
      throw new IllegalArgumentException("valid bounded grain apportionment is required");
    }
    long capacityTotal = sum(capacities);
    if (allocation > capacityTotal) {
      throw new IllegalArgumentException("grain allocation exceeds remaining inventory");
    }
    long[] result = new long[capacities.length];
    long remaining = allocation;
    while (remaining > 0) {
      long[] activeWeights = new long[weights.length];
      long activeWeightTotal = 0;
      for (int index = 0; index < capacities.length; index++) {
        long capacity = capacities[index] - result[index];
        if (capacity < 0 || weights[index] < 0) {
          throw new IllegalArgumentException("bounded grain inputs must be non-negative");
        }
        activeWeights[index] = capacity == 0 ? 0 : weights[index];
        activeWeightTotal = Math.addExact(activeWeightTotal, activeWeights[index]);
      }
      if (activeWeightTotal == 0) {
        for (int index = 0; index < capacities.length; index++) {
          activeWeights[index] = capacities[index] - result[index];
        }
      }
      long[] candidate = apportion(remaining, activeWeights);
      long assigned = 0;
      for (int index = 0; index < result.length; index++) {
        long available = capacities[index] - result[index];
        long amount = Math.min(available, candidate[index]);
        result[index] = Math.addExact(result[index], amount);
        assigned = Math.addExact(assigned, amount);
      }
      if (assigned == 0) {
        for (int index = 0; index < result.length; index++) {
          if (capacities[index] > result[index]) {
            result[index]++;
            assigned = 1;
            break;
          }
        }
      }
      if (assigned <= 0) {
        throw new IllegalArgumentException("bounded grain apportionment stalled");
      }
      remaining = Math.subtractExact(remaining, assigned);
    }
    return result;
  }

  private static void subtractInPlace(long[] values, long[] amounts) {
    for (int index = 0; index < values.length; index++) {
      values[index] = Math.subtractExact(values[index], amounts[index]);
    }
  }

  private static long sum(long[] values) {
    long total = 0;
    for (long value : values) {
      if (value < 0) {
        throw new IllegalArgumentException("grain values must be non-negative");
      }
      total = Math.addExact(total, value);
    }
    return total;
  }

  private record Remainder(int index, long remainder) {}

  /** One claim after finite source capacity has been apportioned. */
  public record ReconciledClaim(
      Point2 parcelPoint,
      StableId parcelBodyId,
      StableId sourceBodyId,
      int upslopeDistanceBlocks,
      long claimedCapacityFixedUnits,
      long requestedMobilizedFixedUnits,
      long allocatedMobilizedFixedUnits,
      long unallocatedMobilizedFixedUnits,
      long retainedFixedUnits,
      long transportLossFixedUnits,
      long bypassedFixedUnits,
      long depositedFixedUnits,
      ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass,
      ColluvialSedimentBudget.GrainMass requestedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass allocatedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass unallocatedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass retainedGrainMass,
      ColluvialSedimentBudget.GrainMass transportLossGrainMass,
      ColluvialSedimentBudget.GrainMass bypassedGrainMass,
      ColluvialSedimentBudget.GrainMass depositedGrainMass) {
    public ReconciledClaim(
        Point2 parcelPoint,
        StableId parcelBodyId,
        StableId sourceBodyId,
        int upslopeDistanceBlocks,
        long claimedCapacityFixedUnits,
        long requestedMobilizedFixedUnits,
        long allocatedMobilizedFixedUnits,
        long unallocatedMobilizedFixedUnits,
        long retainedFixedUnits,
        long transportLossFixedUnits,
        long bypassedFixedUnits,
        long depositedFixedUnits) {
      this(
          parcelPoint,
          parcelBodyId,
          sourceBodyId,
          upslopeDistanceBlocks,
          claimedCapacityFixedUnits,
          requestedMobilizedFixedUnits,
          allocatedMobilizedFixedUnits,
          unallocatedMobilizedFixedUnits,
          retainedFixedUnits,
          transportLossFixedUnits,
          bypassedFixedUnits,
          depositedFixedUnits,
          new ColluvialSedimentBudget.GrainMass(claimedCapacityFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(requestedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(allocatedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(unallocatedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(retainedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(transportLossFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(bypassedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(depositedFixedUnits, 0, 0));
    }

    public ReconciledClaim {
      if (parcelPoint == null
          || parcelBodyId == null
          || sourceBodyId == null
          || upslopeDistanceBlocks < 0
          || claimedCapacityFixedUnits <= 0
          || requestedMobilizedFixedUnits < 0
          || allocatedMobilizedFixedUnits < 0
          || unallocatedMobilizedFixedUnits < 0
          || retainedFixedUnits < 0
          || transportLossFixedUnits < 0
          || bypassedFixedUnits < 0
          || depositedFixedUnits < 0
          || claimedCapacityGrainMass == null
          || requestedMobilizedGrainMass == null
          || allocatedMobilizedGrainMass == null
          || unallocatedMobilizedGrainMass == null
          || retainedGrainMass == null
          || transportLossGrainMass == null
          || bypassedGrainMass == null
          || depositedGrainMass == null
          || requestedMobilizedFixedUnits
              != allocatedMobilizedFixedUnits + unallocatedMobilizedFixedUnits
          || allocatedMobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || claimedCapacityFixedUnits != retainedFixedUnits + allocatedMobilizedFixedUnits
          || claimedCapacityGrainMass.totalFixedUnits() != claimedCapacityFixedUnits
          || requestedMobilizedGrainMass.totalFixedUnits() != requestedMobilizedFixedUnits
          || allocatedMobilizedGrainMass.totalFixedUnits() != allocatedMobilizedFixedUnits
          || unallocatedMobilizedGrainMass.totalFixedUnits() != unallocatedMobilizedFixedUnits
          || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
          || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
          || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
          || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
          || !claimedCapacityGrainMass.equals(retainedGrainMass.add(allocatedMobilizedGrainMass))
          || !requestedMobilizedGrainMass.equals(
              allocatedMobilizedGrainMass.add(unallocatedMobilizedGrainMass))
          || !allocatedMobilizedGrainMass.equals(
              transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
        throw new IllegalArgumentException("reconciled source claim does not close");
      }
    }
  }

  /** Aggregate finite-capacity usage for one source body across all queried parcels. */
  public record SourceCapacityAggregate(
      StableId sourceBodyId,
      long sourceCapacityFixedUnits,
      int claimCount,
      long claimedCapacityFixedUnits,
      long requestedMobilizedFixedUnits,
      long allocatedMobilizedFixedUnits,
      long unallocatedMobilizedFixedUnits,
      long retainedFixedUnits,
      long transportLossFixedUnits,
      long bypassedFixedUnits,
      long depositedFixedUnits,
      ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass,
      ColluvialSedimentBudget.GrainMass requestedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass allocatedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass unallocatedMobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass retainedGrainMass,
      ColluvialSedimentBudget.GrainMass transportLossGrainMass,
      ColluvialSedimentBudget.GrainMass bypassedGrainMass,
      ColluvialSedimentBudget.GrainMass depositedGrainMass) {
    public SourceCapacityAggregate(
        StableId sourceBodyId,
        long sourceCapacityFixedUnits,
        int claimCount,
        long claimedCapacityFixedUnits,
        long requestedMobilizedFixedUnits,
        long allocatedMobilizedFixedUnits,
        long unallocatedMobilizedFixedUnits,
        long retainedFixedUnits,
        long transportLossFixedUnits,
        long bypassedFixedUnits,
        long depositedFixedUnits) {
      this(
          sourceBodyId,
          sourceCapacityFixedUnits,
          claimCount,
          claimedCapacityFixedUnits,
          requestedMobilizedFixedUnits,
          allocatedMobilizedFixedUnits,
          unallocatedMobilizedFixedUnits,
          retainedFixedUnits,
          transportLossFixedUnits,
          bypassedFixedUnits,
          depositedFixedUnits,
          new ColluvialSedimentBudget.GrainMass(claimedCapacityFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(requestedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(allocatedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(unallocatedMobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(retainedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(transportLossFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(bypassedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(depositedFixedUnits, 0, 0));
    }

    public SourceCapacityAggregate {
      if (sourceBodyId == null
          || sourceCapacityFixedUnits < 0
          || claimCount <= 0
          || claimedCapacityFixedUnits <= 0
          || requestedMobilizedFixedUnits < 0
          || allocatedMobilizedFixedUnits < 0
          || unallocatedMobilizedFixedUnits < 0
          || retainedFixedUnits < 0
          || transportLossFixedUnits < 0
          || bypassedFixedUnits < 0
          || depositedFixedUnits < 0
          || claimedCapacityGrainMass == null
          || requestedMobilizedGrainMass == null
          || allocatedMobilizedGrainMass == null
          || unallocatedMobilizedGrainMass == null
          || retainedGrainMass == null
          || transportLossGrainMass == null
          || bypassedGrainMass == null
          || depositedGrainMass == null
          || allocatedMobilizedFixedUnits > sourceCapacityFixedUnits
          || requestedMobilizedFixedUnits
              != allocatedMobilizedFixedUnits + unallocatedMobilizedFixedUnits
          || allocatedMobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || claimedCapacityFixedUnits != retainedFixedUnits + allocatedMobilizedFixedUnits
          || claimedCapacityGrainMass.totalFixedUnits() != claimedCapacityFixedUnits
          || requestedMobilizedGrainMass.totalFixedUnits() != requestedMobilizedFixedUnits
          || allocatedMobilizedGrainMass.totalFixedUnits() != allocatedMobilizedFixedUnits
          || unallocatedMobilizedGrainMass.totalFixedUnits() != unallocatedMobilizedFixedUnits
          || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
          || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
          || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
          || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
          || !claimedCapacityGrainMass.equals(retainedGrainMass.add(allocatedMobilizedGrainMass))
          || !requestedMobilizedGrainMass.equals(
              allocatedMobilizedGrainMass.add(unallocatedMobilizedGrainMass))
          || !allocatedMobilizedGrainMass.equals(
              transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
        throw new IllegalArgumentException("reconciled source aggregate does not close");
      }
    }
  }

  private static final class SourceTotals {
    private final StableId sourceBodyId;
    private final long sourceCapacityFixedUnits;
    private int claimCount;
    private long claimedCapacityFixedUnits;
    private long requestedMobilizedFixedUnits;
    private long allocatedMobilizedFixedUnits;
    private long unallocatedMobilizedFixedUnits;
    private long retainedFixedUnits;
    private long transportLossFixedUnits;
    private long bypassedFixedUnits;
    private long depositedFixedUnits;
    private ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass requestedMobilizedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass allocatedMobilizedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass unallocatedMobilizedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass retainedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass transportLossGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass bypassedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass depositedGrainMass = zeroGrainMass();

    private SourceTotals(StableId sourceBodyId, long sourceCapacityFixedUnits) {
      this.sourceBodyId = sourceBodyId;
      this.sourceCapacityFixedUnits = sourceCapacityFixedUnits;
    }

    private void add(ReconciledClaim claim) {
      claimCount = Math.addExact(claimCount, 1);
      claimedCapacityFixedUnits =
          Math.addExact(claimedCapacityFixedUnits, claim.claimedCapacityFixedUnits());
      requestedMobilizedFixedUnits =
          Math.addExact(requestedMobilizedFixedUnits, claim.requestedMobilizedFixedUnits());
      allocatedMobilizedFixedUnits =
          Math.addExact(allocatedMobilizedFixedUnits, claim.allocatedMobilizedFixedUnits());
      unallocatedMobilizedFixedUnits =
          Math.addExact(unallocatedMobilizedFixedUnits, claim.unallocatedMobilizedFixedUnits());
      retainedFixedUnits = Math.addExact(retainedFixedUnits, claim.retainedFixedUnits());
      transportLossFixedUnits =
          Math.addExact(transportLossFixedUnits, claim.transportLossFixedUnits());
      bypassedFixedUnits = Math.addExact(bypassedFixedUnits, claim.bypassedFixedUnits());
      depositedFixedUnits = Math.addExact(depositedFixedUnits, claim.depositedFixedUnits());
      claimedCapacityGrainMass = claimedCapacityGrainMass.add(claim.claimedCapacityGrainMass());
      requestedMobilizedGrainMass =
          requestedMobilizedGrainMass.add(claim.requestedMobilizedGrainMass());
      allocatedMobilizedGrainMass =
          allocatedMobilizedGrainMass.add(claim.allocatedMobilizedGrainMass());
      unallocatedMobilizedGrainMass =
          unallocatedMobilizedGrainMass.add(claim.unallocatedMobilizedGrainMass());
      retainedGrainMass = retainedGrainMass.add(claim.retainedGrainMass());
      transportLossGrainMass = transportLossGrainMass.add(claim.transportLossGrainMass());
      bypassedGrainMass = bypassedGrainMass.add(claim.bypassedGrainMass());
      depositedGrainMass = depositedGrainMass.add(claim.depositedGrainMass());
    }

    private SourceCapacityAggregate toAggregate() {
      return new SourceCapacityAggregate(
          sourceBodyId,
          sourceCapacityFixedUnits,
          claimCount,
          claimedCapacityFixedUnits,
          requestedMobilizedFixedUnits,
          allocatedMobilizedFixedUnits,
          unallocatedMobilizedFixedUnits,
          retainedFixedUnits,
          transportLossFixedUnits,
          bypassedFixedUnits,
          depositedFixedUnits,
          claimedCapacityGrainMass,
          requestedMobilizedGrainMass,
          allocatedMobilizedGrainMass,
          unallocatedMobilizedGrainMass,
          retainedGrainMass,
          transportLossGrainMass,
          bypassedGrainMass,
          depositedGrainMass);
    }

    private boolean matches(SourceCapacityAggregate aggregate) {
      return sourceBodyId.equals(aggregate.sourceBodyId())
          && (sourceCapacityFixedUnits < 0
              || sourceCapacityFixedUnits == aggregate.sourceCapacityFixedUnits())
          && claimCount == aggregate.claimCount()
          && claimedCapacityFixedUnits == aggregate.claimedCapacityFixedUnits()
          && requestedMobilizedFixedUnits == aggregate.requestedMobilizedFixedUnits()
          && allocatedMobilizedFixedUnits == aggregate.allocatedMobilizedFixedUnits()
          && unallocatedMobilizedFixedUnits == aggregate.unallocatedMobilizedFixedUnits()
          && retainedFixedUnits == aggregate.retainedFixedUnits()
          && transportLossFixedUnits == aggregate.transportLossFixedUnits()
          && bypassedFixedUnits == aggregate.bypassedFixedUnits()
          && depositedFixedUnits == aggregate.depositedFixedUnits()
          && claimedCapacityGrainMass.equals(aggregate.claimedCapacityGrainMass())
          && requestedMobilizedGrainMass.equals(aggregate.requestedMobilizedGrainMass())
          && allocatedMobilizedGrainMass.equals(aggregate.allocatedMobilizedGrainMass())
          && unallocatedMobilizedGrainMass.equals(aggregate.unallocatedMobilizedGrainMass())
          && retainedGrainMass.equals(aggregate.retainedGrainMass())
          && transportLossGrainMass.equals(aggregate.transportLossGrainMass())
          && bypassedGrainMass.equals(aggregate.bypassedGrainMass())
          && depositedGrainMass.equals(aggregate.depositedGrainMass());
    }

    private static ColluvialSedimentBudget.GrainMass zeroGrainMass() {
      return new ColluvialSedimentBudget.GrainMass(0, 0, 0);
    }
  }
}
