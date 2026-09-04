package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.mineral.CarbonatiteKimberliteSystemState;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.MantleCargoState;
import io.github.crunchybubbles.geological.petrology.MantleCargoStatus;
import io.github.crunchybubbles.geological.petrology.PetrologicSample;
import io.github.crunchybubbles.geological.petrology.SurfacePetrologicSample;
import java.util.List;
import java.util.Optional;

/** Explicit policy for actual or review-only alkaline-complex and kimberlite evidence. */
public record CarbonatiteKimberliteHostPolicy(String policyId, Mode mode) {
  public CarbonatiteKimberliteHostPolicy {
    if (policyId == null || policyId.isBlank() || mode == null) {
      throw new IllegalArgumentException("carbonatite/kimberlite host policy identity is required");
    }
  }

  /** Safe default: only actual alkaline, carbonatite, or kimberlite bedrock can satisfy a gate. */
  public static CarbonatiteKimberliteHostPolicy none() {
    return new CarbonatiteKimberliteHostPolicy("none", Mode.ACTUAL_BEDROCK_ONLY);
  }

  /** Deterministic positive fixture for the otherwise ungenerated complex and pipe branches. */
  public static CarbonatiteKimberliteHostPolicy fixture() {
    return new CarbonatiteKimberliteHostPolicy(
        "deterministic-carbonatite-peralkaline-kimberlite-fixture", Mode.ALKALINE_COMPLEX_FIXTURE);
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
      throw new IllegalArgumentException("carbonatite/kimberlite host policy inputs are required");
    }
    Point3 localSurface =
        province
            .frame()
            .toLocal(
                new Point3(worldPoint.x(), surface.surface().fields().elevation(), worldPoint.z()));
    int complexUnit = complexUnit(worldPoint);
    if (mode == Mode.ALKALINE_COMPLEX_FIXTURE) {
      StableId hostId =
          identity.stream(
                  "geological", "carbonatite-kimberlite-host:" + policyId, province.homeCell(), 0)
              .stableId();
      StableId intrusionId =
          identity.stream(
                  "geological", "carbonatite-kimberlite-body:" + policyId, province.homeCell(), 0)
              .stableId();
      StableId structureId =
          identity.stream(
                  "geological",
                  "carbonatite-kimberlite-structure:" + policyId,
                  province.homeCell(),
                  0)
              .stableId();
      CarbonatiteKimberliteSystemState.DepositFamily family =
          switch (complexUnit) {
            case 0 -> CarbonatiteKimberliteSystemState.DepositFamily.CARBONATITE_REE;
            case 1 -> CarbonatiteKimberliteSystemState.DepositFamily.PERALKALINE_REE;
            case 2 -> CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND;
            default -> throw new IllegalArgumentException("unsupported alkaline complex unit");
          };
      Lithology hostLithology =
          family == CarbonatiteKimberliteSystemState.DepositFamily.CARBONATITE_REE
              ? Lithology.CARBONATITIC
              : family == CarbonatiteKimberliteSystemState.DepositFamily.PERALKALINE_REE
                  ? Lithology.ALKALINE
                  : Lithology.KIMBERLITIC;
      Optional<MantleCargoState> cargo =
          family == CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND
              ? Optional.of(
                  new MantleCargoState(
                      hostId,
                      Optional.of(province.geometry().basementId()),
                      MantleCargoStatus.DIAMOND_BEARING,
                      "geological:mineral/diamond",
                      120_000L,
                      List.of(
                          "geological:mineral/chromite",
                          "geological:mineral/diopside",
                          "geological:mineral/ilmenite",
                          "geological:mineral/pyrope")))
              : Optional.empty();
      return new HostEvidence(
          hostId,
          intrusionId,
          structureId,
          province.proofIds().magmaLineageId(),
          family,
          hostLithology,
          localSurface,
          true,
          true,
          cargo,
          780_000L - complexUnit * 45_000L,
          620_000L - complexUnit * 40_000L,
          0.82,
          0.88);
    }

    var bedrock = surface.surface().bedrock();
    CarbonatiteKimberliteSystemState.DepositFamily family =
        switch (bedrock.lithology()) {
          case CARBONATITIC -> CarbonatiteKimberliteSystemState.DepositFamily.CARBONATITE_REE;
          case ALKALINE -> CarbonatiteKimberliteSystemState.DepositFamily.PERALKALINE_REE;
          case KIMBERLITIC -> CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND;
          default -> CarbonatiteKimberliteSystemState.DepositFamily.NONE;
        };
    long calcium = element(parent, ChemicalElement.CA);
    long phosphorus = element(parent, ChemicalElement.P);
    long iron = element(parent, ChemicalElement.FE);
    long sourceBudget = Math.min(300_000L, calcium / 3L + phosphorus / 2L + iron / 4L);
    return new HostEvidence(
        bedrock.rockBodyId(),
        bedrock.rockBodyId(),
        province.geometry().fault().id(),
        province.proofIds().magmaLineageId(),
        family,
        bedrock.lithology(),
        localSurface,
        false,
        family != CarbonatiteKimberliteSystemState.DepositFamily.NONE,
        parent.mantleCargo(),
        sourceBudget,
        phosphorus,
        parent.permeabilityIndex(),
        parent.fractureTensorState().connectivityPpm() / 1_000_000.0);
  }

  private static int complexUnit(Point2 worldPoint) {
    long x = (long) StrictMath.floor(worldPoint.x());
    long z = (long) StrictMath.floor(worldPoint.z());
    return (int) Math.floorMod(Math.floorDiv(x, 8L) + Math.floorDiv(z, 8L), 3L);
  }

  private static long element(PetrologicSample sample, ChemicalElement element) {
    return sample.resolvedComposition().elementMassPpm().getOrDefault(element, 0L);
  }

  public enum Mode {
    ACTUAL_BEDROCK_ONLY,
    ALKALINE_COMPLEX_FIXTURE
  }

  public record HostEvidence(
      StableId hostBodyId,
      StableId intrusionId,
      StableId structureId,
      StableId sourceBodyId,
      CarbonatiteKimberliteSystemState.DepositFamily family,
      Lithology hostLithology,
      Point3 localCenter,
      boolean fixture,
      boolean complexOrPipe,
      Optional<MantleCargoState> mantleCargo,
      long sourceBudgetFixedUnits,
      long incompatibleInventoryFixedUnits,
      double permeabilityIndex,
      double connectivityIndex) {
    public HostEvidence {
      if (hostBodyId == null
          || intrusionId == null
          || structureId == null
          || sourceBodyId == null
          || family == null
          || hostLithology == null
          || localCenter == null
          || mantleCargo == null) {
        throw new IllegalArgumentException("alkaline complex host evidence identity is required");
      }
      if (sourceBudgetFixedUnits < 0L
          || sourceBudgetFixedUnits > 1_000_000L
          || incompatibleInventoryFixedUnits < 0L
          || incompatibleInventoryFixedUnits > 1_000_000L) {
        throw new IllegalArgumentException("alkaline complex source inventories are out of bounds");
      }
      if (!Double.isFinite(permeabilityIndex)
          || permeabilityIndex < 0.0
          || permeabilityIndex > 1.0
          || !Double.isFinite(connectivityIndex)
          || connectivityIndex < 0.0
          || connectivityIndex > 1.0) {
        throw new IllegalArgumentException("alkaline complex pathway indices are out of bounds");
      }
      if (mantleCargo.isPresent()
          && !mantleCargo.orElseThrow().carrierBodyId().equals(hostBodyId)) {
        throw new IllegalArgumentException("mantle cargo carrier must match the host body");
      }
      if (family == CarbonatiteKimberliteSystemState.DepositFamily.KIMBERLITE_DIAMOND
          && mantleCargo.isEmpty()
          && complexOrPipe) {
        // Actual unresolved kimberlite is allowed to remain barren; proof state applies the gate.
      }
      if (fixture && !complexOrPipe) {
        throw new IllegalArgumentException("alkaline complex fixture must be a complex or pipe");
      }
    }
  }
}
