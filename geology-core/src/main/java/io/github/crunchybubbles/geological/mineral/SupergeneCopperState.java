package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.List;
import java.util.Optional;

/**
 * Phase 3 oxidation/leaching and supergene copper profile derived from a porphyry source.
 *
 * <p>The profile is a bounded, normalized process proof. It is not an assay, a thermodynamic
 * calculation, or permission to place a secondary ore blanket without the primary source and
 * preservation gates.
 */
public record SupergeneCopperState(
    StableId systemId,
    FormationStatus status,
    StableId primaryDepositId,
    StableId weatheringProcessId,
    StableId waterTableId,
    AgeKey formationAge,
    SourceClass sourceClass,
    OxidationClass oxidationClass,
    WaterTableClass waterTableClass,
    TrapClass trapClass,
    PreservationClass preservationClass,
    Point3 localCenter,
    double blanketHalfLengthBlocks,
    double blanketHalfWidthBlocks,
    double profileThicknessBlocks,
    List<Horizon> horizons,
    long sourceBudgetFixedUnits,
    long leachableCopperFixedUnits,
    long supergeneAllocationFixedUnits,
    long oxidizedAndDissolvedLossFixedUnits,
    Optional<String> failedGate) {
  private static final long SCALE = 1_000_000L;

  public SupergeneCopperState {
    if (systemId == null
        || status == null
        || primaryDepositId == null
        || weatheringProcessId == null
        || waterTableId == null
        || formationAge == null
        || sourceClass == null
        || oxidationClass == null
        || waterTableClass == null
        || trapClass == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("supergene copper state must be complete");
    }
    requirePositive(blanketHalfLengthBlocks, "blanketHalfLengthBlocks");
    requirePositive(blanketHalfWidthBlocks, "blanketHalfWidthBlocks");
    requirePositive(profileThicknessBlocks, "profileThicknessBlocks");
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("supergene horizons cannot be null");
    }
    if (sourceBudgetFixedUnits < 0L
        || leachableCopperFixedUnits < 0L
        || supergeneAllocationFixedUnits < 0L
        || oxidizedAndDissolvedLossFixedUnits < 0L
        || leachableCopperFixedUnits > sourceBudgetFixedUnits
        || supergeneAllocationFixedUnits > leachableCopperFixedUnits
        || Math.addExact(supergeneAllocationFixedUnits, oxidizedAndDissolvedLossFixedUnits)
            != leachableCopperFixedUnits) {
      throw new IllegalArgumentException("supergene copper budgets are inconsistent");
    }
    validateHorizonSequence(horizons);
    long horizonAllocation = horizons.stream().mapToLong(Horizon::allocationFixedUnits).sum();
    if (horizonAllocation != leachableCopperFixedUnits) {
      throw new IllegalArgumentException(
          "supergene horizon allocations must close to leachable Cu");
    }
    long supergeneHorizonAllocation =
        horizons.stream()
            .filter(horizon -> horizon.kind() == HorizonKind.SUPERGENE_SULFIDE)
            .mapToLong(Horizon::allocationFixedUnits)
            .sum();
    if (supergeneHorizonAllocation != supergeneAllocationFixedUnits) {
      throw new IllegalArgumentException(
          "supergene horizon allocation must close to the enriched blanket");
    }
    long oxidizedHorizonAllocation =
        horizons.stream()
            .filter(horizon -> horizon.kind() == HorizonKind.OXIDIZED_COPPER)
            .mapToLong(Horizon::allocationFixedUnits)
            .sum();
    if (oxidizedHorizonAllocation != oxidizedAndDissolvedLossFixedUnits) {
      throw new IllegalArgumentException(
          "oxidized horizon allocation must close to dissolved copper loss");
    }
    if (status == FormationStatus.FORMED) {
      if (sourceClass != SourceClass.LEACHABLE_PRIMARY_CU_SULFIDE
          || oxidationClass != OxidationClass.OXIDIZING_VADOSE_PROFILE
          || waterTableClass != WaterTableClass.STABLE_PALEO_WATER_TABLE
          || trapClass != TrapClass.REDUCING_SULFIDE_TRAP
          || preservationClass != PreservationClass.PARTLY_PRESERVED_PROFILE
          || failedGate.isPresent()
          || horizons.size() != 3
          || sourceBudgetFixedUnits <= 0L
          || leachableCopperFixedUnits <= 0L
          || supergeneAllocationFixedUnits <= 0L) {
        throw new IllegalArgumentException(
            "formed supergene copper requires all oxidation, water-table, and preservation gates");
      }
    } else if (sourceClass != SourceClass.NO_PRIMARY_CU_SULFIDE
        || oxidationClass != OxidationClass.NO_OXIDATION_PATH
        || waterTableClass != WaterTableClass.NO_STABLE_WATER_TABLE
        || trapClass != TrapClass.NO_REDUCING_TRAP
        || preservationClass != PreservationClass.ERODED_OR_BURIED_PROFILE
        || !horizons.isEmpty()
        || sourceBudgetFixedUnits != 0L
        || leachableCopperFixedUnits != 0L
        || supergeneAllocationFixedUnits != 0L
        || oxidizedAndDissolvedLossFixedUnits != 0L
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException(
          "non-formed supergene copper must retain a failed hard gate");
    }
  }

  /** Derives the profile from the primary porphyry decision and deterministic world identity. */
  public static SupergeneCopperState proofFor(
      Province province, MineralSystemDecision decision, WorldIdentity identity) {
    if (province == null || decision == null || identity == null) {
      throw new IllegalArgumentException(
          "province, porphyry decision, and world identity are required");
    }
    if (!MineralSystemProofs.PORPHYRY_MODEL.equals(decision.modelId())
        || !province.proofIds().porphyrySystemId().equals(decision.candidateId())) {
      throw new IllegalArgumentException(
          "decision does not identify this province's porphyry system");
    }
    RiftArcGeometry geometry = province.geometry();
    StableId waterTableId =
        identity.stream("geological", "porphyry_supergene_water_table", province.homeCell(), 0)
            .stableId();
    Point3 localCenter =
        new Point3(geometry.porphyryCenter().x(), 35.0, geometry.porphyryCenter().z());
    boolean formed =
        decision.status() == FormationStatus.FORMED && province.grammar().formsPlacer();
    Optional<String> failedGate =
        formed
            ? Optional.empty()
            : Optional.of(
                !decision.status().equals(FormationStatus.FORMED)
                    ? "primary_cu_source"
                    : "exposure");
    if (!formed) {
      return new SupergeneCopperState(
          decision.candidateId(),
          FormationStatus.BARREN_SYSTEM,
          province.proofIds().porphyryDepositId(),
          province.proofIds().weatheringId(),
          waterTableId,
          new AgeKey(0.0, 0),
          SourceClass.NO_PRIMARY_CU_SULFIDE,
          OxidationClass.NO_OXIDATION_PATH,
          WaterTableClass.NO_STABLE_WATER_TABLE,
          TrapClass.NO_REDUCING_TRAP,
          PreservationClass.ERODED_OR_BURIED_PROFILE,
          localCenter,
          130.0,
          100.0,
          80.0,
          List.of(),
          0L,
          0L,
          0L,
          0L,
          failedGate);
    }

    long sourceBudget = decision.ledger().allocations().getOrDefault("deposit", 0L);
    long leachableCopper = 40_000L;
    long supergeneAllocation = 24_000L;
    long oxidizedLoss = leachableCopper - supergeneAllocation;
    StableId weatheringId = province.proofIds().weatheringId();
    List<Horizon> horizons =
        List.of(
            new Horizon(
                HorizonKind.LEACHED_CAP,
                Overprint.OXIDIZED_GOSSAN,
                0.0,
                0.20,
                0.95,
                0L,
                identity.stream("geological", "supergene_horizon", province.homeCell(), 0)
                    .stableId()),
            new Horizon(
                HorizonKind.OXIDIZED_COPPER,
                Overprint.OXIDIZED_GOSSAN,
                0.20,
                0.50,
                0.84,
                oxidizedLoss,
                identity.stream("geological", "supergene_horizon", province.homeCell(), 1)
                    .stableId()),
            new Horizon(
                HorizonKind.SUPERGENE_SULFIDE,
                Overprint.NONE,
                0.50,
                1.0,
                0.70,
                supergeneAllocation,
                identity.stream("geological", "supergene_horizon", province.homeCell(), 2)
                    .stableId()));
    return new SupergeneCopperState(
        decision.candidateId(),
        FormationStatus.FORMED,
        province.proofIds().porphyryDepositId(),
        weatheringId,
        waterTableId,
        new AgeKey(0.8, 0),
        SourceClass.LEACHABLE_PRIMARY_CU_SULFIDE,
        OxidationClass.OXIDIZING_VADOSE_PROFILE,
        WaterTableClass.STABLE_PALEO_WATER_TABLE,
        TrapClass.REDUCING_SULFIDE_TRAP,
        PreservationClass.PARTLY_PRESERVED_PROFILE,
        localCenter,
        130.0,
        100.0,
        80.0,
        horizons,
        sourceBudget,
        leachableCopper,
        supergeneAllocation,
        oxidizedLoss,
        failedGate);
  }

  /** Returns whether a local point lies inside the preserved blanket envelope. */
  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > profileThicknessBlocks / 2.0) {
      return false;
    }
    double along = (localPoint.z() - localCenter.z()) / blanketHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / blanketHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  /** Returns the preserved horizon containing a local point, if its radial gate also passes. */
  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double along = (localPoint.z() - localCenter.z()) / blanketHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / blanketHalfWidthBlocks;
    double radial = StrictMath.sqrt(along * along + across * across);
    double top = localCenter.y() + profileThicknessBlocks / 2.0;
    double depth = (top - localPoint.y()) / profileThicknessBlocks;
    return horizons.stream()
        .filter(
            horizon -> horizon.containsDepth(depth) && radial <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  /** Returns primary hypogene Cu left after the leachable weathering debit. */
  public long retainedHypogeneFixedUnits() {
    return sourceBudgetFixedUnits - leachableCopperFixedUnits;
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (horizon.topDepthFraction() < previousBottom - 1.0e-12) {
        throw new IllegalArgumentException("supergene horizons cannot overlap");
      }
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("supergene horizons must form a contiguous profile");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("supergene horizons must cover the normalized profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum SourceClass {
    LEACHABLE_PRIMARY_CU_SULFIDE,
    NO_PRIMARY_CU_SULFIDE
  }

  public enum OxidationClass {
    OXIDIZING_VADOSE_PROFILE,
    NO_OXIDATION_PATH
  }

  public enum WaterTableClass {
    STABLE_PALEO_WATER_TABLE,
    NO_STABLE_WATER_TABLE
  }

  public enum TrapClass {
    REDUCING_SULFIDE_TRAP,
    NO_REDUCING_TRAP
  }

  public enum PreservationClass {
    PARTLY_PRESERVED_PROFILE,
    ERODED_OR_BURIED_PROFILE
  }

  public enum HorizonKind {
    LEACHED_CAP,
    OXIDIZED_COPPER,
    SUPERGENE_SULFIDE
  }

  public record Horizon(
      HorizonKind kind,
      Overprint overprint,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      long allocationFixedUnits,
      StableId bodyId) {
    public Horizon {
      if (kind == null || overprint == null || bodyId == null) {
        throw new IllegalArgumentException("supergene horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("supergene horizon bounds are invalid");
      }
      if (allocationFixedUnits < 0L || allocationFixedUnits > SCALE) {
        throw new IllegalArgumentException("supergene horizon allocation is out of bounds");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
