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
import io.github.crunchybubbles.geological.petrology.SulfurState;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialSchemaTest {
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
        property,
        property,
        property);
  }

  private static MaterialAssemblage assemblage(String mineral) {
    return new MaterialAssemblage(Map.of(mineral, MaterialAssemblage.SCALE));
  }
}
