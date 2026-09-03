package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.model.Lithology;

/**
 * Compact symmetric 3-D fracture-intensity tensor for routing and explainability.
 *
 * <p>Tensor entries are normalized fixed-point weights rather than measured fracture apertures,
 * spacing, or permeability. The matrix is required to be positive semidefinite so every authored
 * state has a physically coherent principal-response proxy.
 */
public record FractureTensorState(
    long xxPpm,
    long yyPpm,
    long zzPpm,
    long xyPpm,
    long xzPpm,
    long yzPpm,
    long intensityPpm,
    long connectivityPpm) {
  public FractureTensorState {
    requireDiagonal(xxPpm, "xx");
    requireDiagonal(yyPpm, "yy");
    requireDiagonal(zzPpm, "zz");
    requireSigned(xyPpm, "xy");
    requireSigned(xzPpm, "xz");
    requireSigned(yzPpm, "yz");
    requireBounded(intensityPpm, "fracture intensity");
    requireBounded(connectivityPpm, "fracture connectivity");
    if (xxPpm + yyPpm + zzPpm != MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("fracture tensor diagonal must close to 1000000");
    }
    if (square(xyPpm) > product(xxPpm, yyPpm)
        || square(xzPpm) > product(xxPpm, zzPpm)
        || square(yzPpm) > product(yyPpm, zzPpm)) {
      throw new IllegalArgumentException("fracture tensor principal minors must be non-negative");
    }
    long determinant =
        product(product(xxPpm, yyPpm), zzPpm)
            + 2L * xyPpm * xzPpm * yzPpm
            - xxPpm * square(yzPpm)
            - yyPpm * square(xzPpm)
            - zzPpm * square(xyPpm);
    if (determinant < 0L) {
      throw new IllegalArgumentException("fracture tensor determinant must be non-negative");
    }
  }

  public static FractureTensorState none() {
    return new FractureTensorState(333_333L, 333_333L, 333_334L, 0L, 0L, 0L, 0L, 0L);
  }

  /** Derives a deterministic tensor proxy from the resolved texture and metamorphic frame. */
  public static FractureTensorState proofFor(
      Lithology lithology,
      RockTexture texture,
      MaterialProcessClass processClass,
      MetamorphicStrainState strainState) {
    if (lithology == null || texture == null || processClass == null || strainState == null) {
      throw new IllegalArgumentException("fracture tensor inputs must be complete");
    }

    Axes axes = axesFor(texture, strainState.frameClass());
    long intensity = intensityFor(lithology, texture, processClass, strainState);
    long connectivity = connectivityFor(texture, processClass, strainState);
    return new FractureTensorState(
        axes.xxPpm(),
        axes.yyPpm(),
        axes.zzPpm(),
        axes.xyPpm(),
        axes.xzPpm(),
        axes.yzPpm(),
        intensity,
        connectivity);
  }

  private static Axes axesFor(RockTexture texture, MetamorphicStrainState.FrameClass frameClass) {
    if (frameClass == MetamorphicStrainState.FrameClass.FRACTURE
        || frameClass == MetamorphicStrainState.FrameClass.FOLIATION
        || frameClass == MetamorphicStrainState.FrameClass.LINEATION
        || texture == RockTexture.FOLIATED_CRYSTALLINE
        || texture == RockTexture.SLATY_PHYLLITIC
        || texture == RockTexture.SCHISTOSE
        || texture == RockTexture.NEMATOBLASTIC) {
      return new Axes(150_000L, 650_000L, 200_000L, 80_000L, 0L, 0L);
    }
    if (texture == RockTexture.SERPENTINIZED_MESH
        || texture == RockTexture.CLASTIC_COARSE
        || texture == RockTexture.CLASTIC_SAND
        || texture == RockTexture.CLASTIC_SILT
        || texture == RockTexture.CLASTIC_MUD
        || texture == RockTexture.SOIL_COLLUVIAL
        || texture == RockTexture.GLACIAL_DIAMICTIC
        || texture == RockTexture.UNCONSOLIDATED_GRANULAR) {
      return new Axes(300_000L, 300_000L, 400_000L, 0L, 40_000L, 0L);
    }
    return new Axes(333_333L, 333_333L, 333_334L, 0L, 0L, 0L);
  }

  private static long intensityFor(
      Lithology lithology,
      RockTexture texture,
      MaterialProcessClass processClass,
      MetamorphicStrainState strainState) {
    long base =
        switch (processClass) {
          case NONE -> 150_000L;
          case ISOCHEMICAL_METAMORPHISM -> 400_000L;
          case HYDROTHERMAL_METASOMATISM -> 600_000L;
          case WEATHERING -> 450_000L;
        };
    if (strainState.frameClass() == MetamorphicStrainState.FrameClass.FRACTURE) {
      base = Math.max(base, 800_000L);
    } else if (strainState.frameClass() == MetamorphicStrainState.FrameClass.FOLIATION
        || strainState.frameClass() == MetamorphicStrainState.FrameClass.LINEATION) {
      base = Math.max(base, 500_000L);
    }
    if (texture == RockTexture.HORNFELSIC
        || texture == RockTexture.MICROCRYSTALLINE_SILICA
        || texture == RockTexture.GRANOBLASTIC
        || lithology == Lithology.QUARTZITE
        || lithology == Lithology.CHERT) {
      base = Math.min(MaterialAssemblage.SCALE, base + 100_000L);
    }
    return base;
  }

  private static long connectivityFor(
      RockTexture texture, MaterialProcessClass processClass, MetamorphicStrainState strainState) {
    long base =
        switch (processClass) {
          case NONE -> 200_000L;
          case ISOCHEMICAL_METAMORPHISM -> 350_000L;
          case HYDROTHERMAL_METASOMATISM -> 700_000L;
          case WEATHERING -> 650_000L;
        };
    if (strainState.frameClass() == MetamorphicStrainState.FrameClass.FRACTURE) {
      base = Math.max(base, 800_000L);
    }
    if (texture == RockTexture.SERPENTINIZED_MESH
        || texture == RockTexture.CLASTIC_COARSE
        || texture == RockTexture.UNCONSOLIDATED_GRANULAR) {
      base = Math.min(MaterialAssemblage.SCALE, base + 100_000L);
    }
    return base;
  }

  private static void requireDiagonal(long value, String name) {
    if (value < 0L || value > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException("fracture tensor " + name + " must lie in [0, 1000000]");
    }
  }

  private static void requireSigned(long value, String name) {
    if (value < -MaterialAssemblage.SCALE || value > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(
          "fracture tensor " + name + " must lie in [-1000000, 1000000]");
    }
  }

  private static void requireBounded(long value, String name) {
    if (value < 0L || value > MaterialAssemblage.SCALE) {
      throw new IllegalArgumentException(name + " must lie in [0, 1000000]");
    }
  }

  private static long square(long value) {
    return Math.multiplyExact(value, value);
  }

  private static long product(long left, long right) {
    return Math.multiplyExact(left, right);
  }

  private record Axes(long xxPpm, long yyPpm, long zzPpm, long xyPpm, long xzPpm, long yzPpm) {}
}
