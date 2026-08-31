package io.github.crunchybubbles.geological.query;

import java.util.List;

/** Conservative transition proof used to partition a block column into uniform intervals. */
public record ColumnIntervalProof(
    String method, List<Double> transitionElevations, List<Integer> splitYCoordinates) {
  public ColumnIntervalProof {
    if (method == null || method.isBlank()) {
      throw new IllegalArgumentException("column interval proof method must be present");
    }
    transitionElevations = List.copyOf(transitionElevations);
    splitYCoordinates = List.copyOf(splitYCoordinates);
    if (splitYCoordinates.size() < 2) {
      throw new IllegalArgumentException("column interval proof must bound at least one interval");
    }
    for (int index = 1; index < splitYCoordinates.size(); index++) {
      if (splitYCoordinates.get(index) <= splitYCoordinates.get(index - 1)) {
        throw new IllegalArgumentException("column proof splits must be strictly increasing");
      }
    }
    for (int index = 0; index < transitionElevations.size(); index++) {
      double transition = transitionElevations.get(index);
      if (!Double.isFinite(transition)
          || (index > 0 && transition <= transitionElevations.get(index - 1))) {
        throw new IllegalArgumentException(
            "column proof transitions must be finite and strictly increasing");
      }
    }
  }

  public int provenUniformIntervals() {
    return splitYCoordinates.size() - 1;
  }
}
