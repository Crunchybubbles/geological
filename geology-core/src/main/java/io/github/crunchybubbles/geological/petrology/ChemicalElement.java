package io.github.crunchybubbles.geological.petrology;

import java.util.Arrays;

/** Element vocabulary required by the Phase 2 rock-constituent and ore-mineral catalog. */
public enum ChemicalElement {
  H("H", 1.008),
  C("C", 12.011),
  N("N", 14.007),
  O("O", 15.999),
  F("F", 18.998403163),
  NA("Na", 22.98976928),
  MG("Mg", 24.305),
  AL("Al", 26.9815385),
  SI("Si", 28.085),
  P("P", 30.973761998),
  S("S", 32.06),
  CL("Cl", 35.45),
  K("K", 39.0983),
  CA("Ca", 40.078),
  TI("Ti", 47.867),
  CR("Cr", 51.9961),
  FE("Fe", 55.845),
  CU("Cu", 63.546),
  ZN("Zn", 65.38),
  AU("Au", 196.96657),
  // Phase 9 extension vocabulary; appended to preserve Phase 2 enum ordinals.
  HE("He", 4.002602),
  LI("Li", 6.94),
  BE("Be", 9.0121831),
  B("B", 10.81),
  V("V", 50.9415),
  MN("Mn", 54.938044),
  CO("Co", 58.933194),
  NI("Ni", 58.6934),
  GA("Ga", 69.723),
  RB("Rb", 85.4678),
  SR("Sr", 87.62),
  Y("Y", 88.90584),
  ZR("Zr", 91.224),
  NB("Nb", 92.90637),
  MO("Mo", 95.95),
  CD("Cd", 112.414),
  IN("In", 114.818),
  AG("Ag", 107.8682),
  SN("Sn", 118.710),
  CS("Cs", 132.90545196),
  LA("La", 138.90547),
  CE("Ce", 140.116),
  ND("Nd", 144.242),
  HF("Hf", 178.49),
  TA("Ta", 180.94788),
  W("W", 183.84),
  RE("Re", 186.207),
  PB("Pb", 207.2),
  TH("Th", 232.0377),
  U("U", 238.02891);

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
