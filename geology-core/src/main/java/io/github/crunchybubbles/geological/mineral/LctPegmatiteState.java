package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.MagmaDifferentiationState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Phase 3 LCT pegmatite child-body state derived from the final pulse of an evolved magma lineage.
 *
 * <p>This is a bounded dike/zone proof rather than an ore-grade or block-placement model. A
 * pegmatite texture never bypasses the evolved-lineage, residual-fluid, and emplacement gates.
 */
public record LctPegmatiteState(
    StableId parentIntrusionId,
    StableId childBodyId,
    FormationStatus status,
    FertilityClass fertilityClass,
    EmplacementClass emplacementClass,
    FluidSourceClass fluidSourceClass,
    double differentiationProgress,
    Point3 localCenter,
    double dikeLengthBlocks,
    double dikeHalfWidthBlocks,
    double dikeHalfHeightBlocks,
    List<InternalZone> internalZones,
    long sourceBudgetFixedUnits,
    long childAllocationFixedUnits,
    Optional<String> failedGate) {
  public LctPegmatiteState {
    if (parentIntrusionId == null
        || childBodyId == null
        || status == null
        || fertilityClass == null
        || emplacementClass == null
        || fluidSourceClass == null
        || localCenter == null
        || internalZones == null
        || failedGate == null) {
      throw new IllegalArgumentException("LCT pegmatite state must be complete");
    }
    if (!Double.isFinite(differentiationProgress)
        || differentiationProgress < 0.0
        || differentiationProgress > 1.0) {
      throw new IllegalArgumentException("differentiation progress must lie in [0, 1]");
    }
    requirePositive(dikeLengthBlocks, "dikeLengthBlocks");
    requirePositive(dikeHalfWidthBlocks, "dikeHalfWidthBlocks");
    requirePositive(dikeHalfHeightBlocks, "dikeHalfHeightBlocks");
    if (sourceBudgetFixedUnits < 0L
        || childAllocationFixedUnits < 0L
        || childAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("LCT pegmatite budgets are out of bounds");
    }
    internalZones =
        List.copyOf(internalZones).stream()
            .sorted(Comparator.comparingDouble(InternalZone::innerRadiusFraction))
            .toList();
    double previousOuter = 0.0;
    for (InternalZone zone : internalZones) {
      if (zone.innerRadiusFraction() < previousOuter) {
        throw new IllegalArgumentException("LCT internal zones must not overlap");
      }
      previousOuter = zone.outerRadiusFraction();
    }
    if (status == FormationStatus.FORMED && internalZones.isEmpty()) {
      throw new IllegalArgumentException("formed LCT pegmatites require internal zones");
    }
    if (status != FormationStatus.FORMED && !internalZones.isEmpty()) {
      throw new IllegalArgumentException("barren LCT candidates cannot publish formed zones");
    }
    if (status == FormationStatus.FORMED && failedGate.isPresent()) {
      throw new IllegalArgumentException("formed LCT pegmatites cannot have a failed gate");
    }
    if (status != FormationStatus.FORMED && failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren LCT candidates require a failed gate");
    }
  }

  /** Derives the child-body state from the province's final differentiated pulse. */
  public static LctPegmatiteState proofFor(Province province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    RiftArcGeometry geometry = province.geometry();
    RiftArcGeometry.PlutonPulse parent = geometry.plutonPulses().getLast();
    int pulseOrder = geometry.plutonPulses().size() - 1;
    MagmaDifferentiationState differentiation =
        MagmaDifferentiationState.arcProofFor(pulseOrder, List.of(geometry.basementId()));
    boolean eligible =
        province.grammar().formsPorphyry()
            && differentiation.residualFluidPotential()
                == MagmaDifferentiationState.ResidualFluidPotential.VERY_HIGH;
    StableId childBodyId =
        identity.stream("geological", "lct_pegmatite_child", province.homeCell(), pulseOrder)
            .stableId();
    Optional<String> failedGate =
        eligible
            ? Optional.empty()
            : Optional.of(province.grammar().formsPorphyry() ? "residual_budget" : "lineage");
    List<InternalZone> zones =
        eligible
            ? List.of(
                new InternalZone(ZoneClass.WALL, 0.70, 1.0, 450_000L),
                new InternalZone(ZoneClass.INTERMEDIATE, 0.35, 0.70, 700_000L),
                new InternalZone(ZoneClass.QUARTZ_CORE, 0.0, 0.35, 900_000L))
            : List.of();
    Point3 parentCenter = parent.center();
    Point3 childCenter =
        new Point3(
            parentCenter.x() + parent.radiusU() * 0.62,
            parentCenter.y() + parent.radiusY() * 0.14,
            parentCenter.z() - parent.radiusV() * 0.18);
    return new LctPegmatiteState(
        parent.id(),
        childBodyId,
        eligible ? FormationStatus.FORMED : FormationStatus.BARREN_SYSTEM,
        eligible ? FertilityClass.LCT_RARE_ELEMENT : FertilityClass.UNRESOLVED_FERTILITY,
        eligible
            ? EmplacementClass.APICAL_FRACTURE_DIKE_SWARM
            : EmplacementClass.NO_EMPLACEMENT_PATH,
        eligible ? FluidSourceClass.RESIDUAL_VOLATILE_MELT : FluidSourceClass.NO_RESIDUAL_FLUID,
        differentiation.cumulativeCrystalFractionPpm() / (double) 1_000_000L,
        childCenter,
        180.0,
        32.0,
        48.0,
        zones,
        eligible ? 900_000L : 0L,
        eligible ? 150_000L : 0L,
        failedGate);
  }

  /** Returns the internal pegmatite zone containing a local point. */
  public Optional<InternalZone> zoneAt(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > dikeHalfHeightBlocks) {
      return Optional.empty();
    }
    double normalizedX = (localPoint.x() - localCenter.x()) / dikeHalfWidthBlocks;
    double normalizedZ = (localPoint.z() - localCenter.z()) / (dikeLengthBlocks / 2.0);
    double radial = StrictMath.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);
    return internalZones.stream()
        .filter(
            zone ->
                radial >= zone.innerRadiusFraction()
                    && (radial < zone.outerRadiusFraction()
                        || radial == zone.outerRadiusFraction()
                            && zone.outerRadiusFraction() == 1.0))
        .findFirst();
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum FertilityClass {
    LCT_RARE_ELEMENT,
    UNRESOLVED_FERTILITY
  }

  public enum EmplacementClass {
    APICAL_FRACTURE_DIKE_SWARM,
    NO_EMPLACEMENT_PATH
  }

  public enum FluidSourceClass {
    RESIDUAL_VOLATILE_MELT,
    NO_RESIDUAL_FLUID
  }

  public enum ZoneClass {
    WALL,
    INTERMEDIATE,
    QUARTZ_CORE
  }

  public record InternalZone(
      ZoneClass kind, double innerRadiusFraction, double outerRadiusFraction, long intensityPpm) {
    public InternalZone {
      if (kind == null) {
        throw new IllegalArgumentException("LCT internal zone kind is required");
      }
      if (!Double.isFinite(innerRadiusFraction)
          || !Double.isFinite(outerRadiusFraction)
          || innerRadiusFraction < 0.0
          || outerRadiusFraction <= innerRadiusFraction
          || outerRadiusFraction > 1.0) {
        throw new IllegalArgumentException("LCT internal zone radii are invalid");
      }
      if (intensityPpm < 0L || intensityPpm > 1_000_000L) {
        throw new IllegalArgumentException("LCT internal-zone intensity must be bounded");
      }
    }
  }
}
