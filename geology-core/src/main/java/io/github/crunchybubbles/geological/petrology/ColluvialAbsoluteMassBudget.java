package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A unit-converted view of one normalized colluvial budget.
 *
 * <p>The conversion is explicit caller calibration, not a world-generated absolute sediment flux.
 * Fixed-unit values remain authoritative so stage closure is exact.
 */
public record ColluvialAbsoluteMassBudget(
    ColluvialSedimentBudget normalizedBudget,
    ColluvialMassScale scale,
    List<InputMassBalance> inputBalances) {
  public ColluvialAbsoluteMassBudget {
    if (normalizedBudget == null || scale == null || inputBalances == null) {
      throw new IllegalArgumentException("colluvial absolute mass budget is incomplete");
    }
    inputBalances = canonicalInputBalances(inputBalances);
    validateInputBalances(normalizedBudget, inputBalances);
  }

  public static ColluvialAbsoluteMassBudget from(
      ColluvialSedimentBudget normalizedBudget, ColluvialMassScale scale) {
    if (normalizedBudget == null || scale == null) {
      throw new IllegalArgumentException("colluvial budget and mass scale are required");
    }
    List<InputMassBalance> inputs = new ArrayList<>(normalizedBudget.sourceBalances().size() + 1);
    inputs.add(
        InputMassBalance.from(Optional.empty(), 0, normalizedBudget.weatheredMatrixBalance()));
    for (ColluvialSedimentBudget.SourceBalance source : normalizedBudget.sourceBalances()) {
      inputs.add(
          InputMassBalance.from(
              Optional.of(source.sourceBodyId()),
              source.upslopeDistanceBlocks(),
              source.balance()));
    }
    return new ColluvialAbsoluteMassBudget(normalizedBudget, scale, inputs);
  }

  public String massUnit() {
    return scale.massUnit();
  }

  public long capacityFixedUnits() {
    return normalizedBudget.sourceCapacityFixedUnits();
  }

  public long mobilizedFixedUnits() {
    return normalizedBudget.mobilizedInventoryFixedUnits();
  }

  public long retainedFixedUnits() {
    return normalizedBudget.retainedInventoryFixedUnits();
  }

  public long transportLossFixedUnits() {
    return normalizedBudget.transportLossFixedUnits();
  }

  public long bypassedFixedUnits() {
    return normalizedBudget.bypassedInventoryFixedUnits();
  }

  public long depositedFixedUnits() {
    return normalizedBudget.depositedInventoryFixedUnits();
  }

  public double capacityMass() {
    return scale.mass(capacityFixedUnits());
  }

  public double mobilizedMass() {
    return scale.mass(mobilizedFixedUnits());
  }

  public double retainedMass() {
    return scale.mass(retainedFixedUnits());
  }

  public double transportLossMass() {
    return scale.mass(transportLossFixedUnits());
  }

  public double bypassedMass() {
    return scale.mass(bypassedFixedUnits());
  }

  public double depositedMass() {
    return scale.mass(depositedFixedUnits());
  }

  public double capacityRate() {
    return scale.productionRate(capacityFixedUnits());
  }

  public double mobilizedRate() {
    return scale.productionRate(mobilizedFixedUnits());
  }

  public double depositedRate() {
    return scale.productionRate(depositedFixedUnits());
  }

  public ColluvialSedimentBudget.GrainMass capacityGrainMass() {
    return normalizedBudget.capacityGrainMass();
  }

  public ColluvialSedimentBudget.GrainMass mobilizedGrainMass() {
    return normalizedBudget.mobilizedGrainMass();
  }

  public ColluvialSedimentBudget.GrainMass retainedGrainMass() {
    return normalizedBudget.retainedGrainMass();
  }

  public ColluvialSedimentBudget.GrainMass transportLossGrainMass() {
    return normalizedBudget.transportLossGrainMass();
  }

  public ColluvialSedimentBudget.GrainMass bypassedGrainMass() {
    return normalizedBudget.bypassedGrainMass();
  }

  public ColluvialSedimentBudget.GrainMass depositedGrainMass() {
    return normalizedBudget.depositedGrainMass();
  }

  private static List<InputMassBalance> canonicalInputBalances(List<InputMassBalance> balances) {
    return List.copyOf(balances).stream()
        .sorted(
            Comparator.comparing(
                    (InputMassBalance balance) ->
                        balance.sourceBodyId().map(StableId::toString).orElse(""))
                .thenComparingInt(InputMassBalance::upslopeDistanceBlocks))
        .toList();
  }

  private static void validateInputBalances(
      ColluvialSedimentBudget budget, List<InputMassBalance> balances) {
    if (balances.size() != budget.sourceBalances().size() + 1) {
      throw new IllegalArgumentException("absolute mass inputs must cover the normalized budget");
    }
    InputMassBalance matrix = balances.getFirst();
    if (matrix.sourceBodyId().isPresent()
        || matrix.upslopeDistanceBlocks() != 0
        || !matrix.matches(budget.weatheredMatrixBalance())) {
      throw new IllegalArgumentException("absolute mass matrix input does not match budget");
    }
    for (int index = 0; index < budget.sourceBalances().size(); index++) {
      ColluvialSedimentBudget.SourceBalance source = budget.sourceBalances().get(index);
      InputMassBalance input =
          balances.stream()
              .filter(
                  balance ->
                      balance.sourceBodyId().equals(Optional.of(source.sourceBodyId()))
                          && balance.upslopeDistanceBlocks() == source.upslopeDistanceBlocks())
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("absolute mass source is missing"));
      if (!input.matches(source.balance())) {
        throw new IllegalArgumentException("absolute mass source does not match budget");
      }
    }
  }

  /** One input tranche converted without losing its exact normalized ledger values. */
  public record InputMassBalance(
      Optional<StableId> sourceBodyId,
      int upslopeDistanceBlocks,
      long capacityFixedUnits,
      long mobilizedFixedUnits,
      long retainedFixedUnits,
      long transportLossFixedUnits,
      long bypassedFixedUnits,
      long depositedFixedUnits,
      ColluvialSedimentBudget.GrainMass capacityGrainMass,
      ColluvialSedimentBudget.GrainMass mobilizedGrainMass,
      ColluvialSedimentBudget.GrainMass retainedGrainMass,
      ColluvialSedimentBudget.GrainMass transportLossGrainMass,
      ColluvialSedimentBudget.GrainMass bypassedGrainMass,
      ColluvialSedimentBudget.GrainMass depositedGrainMass) {
    public InputMassBalance(
        Optional<StableId> sourceBodyId,
        int upslopeDistanceBlocks,
        long capacityFixedUnits,
        long mobilizedFixedUnits,
        long retainedFixedUnits,
        long transportLossFixedUnits,
        long bypassedFixedUnits,
        long depositedFixedUnits) {
      this(
          sourceBodyId,
          upslopeDistanceBlocks,
          capacityFixedUnits,
          mobilizedFixedUnits,
          retainedFixedUnits,
          transportLossFixedUnits,
          bypassedFixedUnits,
          depositedFixedUnits,
          new ColluvialSedimentBudget.GrainMass(capacityFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(mobilizedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(retainedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(transportLossFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(bypassedFixedUnits, 0, 0),
          new ColluvialSedimentBudget.GrainMass(depositedFixedUnits, 0, 0));
    }

    public InputMassBalance {
      if (sourceBodyId == null
          || upslopeDistanceBlocks < 0
          || capacityFixedUnits <= 0
          || mobilizedFixedUnits < 0
          || retainedFixedUnits < 0
          || transportLossFixedUnits < 0
          || bypassedFixedUnits < 0
          || depositedFixedUnits < 0
          || capacityGrainMass == null
          || mobilizedGrainMass == null
          || retainedGrainMass == null
          || transportLossGrainMass == null
          || bypassedGrainMass == null
          || depositedGrainMass == null
          || capacityFixedUnits != retainedFixedUnits + mobilizedFixedUnits
          || mobilizedFixedUnits
              != transportLossFixedUnits + bypassedFixedUnits + depositedFixedUnits
          || capacityGrainMass.totalFixedUnits() != capacityFixedUnits
          || mobilizedGrainMass.totalFixedUnits() != mobilizedFixedUnits
          || retainedGrainMass.totalFixedUnits() != retainedFixedUnits
          || transportLossGrainMass.totalFixedUnits() != transportLossFixedUnits
          || bypassedGrainMass.totalFixedUnits() != bypassedFixedUnits
          || depositedGrainMass.totalFixedUnits() != depositedFixedUnits
          || !capacityGrainMass.equals(retainedGrainMass.add(mobilizedGrainMass))
          || !mobilizedGrainMass.equals(
              transportLossGrainMass.add(bypassedGrainMass).add(depositedGrainMass))) {
        throw new IllegalArgumentException("absolute mass input balance does not close");
      }
    }

    private static InputMassBalance from(
        Optional<StableId> sourceBodyId,
        int upslopeDistanceBlocks,
        ColluvialSedimentBudget.InputBalance balance) {
      return new InputMassBalance(
          sourceBodyId,
          upslopeDistanceBlocks,
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
          balance.depositedGrainMass());
    }

    private boolean matches(ColluvialSedimentBudget.InputBalance balance) {
      return capacityFixedUnits == balance.input().capacityFixedUnits()
          && mobilizedFixedUnits == balance.mobilizedFixedUnits()
          && retainedFixedUnits == balance.retainedFixedUnits()
          && transportLossFixedUnits == balance.transportLossFixedUnits()
          && bypassedFixedUnits == balance.bypassedFixedUnits()
          && depositedFixedUnits == balance.depositedFixedUnits()
          && capacityGrainMass.equals(balance.capacityGrainMass())
          && mobilizedGrainMass.equals(balance.mobilizedGrainMass())
          && retainedGrainMass.equals(balance.retainedGrainMass())
          && transportLossGrainMass.equals(balance.transportLossGrainMass())
          && bypassedGrainMass.equals(balance.bypassedGrainMass())
          && depositedGrainMass.equals(balance.depositedGrainMass());
    }
  }
}
