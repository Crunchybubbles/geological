package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Opt-in glacial source-to-sink transport proof.
 *
 * <p>The default Overworld has no ice-history descriptor, so its state is explicitly barren. A
 * caller may provide a bounded {@link History} when a future climate/ice compiler has established
 * ice presence, flow, entrainment, and deposition evidence.
 */
public record GlacialTransportState(
    StableId systemId,
    FormationStatus status,
    TransportKind transportKind,
    SourceBasis sourceBasis,
    StableId parentBodyId,
    List<StableId> sourceBodyIds,
    StableId iceEventId,
    AgeKey formationAge,
    IceClass iceClass,
    Point2 flowDirection,
    double localCenterX,
    double localCenterY,
    double localCenterZ,
    double envelopeHalfLengthBlocks,
    double envelopeHalfWidthBlocks,
    double profileThicknessBlocks,
    long sourceInventoryFixedUnits,
    long releasedInventoryFixedUnits,
    long transportLossFixedUnits,
    long depositAllocationFixedUnits,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public GlacialTransportState {
    if (systemId == null
        || status == null
        || transportKind == null
        || sourceBasis == null
        || parentBodyId == null
        || sourceBodyIds == null
        || iceEventId == null
        || formationAge == null
        || iceClass == null
        || flowDirection == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("glacial transport state must be complete");
    }
    if (!Double.isFinite(localCenterX)
        || !Double.isFinite(localCenterY)
        || !Double.isFinite(localCenterZ)) {
      throw new IllegalArgumentException("glacial local center must be finite");
    }
    requirePositive(envelopeHalfLengthBlocks, "envelopeHalfLengthBlocks");
    requirePositive(envelopeHalfWidthBlocks, "envelopeHalfWidthBlocks");
    requirePositive(profileThicknessBlocks, "profileThicknessBlocks");
    double flowMagnitude = StrictMath.hypot(flowDirection.x(), flowDirection.z());
    if (!Double.isFinite(flowMagnitude) || flowMagnitude < 0.999 || flowMagnitude > 1.001) {
      throw new IllegalArgumentException("glacial flow direction must be unit length");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(parentBodyId)) {
      throw new IllegalArgumentException("glacial sources must be non-empty and retain the parent");
    }
    if (sourceInventoryFixedUnits < 0
        || releasedInventoryFixedUnits < 0
        || transportLossFixedUnits < 0
        || depositAllocationFixedUnits < 0
        || releasedInventoryFixedUnits > sourceInventoryFixedUnits
        || releasedInventoryFixedUnits != transportLossFixedUnits + depositAllocationFixedUnits) {
      throw new IllegalArgumentException("glacial inventory ledger is not closed");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("glacial horizons cannot be null");
    }
    validateHorizonSequence(horizons);
    if (status == FormationStatus.FORMED) {
      if (transportKind == TransportKind.NONE
          || sourceBasis == SourceBasis.NO_ICE_HISTORY
          || iceClass == IceClass.NONE
          || sourceInventoryFixedUnits == 0
          || releasedInventoryFixedUnits == 0
          || depositAllocationFixedUnits == 0
          || horizons.isEmpty()
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed glacial transport requires ice and deposition proof");
      }
    } else if (transportKind != TransportKind.NONE
        || sourceBasis != SourceBasis.NO_ICE_HISTORY
        || iceClass != IceClass.NONE
        || sourceInventoryFixedUnits != 0
        || releasedInventoryFixedUnits != 0
        || transportLossFixedUnits != 0
        || depositAllocationFixedUnits != 0
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren glacial transport must retain a failed gate");
    }
  }

  /** Resolves a glacial profile from explicit, bounded ice-history evidence. */
  public static GlacialTransportState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      History history) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null
        || history == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, parent, and ice history are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())) {
      throw new IllegalArgumentException("glacial parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("glacial surface point changed between stages");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
    StableId systemId =
        identity.stream(
                "geological",
                "glacial-transport:" + history.iceClass().name().toLowerCase(java.util.Locale.ROOT),
                cell,
                0)
            .stableId();
    List<StableId> sourceBodyIds =
        java.util.stream.Stream.concat(
                surface.context().sourceBodyIds().stream(),
                java.util.stream.Stream.of(parent.geology().rockBodyId()))
            .distinct()
            .sorted()
            .toList();
    var fields = surface.surface().fields();
    String failedGate = failedGate(fields, surface.context().kind(), history);
    long released = Math.round(history.sourceInventoryFixedUnits() * history.entrainmentFraction());
    long deposited = Math.round(released * history.depositionEfficiency());
    if (failedGate == null && (released <= 0 || deposited <= 0)) {
      failedGate = "source_inventory";
    }
    if (failedGate != null) {
      return barren(
          systemId,
          parent.geology().rockBodyId(),
          sourceBodyIds,
          history,
          localSurface,
          failedGate);
    }
    long loss = released - deposited;
    double thickness = Math.max(6.0, Math.min(24.0, history.iceThicknessBlocks() * 0.28));
    TransportKind transportKind = transportKind(history.iceClass());
    return new GlacialTransportState(
        systemId,
        FormationStatus.FORMED,
        transportKind,
        SourceBasis.EXPLICIT_ICE_HISTORY_SOURCE,
        parent.geology().rockBodyId(),
        sourceBodyIds,
        history.iceEventId(),
        history.formationAge(),
        history.iceClass(),
        history.flowDirection(),
        localSurface.x(),
        localSurface.y() - thickness / 2.0,
        localSurface.z(),
        history.iceClass() == IceClass.CONTINENTAL_ICE ? 112.0 : 84.0,
        history.iceClass() == IceClass.CONTINENTAL_ICE ? 88.0 : 64.0,
        thickness,
        history.sourceInventoryFixedUnits(),
        released,
        loss,
        deposited,
        formedHorizons(identity, history.iceClass(), cell),
        Optional.empty());
  }

  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenterY) > profileThicknessBlocks / 2.0) {
      return false;
    }
    double along = (localPoint.z() - localCenterZ) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenterX) / envelopeHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double along = (localPoint.z() - localCenterZ) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenterX) / envelopeHalfWidthBlocks;
    double radial = StrictMath.sqrt(along * along + across * across);
    double top = localCenterY + profileThicknessBlocks / 2.0;
    double depth = (top - localPoint.y()) / profileThicknessBlocks;
    return horizons.stream()
        .filter(
            horizon -> horizon.containsDepth(depth) && radial <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  private static String failedGate(
      io.github.crunchybubbles.geological.surface.SurfaceFields fields,
      SurfaceMaterialKind surfaceKind,
      History history) {
    if (history.iceClass() == IceClass.NONE || !history.connected()) {
      return "ice_history";
    }
    if (history.sourceInventoryFixedUnits() <= 0 || history.entrainmentFraction() <= 0.0) {
      return "source_inventory";
    }
    if (history.iceThicknessBlocks() < 8.0 || history.depositionEfficiency() <= 0.0) {
      return "ice_flow";
    }
    if (surfaceKind == SurfaceMaterialKind.ALLUVIAL_PLACER
        || fields.slope() > 0.65
        || fields.outcrop()) {
      return "preservation";
    }
    return null;
  }

  private static GlacialTransportState barren(
      StableId systemId,
      StableId parentBodyId,
      List<StableId> sourceBodyIds,
      History history,
      Point3 localSurface,
      String failedGate) {
    return new GlacialTransportState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        TransportKind.NONE,
        SourceBasis.NO_ICE_HISTORY,
        parentBodyId,
        sourceBodyIds,
        history.iceEventId(),
        new AgeKey(0.0, 0),
        IceClass.NONE,
        new Point2(1.0, 0.0),
        localSurface.x(),
        localSurface.y() - 3.0,
        localSurface.z(),
        48.0,
        36.0,
        6.0,
        0,
        0,
        0,
        0,
        List.of(),
        Optional.of(failedGate));
  }

  private static TransportKind transportKind(IceClass iceClass) {
    return switch (iceClass) {
      case CONTINENTAL_ICE -> TransportKind.CONTINENTAL_TILL;
      case VALLEY_GLACIER -> TransportKind.VALLEY_OUTWASH;
      case NONE -> TransportKind.NONE;
    };
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, IceClass iceClass, CellKey cell) {
    String purpose = "glacial-horizon:" + iceClass.name().toLowerCase(java.util.Locale.ROOT);
    return List.of(
        horizon(HorizonKind.BASAL_TILL, 0.0, 0.45, 0.95, identity, purpose, cell, 0),
        horizon(HorizonKind.MELT_OUTWASH, 0.45, 0.78, 0.84, identity, purpose, cell, 1),
        horizon(HorizonKind.INDICATOR_TRAIN, 0.78, 1.0, 0.72, identity, purpose, cell, 2));
  }

  private static Horizon horizon(
      HorizonKind kind,
      double top,
      double bottom,
      double radius,
      WorldIdentity identity,
      String purpose,
      CellKey cell,
      long index) {
    StableId bodyId = identity.stream("geological", purpose, cell, index).stableId();
    return new Horizon(kind, top, bottom, radius, bodyId);
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("glacial horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("glacial horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum TransportKind {
    CONTINENTAL_TILL,
    VALLEY_OUTWASH,
    NONE
  }

  public enum SourceBasis {
    EXPLICIT_ICE_HISTORY_SOURCE,
    NO_ICE_HISTORY
  }

  public enum IceClass {
    CONTINENTAL_ICE,
    VALLEY_GLACIER,
    NONE
  }

  public enum HorizonKind {
    BASAL_TILL,
    MELT_OUTWASH,
    INDICATOR_TRAIN
  }

  /** Explicit event-local ice and flow descriptor supplied by a climate/ice compiler. */
  public record History(
      StableId iceEventId,
      AgeKey formationAge,
      IceClass iceClass,
      Point2 flowDirection,
      double iceThicknessBlocks,
      double entrainmentFraction,
      double depositionEfficiency,
      long sourceInventoryFixedUnits,
      boolean connected) {
    public History {
      if (iceEventId == null || formationAge == null || iceClass == null || flowDirection == null) {
        throw new IllegalArgumentException("glacial history identity is required");
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
        throw new IllegalArgumentException("glacial history values are invalid");
      }
      double magnitude = StrictMath.hypot(flowDirection.x(), flowDirection.z());
      if (iceClass != IceClass.NONE
          && (!Double.isFinite(magnitude) || magnitude < 0.999 || magnitude > 1.001)) {
        throw new IllegalArgumentException("formed ice history requires unit flow direction");
      }
      if (iceClass == IceClass.NONE
          && (iceThicknessBlocks != 0.0
              || entrainmentFraction != 0.0
              || depositionEfficiency != 0.0
              || sourceInventoryFixedUnits != 0
              || connected)) {
        throw new IllegalArgumentException("empty ice history cannot carry transport evidence");
      }
    }

    public static History none(StableId eventId) {
      return new History(
          eventId,
          new AgeKey(0.0, 0),
          IceClass.NONE,
          new Point2(1.0, 0.0),
          0.0,
          0.0,
          0.0,
          0,
          false);
    }
  }

  public record Horizon(
      HorizonKind kind,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      StableId bodyId) {
    public Horizon {
      if (kind == null || bodyId == null) {
        throw new IllegalArgumentException("glacial horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("glacial horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
