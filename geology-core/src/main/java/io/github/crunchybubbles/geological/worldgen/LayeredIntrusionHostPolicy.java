package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;

/** Explicit policy for supplying actual or review-only layered-mafic host evidence. */
public record LayeredIntrusionHostPolicy(String policyId, Mode mode) {
  public LayeredIntrusionHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("layered intrusion host policy identity is required");
    }
  }

  /** Safe default: only an actual resolved layered mafic/ultramafic host can satisfy the gate. */
  public static LayeredIntrusionHostPolicy none() {
    return new LayeredIntrusionHostPolicy("none", Mode.ACTUAL_BEDROCK_ONLY);
  }

  /** Deterministic positive fixture for the otherwise ungenerated layered-intrusion branches. */
  public static LayeredIntrusionHostPolicy fixture() {
    return new LayeredIntrusionHostPolicy(
        "deterministic-layered-mafic-ultramafic-chamber-fixture", Mode.LAYERED_CHAMBER_FIXTURE);
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
      throw new IllegalArgumentException("layered intrusion host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    int cyclicUnit = cyclicUnit(worldPoint);
    if (mode == Mode.LAYERED_CHAMBER_FIXTURE) {
      StableId hostId =
          identity.stream(
                  "geological", "layered-intrusion-host:" + policyId, province.homeCell(), 0)
              .stableId();
      StableId intrusionId =
          identity.stream(
                  "geological", "layered-intrusion-body:" + policyId, province.homeCell(), 0)
              .stableId();
      StableId structureId =
          identity.stream(
                  "geological", "layered-intrusion-structure:" + policyId, province.homeCell(), 0)
              .stableId();
      return new HostEvidence(
          hostId,
          intrusionId,
          structureId,
          province.proofIds().magmaLineageId(),
          cyclicUnit == 2 ? Lithology.GABBROIC : Lithology.KOMATIITIC_ULTRAMAFIC,
          localSurface,
          cyclicUnit,
          true,
          true,
          760_000L - cyclicUnit * 35_000L,
          650_000L - cyclicUnit * 22_000L,
          520_000L - cyclicUnit * 18_000L,
          0.82,
          0.86);
    }

    var bedrock = surface.surface().bedrock();
    boolean maficHost = isMaficOrUltramafic(bedrock.lithology());
    boolean actualLayeredChamber =
        maficHost
            && parent.magmaLineage().isPresent()
            && (bedrock.lithology() == Lithology.KOMATIITIC_ULTRAMAFIC
                || bedrock.lithology() == Lithology.GABBROIC);
    long chromium = element(parent, ChemicalElement.CR);
    long sulfur = element(parent, ChemicalElement.S);
    long copper = element(parent, ChemicalElement.CU);
    long sourceBudget =
        Math.min(
            300_000L,
            chromium / 2L + sulfur / 3L + copper / 2L + (actualLayeredChamber ? 80_000L : 0L));
    return new HostEvidence(
        bedrock.rockBodyId(),
        bedrock.rockBodyId(),
        province.geometry().fault().id(),
        province.proofIds().magmaLineageId(),
        bedrock.lithology(),
        localSurface,
        cyclicUnit,
        false,
        actualLayeredChamber,
        sourceBudget,
        sulfur,
        chromium,
        parent.permeabilityIndex(),
        parent.fractureTensorState().connectivityPpm() / 1_000_000.0);
  }

  private static int cyclicUnit(Point2 worldPoint) {
    long x = (long) StrictMath.floor(worldPoint.x());
    long z = (long) StrictMath.floor(worldPoint.z());
    return (int) Math.floorMod(Math.floorDiv(x, 8L) + Math.floorDiv(z, 8L), 3L);
  }

  private static boolean isMaficOrUltramafic(Lithology lithology) {
    return lithology == Lithology.KOMATIITIC_ULTRAMAFIC
        || lithology == Lithology.GABBROIC
        || lithology == Lithology.BASALTIC;
  }

  private static long element(PetrologicSample sample, ChemicalElement element) {
    return sample.resolvedComposition().elementMassPpm().getOrDefault(element, 0L);
  }

  public enum Mode {
    ACTUAL_BEDROCK_ONLY,
    LAYERED_CHAMBER_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId,
      StableId intrusionId,
      StableId structureId,
      StableId magmaSourceId,
      Lithology hostLithology,
      Point3 localCenter,
      int cyclicUnit,
      boolean fixture,
      boolean layeredChamber,
      long sourceBudgetFixedUnits,
      long sulfurInventoryFixedUnits,
      long chromiumInventoryFixedUnits,
      double permeabilityIndex,
      double connectivityIndex) {
    public HostEvidence {
      if (hostBodyId == null
          || intrusionId == null
          || structureId == null
          || magmaSourceId == null
          || hostLithology == null
          || localCenter == null
          || cyclicUnit < 0
          || cyclicUnit > 2) {
        throw new IllegalArgumentException("layered intrusion host evidence identity is required");
      }
      if (sourceBudgetFixedUnits < 0L
          || sourceBudgetFixedUnits > 1_000_000L
          || sulfurInventoryFixedUnits < 0L
          || sulfurInventoryFixedUnits > 1_000_000L
          || chromiumInventoryFixedUnits < 0L
          || chromiumInventoryFixedUnits > 1_000_000L) {
        throw new IllegalArgumentException(
            "layered intrusion source inventories are out of bounds");
      }
      if (!Double.isFinite(permeabilityIndex)
          || permeabilityIndex < 0.0
          || permeabilityIndex > 1.0
          || !Double.isFinite(connectivityIndex)
          || connectivityIndex < 0.0
          || connectivityIndex > 1.0) {
        throw new IllegalArgumentException("layered intrusion pathway indices are out of bounds");
      }
      if (fixture && !layeredChamber) {
        throw new IllegalArgumentException("layered intrusion fixture must be a chamber");
      }
    }
  }
}
