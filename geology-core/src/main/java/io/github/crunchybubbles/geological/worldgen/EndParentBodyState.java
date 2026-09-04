package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Point3;
import java.util.HashSet;
import java.util.List;

/**
 * Stable provenance and bounded composition state for one fictional End parent fragment.
 *
 * <p>The End uses parent-body and void-relative terms rather than an Overworld crust model.
 */
public record EndParentBodyState(
    StableId parentBodyId,
    StableId fragmentId,
    StableId compositionReservoirId,
    StableId metalReservoirId,
    StableId impactorId,
    StableId regolithBodyId,
    List<StableId> sourceBodyIds,
    FragmentRole role,
    ParentFamily parentFamily,
    DifferentiationClass differentiation,
    ImpactClass impactClass,
    Point3 center,
    double horizontalRadiusBlocks,
    double verticalRadiusBlocks,
    double impactCenterX,
    double impactCenterZ,
    double impactRadiusBlocks,
    int impactDepthBlocks,
    int impactMeltThicknessBlocks,
    long parentMaterialBudgetFixedUnits,
    long retainedParentMaterialFixedUnits,
    long impactLossFixedUnits,
    long regolithBudgetFixedUnits,
    long retainedRegolithFixedUnits,
    long voidExposureLossFixedUnits,
    List<Event> events,
    long cellX,
    long cellZ) {
  public EndParentBodyState {
    if (parentBodyId == null
        || fragmentId == null
        || compositionReservoirId == null
        || metalReservoirId == null
        || impactorId == null
        || regolithBodyId == null
        || sourceBodyIds == null
        || role == null
        || parentFamily == null
        || differentiation == null
        || impactClass == null
        || center == null
        || events == null) {
      throw new IllegalArgumentException("End parent-body identities are required");
    }
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()
        || !sourceBodyIds.contains(parentBodyId)
        || !sourceBodyIds.contains(fragmentId)
        || !sourceBodyIds.contains(compositionReservoirId)
        || !sourceBodyIds.contains(metalReservoirId)
        || !sourceBodyIds.contains(impactorId)
        || !sourceBodyIds.contains(regolithBodyId)) {
      throw new IllegalArgumentException("End parent provenance must retain every source identity");
    }
    requirePositive(horizontalRadiusBlocks, "horizontalRadiusBlocks");
    requirePositive(verticalRadiusBlocks, "verticalRadiusBlocks");
    if (!Double.isFinite(impactCenterX)
        || !Double.isFinite(impactCenterZ)
        || !Double.isFinite(impactRadiusBlocks)
        || impactRadiusBlocks < 0.0
        || impactDepthBlocks < 0
        || impactMeltThicknessBlocks < 0
        || impactClass == ImpactClass.NONE
            && (impactRadiusBlocks != 0.0
                || impactDepthBlocks != 0
                || impactMeltThicknessBlocks != 0)) {
      throw new IllegalArgumentException("End impact geometry is invalid");
    }
    if (impactClass != ImpactClass.NONE && impactRadiusBlocks <= 0.0) {
      throw new IllegalArgumentException("End impact classes need a bounded impact radius");
    }
    if (parentMaterialBudgetFixedUnits < 0L
        || retainedParentMaterialFixedUnits < 0L
        || impactLossFixedUnits < 0L
        || retainedParentMaterialFixedUnits + impactLossFixedUnits != parentMaterialBudgetFixedUnits
        || regolithBudgetFixedUnits < 0L
        || retainedRegolithFixedUnits < 0L
        || voidExposureLossFixedUnits < 0L
        || retainedRegolithFixedUnits + voidExposureLossFixedUnits != regolithBudgetFixedUnits) {
      throw new IllegalArgumentException("End parent and regolith ledgers are not closed");
    }
    events = List.copyOf(events);
    if (events.isEmpty() || events.stream().anyMatch(event -> event == null)) {
      throw new IllegalArgumentException("End parent body must contain an ordered event chronicle");
    }
    HashSet<StableId> eventIds = new HashSet<>();
    int expectedSequence = 0;
    for (Event event : events) {
      if (event.sequence() != expectedSequence++ || !eventIds.add(event.eventId())) {
        throw new IllegalArgumentException("End parent events must be ordered and unique");
      }
    }
  }

  /** Builds one parent body before fragmentation, impact, and void-regolith projection. */
  public static EndParentBodyState from(long cellX, long cellZ, WorldIdentity identity) {
    if (identity == null) {
      throw new IllegalArgumentException("End parent identity is required");
    }
    requireEndIdentity(identity);
    CellKey cell = new CellKey("end:parent", cellX, cellZ);
    var stream = identity.stream("geological", "end-parent-body", cell, 0);
    FragmentRole role = role(cellX, cellZ);
    ParentFamily parentFamily =
        ParentFamily.values()[stream.boundedInt("parent-family", 0, ParentFamily.values().length)];
    DifferentiationClass differentiation = differentiation(parentFamily);
    StableId parentBodyId = stream.stableId();
    StableId fragmentId = identity.stream("geological", "end-fragment", cell, 0).stableId();
    StableId compositionReservoirId =
        identity.stream("geological", "end-composition-reservoir", cell, 0).stableId();
    StableId metalReservoirId =
        identity.stream("geological", "end-metal-reservoir", cell, 0).stableId();
    StableId impactorId = identity.stream("geological", "end-impactor", cell, 0).stableId();
    StableId regolithBodyId =
        identity.stream("geological", "end-regolith-body", cell, 0).stableId();
    List<StableId> sourceIds =
        List.of(
            parentBodyId,
            fragmentId,
            compositionReservoirId,
            metalReservoirId,
            impactorId,
            regolithBodyId);
    double centerX = centerX(cellX, cellZ, role, stream);
    double centerZ = centerZ(cellX, cellZ, role, stream);
    double horizontalRadius = horizontalRadius(role, stream);
    double verticalRadius = verticalRadius(role, stream);
    double centerY = centerY(role, stream);
    ImpactClass impactClass = impactClass(role, stream);
    double impactRadius =
        impactClass == ImpactClass.NONE
            ? 0.0
            : Math.max(
                20.0, horizontalRadius * (0.24 + stream.unitDouble("impact-radius", 0) * 0.22));
    int impactDepth =
        impactClass == ImpactClass.NONE
            ? 0
            : 8 + (int) Math.round(stream.unitDouble("impact-depth", 0) * 22.0);
    int meltThickness =
        impactClass == ImpactClass.NONE
            ? 0
            : 2 + (int) Math.round(stream.unitDouble("impact-melt", 0) * 5.0);
    double impactCenterX =
        centerX + (stream.symmetricDouble("impact-offset-x", 0) * impactRadius * 0.35);
    double impactCenterZ =
        centerZ + (stream.symmetricDouble("impact-offset-z", 0) * impactRadius * 0.35);
    EventKind[] chronicle = chronicle(impactClass, differentiation);
    List<Event> events =
        java.util.stream.IntStream.range(0, chronicle.length)
            .mapToObj(
                sequence ->
                    new Event(
                        identity.stream("geological", "end-parent-event", cell, sequence)
                            .stableId(),
                        chronicle[sequence],
                        sequence,
                        32_000L
                            + Math.round(
                                stream.unitDouble("event-" + sequence, sequence) * 84_000L)))
            .toList();
    long parentBudget =
        540_000L + Math.round(stream.unitDouble("parent-material-budget", 0) * 360_000L);
    long impactLoss = Math.round(parentBudget * (impactClass == ImpactClass.NONE ? 0.04 : 0.12));
    long retainedParent = parentBudget - impactLoss;
    long regolithBudget = 72_000L + Math.round(stream.unitDouble("regolith-budget", 0) * 98_000L);
    long regolithLoss = Math.round(regolithBudget * 0.22);
    long retainedRegolith = regolithBudget - regolithLoss;
    return new EndParentBodyState(
        parentBodyId,
        fragmentId,
        compositionReservoirId,
        metalReservoirId,
        impactorId,
        regolithBodyId,
        sourceIds,
        role,
        parentFamily,
        differentiation,
        impactClass,
        new Point3(centerX, centerY, centerZ),
        horizontalRadius,
        verticalRadius,
        impactCenterX,
        impactCenterZ,
        impactRadius,
        impactDepth,
        meltThickness,
        parentBudget,
        retainedParent,
        impactLoss,
        regolithBudget,
        retainedRegolith,
        regolithLoss,
        events,
        cellX,
        cellZ);
  }

  public boolean containsHorizontal(long blockX, long blockZ) {
    double dx = (blockX + 0.5 - center.x()) / horizontalRadiusBlocks;
    double dz = (blockZ + 0.5 - center.z()) / horizontalRadiusBlocks;
    return dx * dx + dz * dz <= 1.0;
  }

  /** Returns the crater excavation depth at a column, using zero outside the impact footprint. */
  public int impactDepthAt(long blockX, long blockZ) {
    if (impactClass == ImpactClass.NONE) {
      return 0;
    }
    double dx = (blockX + 0.5 - impactCenterX) / impactRadiusBlocks;
    double dz = (blockZ + 0.5 - impactCenterZ) / impactRadiusBlocks;
    double radialSquared = dx * dx + dz * dz;
    if (radialSquared >= 1.0) {
      return 0;
    }
    return (int) Math.round(impactDepthBlocks * (1.0 - radialSquared));
  }

  public boolean hasEvent(EventKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("event kind is required");
    }
    return events.stream().anyMatch(event -> event.kind() == kind);
  }

  private static FragmentRole role(long cellX, long cellZ) {
    if (cellX == 0L && cellZ == 0L) {
      return FragmentRole.CENTRAL_PROGRESSION;
    }
    long distance = Math.max(Math.abs(cellX), Math.abs(cellZ));
    if (distance <= 2L) {
      return FragmentRole.GATEWAY_RING;
    }
    return FragmentRole.OUTER_ISLAND;
  }

  private static DifferentiationClass differentiation(ParentFamily parentFamily) {
    return switch (parentFamily) {
      case PRIMITIVE -> DifferentiationClass.PRIMITIVE_UNDIFFERENTIATED;
      case SILICATE_DIFFERENTIATED -> DifferentiationClass.SILICATE_DIFFERENTIATED;
      case METAL_SEPARATED -> DifferentiationClass.METAL_SEPARATED;
      case PREVIOUSLY_MELTED -> DifferentiationClass.PREVIOUSLY_MELTED;
    };
  }

  private static ImpactClass impactClass(
      FragmentRole role, io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    if (role == FragmentRole.CENTRAL_PROGRESSION) {
      return ImpactClass.NONE;
    }
    int index = stream.boundedInt("impact-class", 0, 3);
    return switch (index) {
      case 0 -> ImpactClass.SHOCKED_BRECCIA;
      case 1 -> ImpactClass.IMPACT_MELT_SHEET;
      default -> ImpactClass.POLYMICT_BRECCIA;
    };
  }

  private static EventKind[] chronicle(
      ImpactClass impactClass, DifferentiationClass differentiation) {
    EventKind impactEvent =
        switch (impactClass) {
          case NONE -> EventKind.VOID_EXPOSURE;
          case SHOCKED_BRECCIA -> EventKind.SHOCK_RESPONSE;
          case IMPACT_MELT_SHEET -> EventKind.IMPACT_MELT;
          case POLYMICT_BRECCIA -> EventKind.POLYMICT_BRECCIA;
        };
    EventKind differentiationEvent =
        switch (differentiation) {
          case PRIMITIVE_UNDIFFERENTIATED -> EventKind.PRIMITIVE_ACCUMULATION;
          case SILICATE_DIFFERENTIATED -> EventKind.SILICATE_DIFFERENTIATION;
          case METAL_SEPARATED -> EventKind.METAL_SEPARATION;
          case PREVIOUSLY_MELTED -> EventKind.REMELTING;
        };
    return new EventKind[] {
      EventKind.PARENT_ACCUMULATION,
      differentiationEvent,
      EventKind.FRAGMENTATION,
      impactEvent,
      EventKind.FRACTURE_REACCUMULATION,
      EventKind.VOID_EXPOSURE,
      EventKind.REGOLITH_REACCUMULATION
    };
  }

  private static double centerX(
      long cellX,
      long cellZ,
      FragmentRole role,
      io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    if (role == FragmentRole.CENTRAL_PROGRESSION) {
      return 0.0;
    }
    return cellX * 1024.0 + 256.0 + stream.unitDouble("center-x", 0) * 512.0;
  }

  private static double centerZ(
      long cellX,
      long cellZ,
      FragmentRole role,
      io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    if (role == FragmentRole.CENTRAL_PROGRESSION) {
      return 0.0;
    }
    return cellZ * 1024.0 + 256.0 + stream.unitDouble("center-z", 0) * 512.0;
  }

  private static double centerY(
      FragmentRole role, io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    return switch (role) {
      case CENTRAL_PROGRESSION -> 96.0;
      case GATEWAY_RING -> 76.0 + stream.symmetricDouble("center-y", 0) * 12.0;
      case OUTER_ISLAND -> 92.0 + stream.symmetricDouble("center-y", 0) * 34.0;
    };
  }

  private static double horizontalRadius(
      FragmentRole role, io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    return switch (role) {
      case CENTRAL_PROGRESSION -> 208.0;
      case GATEWAY_RING -> 116.0 + stream.unitDouble("horizontal-radius", 0) * 72.0;
      case OUTER_ISLAND -> 92.0 + stream.unitDouble("horizontal-radius", 0) * 148.0;
    };
  }

  private static double verticalRadius(
      FragmentRole role, io.github.crunchybubbles.geological.determinism.RandomStream stream) {
    return switch (role) {
      case CENTRAL_PROGRESSION -> 92.0;
      case GATEWAY_RING -> 54.0 + stream.unitDouble("vertical-radius", 0) * 28.0;
      case OUTER_ISLAND -> 46.0 + stream.unitDouble("vertical-radius", 0) * 54.0;
    };
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  private static void requireEndIdentity(WorldIdentity identity) {
    DimensionGeologyProfile profile = DimensionGeologyProfiles.require("minecraft:the_end");
    if (!profile.profileId().equals(identity.dimensionProfileId())
        || !profile.version().equals(identity.modelVersion())
        || !profile.scientificDigest().equals(identity.scientificDigest())) {
      throw new IllegalArgumentException("End parent identity does not match profile");
    }
  }

  public enum FragmentRole {
    CENTRAL_PROGRESSION,
    GATEWAY_RING,
    OUTER_ISLAND
  }

  public enum ParentFamily {
    PRIMITIVE,
    SILICATE_DIFFERENTIATED,
    METAL_SEPARATED,
    PREVIOUSLY_MELTED
  }

  public enum DifferentiationClass {
    PRIMITIVE_UNDIFFERENTIATED,
    SILICATE_DIFFERENTIATED,
    METAL_SEPARATED,
    PREVIOUSLY_MELTED
  }

  public enum ImpactClass {
    NONE,
    SHOCKED_BRECCIA,
    IMPACT_MELT_SHEET,
    POLYMICT_BRECCIA
  }

  public enum EventKind {
    PARENT_ACCUMULATION,
    PRIMITIVE_ACCUMULATION,
    SILICATE_DIFFERENTIATION,
    METAL_SEPARATION,
    REMELTING,
    FRAGMENTATION,
    SHOCK_RESPONSE,
    IMPACT_MELT,
    POLYMICT_BRECCIA,
    FRACTURE_REACCUMULATION,
    VOID_EXPOSURE,
    REGOLITH_REACCUMULATION
  }

  public record Event(StableId eventId, EventKind kind, int sequence, long contributionFixedUnits) {
    public Event {
      if (eventId == null || kind == null || sequence < 0 || contributionFixedUnits <= 0L) {
        throw new IllegalArgumentException("End parent event is invalid");
      }
    }
  }
}
