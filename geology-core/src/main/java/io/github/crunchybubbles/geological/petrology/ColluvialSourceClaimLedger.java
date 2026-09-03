package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Exact finite-query aggregation of colluvial source claims across parcels. */
public record ColluvialSourceClaimLedger(
    List<ColluvialSourceClaim> claims, List<SourceAggregate> sourceAggregates) {
  public ColluvialSourceClaimLedger {
    if (claims == null || sourceAggregates == null) {
      throw new IllegalArgumentException("colluvial source claim ledger is incomplete");
    }
    claims = canonicalClaims(claims);
    sourceAggregates = canonicalAggregates(sourceAggregates);
    validateClaims(claims);
    validateAggregates(claims, sourceAggregates);
  }

  public static ColluvialSourceClaimLedger from(List<ColluvialSourceClaim> claims) {
    if (claims == null) {
      throw new IllegalArgumentException("colluvial source claims are required");
    }
    List<ColluvialSourceClaim> canonical = canonicalClaims(claims);
    Map<StableId, AggregateTotals> totals = new TreeMap<>();
    Map<StableId, Set<Point2>> parcels = new TreeMap<>();
    for (ColluvialSourceClaim claim : canonical) {
      AggregateTotals total =
          totals.computeIfAbsent(claim.sourceBodyId(), ignored -> new AggregateTotals());
      total.add(claim);
      parcels
          .computeIfAbsent(claim.sourceBodyId(), ignored -> new HashSet<>())
          .add(claim.parcelPoint());
    }
    List<SourceAggregate> aggregates =
        totals.entrySet().stream()
            .map(
                entry -> {
                  AggregateTotals total = entry.getValue();
                  return new SourceAggregate(
                      entry.getKey(),
                      parcels.get(entry.getKey()).size(),
                      total.trancheCount,
                      total.claimedCapacityFixedUnits,
                      total.mobilizedFixedUnits,
                      total.retainedFixedUnits,
                      total.transportLossFixedUnits,
                      total.bypassedFixedUnits,
                      total.depositedFixedUnits,
                      total.claimedCapacityGrainMass,
                      total.mobilizedGrainMass,
                      total.retainedGrainMass,
                      total.transportLossGrainMass,
                      total.bypassedGrainMass,
                      total.depositedGrainMass);
                })
            .toList();
    return new ColluvialSourceClaimLedger(canonical, aggregates);
  }

  public long claimedCapacityFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::claimedCapacityFixedUnits).sum();
  }

  public long mobilizedFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::mobilizedFixedUnits).sum();
  }

  public long retainedFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::retainedFixedUnits).sum();
  }

  public long transportLossFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::transportLossFixedUnits).sum();
  }

  public long bypassedFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::bypassedFixedUnits).sum();
  }

  public long depositedFixedUnits() {
    return claims.stream().mapToLong(ColluvialSourceClaim::depositedFixedUnits).sum();
  }

  public ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::capacityGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass mobilizedGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::mobilizedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass retainedGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::retainedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass transportLossGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::transportLossGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass bypassedGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::bypassedGrainMass);
  }

  public ColluvialSedimentBudget.GrainMass depositedGrainMass() {
    return totalGrainMass(ColluvialSourceClaim::depositedGrainMass);
  }

  public int parcelCount() {
    return (int) claims.stream().map(ColluvialSourceClaim::parcelPoint).distinct().count();
  }

  public boolean hasCrossParcelReuse() {
    return sourceAggregates.stream().anyMatch(aggregate -> aggregate.parcelCount() > 1);
  }

  private ColluvialSedimentBudget.GrainMass totalGrainMass(
      java.util.function.Function<ColluvialSourceClaim, ColluvialSedimentBudget.GrainMass>
          selector) {
    ColluvialSedimentBudget.GrainMass total = new ColluvialSedimentBudget.GrainMass(0, 0, 0);
    for (ColluvialSourceClaim claim : claims) {
      total = total.add(selector.apply(claim));
    }
    return total;
  }

  /** Reconciles mobilized source inventory against a finite, caller-supplied capacity map. */
  public ColluvialSourceCapacityLedger reconcileSourceCapacity(
      Map<StableId, Long> sourceCapacityFixedUnits) {
    return ColluvialSourceCapacityLedger.from(this, sourceCapacityFixedUnits);
  }

  private static List<ColluvialSourceClaim> canonicalClaims(List<ColluvialSourceClaim> claims) {
    return List.copyOf(claims).stream()
        .sorted(
            Comparator.comparing(ColluvialSourceClaim::sourceBodyId)
                .thenComparing(ColluvialSourceClaim::parcelBodyId)
                .thenComparingDouble(claim -> claim.parcelPoint().x())
                .thenComparingDouble(claim -> claim.parcelPoint().z())
                .thenComparingInt(ColluvialSourceClaim::upslopeDistanceBlocks))
        .toList();
  }

  private static List<SourceAggregate> canonicalAggregates(List<SourceAggregate> aggregates) {
    return List.copyOf(aggregates).stream()
        .sorted(Comparator.comparing(SourceAggregate::sourceBodyId))
        .toList();
  }

  private static void validateClaims(List<ColluvialSourceClaim> claims) {
    Set<String> keys = new HashSet<>();
    for (ColluvialSourceClaim claim : claims) {
      String key =
          claim.parcelPoint() + ":" + claim.sourceBodyId() + ":" + claim.upslopeDistanceBlocks();
      if (!keys.add(key)) {
        throw new IllegalArgumentException("colluvial source claim is duplicated");
      }
    }
  }

  private static void validateAggregates(
      List<ColluvialSourceClaim> claims, List<SourceAggregate> aggregates) {
    Map<StableId, AggregateTotals> expected = new TreeMap<>();
    Map<StableId, Set<Point2>> parcels = new TreeMap<>();
    for (ColluvialSourceClaim claim : claims) {
      expected.computeIfAbsent(claim.sourceBodyId(), ignored -> new AggregateTotals()).add(claim);
      parcels
          .computeIfAbsent(claim.sourceBodyId(), ignored -> new HashSet<>())
          .add(claim.parcelPoint());
    }
    if (expected.size() != aggregates.size()) {
      throw new IllegalArgumentException("colluvial source aggregates do not cover claims");
    }
    for (SourceAggregate aggregate : aggregates) {
      AggregateTotals total = expected.get(aggregate.sourceBodyId());
      if (total == null
          || aggregate.parcelCount() != parcels.get(aggregate.sourceBodyId()).size()
          || aggregate.trancheCount() != total.trancheCount
          || aggregate.claimedCapacityFixedUnits() != total.claimedCapacityFixedUnits
          || aggregate.mobilizedFixedUnits() != total.mobilizedFixedUnits
          || aggregate.retainedFixedUnits() != total.retainedFixedUnits
          || aggregate.transportLossFixedUnits() != total.transportLossFixedUnits
          || aggregate.bypassedFixedUnits() != total.bypassedFixedUnits
          || aggregate.depositedFixedUnits() != total.depositedFixedUnits
          || !aggregate.claimedCapacityGrainMass().equals(total.claimedCapacityGrainMass)
          || !aggregate.mobilizedGrainMass().equals(total.mobilizedGrainMass)
          || !aggregate.retainedGrainMass().equals(total.retainedGrainMass)
          || !aggregate.transportLossGrainMass().equals(total.transportLossGrainMass)
          || !aggregate.bypassedGrainMass().equals(total.bypassedGrainMass)
          || !aggregate.depositedGrainMass().equals(total.depositedGrainMass)) {
        throw new IllegalArgumentException("colluvial source aggregate does not match claims");
      }
    }
  }

  private static final class AggregateTotals {
    private int trancheCount;
    private long claimedCapacityFixedUnits;
    private long mobilizedFixedUnits;
    private long retainedFixedUnits;
    private long transportLossFixedUnits;
    private long bypassedFixedUnits;
    private long depositedFixedUnits;
    private ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass mobilizedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass retainedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass transportLossGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass bypassedGrainMass = zeroGrainMass();
    private ColluvialSedimentBudget.GrainMass depositedGrainMass = zeroGrainMass();

    private void add(ColluvialSourceClaim claim) {
      trancheCount = Math.addExact(trancheCount, 1);
      claimedCapacityFixedUnits =
          Math.addExact(claimedCapacityFixedUnits, claim.claimedCapacityFixedUnits());
      mobilizedFixedUnits = Math.addExact(mobilizedFixedUnits, claim.mobilizedFixedUnits());
      retainedFixedUnits = Math.addExact(retainedFixedUnits, claim.retainedFixedUnits());
      transportLossFixedUnits =
          Math.addExact(transportLossFixedUnits, claim.transportLossFixedUnits());
      bypassedFixedUnits = Math.addExact(bypassedFixedUnits, claim.bypassedFixedUnits());
      depositedFixedUnits = Math.addExact(depositedFixedUnits, claim.depositedFixedUnits());
      claimedCapacityGrainMass = claimedCapacityGrainMass.add(claim.capacityGrainMass());
      mobilizedGrainMass = mobilizedGrainMass.add(claim.mobilizedGrainMass());
      retainedGrainMass = retainedGrainMass.add(claim.retainedGrainMass());
      transportLossGrainMass = transportLossGrainMass.add(claim.transportLossGrainMass());
      bypassedGrainMass = bypassedGrainMass.add(claim.bypassedGrainMass());
      depositedGrainMass = depositedGrainMass.add(claim.depositedGrainMass());
    }

    private static ColluvialSedimentBudget.GrainMass zeroGrainMass() {
      return new ColluvialSedimentBudget.GrainMass(0, 0, 0);
    }
  }

  public record SourceAggregate(
      StableId sourceBodyId,
      int parcelCount,
      int trancheCount,
      long claimedCapacityFixedUnits,
      long mobilizedFixedUnits,
      long retainedFixedUnits,
      long transportLossFixedUnits,
      long bypassedFixedUnits,
      long depositedFixedUnits,
      ColluvialSedimentBudget.GrainMass claimedCapacityGrainMass,
      ColluvialSedimentBudget.GrainMass mobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass retainedGrainMass,
      ColluvialSedimentBudget.GrainMass transportLossGrainMass,
      ColluvialSedimentBudget.GrainMass bypassedGrainMass,
      ColluvialSedimentBudget.GrainMass depositedGrainMass) {
    public SourceAggregate(
        StableId sourceBodyId,
        int parcelCount,
        int trancheCount,
        long claimedCapacityFixedUnits,
        long mobilizedFixedUnits,
        long retainedFixedUnits,
        long transportLossFixedUnits,
        long bypassedFixedUnits,
        long depositedFixedUnits) {
      this(
          sourceBodyId,
          parcelCount,
          trancheCount,
          claimedCapacityFixedUnits,
          mobilizedFixedUnits,
          retainedFixedUnits,
          transportLossFixedUnits,
          bypassedFixedUnits,
          depositedFixedUnits,
          new ColluvialSedimentBudget.GrainMass(claimedCapacityFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(mobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(retainedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(transportLossFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(bypassedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(depositedFixedUnits, 0, 0));
    }

    public SourceAggregate {
      if (sourceBodyId == null
          || parcelCount <= 0
          || trancheCount <= 0
          || claimedCapacityFixedUnits <= 0
          || mobilizedFixedUnits < 0
          || retainedFixedUnits < 0
          || transportLossFixedUnits < 0
          || bypassedFixedUnits < 0
          || depositedFixedUnits < 0
          || claimedCapacityGrainMass == null
          || mobilizedGrainMass == null
          || retainedGrainMass == null
          || transportLossGrainMass == null
          || bypassedGrainMass == null
          || depositedGrainMass == null
          || claimedCapacityFixedUnits != retainedFixedUnits + mobilizedFixedUnits
          || mobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || claimedCapacityGrainMass.totalFixedUnits() != claimedCapacityFixedUnits
          || mobilizedGrainMass.totalFixedUnits() != mobilizedFixedUnits
          || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
          || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
          || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
          || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
          || !claimedCapacityGrainMass.equals(retainedGrainMass.add(mobilizedGrainMass))
          || !mobilizedGrainMass.equals(
              transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
        throw new IllegalArgumentException("colluvial source aggregate does not close");
      }
    }
  }
}
