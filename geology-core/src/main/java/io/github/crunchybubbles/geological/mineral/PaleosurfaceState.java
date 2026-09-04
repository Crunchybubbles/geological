package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfaceMaterialKind;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Bounded regolith and paleosurface refinement derived from the existing terrain/material facade.
 *
 * <p>Present residual regolith and buried unconformity weathering are structural profiles, not new
 * ore inventories. Karst bauxite remains source-gated by a carbonate trap and an external
 * aluminum-bearing detrital source; a carbonate parent by itself is never enough.
 */
public record PaleosurfaceState(
    StableId systemId,
    FormationStatus status,
    RefinementKind refinementKind,
    SourceBasis sourceBasis,
    StableId parentBodyId,
    List<StableId> sourceBodyIds,
    StableId paleosurfaceId,
    StableId weatheringProcessId,
    AgeKey formationAge,
    PreservationClass preservationClass,
    Point3 localCenter,
    double envelopeHalfLengthBlocks,
    double envelopeHalfWidthBlocks,
    double profileThicknessBlocks,
    List<Horizon> horizons,
    Optional<String> failedGate) {
  public PaleosurfaceState {
    if (systemId == null
        || status == null
        || refinementKind == null
        || sourceBasis == null
        || parentBodyId == null
        || sourceBodyIds == null
        || paleosurfaceId == null
        || weatheringProcessId == null
        || formationAge == null
        || preservationClass == null
        || localCenter == null
        || horizons == null
        || failedGate == null) {
      throw new IllegalArgumentException("paleosurface state must be complete");
    }
    requirePositive(envelopeHalfLengthBlocks, "envelopeHalfLengthBlocks");
    requirePositive(envelopeHalfWidthBlocks, "envelopeHalfWidthBlocks");
    requirePositive(profileThicknessBlocks, "profileThicknessBlocks");
    sourceBodyIds = List.copyOf(sourceBodyIds).stream().sorted().toList();
    if (sourceBodyIds.isEmpty()
        || sourceBodyIds.stream().anyMatch(id -> id == null)
        || sourceBodyIds.size() != new HashSet<>(sourceBodyIds).size()) {
      throw new IllegalArgumentException("paleosurface source bodies must be non-empty and unique");
    }
    if (!sourceBodyIds.contains(parentBodyId)) {
      throw new IllegalArgumentException("paleosurface sources must retain the parent body");
    }
    horizons = List.copyOf(horizons);
    if (horizons.stream().anyMatch(horizon -> horizon == null)) {
      throw new IllegalArgumentException("paleosurface horizons cannot be null");
    }
    validateHorizonSequence(horizons);
    if (status == FormationStatus.FORMED) {
      if (sourceBasis == SourceBasis.NO_ELIGIBLE_SOURCE
          || preservationClass == PreservationClass.STRIPPED_OR_COVERED
          || horizons.isEmpty()
          || failedGate.isPresent()) {
        throw new IllegalArgumentException(
            "formed paleosurfaces require a source and preservation proof");
      }
    } else if (sourceBasis != SourceBasis.NO_ELIGIBLE_SOURCE
        || preservationClass != PreservationClass.STRIPPED_OR_COVERED
        || !horizons.isEmpty()
        || failedGate.isEmpty()) {
      throw new IllegalArgumentException("non-formed paleosurfaces must retain a failed gate");
    }
  }

  /** Derives one refinement family from current surface and buried unconformity evidence. */
  public static PaleosurfaceState proofFor(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent,
      RefinementKind refinementKind) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null
        || refinementKind == null) {
      throw new IllegalArgumentException(
          "province, identity, point, surface, parent, and refinement kind are required");
    }
    if (!province.id().equals(surface.surface().bedrock().provinceId())
        || !province.id().equals(parent.geology().provinceId())) {
      throw new IllegalArgumentException(
          "paleosurface parent and surface must belong to the province");
    }
    if (!worldPoint.equals(surface.surface().fields().point())) {
      throw new IllegalArgumentException("paleosurface surface point changed between stages");
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
                "paleosurface:" + refinementKind.name().toLowerCase(java.util.Locale.ROOT),
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
    var unconformity = province.geometry().unconformity();
    Point2 localHorizontal = new Point2(localSurface.x(), localSurface.z());
    boolean insideUnconformity = unconformity.insideFootprint(localHorizontal);
    double ancientSurface = unconformity.elevation(localHorizontal);
    double burialDepth = fields.elevation() - ancientSurface;
    boolean exposedResidual =
        refinementKind == RefinementKind.EXPOSED_RESIDUAL_REGOLITH
            && surface.context().kind() == SurfaceMaterialKind.IN_SITU_REGOLITH
            && !fields.outcrop()
            && !fields.drainage().channel()
            && fields.weatheringDepth() >= 4.0
            && fields.slope() <= 0.22;
    boolean buriedPaleosurface =
        refinementKind == RefinementKind.BURIED_PALEOSURFACE
            && insideUnconformity
            && !fields.outcrop()
            && surface.context().kind() != SurfaceMaterialKind.BEDROCK_OUTCROP
            && burialDepth >= unconformity.weatheringThickness() + 1.0;
    boolean carbonateParent =
        parent.geology().lithology() == Lithology.LIMESTONE
            || parent.geology().lithology() == Lithology.DOLOSTONE;
    boolean externalAluminumSource =
        surface.context().sourceBodyIds().stream()
            .anyMatch(sourceId -> !sourceId.equals(parent.geology().rockBodyId()));
    boolean karstPocket =
        refinementKind == RefinementKind.KARST_BAUXITE_POCKET
            && carbonateParent
            && insideUnconformity
            && externalAluminumSource
            && fields.weatheringDepth() >= 4.0
            && fields.slope() <= 0.24;
    String failedGate =
        switch (refinementKind) {
          case EXPOSED_RESIDUAL_REGOLITH ->
              exposedResidual
                  ? null
                  : surface.context().kind() != SurfaceMaterialKind.IN_SITU_REGOLITH
                      ? "surface_material"
                      : fields.weatheringDepth() < 4.0 ? "weathering" : "preservation";
          case BURIED_PALEOSURFACE ->
              buriedPaleosurface
                  ? null
                  : !insideUnconformity ? "paleosurface" : "buried_preservation";
          case KARST_BAUXITE_POCKET ->
              !carbonateParent
                  ? "carbonate_parent"
                  : !insideUnconformity
                      ? "paleokarst"
                      : !externalAluminumSource ? "aluminum_source" : "preservation";
        };
    if (failedGate != null) {
      return barren(
          systemId,
          refinementKind,
          parent.geology().rockBodyId(),
          sourceBodyIds,
          refinementKind == RefinementKind.EXPOSED_RESIDUAL_REGOLITH
              ? province.proofIds().weatheringId()
              : unconformity.id(),
          province.proofIds().weatheringId(),
          localSurface,
          failedGate);
    }
    double thickness =
        switch (refinementKind) {
          case EXPOSED_RESIDUAL_REGOLITH -> Math.max(4.0, Math.min(12.0, fields.weatheringDepth()));
          case BURIED_PALEOSURFACE -> unconformity.weatheringThickness();
          case KARST_BAUXITE_POCKET -> 18.0;
        };
    Point3 profileSurface =
        refinementKind == RefinementKind.BURIED_PALEOSURFACE
            ? new Point3(localSurface.x(), ancientSurface, localSurface.z())
            : localSurface;
    return new PaleosurfaceState(
        systemId,
        FormationStatus.FORMED,
        refinementKind,
        switch (refinementKind) {
          case EXPOSED_RESIDUAL_REGOLITH -> SourceBasis.PRESENT_PARENT_WEATHERING;
          case BURIED_PALEOSURFACE -> SourceBasis.UNCONFORMITY_WEATHERING_PROFILE;
          case KARST_BAUXITE_POCKET -> SourceBasis.EXTERNAL_ALUMINUM_DETRITUS;
        },
        parent.geology().rockBodyId(),
        sourceBodyIds,
        refinementKind == RefinementKind.EXPOSED_RESIDUAL_REGOLITH
            ? province.proofIds().weatheringId()
            : unconformity.id(),
        province.proofIds().weatheringId(),
        refinementKind == RefinementKind.BURIED_PALEOSURFACE
            ? unconformity.age()
            : new AgeKey(refinementKind == RefinementKind.KARST_BAUXITE_POCKET ? 3.0 : 0.02, 0),
        refinementKind == RefinementKind.BURIED_PALEOSURFACE
            ? PreservationClass.PRESERVED_BURIED
            : PreservationClass.PRESERVED_IN_SITU,
        new Point3(profileSurface.x(), profileSurface.y() - thickness / 2.0, profileSurface.z()),
        refinementKind == RefinementKind.BURIED_PALEOSURFACE ? unconformity.radiusV() : 48.0,
        refinementKind == RefinementKind.BURIED_PALEOSURFACE ? unconformity.radiusU() : 36.0,
        thickness,
        formedHorizons(identity, refinementKind, cell),
        Optional.empty());
  }

  public boolean contains(Point3 localPoint) {
    if (localPoint == null) {
      throw new IllegalArgumentException("local point is required");
    }
    if (status != FormationStatus.FORMED
        || StrictMath.abs(localPoint.y() - localCenter.y()) > profileThicknessBlocks / 2.0) {
      return false;
    }
    double along = (localPoint.z() - localCenter.z()) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / envelopeHalfWidthBlocks;
    return along * along + across * across <= 1.0;
  }

  public Optional<Horizon> zoneAt(Point3 localPoint) {
    if (!contains(localPoint)) {
      return Optional.empty();
    }
    double along = (localPoint.z() - localCenter.z()) / envelopeHalfLengthBlocks;
    double across = (localPoint.x() - localCenter.x()) / envelopeHalfWidthBlocks;
    double radial = StrictMath.sqrt(along * along + across * across);
    double top = localCenter.y() + profileThicknessBlocks / 2.0;
    double depth = (top - localPoint.y()) / profileThicknessBlocks;
    return horizons.stream()
        .filter(
            horizon -> horizon.containsDepth(depth) && radial <= horizon.maximumRadiusFraction())
        .findFirst();
  }

  private static PaleosurfaceState barren(
      StableId systemId,
      RefinementKind refinementKind,
      StableId parentBodyId,
      List<StableId> sourceBodyIds,
      StableId paleosurfaceId,
      StableId weatheringProcessId,
      Point3 localSurface,
      String failedGate) {
    double thickness =
        refinementKind == RefinementKind.KARST_BAUXITE_POCKET
            ? 18.0
            : refinementKind == RefinementKind.BURIED_PALEOSURFACE ? 6.0 : 4.0;
    return new PaleosurfaceState(
        systemId,
        FormationStatus.BARREN_SYSTEM,
        refinementKind,
        SourceBasis.NO_ELIGIBLE_SOURCE,
        parentBodyId,
        sourceBodyIds,
        paleosurfaceId,
        weatheringProcessId,
        new AgeKey(0.0, 0),
        PreservationClass.STRIPPED_OR_COVERED,
        new Point3(localSurface.x(), localSurface.y() - thickness / 2.0, localSurface.z()),
        refinementKind == RefinementKind.BURIED_PALEOSURFACE ? 128.0 : 48.0,
        refinementKind == RefinementKind.BURIED_PALEOSURFACE ? 128.0 : 36.0,
        thickness,
        List.of(),
        Optional.of(failedGate));
  }

  private static List<Horizon> formedHorizons(
      WorldIdentity identity, RefinementKind kind, CellKey cell) {
    return switch (kind) {
      case EXPOSED_RESIDUAL_REGOLITH ->
          List.of(
              horizon(
                  HorizonKind.RESIDUAL_SOIL,
                  Overprint.WEATHERED_REGOLITH,
                  0.0,
                  0.30,
                  0.95,
                  identity,
                  cell,
                  0),
              horizon(
                  HorizonKind.FERRICRETE,
                  Overprint.WEATHERED_REGOLITH,
                  0.30,
                  0.55,
                  0.84,
                  identity,
                  cell,
                  1),
              horizon(
                  HorizonKind.SAPROLITE,
                  Overprint.WEATHERED_REGOLITH,
                  0.55,
                  1.0,
                  0.76,
                  identity,
                  cell,
                  2));
      case BURIED_PALEOSURFACE ->
          List.of(
              horizon(
                  HorizonKind.PALEOSOL,
                  Overprint.WEATHERED_UNCONFORMITY,
                  0.0,
                  0.45,
                  0.92,
                  identity,
                  cell,
                  10),
              horizon(
                  HorizonKind.SAPROLITE,
                  Overprint.WEATHERED_UNCONFORMITY,
                  0.45,
                  1.0,
                  0.78,
                  identity,
                  cell,
                  11));
      case KARST_BAUXITE_POCKET ->
          List.of(
              horizon(
                  HorizonKind.FERRICRETE,
                  Overprint.WEATHERED_REGOLITH,
                  0.0,
                  0.25,
                  0.94,
                  identity,
                  cell,
                  20),
              horizon(
                  HorizonKind.KARST_FILL,
                  Overprint.WEATHERED_REGOLITH,
                  0.25,
                  0.75,
                  0.82,
                  identity,
                  cell,
                  21),
              horizon(
                  HorizonKind.SAPROLITE,
                  Overprint.WEATHERED_REGOLITH,
                  0.75,
                  1.0,
                  0.70,
                  identity,
                  cell,
                  22));
    };
  }

  private static Horizon horizon(
      HorizonKind kind,
      Overprint overprint,
      double top,
      double bottom,
      double radius,
      WorldIdentity identity,
      CellKey cell,
      long index) {
    StableId bodyId = identity.stream("geological", "paleosurface-horizon", cell, index).stableId();
    return new Horizon(kind, overprint, top, bottom, radius, bodyId);
  }

  private static void validateHorizonSequence(List<Horizon> horizons) {
    double previousBottom = 0.0;
    for (Horizon horizon : horizons) {
      if (Math.abs(horizon.topDepthFraction() - previousBottom) > 1.0e-12) {
        throw new IllegalArgumentException("paleosurface horizons must be contiguous");
      }
      previousBottom = horizon.bottomDepthFraction();
    }
    if (!horizons.isEmpty() && Math.abs(previousBottom - 1.0) > 1.0e-12) {
      throw new IllegalArgumentException("paleosurface horizons must cover the profile");
    }
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }

  public enum RefinementKind {
    EXPOSED_RESIDUAL_REGOLITH,
    BURIED_PALEOSURFACE,
    KARST_BAUXITE_POCKET
  }

  public enum SourceBasis {
    PRESENT_PARENT_WEATHERING,
    UNCONFORMITY_WEATHERING_PROFILE,
    EXTERNAL_ALUMINUM_DETRITUS,
    NO_ELIGIBLE_SOURCE
  }

  public enum PreservationClass {
    PRESERVED_IN_SITU,
    PRESERVED_BURIED,
    STRIPPED_OR_COVERED
  }

  public enum HorizonKind {
    RESIDUAL_SOIL,
    FERRICRETE,
    PALEOSOL,
    KARST_FILL,
    SAPROLITE
  }

  public record Horizon(
      HorizonKind kind,
      Overprint overprint,
      double topDepthFraction,
      double bottomDepthFraction,
      double maximumRadiusFraction,
      StableId bodyId) {
    public Horizon {
      if (kind == null || overprint == null || bodyId == null) {
        throw new IllegalArgumentException("paleosurface horizon identity is required");
      }
      if (!Double.isFinite(topDepthFraction)
          || !Double.isFinite(bottomDepthFraction)
          || topDepthFraction < 0.0
          || bottomDepthFraction <= topDepthFraction
          || bottomDepthFraction > 1.0
          || !Double.isFinite(maximumRadiusFraction)
          || maximumRadiusFraction <= 0.0
          || maximumRadiusFraction > 1.0) {
        throw new IllegalArgumentException("paleosurface horizon bounds are invalid");
      }
    }

    private boolean containsDepth(double depthFraction) {
      return depthFraction >= topDepthFraction
          && (depthFraction < bottomDepthFraction
              || depthFraction == bottomDepthFraction && bottomDepthFraction == 1.0);
    }
  }
}
