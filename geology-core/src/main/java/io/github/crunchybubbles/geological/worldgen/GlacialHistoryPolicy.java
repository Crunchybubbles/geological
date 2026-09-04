package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.GlacialTransportState;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;

/** Explicit, immutable policy for supplying event-local ice history to the glacial prototype. */
public record GlacialHistoryPolicy(
    String policyId,
    GlacialTransportState.IceClass iceClass,
    Point2 flowDirection,
    double iceThicknessBlocks,
    double entrainmentFraction,
    double depositionEfficiency,
    long sourceInventoryFixedUnits,
    AgeKey formationAge,
    boolean connected) {
  public GlacialHistoryPolicy {
    if (policyId == null
        || policyId.isBlank()
        || iceClass == null
        || flowDirection == null
        || formationAge == null) {
      throw new IllegalArgumentException("glacial history policy identity is required");
    }
    if (!Double.isFinite(iceThicknessBlocks)
        || iceThicknessBlocks < 0.0
        || !Double.isFinite(entrainmentFraction)
        || entrainmentFraction < 0.0
        || entrainmentFraction > 1.0
        || !Double.isFinite(depositionEfficiency)
        || depositionEfficiency < 0.0
        || depositionEfficiency > 1.0
        || sourceInventoryFixedUnits < 0) {
      throw new IllegalArgumentException("glacial history policy values are invalid");
    }
    double magnitude = StrictMath.hypot(flowDirection.x(), flowDirection.z());
    if (iceClass != GlacialTransportState.IceClass.NONE
        && (!Double.isFinite(magnitude) || magnitude < 0.999 || magnitude > 1.001)) {
      throw new IllegalArgumentException("glacial flow direction must be unit length");
    }
    if (iceClass == GlacialTransportState.IceClass.NONE
        && (iceThicknessBlocks != 0.0
            || entrainmentFraction != 0.0
            || depositionEfficiency != 0.0
            || sourceInventoryFixedUnits != 0
            || connected)) {
      throw new IllegalArgumentException("empty glacial policy cannot carry ice evidence");
    }
  }

  /** The safe default: no climate/ice compiler has authorized a glacial event. */
  public static GlacialHistoryPolicy none() {
    return new GlacialHistoryPolicy(
        "none",
        GlacialTransportState.IceClass.NONE,
        new Point2(1.0, 0.0),
        0.0,
        0.0,
        0.0,
        0,
        new AgeKey(0.0, 0),
        false);
  }

  /** Small deterministic positive fixture used only by review tests and the packet generator. */
  public static GlacialHistoryPolicy fixture() {
    return new GlacialHistoryPolicy(
        "deterministic-fixture",
        GlacialTransportState.IceClass.CONTINENTAL_ICE,
        new Point2(0.6, 0.8),
        32.0,
        0.78,
        0.42,
        100_000,
        new AgeKey(12.0, 0),
        true);
  }

  public GlacialTransportState.History history(
      Province province, WorldIdentity identity, CellKey cell) {
    if (province == null || identity == null || cell == null) {
      throw new IllegalArgumentException("province, identity, and cell are required");
    }
    StableId eventId =
        identity.stream(
                "geological",
                "glacial-history:"
                    + policyId
                    + ":"
                    + iceClass.name().toLowerCase(java.util.Locale.ROOT),
                cell,
                0)
            .stableId();
    return new GlacialTransportState.History(
        eventId,
        formationAge,
        iceClass,
        flowDirection,
        iceThicknessBlocks,
        entrainmentFraction,
        depositionEfficiency,
        sourceInventoryFixedUnits,
        connected);
  }
}
