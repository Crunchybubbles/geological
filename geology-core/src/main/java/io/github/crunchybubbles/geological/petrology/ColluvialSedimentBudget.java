package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

/** Exact normalized source-capacity, mobilization, transport, and deposition ledger. */
public record ColluvialSedimentBudget(
    String unit,
    double depositionSlope,
    InputBalance weatheredMatrixBalance,
    List<SourceBalance> sourceBalances) {
  public static final String NORMALIZED_MASS_UNIT = "phase2_normalized_sediment_mass";

  private static final double WEATHERING_DEPTH_REFERENCE = 12.0;
  private static final double SLOPE_MOBILITY_REFERENCE = 0.24;
  private static final double MINIMUM_SLOPE_MOBILITY = 0.25;
  private static final double TRANSPORT_E_FOLDING_DISTANCE_BLOCKS = 384.0;
  private static final double MAXIMUM_BYPASS_FRACTION = 0.50;

  public ColluvialSedimentBudget {
    if (!NORMALIZED_MASS_UNIT.equals(unit)
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
        NORMALIZED_MASS_UNIT, depositionSlope, matrixBalance, balances);
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

  private long[] normalizedDepositFractions() {
    long deposited = depositedInventoryFixedUnits();
    long[] amounts = new long[sourceBalances.size() + 1];
    amounts[0] = weatheredMatrixBalance.depositedFixedUnits();
    for (int index = 0; index < sourceBalances.size(); index++) {
      amounts[index + 1] = sourceBalances.get(index).balance().depositedFixedUnits();
    }

    long[] fractions = new long[amounts.length];
    List<Remainder> remainders = new ArrayList<>(amounts.length);
    long allocated = 0;
    for (int index = 0; index < amounts.length; index++) {
      long numerator = Math.multiplyExact(amounts[index], MaterialAssemblage.SCALE);
      fractions[index] = numerator / deposited;
      allocated = Math.addExact(allocated, fractions[index]);
      remainders.add(new Remainder(index, numerator % deposited));
    }
    long missing = MaterialAssemblage.SCALE - allocated;
    remainders.stream()
        .sorted(
            Comparator.comparingLong(Remainder::remainder)
                .reversed()
                .thenComparingInt(Remainder::index))
        .limit(missing)
        .forEach(remainder -> fractions[remainder.index()]++);
    return fractions;
  }

  private static InputBalance deriveInputBalance(
      ProductionInput input, int upslopeDistanceBlocks, double depositionSlope) {
    if (input == null
        || upslopeDistanceBlocks < 0
        || !Double.isFinite(depositionSlope)
        || depositionSlope < 0.0) {
      throw new IllegalArgumentException("valid colluvial transport inputs are required");
    }
    double weatheringAvailability = clamp(input.weatheringDepth() / WEATHERING_DEPTH_REFERENCE);
    double erodibilityResponse = 0.5 + 0.5 * input.erodibilityIndex();
    double slopeMobility =
        MINIMUM_SLOPE_MOBILITY
            + (1.0 - MINIMUM_SLOPE_MOBILITY) * clamp(input.slope() / SLOPE_MOBILITY_REFERENCE);
    long mobilized =
        roundedPortion(
            input.capacityFixedUnits(),
            weatheringAvailability * erodibilityResponse * slopeMobility);
    long retained = input.capacityFixedUnits() - mobilized;
    double transportSurvival =
        StrictMath.exp(-upslopeDistanceBlocks / TRANSPORT_E_FOLDING_DISTANCE_BLOCKS);
    long arrived = roundedPortion(mobilized, transportSurvival);
    long transportLoss = mobilized - arrived;
    double depositionFraction =
        1.0 - MAXIMUM_BYPASS_FRACTION * clamp(depositionSlope / SLOPE_MOBILITY_REFERENCE);
    long deposited = roundedPortion(arrived, depositionFraction);
    long bypassed = arrived - deposited;
    return new InputBalance(input, mobilized, retained, transportLoss, bypassed, deposited);
  }

  private static long roundedPortion(long inventory, double fraction) {
    double bounded = clamp(fraction);
    return StrictMath.min(inventory, StrictMath.max(0L, StrictMath.round(inventory * bounded)));
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  /** Inputs controlling the mobile share of one normalized source-capacity tranche. */
  public record ProductionInput(
      long capacityFixedUnits, double weatheringDepth, double slope, double erodibilityIndex) {
    public ProductionInput {
      if (capacityFixedUnits <= 0
          || !Double.isFinite(weatheringDepth)
          || weatheringDepth < 0.0
          || !Double.isFinite(slope)
          || slope < 0.0
          || !Double.isFinite(erodibilityIndex)
          || erodibilityIndex < 0.0
          || erodibilityIndex > 1.0) {
        throw new IllegalArgumentException("colluvial sediment production input is invalid");
      }
    }
  }

  /** Production inputs tied to one bounded bedrock-source tranche. */
  public record SourceProductionInput(
      StableId sourceBodyId, int upslopeDistanceBlocks, ProductionInput input) {
    public SourceProductionInput {
      if (sourceBodyId == null || upslopeDistanceBlocks < 0 || input == null) {
        throw new IllegalArgumentException("colluvial source production input is incomplete");
      }
    }
  }

  /** Exact partition of one capacity tranche into retained, lost, bypassed, and deposited mass. */
  public record InputBalance(
      ProductionInput input,
      long mobilizedFixedUnits,
      long retainedFixedUnits,
      long transportLossFixedUnits,
      long bypassedFixedUnits,
      long depositedFixedUnits) {
    public InputBalance {
      if (input == null
          || mobilizedFixedUnits < 0
          || retainedFixedUnits < 0
          || transportLossFixedUnits < 0
          || bypassedFixedUnits < 0
          || depositedFixedUnits < 0) {
        throw new IllegalArgumentException("colluvial sediment input balance is invalid");
      }
      long mobilizedAllocation =
          Math.addExact(
              Math.addExact(transportLossFixedUnits, bypassedFixedUnits), depositedFixedUnits);
      if (Math.addExact(retainedFixedUnits, mobilizedFixedUnits) != input.capacityFixedUnits()
          || mobilizedAllocation != mobilizedFixedUnits) {
        throw new IllegalArgumentException("colluvial sediment input balance does not close");
      }
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
