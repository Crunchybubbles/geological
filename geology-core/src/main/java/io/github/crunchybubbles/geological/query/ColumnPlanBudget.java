package io.github.crunchybubbles.geological.query;

/** Configurable diagnostic ceilings; exceeding them never changes geological output. */
public record ColumnPlanBudget(
    int maximumCandidates,
    int maximumTransitions,
    int maximumPointEvaluations,
    int maximumMaterialRuns) {
  public static final ColumnPlanBudget PHASE1_REVIEW = new ColumnPlanBudget(16, 64, 64, 32);

  public ColumnPlanBudget {
    if (maximumCandidates <= 0
        || maximumTransitions <= 0
        || maximumPointEvaluations <= 0
        || maximumMaterialRuns <= 0) {
      throw new IllegalArgumentException("column plan diagnostic ceilings must be positive");
    }
  }
}
