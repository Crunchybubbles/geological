package io.github.crunchybubbles.geological.petrology;

import java.util.Arrays;

/** Element vocabulary required by the first Phase 2 rock-forming and ore-mineral pack. */
public enum ChemicalElement {
  H("H", 1.008),
  C("C", 12.011),
  O("O", 15.999),
  NA("Na", 22.98976928),
  MG("Mg", 24.305),
  AL("Al", 26.9815385),
  SI("Si", 28.085),
  S("S", 32.06),
  K("K", 39.0983),
  CA("Ca", 40.078),
  TI("Ti", 47.867),
  FE("Fe", 55.845),
  CU("Cu", 63.546),
  ZN("Zn", 65.38),
  AU("Au", 196.96657);

  private final String symbol;
  private final double atomicWeight;

  ChemicalElement(String symbol, double atomicWeight) {
    this.symbol = symbol;
    this.atomicWeight = atomicWeight;
  }

  public String symbol() {
    return symbol;
  }

  public double atomicWeight() {
    return atomicWeight;
  }

  public static ChemicalElement fromSymbol(String symbol) {
    return Arrays.stream(values())
        .filter(element -> element.symbol.equals(symbol))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unsupported chemical element " + symbol));
  }
}
