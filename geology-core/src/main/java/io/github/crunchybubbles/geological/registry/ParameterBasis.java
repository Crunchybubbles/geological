package io.github.crunchybubbles.geological.registry;

/** Source attribution for a scientifically meaningful registry number. */
public record ParameterBasis(String citationId, String tunableDesignRationale) {
  public ParameterBasis {
    citationId = normalize(citationId);
    tunableDesignRationale = normalize(tunableDesignRationale);
    if ((citationId == null) == (tunableDesignRationale == null)) {
      throw new IllegalArgumentException(
          "a parameter must be either cited or an explicitly justified tunable design value");
    }
  }

  public static ParameterBasis cited(String citationId) {
    return new ParameterBasis(citationId, null);
  }

  public static ParameterBasis tunable(String rationale) {
    return new ParameterBasis(null, rationale);
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
