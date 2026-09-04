package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;

/** Explicit policy for supplying an actual or review-only carbonate host to the basin proof. */
public record BasinHydrothermalHostPolicy(String policyId, Mode mode) {
  public BasinHydrothermalHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("basin hydrothermal host policy identity is required");
    }
  }

  /** Safe default: every family must use an actual resolved bedrock host. */
  public static BasinHydrothermalHostPolicy none() {
    return new BasinHydrothermalHostPolicy("none", Mode.ACTUAL_BEDROCK_ONLY);
  }

  /** Deterministic positive fixture for the otherwise ungenerated carbonate MVT branch. */
  public static BasinHydrothermalHostPolicy fixture() {
    return new BasinHydrothermalHostPolicy(
        "deterministic-dolostone-basin-brine-fixture", Mode.CARBONATE_FIXTURE);
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
      throw new IllegalArgumentException("basin hydrothermal host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    if (mode == Mode.CARBONATE_FIXTURE) {
      StableId hostId =
          identity.stream(
                  "geological",
                  "basin-hydrothermal-carbonate-host:" + policyId,
                  province.homeCell(),
                  0)
              .stableId();
      return new HostEvidence(hostId, Lithology.DOLOSTONE, localSurface, true);
    }
    var bedrock = surface.surface().bedrock();
    return new HostEvidence(bedrock.rockBodyId(), bedrock.lithology(), localSurface, false);
  }

  public enum Mode {
    ACTUAL_BEDROCK_ONLY,
    CARBONATE_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId, Lithology hostLithology, Point3 localCenter, boolean fixture) {
    public HostEvidence {
      if (hostBodyId == null || hostLithology == null || localCenter == null) {
        throw new IllegalArgumentException("basin hydrothermal host evidence identity is required");
      }
      if (fixture && hostLithology != Lithology.DOLOSTONE) {
        throw new IllegalArgumentException("basin carbonate fixture must be dolostone");
      }
    }
  }
}
