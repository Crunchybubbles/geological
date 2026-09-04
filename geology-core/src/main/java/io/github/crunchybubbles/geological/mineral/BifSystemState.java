package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import java.util.Optional;

/**
 * Phase 3 BIF sheet state derived from an ancient, iron-supplying basin context.
 *
 * <p>The current proof represents the volcano-sedimentary Algoma branch. It keeps banding and
 * ocean-redox evidence as metadata and does not imply a supergene upgrade or economic grade.
 */
public record BifSystemState(
    StableId sheetId,
    FormationStatus status,
    BifType type,
    StableId basinId,
    StableId ironSourceId,
    AgeKey formationAge,
    RedoxClass oceanRedoxClass,
    GeometryClass geometryClass,
    Point2 localCenter,
    double strikeLengthBlocks,
    double halfWidthBlocks,
    double thicknessBlocks,
    long sourceBudgetFixedUnits,
    long sheetAllocationFixedUnits,
    Optional<String> failedGate) {
  public BifSystemState {
    if (sheetId == null
        || status == null
        || type == null
        || basinId == null
        || ironSourceId == null
        || formationAge == null
        || oceanRedoxClass == null
        || geometryClass == null
        || localCenter == null
        || failedGate == null) {
      throw new IllegalArgumentException("BIF system state must be complete");
    }
    requirePositive(strikeLengthBlocks, "strikeLengthBlocks");
    requirePositive(halfWidthBlocks, "halfWidthBlocks");
    requirePositive(thicknessBlocks, "thicknessBlocks");
    if (sourceBudgetFixedUnits < 0L
        || sheetAllocationFixedUnits < 0L
        || sheetAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("BIF budgets are out of bounds");
    }
    if (status == FormationStatus.FORMED) {
      if (type != BifType.ALGOMA_TYPE
          || oceanRedoxClass != RedoxClass.REDUCING
          || geometryClass != GeometryClass.BANDED_STRATIFORM_SHEET
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed BIF state must carry basin/redox sheet evidence");
      }
    } else if (type != BifType.UNRESOLVED
        || oceanRedoxClass != RedoxClass.BUFFERED
        || geometryClass != GeometryClass.NO_SHEET
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed BIF state must retain the failed gate");
    }
  }

  /** Derives a BIF candidate from the province's marine volcano-sedimentary context. */
  public static BifSystemState proofFor(Province province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    RiftArcGeometry geometry = province.geometry();
    boolean eligible = province.grammar().formsVms();
    StableId sheetId =
        identity.stream("geological", "bif_sheet", province.homeCell(), 0).stableId();
    Point2 basinCenter = geometry.basin().center();
    return new BifSystemState(
        sheetId,
        eligible ? FormationStatus.FORMED : FormationStatus.BARREN_SYSTEM,
        eligible ? BifType.ALGOMA_TYPE : BifType.UNRESOLVED,
        geometry.basin().id(),
        geometry.plutonPulses().getFirst().id(),
        new AgeKey(2_500.0, 0),
        eligible ? RedoxClass.REDUCING : RedoxClass.BUFFERED,
        eligible ? GeometryClass.BANDED_STRATIFORM_SHEET : GeometryClass.NO_SHEET,
        basinCenter,
        900.0,
        180.0,
        24.0,
        eligible ? 700_000L : 0L,
        eligible ? 180_000L : 0L,
        eligible ? Optional.empty() : Optional.of("volcano_sedimentary_basin"));
  }

  /** Returns whether a local horizontal point lies within the bounded BIF sheet envelope. */
  public boolean contains(Point2 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED) {
      return false;
    }
    double along = (localPoint.x() - localCenter.x()) / (strikeLengthBlocks / 2.0);
    double across = (localPoint.z() - localCenter.z()) / halfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum BifType {
    ALGOMA_TYPE,
    UNRESOLVED
  }

  public enum GeometryClass {
    BANDED_STRATIFORM_SHEET,
    NO_SHEET
  }
}
