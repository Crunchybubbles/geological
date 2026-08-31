package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.spatial.ProvinceSpatialIndex;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Candidate-filtered vertical evaluator using conservative analytic transition proofs. */
final class ColumnQueryEngine {
  private final GeologyQueryEngine pointQueries;
  private final Function<Province, ProvinceSpatialIndex> indexResolver;

  ColumnQueryEngine(
      GeologyQueryEngine pointQueries, Function<Province, ProvinceSpatialIndex> indexResolver) {
    this.pointQueries = pointQueries;
    this.indexResolver = indexResolver;
  }

  ColumnQueryResult query(ColumnRequest request) {
    Province province = pointQueries.atlas().provinceAt(request.horizontalPoint());
    List<SpatialCandidate> candidates = indexResolver.apply(province).at(request.horizontalPoint());
    ColumnIntervalProof proof = ColumnTransitionPlanner.plan(province, request, candidates);
    RunAccumulator accumulator = new RunAccumulator();
    int evaluations = 0;
    List<Integer> splits = proof.splitYCoordinates();
    for (int index = 0; index < splits.size() - 1; index++) {
      int start = splits.get(index);
      int end = splits.get(index + 1);
      accumulator.append(start, end, sample(province, request, start));
      evaluations++;
    }
    return new ColumnQueryResult(
        request, province.id(), candidates, proof, accumulator.runs(), evaluations);
  }

  private MaterialState sample(Province province, ColumnRequest request, int blockY) {
    GeologicalSample sample =
        pointQueries.sample(province, new Point3(request.x(), blockY + 0.5, request.z()));
    return MaterialState.from(sample);
  }

  private static final class RunAccumulator {
    private final List<MaterialRun> runs = new ArrayList<>();

    void append(int start, int end, MaterialState state) {
      if (!runs.isEmpty()) {
        MaterialRun previous = runs.getLast();
        if (previous.maxYExclusive() == start && previous.state().equals(state)) {
          runs.set(runs.size() - 1, new MaterialRun(previous.minYInclusive(), end, state));
          return;
        }
      }
      runs.add(new MaterialRun(start, end, state));
    }

    List<MaterialRun> runs() {
      return List.copyOf(runs);
    }
  }
}
