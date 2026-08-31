package io.github.crunchybubbles.geological.registry;

import java.util.List;

/** Validated scientific content authored for the Phase 1 atlas/query proof. */
public final class Phase1ScientificRegistry {
  private static final String RESOURCE = "/data/geological/registry/phase1-scientific.json";
  private static final String PACKAGE = "geological:kernel/stratigraphic_package_v1";
  private static final String FAULT = "geological:kernel/finite_fault_cosine_taper_v1";
  private static final RegistrySnapshot SNAPSHOT =
      new RegistryJsonLoader().loadResource(Phase1ScientificRegistry.class, RESOURCE);

  public static final double PACKAGE_MAXIMUM_THICKNESS =
      quantity(PACKAGE, "maximum_thickness", ScientificUnit.BLOCK);
  public static final List<Double> PACKAGE_MEMBER_TOP_FRACTIONS =
      List.of(
          quantity(PACKAGE, "basal_fraction", ScientificUnit.FRACTION),
          quantity(PACKAGE, "volcaniclastic_top_fraction", ScientificUnit.FRACTION),
          quantity(PACKAGE, "shale_top_fraction", ScientificUnit.FRACTION));
  public static final double UNCONFORMITY_MAXIMUM_RELIEF =
      quantity(PACKAGE, "unconformity_relief", ScientificUnit.BLOCK);
  public static final double UNCONFORMITY_WEATHERING_THICKNESS =
      quantity(PACKAGE, "weathering_profile", ScientificUnit.BLOCK);
  public static final double FAULT_DAMAGE_HALF_WIDTH =
      quantity(FAULT, "damage_half_width", ScientificUnit.BLOCK);
  public static final double FAULT_VERTICAL_SLIP =
      quantity(FAULT, "vertical_slip", ScientificUnit.BLOCK);

  private Phase1ScientificRegistry() {}

  public static RegistrySnapshot snapshot() {
    return SNAPSHOT;
  }

  private static double quantity(String definition, String parameter, ScientificUnit unit) {
    return SNAPSHOT.requireQuantity(definition, parameter, unit);
  }
}
