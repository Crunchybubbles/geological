package io.github.crunchybubbles.geological.registry;

/** Small Phase 1 unit vocabulary; definitions never carry ambiguous bare numbers. */
public enum ScientificUnit {
  ONE("geological:one", "1", QuantityDimension.DIMENSIONLESS),
  FRACTION("geological:fraction", "fraction", QuantityDimension.DIMENSIONLESS),
  BLOCK("geological:block", "block", QuantityDimension.LENGTH),
  MEGAYEAR("geological:megayear", "Ma", QuantityDimension.TIME);

  private final String id;
  private final String symbol;
  private final QuantityDimension dimension;

  ScientificUnit(String id, String symbol, QuantityDimension dimension) {
    this.id = id;
    this.symbol = symbol;
    this.dimension = dimension;
  }

  public String id() {
    return id;
  }

  public String symbol() {
    return symbol;
  }

  public QuantityDimension dimension() {
    return dimension;
  }
}
