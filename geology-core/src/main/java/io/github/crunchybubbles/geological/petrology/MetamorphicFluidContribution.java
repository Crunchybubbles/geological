package io.github.crunchybubbles.geological.petrology;

/** One bounded normalized fluid input or release attributed to a metamorphic reaction. */
public record MetamorphicFluidContribution(
    FluidSpecies fluidSpecies, Direction direction, long amountPpm) {
  public MetamorphicFluidContribution {
    if (fluidSpecies == null
        || direction == null
        || amountPpm <= 0
        || amountPpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("metamorphic fluid contribution is incomplete or invalid");
    }
  }

  public enum FluidSpecies {
    WATER,
    CARBON_DIOXIDE,
    HYDROGEN
  }

  public enum Direction {
    INPUT,
    OUTPUT
  }
}
