package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.spatial.ProvinceSpatialIndex;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Candidate-filtered vertical evaluator with conservative voxel refinement near finite objects. */
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
    List<Interval> refinement = refinementIntervals(request, candidates);
    RunAccumulator accumulator = new RunAccumulator();
    int evaluations = 0;
    int nextY = request.minYInclusive();
    for (Interval interval : refinement) {
      if (nextY < interval.start()) {
        MaterialState state = sample(province, request, nextY);
        evaluations++;
        accumulator.append(nextY, interval.start(), state);
      }
      for (int y = interval.start(); y < interval.end(); y++) {
        accumulator.append(y, y + 1, sample(province, request, y));
        evaluations++;
      }
      nextY = interval.end();
    }
    if (nextY < request.maxYExclusive()) {
      accumulator.append(nextY, request.maxYExclusive(), sample(province, request, nextY));
      evaluations++;
    }
    return new ColumnQueryResult(
        request, province.id(), candidates, accumulator.runs(), evaluations);
  }

  private MaterialState sample(Province province, ColumnRequest request, int blockY) {
    GeologicalSample sample =
        pointQueries.sample(province, new Point3(request.x(), blockY + 0.5, request.z()));
    return MaterialState.from(sample);
  }

  private static List<Interval> refinementIntervals(
      ColumnRequest request, List<SpatialCandidate> candidates) {
    List<Interval> intervals =
        candidates.stream()
            .filter(SpatialCandidate::affectsColumnState)
            .map(candidate -> interval(request, candidate))
            .filter(interval -> interval.end() > interval.start())
            .sorted(Comparator.comparingInt(Interval::start).thenComparingInt(Interval::end))
            .toList();
    if (intervals.isEmpty()) {
      return List.of();
    }
    List<Interval> merged = new ArrayList<>();
    int start = intervals.getFirst().start();
    int end = intervals.getFirst().end();
    for (Interval interval : intervals.subList(1, intervals.size())) {
      if (interval.start() <= end) {
        end = StrictMath.max(end, interval.end());
      } else {
        merged.add(new Interval(start, end));
        start = interval.start();
        end = interval.end();
      }
    }
    merged.add(new Interval(start, end));
    return List.copyOf(merged);
  }

  private static Interval interval(ColumnRequest request, SpatialCandidate candidate) {
    double lower = StrictMath.ceil(candidate.bounds().minY() - 0.5);
    double upper = StrictMath.floor(candidate.bounds().maxY() - 0.5) + 1.0;
    int start = (int) StrictMath.max(request.minYInclusive(), lower);
    int end = (int) StrictMath.min(request.maxYExclusive(), upper);
    return new Interval(start, StrictMath.max(start, end));
  }

  private record Interval(int start, int end) {}

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
