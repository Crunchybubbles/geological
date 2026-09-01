package io.github.crunchybubbles.geological.petrology;

import java.util.List;
import java.util.TreeSet;

/** Catalog definition of a mineral phase represented by compositional endmembers. */
public record SolidSolutionDefinition(
    String id, SolidSolutionMixingModel mixingModel, List<String> endmemberIds) {
  public SolidSolutionDefinition {
    if (id == null || id.isBlank() || mixingModel == null || endmemberIds == null) {
      throw new IllegalArgumentException("solid-solution definition must be complete");
    }
    TreeSet<String> unique = new TreeSet<>();
    for (String endmemberId : endmemberIds) {
      if (endmemberId == null || endmemberId.isBlank()) {
        throw new IllegalArgumentException("solid-solution endmember IDs must be present");
      }
      if (!unique.add(endmemberId)) {
        throw new IllegalArgumentException(
            "solid-solution endmember IDs must be unique: " + endmemberId);
      }
    }
    if (unique.size() < 2) {
      throw new IllegalArgumentException("solid solutions require at least two endmembers");
    }
    endmemberIds = List.copyOf(unique);
  }
}
