package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.spatial.CandidateKind;
import io.github.crunchybubbles.geological.spatial.ProvinceSpatialIndex;
import io.github.crunchybubbles.geological.spatial.SpatialCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpatialIndexTest {
  @Test
  void finiteCandidatesAreStableFilteredAndGrammarAware() {
    GeologyQueryEngine query = Phase1World.create(7_171L);
    Province fertile =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC);
    Province barren =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    ProvinceSpatialIndex fertileIndex = ProvinceSpatialIndex.compile(fertile);
    ProvinceSpatialIndex repeated = ProvinceSpatialIndex.compile(fertile);
    ProvinceSpatialIndex barrenIndex = ProvinceSpatialIndex.compile(barren);

    assertEquals(fertileIndex.allCandidates(), repeated.allCandidates());
    assertTrue(
        fertileIndex.allCandidates().stream()
            .anyMatch(candidate -> candidate.kind() == CandidateKind.PORPHYRY_SYSTEM));
    assertTrue(
        fertileIndex.allCandidates().stream()
            .anyMatch(candidate -> candidate.kind() == CandidateKind.VMS_DEPOSIT));
    assertTrue(
        barrenIndex.allCandidates().stream()
            .noneMatch(
                candidate ->
                    candidate.kind() == CandidateKind.PORPHYRY_SYSTEM
                        || candidate.kind() == CandidateKind.VMS_DEPOSIT));

    List<SpatialCandidate> atSite = fertileIndex.at(fertile.site());
    assertFalse(atSite.isEmpty());
    assertEquals(
        atSite,
        atSite.stream().sorted(java.util.Comparator.comparing(SpatialCandidate::id)).toList());
    assertTrue(fertileIndex.at(fertile.site().add(20_000.0, 20_000.0)).isEmpty());
  }

  @Test
  void publicBoundedQueryAgreesRegardlessOfCacheStateAndRejectsHugeFootprints() {
    GeologyQueryEngine query = Phase1World.create(99L);
    Bounds2D bounds = new Bounds2D(-512.0, -512.0, 512.0, 512.0);
    List<SpatialCandidate> first = query.spatialCandidates(bounds);
    query.clearCaches();
    assertEquals(first, query.spatialCandidates(bounds));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            query.spatialCandidates(
                new Bounds2D(-1_000_000.0, -1_000_000.0, 1_000_000.0, 1_000_000.0)));
  }
}
