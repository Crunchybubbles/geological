package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exact normalized mass ledger for the inputs deposited in one modeled colluvial parcel. */
public record ColluvialSedimentBudget(
    String unit,
    long sourceInventoryFixedUnits,
    long depositedInventoryFixedUnits,
    long weatheredMatrixInputFixedUnits,
    List<SourceDebit> sourceDebits) {
  public static final String NORMALIZED_MASS_UNIT = "phase2_normalized_sediment_mass";

  public ColluvialSedimentBudget {
    if (unit == null || unit.isBlank() || sourceDebits == null) {
      throw new IllegalArgumentException("colluvial sediment budget identity must be complete");
    }
    if (sourceInventoryFixedUnits <= 0
        || depositedInventoryFixedUnits <= 0
        || weatheredMatrixInputFixedUnits <= 0) {
      throw new IllegalArgumentException("colluvial sediment budget amounts must be positive");
    }
    sourceDebits =
        List.copyOf(sourceDebits).stream()
            .sorted(
                Comparator.comparingInt(SourceDebit::upslopeDistanceBlocks)
                    .thenComparing(SourceDebit::sourceBodyId))
            .toList();
    if (sourceDebits.isEmpty()) {
      throw new IllegalArgumentException("colluvial sediment budget requires source debits");
    }
    Set<Integer> distances = new HashSet<>();
    long allocated = weatheredMatrixInputFixedUnits;
    for (SourceDebit debit : sourceDebits) {
      if (!distances.add(debit.upslopeDistanceBlocks())) {
        throw new IllegalArgumentException("colluvial sediment debit distances must be unique");
      }
      allocated = Math.addExact(allocated, debit.debitedFixedUnits());
    }
    if (allocated != sourceInventoryFixedUnits
        || depositedInventoryFixedUnits != sourceInventoryFixedUnits) {
      throw new IllegalArgumentException(
          "colluvial sediment inputs and deposited inventory must close exactly");
    }
  }

  public static ColluvialSedimentBudget normalizedParcel(
      List<ColluvialSourceContribution> contributions, long weatheredMatrixFractionPpm) {
    if (contributions == null || contributions.isEmpty()) {
      throw new IllegalArgumentException("normalized colluvial budget requires source inputs");
    }
    List<SourceDebit> debits = List.copyOf(contributions).stream().map(SourceDebit::from).toList();
    return new ColluvialSedimentBudget(
        NORMALIZED_MASS_UNIT,
        MaterialAssemblage.SCALE,
        MaterialAssemblage.SCALE,
        weatheredMatrixFractionPpm,
        debits);
  }

  /** One exact withdrawal from a bounded bedrock-source tranche. */
  public record SourceDebit(
      StableId sourceBodyId, int upslopeDistanceBlocks, long debitedFixedUnits) {
    public SourceDebit {
      if (sourceBodyId == null || upslopeDistanceBlocks < 0 || debitedFixedUnits <= 0) {
        throw new IllegalArgumentException(
            "colluvial sediment source debit must be complete and positive");
      }
    }

    private static SourceDebit from(ColluvialSourceContribution contribution) {
      return new SourceDebit(
          contribution.sourceBodyId(),
          contribution.upslopeDistanceBlocks(),
          contribution.assemblageFractionPpm());
    }
  }
}
