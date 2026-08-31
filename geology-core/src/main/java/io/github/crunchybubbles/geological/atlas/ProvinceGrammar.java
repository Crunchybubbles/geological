package io.github.crunchybubbles.geological.atlas;

/** Deterministic Phase 1 chronicle outcome selected before mineral-system evaluation. */
public enum ProvinceGrammar {
  EXHUMED_FERTILE_RIFT_TO_ARC(true, true, true),
  BURIED_FERTILE_RIFT_TO_ARC(true, true, false),
  BARREN_DRY_RIFT_TO_ARC(false, false, false);

  private final boolean formsPorphyry;
  private final boolean formsVms;
  private final boolean formsPlacer;

  ProvinceGrammar(boolean formsPorphyry, boolean formsVms, boolean formsPlacer) {
    this.formsPorphyry = formsPorphyry;
    this.formsVms = formsVms;
    this.formsPlacer = formsPlacer;
  }

  public boolean formsPorphyry() {
    return formsPorphyry;
  }

  public boolean formsVms() {
    return formsVms;
  }

  public boolean formsPlacer() {
    return formsPlacer;
  }
}
