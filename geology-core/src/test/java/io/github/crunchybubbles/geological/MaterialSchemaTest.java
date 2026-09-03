package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.petrology.AcidityClass;
import io.github.crunchybubbles.geological.petrology.AlterationAssemblageRecipe;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.BodyCompositionSampler;
import io.github.crunchybubbles.geological.petrology.ClastShape;
import io.github.crunchybubbles.geological.petrology.ColluvialHorizonState;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialSedimentBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialTextureState;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcess;
import io.github.crunchybubbles.geological.petrology.FluidMedium;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.LigandCapacities;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MetamorphicGrade;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
import io.github.crunchybubbles.geological.petrology.ModalVariationAxis;
import io.github.crunchybubbles.geological.petrology.PrimaryMetamorphicDefinition;
import io.github.crunchybubbles.geological.petrology.ProcessFluidState;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import io.github.crunchybubbles.geological.petrology.RockDefinition;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentGrainSize;
import io.github.crunchybubbles.geological.petrology.SedimentSorting;
import io.github.crunchybubbles.geological.petrology.SedimentSupport;
import io.github.crunchybubbles.geological.petrology.SulfurState;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialSchemaTest {
  @Test
  void sedimentGrainSizeBlendsCloseExactlyAndIgnoreShareOrder() {
    SedimentGrainSize coarse = new SedimentGrainSize(600_000L, 300_000L, 100_000L);
    SedimentGrainSize matrix = new SedimentGrainSize(100_000L, 400_000L, 500_000L);
    SedimentGrainSize expected = new SedimentGrainSize(275_000L, 365_000L, 360_000L);

    List<SedimentGrainSize.Share> shares =
        List.of(
            new SedimentGrainSize.Share(coarse, 350_000L),
            new SedimentGrainSize.Share(matrix, 650_000L));
    assertEquals(expected, SedimentGrainSize.weightedBlend(shares));
    assertEquals(expected, SedimentGrainSize.weightedBlend(shares.reversed()));
    assertThrows(
        IllegalArgumentException.class, () -> new SedimentGrainSize(600_000L, 300_000L, 99_999L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SedimentGrainSize.weightedBlend(
                List.of(new SedimentGrainSize.Share(coarse, 999_999L))));
  }

  @Test
  void colluvialTextureClassifiesSupportAndCoarseSorting() {
    ColluvialTextureState matrix =
        ColluvialTextureState.from(new SedimentGrainSize(250_000L, 350_000L, 400_000L));
    ColluvialTextureState mixed =
        ColluvialTextureState.from(new SedimentGrainSize(400_000L, 350_000L, 250_000L));
    ColluvialTextureState clast =
        ColluvialTextureState.from(new SedimentGrainSize(500_000L, 350_000L, 150_000L));

    assertEquals(SedimentSupport.MATRIX_SUPPORTED, matrix.support());
    assertEquals(SedimentSupport.MIXED_SUPPORT, mixed.support());
    assertEquals(SedimentSupport.CLAST_SUPPORTED, clast.support());
    assertEquals(SedimentSorting.UNSORTED_TO_POORLY_SORTED, mixed.sorting());
    assertEquals(SedimentSorting.UNSORTED_TO_POORLY_SORTED, clast.sorting());
    assertEquals(0.1, mixed.sortingDominanceIndex(), 1.0e-15);
    assertEquals(0.25, clast.sortingDominanceIndex(), 1.0e-15);
    assertEquals(ClastShape.ANGULAR_TO_SUBROUNDED, mixed.clastShape());
  }

  @Test
  void colluvialTransportProcessSelectionUsesSlopeRunoffAndRouteEvidence() {
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath path = terrainPath(100.0);
    ColluvialSedimentBudget.ProductionInput creepInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.02, 0.8, 0.25, 0.0, path, grainYield);
    ColluvialSedimentBudget.ProductionInput sheetwashInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.12, 0.8, 0.25, 1.0, path, grainYield);
    ColluvialSedimentBudget.ProductionInput dryRavelInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.24, 0.8, 0.25, 0.0, path, grainYield);
    ColluvialTransportProcess creep = ColluvialTransportProcess.from(creepInput);
    ColluvialTransportProcess sheetwash = ColluvialTransportProcess.from(sheetwashInput);
    ColluvialTransportProcess dryRavel = ColluvialTransportProcess.from(dryRavelInput);

    assertEquals(ColluvialTransportProcess.ProcessClass.HILLSLOPE_CREEP, creep.processClass());
    assertEquals(ColluvialTransportProcess.ProcessClass.SHEETWASH, sheetwash.processClass());
    assertEquals(ColluvialTransportProcess.ProcessClass.DRY_RAVEL, dryRavel.processClass());
    assertEquals(creep, ColluvialTransportProcess.from(creepInput));
    assertEquals(sheetwash, ColluvialTransportProcess.from(sheetwashInput));
    assertEquals(dryRavel, ColluvialTransportProcess.from(dryRavelInput));
    for (ColluvialTransportProcess process : List.of(creep, sheetwash, dryRavel)) {
      assertTrue(process.selectedScore() >= 0.0);
      assertTrue(process.selectedScore() <= 1.0);
      assertTrue(process.selectionMargin() >= 0.0);
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportProcess(
                ColluvialTransportProcess.ProcessClass.DRY_RAVEL, 1.0, 0.0, 0.0));
  }

  @Test
  void colluvialPhysicalStateUsesTextureToSelectValuesInsideAuthoredEnvelopes() {
    UnitIntervalDistribution porosity = new UnitIntervalDistribution(0.22, 0.38, 0.58);
    UnitIntervalDistribution permeability = new UnitIntervalDistribution(0.16, 0.38, 0.68);
    UnitIntervalDistribution erodibility = new UnitIntervalDistribution(0.68, 0.82, 0.95);
    ColluvialTextureState fineMatrix =
        ColluvialTextureState.from(new SedimentGrainSize(200_000L, 200_000L, 600_000L));
    ColluvialTextureState coarseClast =
        ColluvialTextureState.from(new SedimentGrainSize(600_000L, 350_000L, 50_000L));
    ColluvialTextureState sortedMatrix =
        new ColluvialTextureState(
            fineMatrix.grainSize(),
            SedimentSorting.WELL_SORTED,
            fineMatrix.support(),
            fineMatrix.clastShape());
    ColluvialTextureState sandyMatrix =
        ColluvialTextureState.from(new SedimentGrainSize(100_000L, 800_000L, 100_000L));

    assertEquals(SedimentSorting.MODERATELY_SORTED, fineMatrix.sorting());
    assertEquals(SedimentSorting.MODERATELY_SORTED, coarseClast.sorting());
    assertEquals(SedimentSorting.WELL_SORTED, sandyMatrix.sorting());

    ColluvialPhysicalState fine =
        ColluvialPhysicalState.derive(fineMatrix, porosity, permeability, erodibility);
    ColluvialPhysicalState coarse =
        ColluvialPhysicalState.derive(coarseClast, porosity, permeability, erodibility);
    ColluvialPhysicalState sorted =
        ColluvialPhysicalState.derive(sortedMatrix, porosity, permeability, erodibility);
    ColluvialPhysicalState sandy =
        ColluvialPhysicalState.derive(sandyMatrix, porosity, permeability, erodibility);

    assertTrue(porosity.contains(fine.porosityFraction()));
    assertTrue(permeability.contains(fine.permeabilityIndex()));
    assertTrue(erodibility.contains(fine.erodibilityIndex()));
    assertTrue(coarse.permeabilityIndex() > fine.permeabilityIndex());
    assertTrue(coarse.porosityFraction() > fine.porosityFraction());
    assertTrue(sorted.permeabilityIndex() > fine.permeabilityIndex());
    assertTrue(sorted.porosityFraction() > fine.porosityFraction());
    assertTrue(sandy.erodibilityIndex() > coarse.erodibilityIndex());
    assertEquals(
        fine, ColluvialPhysicalState.derive(fineMatrix, porosity, permeability, erodibility));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ColluvialPhysicalState(fineMatrix, -0.1, 0.2, 0.3, 0.4, 0.5, 0.6));
  }

  @Test
  void colluvialSedimentBudgetClosesCapacityAndDerivesDepositSharesFromDelivery() {
    StableId local = StableId.parse("00000000000000000000000000000c01");
    StableId far = StableId.parse("00000000000000000000000000000c02");
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath localPath = terrainPath(100.0);
    ColluvialSedimentBudget.TerrainPath farPath =
        terrainPath(100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    ColluvialSedimentBudget.ProductionInput matrix =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield);
    ColluvialSedimentBudget.SourceProductionInput localInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            local,
            0,
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield));
    ColluvialSedimentBudget.SourceProductionInput farInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            far,
            192,
            new ColluvialSedimentBudget.ProductionInput(
                300_000L, 8.0, 0.12, 0.8, 0.25, farPath, grainYield));

    ColluvialSedimentBudget budget =
        ColluvialSedimentBudget.derive(0.12, matrix, List.of(farInput, localInput));

    ColluvialHorizonState horizon = ColluvialHorizonState.from(budget);
    assertEquals(ColluvialHorizonState.ProfileClass.BALANCED_MIXED_PROFILE, horizon.profileClass());
    assertEquals(2.0 / 3.0, horizon.weatheringIndex(), 1.0e-15);
    assertEquals(427_991L, horizon.weatheredMatrixFractionPpm());
    assertEquals(572_009L, horizon.transportedSourceFractionPpm());
    assertTrue(horizon.matches(budget));

    assertEquals(
        ColluvialSedimentBudget.GrainTransportModel
            .SLOPE_ROUGHNESS_PATH_GRADE_RUNOFF_CONDITIONED_DRY_RAVEL_PROOF,
        budget.grainTransportModel());
    assertEquals(MaterialAssemblage.SCALE, budget.sourceCapacityFixedUnits());
    assertEquals(
        budget.sourceCapacityFixedUnits(),
        budget.retainedInventoryFixedUnits() + budget.mobilizedInventoryFixedUnits());
    assertEquals(
        budget.mobilizedInventoryFixedUnits(),
        budget.transportLossFixedUnits()
            + budget.bypassedInventoryFixedUnits()
            + budget.depositedInventoryFixedUnits());
    assertEquals(
        MaterialAssemblage.SCALE,
        budget.weatheredMatrixFractionPpm()
            + budget.sourceDepositShares().stream()
                .mapToLong(ColluvialSedimentBudget.SourceDepositShare::fractionPpm)
                .sum());
    assertTrue(budget.retainedInventoryFixedUnits() > 0);
    assertTrue(budget.transportLossFixedUnits() > 0);
    assertTrue(budget.bypassedInventoryFixedUnits() > 0);
    assertTrue(budget.depositedInventoryFixedUnits() > 0);
    assertEquals(375_000L, budget.mobilizedInventoryFixedUnits());
    assertEquals(625_000L, budget.retainedInventoryFixedUnits());
    assertEquals(68_335L, budget.transportLossFixedUnits());
    assertEquals(76_665L, budget.bypassedInventoryFixedUnits());
    assertEquals(230_000L, budget.depositedInventoryFixedUnits());
    assertEquals(427_991L, budget.weatheredMatrixFractionPpm());
    assertEquals(427_991L, budget.sourceFractionPpm(local, 0));
    assertEquals(144_018L, budget.sourceFractionPpm(far, 192));
    assertEquals(budget.sourceCapacityFixedUnits(), budget.capacityGrainMass().totalFixedUnits());
    assertEquals(
        budget.mobilizedInventoryFixedUnits(), budget.mobilizedGrainMass().totalFixedUnits());
    assertEquals(
        budget.retainedInventoryFixedUnits(), budget.retainedGrainMass().totalFixedUnits());
    assertEquals(
        budget.transportLossFixedUnits(), budget.transportLossGrainMass().totalFixedUnits());
    assertEquals(
        budget.bypassedInventoryFixedUnits(), budget.bypassedGrainMass().totalFixedUnits());
    assertEquals(
        budget.depositedInventoryFixedUnits(), budget.depositedGrainMass().totalFixedUnits());
    assertEquals(
        new ColluvialSedimentBudget.GrainMass(22_890L, 24_109L, 21_336L),
        budget.sourceBalances().getLast().balance().transportLossGrainMass());
    assertEquals(
        new ColluvialSedimentBudget.GrainMass(16_583L, 11_449L, 5_092L),
        budget.sourceBalances().getLast().balance().depositedGrainMass());
    assertEquals(new SedimentGrainSize(414_491L, 349_378L, 236_131L), budget.depositedGrainSize());
    ColluvialSedimentBudget.InputBalance farBalance = budget.sourceBalances().getLast().balance();
    assertEquals(6, farPath.reachCount());
    assertEquals(18.0, farPath.cumulativeDownslopeReliefBlocks());
    assertEquals(2.0, farPath.cumulativeBarrierReliefBlocks());
    assertEquals(2.0 / 3.0, farPath.descendingReachFraction());
    assertEquals(47.0 / 60.0, farPath.downslopeContinuityIndex(), 1.0e-15);
    assertEquals(192.0, farPath.straightLineDistanceBlocks());
    assertEquals(192.0, farPath.routedDistanceBlocks());
    assertEquals(1.0, farPath.routeDirectnessIndex(), 1.0e-15);
    assertEquals(16.0, farPath.netUpslopeReliefBlocks(), 1.0e-15);
    assertEquals(25.0 / 72.0, farPath.routeGradeIndex(), 1.0e-15);
    assertEquals(
        0.5 + 0.5 * (47.0 / 60.0) * (0.75 + 0.25 * (25.0 / 72.0)),
        farBalance.transportPathResponse(),
        1.0e-15);
    assertEquals(0.5276898871527778, farBalance.transportDistanceScale(), 1.0e-15);
    assertEquals(
        270.1772222222222, farBalance.grainTransportLengths().gravelAndCoarserBlocks(), 1.0e-12);
    assertEquals(202.63291666666666, farBalance.grainTransportLengths().sandBlocks(), 1.0e-12);
    assertEquals(135.0886111111111, farBalance.grainTransportLengths().finesBlocks(), 1.0e-12);
    assertTrue(
        budget.depositedGrainSize().gravelAndCoarserPpm() > grainYield.gravelAndCoarserPpm());
    assertTrue(budget.depositedGrainSize().finesPpm() < grainYield.finesPpm());
    for (ColluvialSedimentBudget.InputBalance balance :
        List.of(
            budget.weatheredMatrixBalance(),
            budget.sourceBalances().getFirst().balance(),
            budget.sourceBalances().getLast().balance())) {
      assertEquals(
          balance.capacityGrainMass(),
          balance.retainedGrainMass().add(balance.mobilizedGrainMass()));
      assertEquals(
          balance.mobilizedGrainMass(),
          balance
              .transportLossGrainMass()
              .add(balance.bypassedGrainMass())
              .add(balance.depositedGrainMass()));
    }
    assertEquals(
        budget, ColluvialSedimentBudget.derive(0.12, matrix, List.of(localInput, farInput)));

    ColluvialSedimentBudget lowResponse =
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.06, 0.2);
    long lowMobilized = lowResponse.sourceBalances().getFirst().balance().mobilizedFixedUnits();
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 10.0, 0.06, 0.2)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    ColluvialSedimentBudget steepSourceResponse =
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.20, 0.2);
    assertTrue(
        steepSourceResponse.sourceBalances().getFirst().balance().mobilizedFixedUnits()
            > lowMobilized);
    assertTrue(
        steepSourceResponse.sourceBalances().getFirst().balance().transportDistanceScale()
            > lowResponse.sourceBalances().getFirst().balance().transportDistanceScale());
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.06, 0.8)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    ColluvialSedimentBudget.TerrainPath runoffPath = monotonicTerrainPath(96, 0.12);
    ColluvialSedimentBudget lowRunoffBudget =
        ColluvialSedimentBudget.derive(
            0.12,
            matrix,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    local,
                    96,
                    new ColluvialSedimentBudget.ProductionInput(
                        650_000L, 8.0, 0.12, 0.8, 0.25, 0.0, runoffPath, matrix.sedimentYield()))));
    ColluvialSedimentBudget highRunoffBudget =
        ColluvialSedimentBudget.derive(
            0.12,
            matrix,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    local,
                    96,
                    new ColluvialSedimentBudget.ProductionInput(
                        650_000L, 8.0, 0.12, 0.8, 0.25, 1.0, runoffPath, matrix.sedimentYield()))));
    ColluvialSedimentBudget.InputBalance lowRunoff =
        lowRunoffBudget.sourceBalances().getFirst().balance();
    ColluvialSedimentBudget.InputBalance highRunoff =
        highRunoffBudget.sourceBalances().getFirst().balance();
    assertTrue(lowRunoff.mobilizedFixedUnits() < highRunoff.mobilizedFixedUnits());
    assertTrue(lowRunoff.transportDistanceScale() < highRunoff.transportDistanceScale());
    ColluvialSedimentBudget gentleTarget =
        singleSourceBudget(local, matrix, 96, 0.02, 8.0, 0.12, 0.8);
    ColluvialSedimentBudget steepTarget =
        singleSourceBudget(local, matrix, 96, 0.24, 8.0, 0.12, 0.8);
    assertTrue(
        steepTarget.bypassedInventoryFixedUnits() > gentleTarget.bypassedInventoryFixedUnits());
    assertTrue(
        steepTarget.depositedInventoryFixedUnits() < gentleTarget.depositedInventoryFixedUnits());

    ColluvialSedimentBudget.InputBalance smoothSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.0)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance roughSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 1.0)
            .sourceBalances()
            .getFirst()
            .balance();
    assertEquals(smoothSource.mobilizedFixedUnits(), roughSource.mobilizedFixedUnits());
    assertTrue(smoothSource.transportDistanceScale() > roughSource.transportDistanceScale());
    assertTrue(smoothSource.transportLossFixedUnits() < roughSource.transportLossFixedUnits());
    assertTrue(
        Math.multiplyExact(
                roughSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                smoothSource.depositedFixedUnits())
            > Math.multiplyExact(
                smoothSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                roughSource.depositedFixedUnits()));

    ColluvialSedimentBudget.TerrainPath connectedPath =
        terrainPath(100.0, 104.0, 108.0, 112.0, 116.0, 120.0, 124.0);
    ColluvialSedimentBudget.TerrainPath barrierPath =
        terrainPath(100.0, 104.0, 102.0, 108.0, 106.0, 112.0, 110.0);
    ColluvialSedimentBudget.InputBalance connectedSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.25, connectedPath)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance barrierSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.25, barrierPath)
            .sourceBalances()
            .getFirst()
            .balance();
    assertEquals(1.0, connectedPath.downslopeContinuityIndex());
    assertEquals(6.0, barrierPath.cumulativeBarrierReliefBlocks());
    assertEquals(0.5, barrierPath.descendingReachFraction());
    assertEquals(27.0 / 44.0, barrierPath.downslopeContinuityIndex(), 1.0e-15);
    assertEquals(connectedSource.mobilizedFixedUnits(), barrierSource.mobilizedFixedUnits());
    assertTrue(connectedSource.transportPathResponse() > barrierSource.transportPathResponse());
    assertTrue(connectedSource.transportLossFixedUnits() < barrierSource.transportLossFixedUnits());
    assertTrue(
        Math.multiplyExact(
                barrierSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                connectedSource.depositedFixedUnits())
            > Math.multiplyExact(
                connectedSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                barrierSource.depositedFixedUnits()));

    ColluvialSedimentBudget.TerrainPath curvedPath =
        new ColluvialSedimentBudget.TerrainPath(
            32,
            List.of(
                new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                new ColluvialSedimentBudget.TerrainPathSample(32, new Point2(32.0, 0.0), 104.0),
                new ColluvialSedimentBudget.TerrainPathSample(64, new Point2(32.0, 32.0), 108.0)));
    assertEquals(64, curvedPath.distanceBlocks());
    assertEquals(StrictMath.sqrt(2.0) * 32.0, curvedPath.straightLineDistanceBlocks(), 1.0e-12);
    assertEquals(64.0, curvedPath.routedDistanceBlocks());
    assertEquals(StrictMath.sqrt(0.5), curvedPath.routeDirectnessIndex(), 1.0e-12);
    assertEquals(8.0, curvedPath.netUpslopeReliefBlocks(), 1.0e-12);
    assertEquals(25.0 / 48.0, curvedPath.routeGradeIndex(), 1.0e-12);
    assertEquals(90.0, curvedPath.maximumDeflectionFromInitialDegrees(), 1.0e-12);
    assertEquals(2, curvedPath.reaches().size());
    assertEquals(new Point2(1.0, 0.0), curvedPath.reaches().getFirst().routedUpslopeDirection());
    assertEquals(new Point2(0.0, 1.0), curvedPath.reaches().getLast().routedUpslopeDirection());
    ColluvialSedimentBudget curvedBudget =
        singleSourceBudget(local, matrix, 64, 0.12, 8.0, 0.12, 0.8, 0.25, curvedPath);
    assertEquals(
        0.5 + 0.5 * StrictMath.sqrt(0.5) * (0.75 + 0.25 * (25.0 / 48.0)),
        curvedBudget.sourceBalances().getFirst().balance().transportPathResponse(),
        1.0e-15);

    ColluvialSedimentBudget.TerrainPath gentleGradePath = terrainPath(100.0, 104.0, 108.0, 112.0);
    ColluvialSedimentBudget.TerrainPath steepGradePath = terrainPath(100.0, 112.0, 124.0, 136.0);
    ColluvialSedimentBudget.InputBalance gentleGrade =
        singleSourceBudget(local, matrix, 96, 0.12, 8.0, 0.12, 0.8, 0.25, gentleGradePath)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance steepGrade =
        singleSourceBudget(local, matrix, 96, 0.12, 8.0, 0.12, 0.8, 0.25, steepGradePath)
            .sourceBalances()
            .getFirst()
            .balance();
    assertTrue(steepGradePath.routeGradeIndex() > gentleGradePath.routeGradeIndex());
    assertTrue(steepGrade.transportPathResponse() > gentleGrade.transportPathResponse());
    assertTrue(steepGrade.transportDistanceScale() > gentleGrade.transportDistanceScale());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(32.0, 0.0), 104.0)),
                List.of(
                    new ColluvialSedimentBudget.TerrainPathReach(
                        0,
                        new Point2(0.0, 0.0),
                        new Point2(32.0, 0.0),
                        new Point2(1.0, 0.0),
                        new Point2(0.0, 1.0),
                        false,
                        false))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPathReach(
                0,
                new Point2(0.0, 0.0),
                new Point2(32.0, 0.0),
                new Point2(2.0, 0.0),
                new Point2(1.0, 0.0),
                false,
                false));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.ProductionInput(
                -1L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.12, 0.8, 1.01, localPath, grainYield));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        64, new Point2(64.0, 0.0), 104.0))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(
                        0, new Point2(0.0, 0.0), -Double.MAX_VALUE),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(32.0, 0.0), Double.MAX_VALUE))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(31.0, 0.0), 104.0))));
    assertThrows(
        IllegalArgumentException.class, () -> new ColluvialSedimentBudget.GrainMass(-1L, 1L, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                new ColluvialSedimentBudget.ProductionInput(
                    349_999L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield),
                List.of(localInput, farInput)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                matrix,
                List.of(
                    localInput,
                    new ColluvialSedimentBudget.SourceProductionInput(far, 0, farInput.input()))));
    ColluvialSedimentBudget.TerrainPath shiftedFarPath =
        terrainPath(new Point2(1.0, 0.0), 100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                matrix,
                List.of(
                    localInput,
                    new ColluvialSedimentBudget.SourceProductionInput(
                        far,
                        192,
                        new ColluvialSedimentBudget.ProductionInput(
                            300_000L, 8.0, 0.12, 0.8, 0.25, shiftedFarPath, grainYield)))));
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility) {
    return singleSourceBudget(
        source,
        matrix,
        distance,
        depositionSlope,
        weatheringDepth,
        sourceSlope,
        erodibility,
        matrix.terrainRoughnessIndex());
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility,
      double terrainRoughnessIndex) {
    return singleSourceBudget(
        source,
        matrix,
        distance,
        depositionSlope,
        weatheringDepth,
        sourceSlope,
        erodibility,
        terrainRoughnessIndex,
        monotonicTerrainPath(distance, sourceSlope));
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility,
      double terrainRoughnessIndex,
      ColluvialSedimentBudget.TerrainPath terrainPath) {
    return ColluvialSedimentBudget.derive(
        depositionSlope,
        matrix,
        List.of(
            new ColluvialSedimentBudget.SourceProductionInput(
                source,
                distance,
                new ColluvialSedimentBudget.ProductionInput(
                    650_000L,
                    weatheringDepth,
                    sourceSlope,
                    erodibility,
                    terrainRoughnessIndex,
                    terrainPath,
                    matrix.sedimentYield()))));
  }

  private static ColluvialSedimentBudget.TerrainPath monotonicTerrainPath(
      int distanceBlocks, double slope) {
    if (distanceBlocks % 32 != 0) {
      throw new IllegalArgumentException("test terrain-path distance must be divisible by 32");
    }
    double[] elevations = new double[distanceBlocks / 32 + 1];
    for (int index = 0; index < elevations.length; index++) {
      elevations[index] = 100.0 + index * 32.0 * slope;
    }
    return terrainPath(elevations);
  }

  private static ColluvialSedimentBudget.TerrainPath terrainPath(double... elevations) {
    return terrainPath(new Point2(0.0, 0.0), elevations);
  }

  private static ColluvialSedimentBudget.TerrainPath terrainPath(
      Point2 origin, double... elevations) {
    List<ColluvialSedimentBudget.TerrainPathSample> samples = new java.util.ArrayList<>();
    for (int index = 0; index < elevations.length; index++) {
      samples.add(
          new ColluvialSedimentBudget.TerrainPathSample(
              index * 32, origin.add(index * 32.0, 0.0), elevations[index]));
    }
    return new ColluvialSedimentBudget.TerrainPath(32, samples);
  }

  @Test
  void triangularPropertyDistributionIsBoundedAndHonorsItsMode() {
    UnitIntervalDistribution distribution = new UnitIntervalDistribution(0.1, 0.3, 0.8);

    assertEquals(0.1, distribution.sample(0.0));
    assertEquals(0.3, distribution.sample((0.3 - 0.1) / (0.8 - 0.1)), 1.0e-15);
    assertTrue(distribution.contains(distribution.sample(0.999_999)));
    assertEquals(0.4, new UnitIntervalDistribution(0.4, 0.4, 0.4).sample(0.75));
    assertThrows(IllegalArgumentException.class, () -> new UnitIntervalDistribution(0.4, 0.2, 0.8));
  }

  @Test
  void alterationRecipesSelectByProtolithFamilyAndRequireExactCoverage() {
    MaterialAssemblage felsic = assemblage("test:felsic");
    MaterialAssemblage mafic = assemblage("test:mafic");
    AlterationAssemblageRecipe first =
        new AlterationAssemblageRecipe(
            List.of(GeneticFamily.IGNEOUS, GeneticFamily.SEDIMENTARY), felsic);
    AlterationAssemblageRecipe second =
        new AlterationAssemblageRecipe(
            List.of(GeneticFamily.METAMORPHIC, GeneticFamily.HYDROTHERMAL, GeneticFamily.SURFICIAL),
            mafic);

    AlterationDefinition definition = alteration(List.of(first, second));

    assertSame(felsic, definition.targetAssemblage(GeneticFamily.IGNEOUS));
    assertSame(felsic, definition.targetAssemblage(GeneticFamily.SEDIMENTARY));
    assertSame(mafic, definition.targetAssemblage(GeneticFamily.METAMORPHIC));
    assertThrows(IllegalArgumentException.class, () -> alteration(List.of(first)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            alteration(
                List.of(
                    first,
                    second,
                    new AlterationAssemblageRecipe(List.of(GeneticFamily.IGNEOUS), mafic))));
    assertThrows(
        IllegalArgumentException.class, () -> alteration(List.of(first, second), Optional.empty()));
  }

  @Test
  void fluidStateRequiresBoundedIndependentTransportAxes() {
    ProcessFluidState state = fluidState();

    assertEquals(3, state.ligandCapacities().chloride());
    assertEquals(2, state.ligandCapacities().reducedSulfur());
    assertEquals(3, state.integratedFluxClass());
    assertThrows(IllegalArgumentException.class, () -> new LigandCapacities(4, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ProcessFluidState(
                state.medium(),
                state.redox(),
                state.acidity(),
                state.salinity(),
                state.sulfurState(),
                state.ligandCapacities(),
                -1));
  }

  @Test
  void responseTextureIsRequiredExactlyForIsochemicalMetamorphism() {
    AlterationDefinition hornfels =
        new AlterationDefinition(
            Overprint.CONTACT_HORNFELS,
            MaterialProcessClass.ISOCHEMICAL_METAMORPHISM,
            Optional.empty(),
            0,
            List.of(),
            Optional.of(RockTexture.HORNFELSIC),
            MetamorphicFacies.HORNBLENDE_HORNFELS,
            MetamorphicPath.CONTACT_LOW_P,
            500.0,
            700.0,
            100.0,
            300.0,
            0.7,
            -0.08);

    assertEquals(RockTexture.HORNFELSIC, hornfels.responseTexture().orElseThrow());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AlterationDefinition(
                hornfels.overprint(),
                hornfels.processClass(),
                hornfels.fluidState(),
                hornfels.replacementPpm(),
                hornfels.targetRecipes(),
                Optional.empty(),
                hornfels.facies(),
                hornfels.path(),
                hornfels.minimumTemperatureCelsius(),
                hornfels.maximumTemperatureCelsius(),
                hornfels.minimumPressureMpa(),
                hornfels.maximumPressureMpa(),
                hornfels.porosityMultiplier(),
                hornfels.erodibilityDelta()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AlterationDefinition(
                Overprint.NONE,
                MaterialProcessClass.NONE,
                Optional.empty(),
                0,
                List.of(),
                Optional.of(RockTexture.HORNFELSIC),
                MetamorphicFacies.NONE,
                MetamorphicPath.NONE,
                0.0,
                0.0,
                0.0,
                0.0,
                1.0,
                0.0));
  }

  @Test
  void primaryMetamorphismIsRequiredOnlyForMetamorphicRockRecipes() {
    PrimaryMetamorphicDefinition metamorphism =
        new PrimaryMetamorphicDefinition(
            "test:shale",
            MetamorphicGrade.LOW,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.COLLISION_CLOCKWISE,
            250.0,
            450.0,
            200.0,
            600.0);
    UnitIntervalDistribution property = new UnitIntervalDistribution(0.1, 0.2, 0.3);
    MaterialAssemblage assemblage = assemblage("test:quartz");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PrimaryMetamorphicDefinition(
                "test:shale",
                MetamorphicGrade.NONE,
                MetamorphicFacies.GREENSCHIST,
                MetamorphicPath.COLLISION_CLOCKWISE,
                250.0,
                450.0,
                200.0,
                600.0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockDefinition(
                "test:metamorphic",
                Lithology.GRANITIC_GNEISS,
                GeneticFamily.METAMORPHIC,
                RockTexture.FOLIATED_CRYSTALLINE,
                Optional.empty(),
                assemblage,
                0.0,
                List.of(),
                sedimentYield(),
                property,
                property,
                property));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockDefinition(
                "test:igneous",
                Lithology.FELSIC_STOCK,
                GeneticFamily.IGNEOUS,
                RockTexture.PHANERITIC_CRYSTALLINE,
                Optional.of(metamorphism),
                assemblage,
                0.0,
                List.of(),
                sedimentYield(),
                property,
                property,
                property));
  }

  @Test
  void modalVariationAxesConserveMassAndRespectTheRockSpreadEnvelope() {
    ModalVariationAxis axis =
        new ModalVariationAxis(
            "quartz_feldspar_balance", Map.of("test:quartz", 50_000L, "test:feldspar", -50_000L));
    MaterialAssemblage central =
        new MaterialAssemblage(Map.of("test:quartz", 500_000L, "test:feldspar", 500_000L));

    RockDefinition rock = rock(central, 0.1, List.of(axis));

    assertEquals(
        0L,
        rock.modalVariationAxes().getFirst().loadingsPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModalVariationAxis(
                "unbalanced", Map.of("test:quartz", 50_000L, "test:feldspar", -49_999L)));
    ModalVariationAxis excessive =
        new ModalVariationAxis(
            "excessive", Map.of("test:quartz", 50_001L, "test:feldspar", -50_001L));
    assertThrows(IllegalArgumentException.class, () -> rock(central, 0.1, List.of(excessive)));
    assertThrows(IllegalArgumentException.class, () -> rock(central, 0.1, List.of(axis, axis)));
  }

  @Test
  void bodySamplerFollowsAuthoredCorrelationWithoutLeakingModalMass() {
    MaterialAssemblage central =
        new MaterialAssemblage(
            Map.of(
                "test:quartz", 400_000L,
                "test:albite", 300_000L,
                "test:orthoclase", 300_000L));
    ModalVariationAxis axis =
        new ModalVariationAxis(
            "quartz_feldspar_balance",
            Map.of(
                "test:quartz", 60_000L,
                "test:albite", -30_000L,
                "test:orthoclase", -30_000L));
    RockDefinition rock = rock(central, 0.15, List.of(axis));
    BodyCompositionSampler sampler =
        new BodyCompositionSampler(
            new WorldIdentity(17L, "test-model", "test-digest", "test:profile"));
    boolean sawPositive = false;
    boolean sawNegative = false;

    for (int index = 1; index <= 128; index++) {
      MaterialAssemblage sampled = sampler.sample(rock, StableId.parse("%032x".formatted(index)));
      long quartzDelta = sampled.modesPpm().get("test:quartz") - 400_000L;
      long albiteDelta = sampled.modesPpm().get("test:albite") - 300_000L;
      long orthoclaseDelta = sampled.modesPpm().get("test:orthoclase") - 300_000L;

      assertEquals(0L, quartzDelta + albiteDelta + orthoclaseDelta);
      assertTrue(StrictMath.abs(albiteDelta - orthoclaseDelta) <= 1L);
      assertTrue(quartzDelta == 0L || Long.signum(quartzDelta) == -Long.signum(albiteDelta));
      assertTrue(StrictMath.abs(quartzDelta) <= 60_000L);
      sawPositive |= quartzDelta > 0L;
      sawNegative |= quartzDelta < 0L;
    }
    assertTrue(sawPositive && sawNegative);
  }

  @Test
  void weightedAssemblageBlendIsExactAndIndependentOfShareOrder() {
    MaterialAssemblage quartz = assemblage("test:quartz");
    MaterialAssemblage feldspar = assemblage("test:feldspar");
    MaterialAssemblage clay = assemblage("test:clay");
    List<MaterialAssemblage.Share> shares =
        List.of(
            new MaterialAssemblage.Share(quartz, 333_333L),
            new MaterialAssemblage.Share(feldspar, 333_333L),
            new MaterialAssemblage.Share(clay, 333_334L));

    MaterialAssemblage blended = MaterialAssemblage.weightedBlend(shares);

    assertEquals(
        Map.of("test:quartz", 333_333L, "test:feldspar", 333_333L, "test:clay", 333_334L),
        blended.modesPpm());
    assertEquals(
        blended,
        MaterialAssemblage.weightedBlend(List.of(shares.get(2), shares.get(0), shares.get(1))));
    MaterialAssemblage mixedA =
        new MaterialAssemblage(Map.of("test:quartz", 500_001L, "test:feldspar", 499_999L));
    MaterialAssemblage mixedB =
        new MaterialAssemblage(Map.of("test:quartz", 333_333L, "test:clay", 666_667L));
    MaterialAssemblage mixedC =
        new MaterialAssemblage(Map.of("test:feldspar", 250_001L, "test:clay", 749_999L));
    List<MaterialAssemblage.Share> roundedShares =
        List.of(
            new MaterialAssemblage.Share(mixedA, 333_333L),
            new MaterialAssemblage.Share(mixedB, 333_333L),
            new MaterialAssemblage.Share(mixedC, 333_334L));
    MaterialAssemblage rounded = MaterialAssemblage.weightedBlend(roundedShares);
    assertEquals(
        Map.of("test:quartz", 277_778L, "test:feldspar", 250_000L, "test:clay", 472_222L),
        rounded.modesPpm());
    assertEquals(
        rounded,
        MaterialAssemblage.weightedBlend(
            List.of(roundedShares.get(2), roundedShares.get(0), roundedShares.get(1))));
    assertEquals(
        MaterialAssemblage.blend(quartz, feldspar, 500_000L),
        MaterialAssemblage.weightedBlend(
            List.of(
                new MaterialAssemblage.Share(quartz, 500_000L),
                new MaterialAssemblage.Share(feldspar, 500_000L))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MaterialAssemblage.weightedBlend(
                List.of(new MaterialAssemblage.Share(quartz, 999_999L))));
  }

  private static AlterationDefinition alteration(List<AlterationAssemblageRecipe> recipes) {
    return alteration(recipes, Optional.of(fluidState()));
  }

  private static AlterationDefinition alteration(
      List<AlterationAssemblageRecipe> recipes, Optional<ProcessFluidState> fluidState) {
    return new AlterationDefinition(
        Overprint.POTASSIC_ALTERATION,
        MaterialProcessClass.HYDROTHERMAL_METASOMATISM,
        fluidState,
        250_000,
        recipes,
        Optional.empty(),
        MetamorphicFacies.NONE,
        MetamorphicPath.NONE,
        300.0,
        500.0,
        50.0,
        150.0,
        1.0,
        0.0);
  }

  private static ProcessFluidState fluidState() {
    return new ProcessFluidState(
        FluidMedium.MAGMATIC_HYDROTHERMAL,
        RedoxClass.OXIDIZING,
        AcidityClass.NEAR_NEUTRAL,
        SalinityClass.CONCENTRATED_BRINE,
        SulfurState.REDUCED_SULFUR_BUFFERED,
        new LigandCapacities(3, 2, 1, 2),
        3);
  }

  private static RockDefinition rock(
      MaterialAssemblage central, double spread, List<ModalVariationAxis> axes) {
    UnitIntervalDistribution property = new UnitIntervalDistribution(0.1, 0.2, 0.3);
    return new RockDefinition(
        "test:rock",
        Lithology.GRANITIC_GNEISS,
        GeneticFamily.METAMORPHIC,
        RockTexture.FOLIATED_CRYSTALLINE,
        Optional.of(
            new PrimaryMetamorphicDefinition(
                "test:protolith",
                MetamorphicGrade.HIGH,
                MetamorphicFacies.AMPHIBOLITE,
                MetamorphicPath.COLLISION_CLOCKWISE,
                600.0,
                750.0,
                400.0,
                800.0)),
        central,
        spread,
        axes,
        sedimentYield(),
        property,
        property,
        property);
  }

  private static MaterialAssemblage assemblage(String mineral) {
    return new MaterialAssemblage(Map.of(mineral, MaterialAssemblage.SCALE));
  }

  private static SedimentGrainSize sedimentYield() {
    return new SedimentGrainSize(400_000L, 400_000L, 200_000L);
  }
}
