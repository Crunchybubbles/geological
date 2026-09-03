package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.AcidityClass;
import io.github.crunchybubbles.geological.petrology.AlterationAssemblageRecipe;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.BodyCompositionSampler;
import io.github.crunchybubbles.geological.petrology.ClastShape;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialSedimentBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialTextureState;
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
  void colluvialTextureClassifiesSupportWithoutInventingTransportSorting() {
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
    assertEquals(ClastShape.ANGULAR_TO_SUBROUNDED, mixed.clastShape());
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
    ColluvialSedimentBudget.ProductionInput matrix =
        new ColluvialSedimentBudget.ProductionInput(350_000L, 8.0, 0.12, 0.8, grainYield);
    ColluvialSedimentBudget.SourceProductionInput localInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            local,
            0,
            new ColluvialSedimentBudget.ProductionInput(350_000L, 8.0, 0.12, 0.8, grainYield));
    ColluvialSedimentBudget.SourceProductionInput farInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            far,
            192,
            new ColluvialSedimentBudget.ProductionInput(300_000L, 8.0, 0.12, 0.8, grainYield));

    ColluvialSedimentBudget budget =
        ColluvialSedimentBudget.derive(0.12, matrix, List.of(farInput, localInput));

    assertEquals(
        ColluvialSedimentBudget.GrainTransportModel.DRY_RAVEL_COARSE_SURVIVAL_PROOF,
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
    assertEquals(44_405L, budget.transportLossFixedUnits());
    assertEquals(82_648L, budget.bypassedInventoryFixedUnits());
    assertEquals(247_947L, budget.depositedInventoryFixedUnits());
    assertEquals(397_012L, budget.weatheredMatrixFractionPpm());
    assertEquals(397_012L, budget.sourceFractionPpm(local, 0));
    assertEquals(205_976L, budget.sourceFractionPpm(far, 192));
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
        new ColluvialSedimentBudget.GrainMass(14_072L, 15_493L, 14_840L),
        budget.sourceBalances().getLast().balance().transportLossGrainMass());
    assertEquals(
        new ColluvialSedimentBudget.GrainMass(23_196L, 17_911L, 9_964L),
        budget.sourceBalances().getLast().balance().depositedGrainMass());
    assertEquals(new SedimentGrainSize(411_161L, 350_151L, 238_688L), budget.depositedGrainSize());
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
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.20, 0.2)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.06, 0.8)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    ColluvialSedimentBudget gentleTarget =
        singleSourceBudget(local, matrix, 96, 0.02, 8.0, 0.12, 0.8);
    ColluvialSedimentBudget steepTarget =
        singleSourceBudget(local, matrix, 96, 0.24, 8.0, 0.12, 0.8);
    assertTrue(
        steepTarget.bypassedInventoryFixedUnits() > gentleTarget.bypassedInventoryFixedUnits());
    assertTrue(
        steepTarget.depositedInventoryFixedUnits() < gentleTarget.depositedInventoryFixedUnits());

    assertThrows(
        IllegalArgumentException.class,
        () -> new ColluvialSedimentBudget.ProductionInput(-1L, 8.0, 0.12, 0.8, grainYield));
    assertThrows(
        IllegalArgumentException.class, () -> new ColluvialSedimentBudget.GrainMass(-1L, 1L, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                new ColluvialSedimentBudget.ProductionInput(349_999L, 8.0, 0.12, 0.8, grainYield),
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
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility) {
    return ColluvialSedimentBudget.derive(
        depositionSlope,
        matrix,
        List.of(
            new ColluvialSedimentBudget.SourceProductionInput(
                source,
                distance,
                new ColluvialSedimentBudget.ProductionInput(
                    650_000L, weatheringDepth, sourceSlope, erodibility, matrix.sedimentYield()))));
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
