package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Phase 3 restricted-basin evaporite and potash sequence.
 *
 * <p>The state is a compact sedimentary-system proof: a replenished brine source, limited outflow,
 * repeated concentration/reflooding, and a basin-bound sulfate-to-halite-to-potash succession. It
 * is not a universal seawater recipe or a block-placement instruction.
 */
public record EvaporitePotashState(
    StableId systemId,
    FormationStatus status,
    StableId basinId,
    StableId brineSourceId,
    StableId sulfateBodyId,
    StableId haliteBodyId,
    StableId potashBodyId,
    AgeKey formationAge,
    BasinSetting basinSetting,
    RestrictionClass restrictionClass,
    SoluteSourceClass soluteSourceClass,
    BrineEvolutionClass brineEvolutionClass,
    Point3 localCenter,
    double halfLengthBlocks,
    double halfWidthBlocks,
    double sequenceThicknessBlocks,
    int concentrationEpisodes,
    List<BrineStage> brineSequence,
    long soluteSourceBudgetFixedUnits,
    long sulfateAllocationFixedUnits,
    long haliteAllocationFixedUnits,
    long potashAllocationFixedUnits,
    Optional<String> failedGate) {
  private static final long FIXED_SCALE = 1_000_000L;

  public EvaporitePotashState {
    if (systemId == null
        || status == null
        || basinId == null
        || brineSourceId == null
        || sulfateBodyId == null
        || haliteBodyId == null
        || potashBodyId == null
        || formationAge == null
        || basinSetting == null
        || restrictionClass == null
        || soluteSourceClass == null
        || brineEvolutionClass == null
        || localCenter == null
        || brineSequence == null
        || failedGate == null) {
      throw new IllegalArgumentException("evaporite/potash state must be complete");
    }
    if (!Double.isFinite(halfLengthBlocks)
        || halfLengthBlocks <= 0.0
        || !Double.isFinite(halfWidthBlocks)
        || halfWidthBlocks <= 0.0
        || !Double.isFinite(sequenceThicknessBlocks)
        || sequenceThicknessBlocks <= 0.0) {
      throw new IllegalArgumentException("evaporite sequence extents must be finite and positive");
    }
    if (concentrationEpisodes < 0 || concentrationEpisodes > 6) {
      throw new IllegalArgumentException("evaporite concentration episodes must be bounded");
    }
    if (soluteSourceBudgetFixedUnits < 0L
        || sulfateAllocationFixedUnits < 0L
        || haliteAllocationFixedUnits < 0L
        || potashAllocationFixedUnits < 0L) {
      throw new IllegalArgumentException("evaporite budgets cannot be negative");
    }
    long sequenceAllocation =
        Math.addExact(
            Math.addExact(sulfateAllocationFixedUnits, haliteAllocationFixedUnits),
            potashAllocationFixedUnits);
    if (sequenceAllocation > soluteSourceBudgetFixedUnits) {
      throw new IllegalArgumentException("evaporite sequence exceeds its brine source budget");
    }
    brineSequence =
        List.copyOf(brineSequence).stream()
            .sorted(Comparator.comparingDouble(BrineStage::lowerDepthFraction))
            .toList();
    if (brineSequence.stream().anyMatch(stage -> stage == null)
        || brineSequence.stream().map(BrineStage::kind).distinct().count() != brineSequence.size()
        || brineSequence.stream().map(BrineStage::bodyId).distinct().count()
            != brineSequence.size()) {
      throw new IllegalArgumentException("evaporite stages must be unique");
    }
    double previousUpper = 0.0;
    AgeKey previousAge = null;
    long sulfateStageAllocation = 0L;
    long haliteStageAllocation = 0L;
    long potashStageAllocation = 0L;
    for (BrineStage stage : brineSequence) {
      if (stage.lowerDepthFraction() < previousUpper) {
        throw new IllegalArgumentException("evaporite stages must not overlap vertically");
      }
      if (previousAge != null && !stage.age().youngerThan(previousAge)) {
        throw new IllegalArgumentException("evaporite stages must become younger upward");
      }
      previousUpper = stage.upperDepthFraction();
      previousAge = stage.age();
      switch (stage.kind()) {
        case MARGINAL_SULFATE -> sulfateStageAllocation = stage.allocationFixedUnits();
        case BASIN_CENTER_HALITE -> haliteStageAllocation = stage.allocationFixedUnits();
        case LATE_POTASH -> potashStageAllocation = stage.allocationFixedUnits();
      }
    }
    if (sulfateStageAllocation != sulfateAllocationFixedUnits
        || haliteStageAllocation != haliteAllocationFixedUnits
        || potashStageAllocation != potashAllocationFixedUnits) {
      throw new IllegalArgumentException("evaporite stage allocations must match summary budgets");
    }
    if (status == FormationStatus.FORMED) {
      if (basinSetting != BasinSetting.RESTRICTED_MARINE_EMBAYMENT
          || restrictionClass != RestrictionClass.LIMITED_OUTFLOW
          || soluteSourceClass != SoluteSourceClass.REPLENISHED_SEAWATER
          || brineEvolutionClass != BrineEvolutionClass.REFLOODING_HALITE_TO_POTASH
          || concentrationEpisodes < 2
          || brineSequence.size() != 3
          || previousUpper != 1.0
          || failedGate.isPresent()
          || sequenceAllocation <= 0L) {
        throw new IllegalArgumentException(
            "formed evaporites require restricted replenished brine sequence evidence");
      }
    } else if (basinSetting != BasinSetting.NO_RESTRICTED_BASIN
        || restrictionClass != RestrictionClass.OPEN_OUTFLOW
        || soluteSourceClass != SoluteSourceClass.NO_SOLUTE_REPLENISHMENT
        || brineEvolutionClass != BrineEvolutionClass.UNRESOLVED
        || concentrationEpisodes != 0
        || !brineSequence.isEmpty()
        || sequenceAllocation != 0L
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed evaporites must retain the failed basin gate");
    }
  }

  /** Derives the deterministic restricted-basin branch from the province's sedimentary context. */
  public static EvaporitePotashState proofFor(Province province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    RiftArcGeometry.Basin basin = province.geometry().basin();
    StableId systemId =
        identity.stream("geological", "evaporite_potash_system", province.homeCell(), 0).stableId();
    StableId brineSourceId =
        identity.stream("geological", "evaporite_brine_source", province.homeCell(), 0).stableId();
    StableId sulfateBodyId =
        identity.stream("geological", "evaporite_sulfate_body", province.homeCell(), 0).stableId();
    StableId haliteBodyId =
        identity.stream("geological", "evaporite_halite_body", province.homeCell(), 0).stableId();
    StableId potashBodyId =
        identity.stream("geological", "evaporite_potash_body", province.homeCell(), 0).stableId();

    double sequenceThickness = 144.0;
    boolean hasMarineBasin = province.grammar().formsVms();
    boolean hasAccommodation = basin.maximumThickness() >= sequenceThickness;
    boolean eligible = hasMarineBasin && hasAccommodation;
    Point3 center =
        new Point3(basin.center().x(), basin.baseElevation() + 90.0, basin.center().z());
    if (!eligible) {
      return new EvaporitePotashState(
          systemId,
          FormationStatus.BARREN_SYSTEM,
          basin.id(),
          brineSourceId,
          sulfateBodyId,
          haliteBodyId,
          potashBodyId,
          basin.birthAge(),
          BasinSetting.NO_RESTRICTED_BASIN,
          RestrictionClass.OPEN_OUTFLOW,
          SoluteSourceClass.NO_SOLUTE_REPLENISHMENT,
          BrineEvolutionClass.UNRESOLVED,
          center,
          420.0,
          300.0,
          sequenceThickness,
          0,
          List.of(),
          0L,
          0L,
          0L,
          0L,
          Optional.of(hasMarineBasin ? "basin_water_or_solute_budget" : "restriction"));
    }

    List<BrineStage> sequence =
        List.of(
            new BrineStage(
                StageKind.MARGINAL_SULFATE,
                new AgeKey(248.0, 0),
                0.0,
                0.24,
                1.0,
                90_000L,
                sulfateBodyId),
            new BrineStage(
                StageKind.BASIN_CENTER_HALITE,
                new AgeKey(246.0, 1),
                0.24,
                0.76,
                0.84,
                250_000L,
                haliteBodyId),
            new BrineStage(
                StageKind.LATE_POTASH,
                new AgeKey(244.0, 2),
                0.76,
                1.0,
                0.58,
                90_000L,
                potashBodyId));
    return new EvaporitePotashState(
        systemId,
        FormationStatus.FORMED,
        basin.id(),
        brineSourceId,
        sulfateBodyId,
        haliteBodyId,
        potashBodyId,
        sequence.getFirst().age(),
        BasinSetting.RESTRICTED_MARINE_EMBAYMENT,
        RestrictionClass.LIMITED_OUTFLOW,
        SoluteSourceClass.REPLENISHED_SEAWATER,
        BrineEvolutionClass.REFLOODING_HALITE_TO_POTASH,
        center,
        420.0,
        300.0,
        sequenceThickness,
        3,
        sequence,
        1_000_000L,
        90_000L,
        250_000L,
        90_000L,
        Optional.empty());
  }

  /** Returns whether a local point lies inside the basin-bound sequence envelope. */
  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED) {
      return false;
    }
    double horizontal =
        StrictMath.pow((localPoint.x() - localCenter.x()) / halfLengthBlocks, 2.0)
            + StrictMath.pow((localPoint.z() - localCenter.z()) / halfWidthBlocks, 2.0);
    double vertical =
        StrictMath.abs(localPoint.y() - localCenter.y()) / (sequenceThicknessBlocks / 2.0);
    return horizontal <= 1.0 && vertical <= 1.0;
  }

  /** Classifies a local point into the applicable sulfate, halite, or potash stage. */
  public Optional<BrineStage> zoneAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED) {
      return Optional.empty();
    }
    double horizontal =
        StrictMath.pow((localPoint.x() - localCenter.x()) / halfLengthBlocks, 2.0)
            + StrictMath.pow((localPoint.z() - localCenter.z()) / halfWidthBlocks, 2.0);
    if (horizontal > 1.0) {
      return Optional.empty();
    }
    double depthFraction =
        (localPoint.y() - (localCenter.y() - sequenceThicknessBlocks / 2.0))
            / sequenceThicknessBlocks;
    return brineSequence.stream()
        .filter(
            stage ->
                depthFraction >= stage.lowerDepthFraction()
                    && (depthFraction < stage.upperDepthFraction()
                        || (depthFraction == stage.upperDepthFraction()
                            && stage.upperDepthFraction() == 1.0))
                    && StrictMath.sqrt(horizontal) <= stage.maximumRadiusFraction())
        .findFirst();
  }

  public enum BasinSetting {
    RESTRICTED_MARINE_EMBAYMENT,
    NO_RESTRICTED_BASIN
  }

  public enum RestrictionClass {
    LIMITED_OUTFLOW,
    OPEN_OUTFLOW
  }

  public enum SoluteSourceClass {
    REPLENISHED_SEAWATER,
    NO_SOLUTE_REPLENISHMENT
  }

  public enum BrineEvolutionClass {
    REFLOODING_HALITE_TO_POTASH,
    UNRESOLVED
  }

  public enum StageKind {
    MARGINAL_SULFATE,
    BASIN_CENTER_HALITE,
    LATE_POTASH
  }

  public record BrineStage(
      StageKind kind,
      AgeKey age,
      double lowerDepthFraction,
      double upperDepthFraction,
      double maximumRadiusFraction,
      long allocationFixedUnits,
      StableId bodyId) {
    public BrineStage {
      if (kind == null || age == null || bodyId == null) {
        throw new IllegalArgumentException("evaporite stage identity is required");
      }
      if (!Double.isFinite(lowerDepthFraction)
          || !Double.isFinite(upperDepthFraction)
          || lowerDepthFraction < 0.0
          || upperDepthFraction <= lowerDepthFraction
          || upperDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("evaporite stage geometry is invalid");
      }
      if (allocationFixedUnits <= 0L || allocationFixedUnits > FIXED_SCALE) {
        throw new IllegalArgumentException("evaporite stage allocation is out of bounds");
      }
    }
  }
}
