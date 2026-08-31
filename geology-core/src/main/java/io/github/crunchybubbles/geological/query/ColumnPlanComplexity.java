package io.github.crunchybubbles.geological.query;

import java.util.ArrayList;
import java.util.List;

/** Observable work represented by one adaptive column plan. */
public record ColumnPlanComplexity(
    int candidates, int transitions, int pointEvaluations, int materialRuns) {
  public ColumnPlanComplexity {
    if (candidates < 0 || transitions < 0 || pointEvaluations <= 0 || materialRuns <= 0) {
      throw new IllegalArgumentException("column plan complexity counts are outside their bounds");
    }
  }

  public List<String> violations(ColumnPlanBudget budget) {
    if (budget == null) {
      throw new IllegalArgumentException("column plan budget is required");
    }
    List<String> result = new ArrayList<>();
    addViolation(result, "candidates", candidates, budget.maximumCandidates());
    addViolation(result, "transitions", transitions, budget.maximumTransitions());
    addViolation(result, "point_evaluations", pointEvaluations, budget.maximumPointEvaluations());
    addViolation(result, "material_runs", materialRuns, budget.maximumMaterialRuns());
    return List.copyOf(result);
  }

  public boolean within(ColumnPlanBudget budget) {
    return violations(budget).isEmpty();
  }

  private static void addViolation(List<String> result, String name, int actual, int maximum) {
    if (actual > maximum) {
      result.add(name + "=" + actual + " exceeds " + maximum);
    }
  }
}
