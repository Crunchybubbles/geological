package io.github.crunchybubbles.geological.petrology;

/** Exact normalized source-mix proof budget for one sedimentary facies. */
public record SedimentaryInputBudget(
    long clasticPpm,
    long volcanicPpm,
    long carbonatePpm,
    long organicPpm,
    long chemicalPrecipitatePpm,
    long evaporiticBrinePpm) {
  public SedimentaryInputBudget {
    if (clasticPpm < 0
        || clasticPpm > MaterialAssemblage.SCALE
        || volcanicPpm < 0
        || volcanicPpm > MaterialAssemblage.SCALE
        || carbonatePpm < 0
        || carbonatePpm > MaterialAssemblage.SCALE
        || organicPpm < 0
        || organicPpm > MaterialAssemblage.SCALE
        || chemicalPrecipitatePpm < 0
        || chemicalPrecipitatePpm > MaterialAssemblage.SCALE
        || evaporiticBrinePpm < 0
        || evaporiticBrinePpm > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("sedimentary input budget is out of bounds");
    }
    if (clasticPpm
            + volcanicPpm
            + carbonatePpm
            + organicPpm
            + chemicalPrecipitatePpm
            + evaporiticBrinePpm
        != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "sedimentary input budget must close to " + MaterialAssemblage.SCALE);
    }
  }

  /** Returns the deterministic source-mix proof associated with the existing facies vocabulary. */
  public static SedimentaryInputBudget proofFor(String faciesClass) {
    if (faciesClass == null || faciesClass.isBlank()) {
      throw new IllegalArgumentException("sedimentary facies is required");
    }
    return switch (faciesClass) {
      case "rift_margin_alluvial_fan" -> new SedimentaryInputBudget(950_000, 50_000, 0, 0, 0, 0);
      case "submarine_volcanic_apron" ->
          new SedimentaryInputBudget(400_000, 450_000, 100_000, 0, 50_000, 0);
      case "offshore_low_energy" -> new SedimentaryInputBudget(850_000, 0, 50_000, 100_000, 0, 0);
      case "shallow_marine_shoreface" ->
          new SedimentaryInputBudget(800_000, 50_000, 100_000, 50_000, 0, 0);
      case "delta_front_to_offshore_transition" ->
          new SedimentaryInputBudget(850_000, 0, 50_000, 100_000, 0, 0);
      case "carbonate_platform" -> new SedimentaryInputBudget(100_000, 0, 850_000, 50_000, 0, 0);
      case "dolomitized_carbonate_platform" ->
          new SedimentaryInputBudget(100_000, 0, 800_000, 0, 50_000, 50_000);
      case "marine_bedded_silica" -> new SedimentaryInputBudget(150_000, 50_000, 0, 0, 800_000, 0);
      case "ancient_iron_silica_precipitation_basin" ->
          new SedimentaryInputBudget(100_000, 100_000, 100_000, 0, 700_000, 0);
      case "restricted_evaporite_margin" ->
          new SedimentaryInputBudget(100_000, 0, 50_000, 0, 150_000, 700_000);
      case "restricted_evaporite_basin_center" ->
          new SedimentaryInputBudget(50_000, 0, 0, 0, 50_000, 900_000);
      case "buried_peat_mire" -> new SedimentaryInputBudget(150_000, 0, 0, 800_000, 50_000, 0);
      default -> new SedimentaryInputBudget(MaterialAssemblage.SCALE, 0, 0, 0, 0, 0);
    };
  }
}
