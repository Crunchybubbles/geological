package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.GeothermalSystemState;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;

/** Explicit policy for actual or review-only geothermal heat and reservoir evidence. */
public record GeothermalHostPolicy(String policyId, Mode mode) {
  public GeothermalHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("geothermal host policy identity is required");
    }
  }

  /** Safe default: geothermal formation must be supported by actual resolved material evidence. */
  public static GeothermalHostPolicy none() {
    return new GeothermalHostPolicy("none", Mode.ACTUAL_EVIDENCE_ONLY);
  }

  /** Deterministic positive fixture covering each geothermal reservoir type. */
  public static GeothermalHostPolicy fixture() {
    return new GeothermalHostPolicy(
        "deterministic-geothermal-heat-reservoir-fixture", Mode.GEOTHERMAL_RESERVOIR_FIXTURE);
  }

  HostEvidence resolve(
      Province province,
      WorldIdentity identity,
      Point2 worldPoint,
      SurfacePetrologicSample surface,
      PetrologicSample parent) {
    if (province == null
        || identity == null
        || worldPoint == null
        || surface == null
        || parent == null) {
      throw new IllegalArgumentException("geothermal host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    int unit = geothermalUnit(worldPoint);
    if (mode == Mode.GEOTHERMAL_RESERVOIR_FIXTURE) {
      GeothermalSystemState.GeothermalType type =
          GeothermalSystemState.GeothermalType.values()[unit];
      StableId hostId =
          identity.stream(
                  "geological",
                  "geothermal-reservoir-host:" + policyId,
                  province.homeCell(),
                  type.ordinal())
              .stableId();
      StableId heatSourceId =
          identity.stream(
                  "geological",
                  "geothermal-heat-source:" + policyId,
                  province.homeCell(),
                  type.ordinal())
              .stableId();
      return new HostEvidence(
          hostId,
          heatSourceId,
          localSurface,
          type,
          fixtureLithology(type),
          860_000L - type.ordinal() * 75_000L,
          760_000L - type.ordinal() * 55_000L,
          720_000L - type.ordinal() * 45_000L,
          0.78,
          0.82,
          true);
    }
    var bedrock = surface.surface().bedrock();
    long heat =
        parent
            .magmaThermalState()
            .map(state -> state.thermalPotentialPpm())
            .orElseGet(
                () ->
                    parent.magmaLineage().isPresent()
                        ? 320_000L
                        : element(parent, ChemicalElement.FE) / 4L);
    long fluid =
        parent
            .fluidState()
            .map(state -> 420_000L + state.integratedFluxClass() * 90_000L)
            .orElseGet(
                () ->
                    parent
                        .sedimentaryState()
                        .map(state -> state.reservoirState().waterInventoryPpm())
                        .orElse(0L));
    double permeability = parent.permeabilityIndex();
    double connectivity = parent.fractureTensorState().connectivityPpm() / 1_000_000.0;
    StableId sourceId =
        parent
            .magmaLineage()
            .map(lineage -> lineage.systemId())
            .orElse(province.geometry().basementId());
    return new HostEvidence(
        bedrock.rockBodyId(),
        sourceId,
        localSurface,
        GeothermalSystemState.GeothermalType.NONE,
        bedrock.lithology(),
        Math.min(1_000_000L, heat),
        Math.min(1_000_000L, fluid),
        Math.min(1_000_000L, Math.round(permeability * 1_000_000.0)),
        Math.min(1.0, permeability),
        Math.min(1.0, connectivity),
        false);
  }

  private static int geothermalUnit(Point2 worldPoint) {
    long x = (long) StrictMath.floor(worldPoint.x());
    long z = (long) StrictMath.floor(worldPoint.z());
    return (int) Math.floorMod(Math.floorDiv(x, 16L) + Math.floorDiv(z, 16L), 4L);
  }

  private static Lithology fixtureLithology(GeothermalSystemState.GeothermalType type) {
    return switch (type) {
      case VOLCANIC_HIGH_ENTHALPY -> Lithology.ANDESITIC;
      case FAULT_CONTROLLED -> Lithology.GRANITIC_GNEISS;
      case SEDIMENTARY_AQUIFER -> Lithology.BASIN_SANDSTONE;
      case HOT_DRY_ROCK -> Lithology.GRANULITE;
      case NONE -> throw new IllegalArgumentException("barren geothermal fixture has no type");
    };
  }

  private static long element(PetrologicSample sample, ChemicalElement element) {
    return sample.resolvedComposition().elementMassPpm().getOrDefault(element, 0L);
  }

  public enum Mode {
    ACTUAL_EVIDENCE_ONLY,
    GEOTHERMAL_RESERVOIR_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId,
      StableId heatSourceId,
      Point3 localCenter,
      GeothermalSystemState.GeothermalType fixtureType,
      Lithology hostLithology,
      long heatPotentialFixedUnits,
      long fluidInventoryFixedUnits,
      long permeabilityPotentialFixedUnits,
      double rechargeIndex,
      double connectivityIndex,
      boolean fixture) {
    public HostEvidence {
      if (hostBodyId == null
          || heatSourceId == null
          || localCenter == null
          || fixtureType == null
          || hostLithology == null) {
        throw new IllegalArgumentException("geothermal host evidence identity is required");
      }
      if (heatPotentialFixedUnits < 0L
          || heatPotentialFixedUnits > 1_000_000L
          || fluidInventoryFixedUnits < 0L
          || fluidInventoryFixedUnits > 1_000_000L
          || permeabilityPotentialFixedUnits < 0L
          || permeabilityPotentialFixedUnits > 1_000_000L) {
        throw new IllegalArgumentException("geothermal host potentials are out of bounds");
      }
      if (!Double.isFinite(rechargeIndex)
          || rechargeIndex < 0.0
          || rechargeIndex > 1.0
          || !Double.isFinite(connectivityIndex)
          || connectivityIndex < 0.0
          || connectivityIndex > 1.0) {
        throw new IllegalArgumentException("geothermal pathway indices are out of bounds");
      }
      if (fixture != (fixtureType != GeothermalSystemState.GeothermalType.NONE)) {
        throw new IllegalArgumentException("geothermal fixture type and mode disagree");
      }
    }
  }
}
