package io.github.crunchybubbles.geological.model;

import java.util.EnumSet;
import java.util.Set;

/** Dimension-neutral capabilities selected by a frozen geological profile. */
public record DimensionProfile(
    String id,
    String premiseClass,
    SurfaceTopology surfaceTopology,
    String chronicleGrammarId,
    Set<ProcessFamily> allowedProcesses) {
  public DimensionProfile {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("profile ID must be present");
    }
    if (premiseClass == null || premiseClass.isBlank()) {
      throw new IllegalArgumentException("premise class must be present");
    }
    if (surfaceTopology == null) {
      throw new IllegalArgumentException("surface topology must be present");
    }
    if (chronicleGrammarId == null || chronicleGrammarId.isBlank()) {
      throw new IllegalArgumentException("chronicle grammar ID must be present");
    }
    if (allowedProcesses == null) {
      throw new IllegalArgumentException("allowed processes must be present");
    }
    allowedProcesses = Set.copyOf(allowedProcesses);
  }

  public static DimensionProfile overworldPhase0() {
    return new DimensionProfile(
        "geological:overworld_phase0",
        "earth_analogue",
        SurfaceTopology.SINGLE_VALUED_SURFACE,
        "geological:fixed_rift_to_arc_proof_v1",
        EnumSet.allOf(ProcessFamily.class));
  }

  public static DimensionProfile overworldPhase1() {
    return new DimensionProfile(
        "geological:overworld_phase1",
        "earth_analogue",
        SurfaceTopology.SINGLE_VALUED_SURFACE,
        "geological:varied_rift_to_arc_grammar_v1",
        EnumSet.allOf(ProcessFamily.class));
  }

  public static DimensionProfile overworldPhase2() {
    return new DimensionProfile(
        "geological:overworld_phase2",
        "earth_analogue",
        SurfaceTopology.SINGLE_VALUED_SURFACE,
        "geological:varied_rift_to_arc_grammar_v1",
        EnumSet.allOf(ProcessFamily.class));
  }

  /** Phase 4 Overworld profile used by the worldgen adapter's terrain-control boundary. */
  public static DimensionProfile overworldPhase4() {
    return new DimensionProfile(
        "geological:overworld_phase4",
        "earth_analogue",
        SurfaceTopology.SINGLE_VALUED_SURFACE,
        "geological:varied_rift_to_arc_grammar_v1",
        EnumSet.allOf(ProcessFamily.class));
  }

  public enum SurfaceTopology {
    SINGLE_VALUED_SURFACE,
    CAVERN_VOLUME,
    BOUNDED_BODIES_IN_VOID
  }
}
