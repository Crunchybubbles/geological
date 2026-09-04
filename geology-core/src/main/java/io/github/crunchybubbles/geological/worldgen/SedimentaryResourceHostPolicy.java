package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.SedimentaryResourceSystemState;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;

/** Explicit policy for actual or review-only sedimentary-resource host evidence. */
public record SedimentaryResourceHostPolicy(String policyId, Mode mode) {
  public SedimentaryResourceHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("sedimentary resource host policy identity is required");
    }
  }

  /** Safe default: only actual resolved sedimentary hosts can satisfy a resource gate. */
  public static SedimentaryResourceHostPolicy none() {
    return new SedimentaryResourceHostPolicy("none", Mode.ACTUAL_BEDROCK_ONLY);
  }

  /** Deterministic positive fixture covering the sedimentary resource branches. */
  public static SedimentaryResourceHostPolicy fixture() {
    return new SedimentaryResourceHostPolicy(
        "deterministic-sedimentary-resource-facies-fixture", Mode.SEDIMENTARY_RESOURCE_FIXTURE);
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
      throw new IllegalArgumentException("sedimentary resource host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    int unit = resourceUnit(worldPoint);
    if (mode == Mode.SEDIMENTARY_RESOURCE_FIXTURE) {
      SedimentaryResourceSystemState.ResourceFamily family =
          SedimentaryResourceSystemState.ResourceFamily.values()[unit];
      StableId hostId =
          identity.stream(
                  "geological",
                  "sedimentary-resource-host:" + policyId,
                  province.homeCell(),
                  family.ordinal())
              .stableId();
      return new HostEvidence(
          hostId, localSurface, family, fixtureLithology(family), fixtureFacies(family), true);
    }
    var bedrock = surface.surface().bedrock();
    return new HostEvidence(
        bedrock.rockBodyId(),
        localSurface,
        SedimentaryResourceSystemState.ResourceFamily.NONE,
        bedrock.lithology(),
        parent.sedimentaryState().map(state -> state.faciesClass()).orElse("none"),
        false);
  }

  private static int resourceUnit(Point2 worldPoint) {
    long x = (long) StrictMath.floor(worldPoint.x());
    long z = (long) StrictMath.floor(worldPoint.z());
    return (int) Math.floorMod(Math.floorDiv(x, 16L) + Math.floorDiv(z, 16L), 6L);
  }

  private static Lithology fixtureLithology(SedimentaryResourceSystemState.ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE -> Lithology.BASIN_SANDSTONE;
      case SEDIMENTARY_MANGANESE -> Lithology.BASIN_SHALE;
      case COAL -> Lithology.COAL;
      case LITHIUM_BRINE -> Lithology.BASIN_SANDSTONE;
      case POTASH_BORATE_BRINE -> Lithology.HALITE_POTASH_EVAPORITE;
      case HELIUM_GAS -> Lithology.BASIN_SANDSTONE;
      case NONE -> throw new IllegalArgumentException("barren resource fixture has no family");
    };
  }

  private static String fixtureFacies(SedimentaryResourceSystemState.ResourceFamily family) {
    return switch (family) {
      case PHOSPHORITE -> "shallow_marine_shoreface";
      case SEDIMENTARY_MANGANESE -> "offshore_low_energy";
      case COAL -> "buried_peat_mire";
      case LITHIUM_BRINE -> "restricted_evaporite_basin_center";
      case POTASH_BORATE_BRINE -> "restricted_evaporite_basin_center";
      case HELIUM_GAS -> "restricted_evaporite_basin_center";
      case NONE -> "none";
    };
  }

  public enum Mode {
    ACTUAL_BEDROCK_ONLY,
    SEDIMENTARY_RESOURCE_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId,
      Point3 localCenter,
      SedimentaryResourceSystemState.ResourceFamily fixtureFamily,
      Lithology hostLithology,
      String faciesClass,
      boolean fixture) {
    public HostEvidence {
      if (hostBodyId == null
          || localCenter == null
          || fixtureFamily == null
          || hostLithology == null
          || faciesClass == null
          || faciesClass.isBlank()) {
        throw new IllegalArgumentException("sedimentary resource host evidence is incomplete");
      }
      if (fixture != (fixtureFamily != SedimentaryResourceSystemState.ResourceFamily.NONE)) {
        throw new IllegalArgumentException("fixture resource family and mode disagree");
      }
    }
  }
}
