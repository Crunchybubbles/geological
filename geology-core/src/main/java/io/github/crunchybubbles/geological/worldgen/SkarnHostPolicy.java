package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;

/** Explicit policy for supplying a reactive carbonate host to the skarn proof. */
public record SkarnHostPolicy(String policyId, Mode mode) {
  public SkarnHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("skarn host policy identity is required");
    }
  }

  /** Safe default: only an actual resolved bedrock carbonate can satisfy the host gate. */
  public static SkarnHostPolicy none() {
    return new SkarnHostPolicy("none", Mode.ACTUAL_BEDROCK_ONLY);
  }

  /** Deterministic positive fixture used only by review tests and the packet generator. */
  public static SkarnHostPolicy fixture() {
    return new SkarnHostPolicy("deterministic-carbonate-contact-fixture", Mode.CARBONATE_FIXTURE);
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
      throw new IllegalArgumentException("skarn host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    if (mode == Mode.CARBONATE_FIXTURE) {
      CellKey cell = new CellKey("province", province.homeCell().x(), province.homeCell().z());
      StableId hostId =
          identity.stream("geological", "skarn-reactive-host:" + policyId, cell, 0).stableId();
      return new HostEvidence(hostId, Lithology.LIMESTONE, localSurface, 700_000L, true, true);
    }

    var bedrock = surface.surface().bedrock();
    Point3 localBedrock = province.frame().toLocal(bedrock.point());
    RiftArcGeometry.PlutonPulse youngest = province.geometry().plutonPulses().getLast();
    double contactDistance = StrictMath.abs(youngest.approximateOutsideDistance(localBedrock));
    long reactiveInventory =
        isReactiveCarbonate(bedrock.lithology()) ? reactiveInventory(parent) : 0L;
    return new HostEvidence(
        bedrock.rockBodyId(),
        bedrock.lithology(),
        localSurface,
        reactiveInventory,
        contactDistance <= 64.0,
        false);
  }

  private static long reactiveInventory(PetrologicSample host) {
    long calcium = host.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.CA, 0L);
    long magnesium =
        host.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.MG, 0L);
    long carbon = host.resolvedComposition().elementMassPpm().getOrDefault(ChemicalElement.C, 0L);
    return Math.min(700_000L, Math.addExact(Math.addExact(calcium, magnesium), carbon));
  }

  private static boolean isReactiveCarbonate(Lithology lithology) {
    return lithology == Lithology.LIMESTONE
        || lithology == Lithology.DOLOSTONE
        || lithology == Lithology.MARBLE
        || lithology == Lithology.CARBONATITIC;
  }

  public enum Mode {
    ACTUAL_BEDROCK_ONLY,
    CARBONATE_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId,
      Lithology hostLithology,
      Point3 localCenter,
      long reactiveInventoryFixedUnits,
      boolean contactPermeability,
      boolean fixture) {
    public HostEvidence {
      if (hostBodyId == null || hostLithology == null || localCenter == null) {
        throw new IllegalArgumentException("skarn host evidence identity is required");
      }
      if (reactiveInventoryFixedUnits < 0L || reactiveInventoryFixedUnits > 1_000_000L) {
        throw new IllegalArgumentException("skarn host inventory is out of bounds");
      }
      if (fixture && hostLithology != Lithology.LIMESTONE) {
        throw new IllegalArgumentException("skarn carbonate fixture must be limestone");
      }
    }
  }
}
