package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.PaleosurfaceState;
import java.util.List;

/** Bounded, read-only world-column projection of structural paleosurface refinements. */
public record OverworldPaleosurfaceColumnPlan(
    long blockX,
    long blockZ,
    int minYInclusive,
    int maxYExclusive,
    int solidMaxYExclusive,
    double surfaceElevation,
    StableId provinceId,
    List<PaleosurfaceState> profiles,
    List<OverworldPaleosurfaceInterval> intervals) {
  public OverworldPaleosurfaceColumnPlan {
    if (maxYExclusive <= minYInclusive
        || solidMaxYExclusive < minYInclusive
        || solidMaxYExclusive > maxYExclusive
        || !Double.isFinite(surfaceElevation)
        || provinceId == null
        || profiles == null
        || intervals == null) {
      throw new IllegalArgumentException("paleosurface column plan values are invalid");
    }
    profiles = List.copyOf(profiles);
    if (profiles.size()
            != java.util.Arrays.stream(PaleosurfaceState.RefinementKind.values()).count()
        || profiles.stream().anyMatch(profile -> profile == null)
        || profiles.stream().map(PaleosurfaceState::refinementKind).distinct().count()
            != profiles.size()) {
      throw new IllegalArgumentException(
          "paleosurface columns require one profile per refinement kind");
    }
    intervals = List.copyOf(intervals);
    for (OverworldPaleosurfaceInterval interval : intervals) {
      if (interval.minYInclusive() < minYInclusive
          || interval.maxYExclusive() > solidMaxYExclusive
          || profiles.stream().noneMatch(profile -> profile.equals(interval.profile()))) {
        throw new IllegalArgumentException("paleosurface intervals are inconsistent");
      }
    }
    if (profiles.stream().filter(profile -> profile.status() != FormationStatus.FORMED).count()
            == profiles.size()
        && !intervals.isEmpty()) {
      throw new IllegalArgumentException("barren paleosurface columns cannot carry horizons");
    }
  }

  public boolean hasKind(PaleosurfaceState.RefinementKind kind) {
    return profile(kind).status() == FormationStatus.FORMED
        && intervals.stream().anyMatch(interval -> interval.profile().refinementKind() == kind);
  }

  public PaleosurfaceState profile(PaleosurfaceState.RefinementKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("paleosurface refinement kind is required");
    }
    return profiles.stream()
        .filter(profile -> profile.refinementKind() == kind)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("missing paleosurface refinement kind"));
  }

  public List<OverworldPaleosurfaceInterval> at(int blockY) {
    return intervals.stream()
        .filter(interval -> blockY >= interval.minYInclusive() && blockY < interval.maxYExclusive())
        .toList();
  }

  public long formedProfileCount() {
    return profiles.stream().filter(profile -> profile.status() == FormationStatus.FORMED).count();
  }

  public String summary() {
    return "paleosurface column x=%d z=%d formed=%d intervals=%d"
        .formatted(blockX, blockZ, formedProfileCount(), intervals.size());
  }
}
