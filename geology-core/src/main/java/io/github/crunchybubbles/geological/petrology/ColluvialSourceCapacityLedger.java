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
        long[] stageAllocation =
            apportion(
                allocatedMobilized,
                new long[] {
                  claim.transportLossFixedUnits(),
                  claim.bypassedFixedUnits(),
                  claim.depositedFixedUnits()
                });
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
                stageAllocation[0],
                stageAllocation[1],
                stageAllocation[2]));
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

  public long remainingSourceCapacityFixedUnits() {
    return Math.subtractExact(sourceCapacityFixedUnits(), allocatedMobilizedFixedUnits());
  }

  public boolean hasDepletion() {
    return unallocatedMobilizedFixedUnits() > 0;
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
      long depositedFixedUnits) {
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
          || requestedMobilizedFixedUnits
              != allocatedMobilizedFixedUnits + unallocatedMobilizedFixedUnits
          || allocatedMobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || claimedCapacityFixedUnits != retainedFixedUnits + allocatedMobilizedFixedUnits) {
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
      long depositedFixedUnits) {
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
          || allocatedMobilizedFixedUnits > sourceCapacityFixedUnits
          || requestedMobilizedFixedUnits
              != allocatedMobilizedFixedUnits + unallocatedMobilizedFixedUnits
          || allocatedMobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || claimedCapacityFixedUnits != retainedFixedUnits + allocatedMobilizedFixedUnits) {
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
          depositedFixedUnits);
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
          && depositedFixedUnits == aggregate.depositedFixedUnits();
    }
  }
}
