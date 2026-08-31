package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.petrology.AlterationAssemblageRecipe;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
import io.github.crunchybubbles.geological.petrology.MineralAssemblage;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import java.util.List;
import java.util.Map;
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
    MineralAssemblage felsic = assemblage("test:felsic");
    MineralAssemblage mafic = assemblage("test:mafic");
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
  }

  private static AlterationDefinition alteration(List<AlterationAssemblageRecipe> recipes) {
    return new AlterationDefinition(
        Overprint.POTASSIC_ALTERATION,
        MaterialProcessClass.HYDROTHERMAL_METASOMATISM,
        250_000,
        recipes,
        MetamorphicFacies.NONE,
        MetamorphicPath.NONE,
        300.0,
        500.0,
        50.0,
        150.0,
        1.0,
        0.0);
  }

  private static MineralAssemblage assemblage(String mineral) {
    return new MineralAssemblage(Map.of(mineral, MineralAssemblage.SCALE));
  }
}
