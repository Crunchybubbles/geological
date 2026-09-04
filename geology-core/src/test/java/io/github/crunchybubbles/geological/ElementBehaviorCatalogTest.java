package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ElementBehaviorCatalog;
import io.github.crunchybubbles.geological.petrology.ElementBehaviorCatalog.AffinityClass;
import io.github.crunchybubbles.geological.petrology.ElementBehaviorCatalog.HostClass;
import io.github.crunchybubbles.geological.petrology.ElementBehaviorCatalog.MobilityClass;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ElementBehaviorCatalogTest {
  @Test
  void coversEveryElementWithConditionQualifiedHostsAndMobility() {
    assertEquals(ChemicalElement.values().length, ElementBehaviorCatalog.all().size());
    assertEquals(
        ChemicalElement.values().length,
        new HashSet<>(
                ElementBehaviorCatalog.all().stream().map(profile -> profile.element()).toList())
            .size());
    assertTrue(
        ElementBehaviorCatalog.all().stream()
            .allMatch(
                profile -> !profile.affinities().isEmpty() && !profile.hostClasses().isEmpty()));
    assertEquals(MobilityClass.HIGH, ElementBehaviorCatalog.require(ChemicalElement.S).mobility());
    assertTrue(
        ElementBehaviorCatalog.require(ChemicalElement.CU).affinities().stream()
            .anyMatch(affinity -> affinity.affinity() == AffinityClass.CHALCOPHILE));
    assertTrue(
        ElementBehaviorCatalog.require(ChemicalElement.CU)
            .hostClasses()
            .contains(HostClass.SULFIDE));
  }

  @Test
  void profileOrderingAndLookupAreDeterministic() {
    assertEquals(ElementBehaviorCatalog.all(), ElementBehaviorCatalog.all());
    assertEquals(
        ElementBehaviorCatalog.require(ChemicalElement.AU),
        ElementBehaviorCatalog.all().stream()
            .filter(profile -> profile.element() == ChemicalElement.AU)
            .findFirst()
            .orElseThrow());
    assertThrows(NullPointerException.class, () -> ElementBehaviorCatalog.require(null));
  }

  @Test
  void conditionQualifiedAffinityRejectsDuplicateClassesAndIncompleteProfiles() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElementBehaviorCatalog.ElementBehavior(
                ChemicalElement.CU,
                java.util.List.of(
                    new ElementBehaviorCatalog.ConditionedAffinity(
                        AffinityClass.CHALCOPHILE, "sulfide"),
                    new ElementBehaviorCatalog.ConditionedAffinity(
                        AffinityClass.CHALCOPHILE, "chloride fluid")),
                MobilityClass.CONDITIONAL,
                java.util.Set.of(HostClass.SULFIDE),
                false,
                false));
  }
}
