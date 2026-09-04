package io.github.crunchybubbles.geological.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs a bounded Overworld generation-order and seam observation against immutable core plans.
 *
 * <p>This is deliberately an engineering observation rather than a pass/fail microbenchmark:
 * timings are reported for the current runtime, while generation-order and seam invariants are hard
 * booleans. The harness has no live server, chunk, or neighbor-generation dependency.
 */
public final class OverworldGenerationBenchmark {
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private static final int COLUMNS_PER_CHUNK = 256;
  private static final int SEAM_COLUMNS_CHECKED = 32;
  private static final int MAX_WARM_ITERATIONS = 64;
  private static final long SHUFFLE_SALT = 0x9e3779b97f4a7c15L;

  private OverworldGenerationBenchmark() {}

  /** Runs one cold serial/shuffled comparison, seam checks, and bounded warm observations. */
  public static Report run(long worldSeed, long chunkX, long chunkZ, int warmIterations) {
    if (warmIterations < 1 || warmIterations > MAX_WARM_ITERATIONS) {
      throw new IllegalArgumentException(
          "warm iterations must be between 1 and " + MAX_WARM_ITERATIONS);
    }
    List<ColumnCoordinate> coordinates = targetCoordinates(chunkX, chunkZ);
    OverworldRegolithPlanner planner = planner(worldSeed, chunkX, chunkZ);

    long coldStart = System.nanoTime();
    Map<ColumnCoordinate, OverworldColumnDebugTrace> serial = generate(planner, coordinates);
    long coldNanos = System.nanoTime() - coldStart;

    long shuffleSeed = shuffleSeed(worldSeed, chunkX, chunkZ);
    List<ColumnCoordinate> shuffledCoordinates = new ArrayList<>(coordinates);
    Collections.shuffle(shuffledCoordinates, new Random(shuffleSeed));
    long shuffledStart = System.nanoTime();
    Map<ColumnCoordinate, OverworldColumnDebugTrace> shuffled =
        generate(planner, shuffledCoordinates);
    long shuffledNanos = System.nanoTime() - shuffledStart;

    List<Long> warmNanos = new ArrayList<>(warmIterations);
    boolean warmResultStable = true;
    for (int iteration = 0; iteration < warmIterations; iteration++) {
      long start = System.nanoTime();
      Map<ColumnCoordinate, OverworldColumnDebugTrace> warm = generate(planner, coordinates);
      warmNanos.add(System.nanoTime() - start);
      warmResultStable &= serial.equals(warm);
    }

    boolean seamStable = seamsStable(worldSeed, chunkX, chunkZ);
    return new Report(
        worldSeed,
        chunkX,
        chunkZ,
        warmIterations,
        shuffleSeed,
        COLUMNS_PER_CHUNK,
        !serial.isEmpty() && serial.equals(shuffled),
        warmResultStable,
        seamStable,
        SEAM_COLUMNS_CHECKED,
        coldNanos,
        shuffledNanos,
        percentile(warmNanos, 0.50),
        percentile(warmNanos, 0.95),
        signature(serial));
  }

  private static Map<ColumnCoordinate, OverworldColumnDebugTrace> generate(
      OverworldRegolithPlanner planner, List<ColumnCoordinate> coordinates) {
    OverworldAirFluidPlanner air = OverworldAirFluidPlanner.from(planner.baseTerrain());
    Map<ColumnCoordinate, OverworldColumnDebugTrace> traces =
        new LinkedHashMap<>(coordinates.size());
    for (ColumnCoordinate coordinate : coordinates) {
      OverworldBaseTerrainColumnPlan base =
          planner.baseTerrain().plan(coordinate.blockX(), coordinate.blockZ());
      OverworldAirFluidColumnPlan airPlan = air.plan(coordinate.blockX(), coordinate.blockZ());
      OverworldRegolithColumnPlan regolith = planner.plan(coordinate.blockX(), coordinate.blockZ());
      traces.put(coordinate, OverworldColumnDebugTrace.from(base, airPlan, regolith));
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(traces));
  }

  private static boolean seamsStable(long worldSeed, long chunkX, long chunkZ) {
    long originX = Math.multiplyExact(chunkX, 16L);
    long originZ = Math.multiplyExact(chunkZ, 16L);
    OverworldRegolithPlanner lower = planner(worldSeed, chunkX, chunkZ);
    OverworldRegolithPlanner right = planner(worldSeed, Math.addExact(chunkX, 1L), chunkZ);
    OverworldRegolithPlanner upper = planner(worldSeed, chunkX, Math.addExact(chunkZ, 1L));
    for (int offset = 0; offset < 16; offset++) {
      long boundaryX = Math.addExact(originX, 16L);
      long blockZ = Math.addExact(originZ, offset);
      if (!trace(lower, boundaryX, blockZ).equals(trace(right, boundaryX, blockZ))) {
        return false;
      }
      long blockX = Math.addExact(originX, offset);
      long boundaryZ = Math.addExact(originZ, 16L);
      if (!trace(lower, blockX, boundaryZ).equals(trace(upper, blockX, boundaryZ))) {
        return false;
      }
    }
    return true;
  }

  private static OverworldColumnDebugTrace trace(
      OverworldRegolithPlanner planner, long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = planner.baseTerrain().plan(blockX, blockZ);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(planner.baseTerrain()).plan(blockX, blockZ);
    return OverworldColumnDebugTrace.from(base, air, planner.plan(blockX, blockZ));
  }

  private static OverworldRegolithPlanner planner(long worldSeed, long chunkX, long chunkZ) {
    WorldgenExecutionContext context =
        new WorldgenExecutionContext(
            WorldgenChunkRequest.forStage(
                worldSeed, OVERWORLD, chunkX, chunkZ, WorldgenStage.REGOLITH_SURFACE_CLUES),
            WorldgenStage.REGOLITH_SURFACE_CLUES,
            WorldgenSnapshot.forProfile(OVERWORLD),
            Runnable::run);
    return OverworldRegolithPlanner.from(context);
  }

  private static List<ColumnCoordinate> targetCoordinates(long chunkX, long chunkZ) {
    long originX = Math.multiplyExact(chunkX, 16L);
    long originZ = Math.multiplyExact(chunkZ, 16L);
    List<ColumnCoordinate> coordinates = new ArrayList<>(COLUMNS_PER_CHUNK);
    for (int offsetX = 0; offsetX < 16; offsetX++) {
      for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
        coordinates.add(
            new ColumnCoordinate(Math.addExact(originX, offsetX), Math.addExact(originZ, offsetZ)));
      }
    }
    return List.copyOf(coordinates);
  }

  private static long shuffleSeed(long worldSeed, long chunkX, long chunkZ) {
    long mixed = worldSeed ^ (chunkX * 0x632be59bd9b4e019L) ^ (chunkZ * 0x8cb92ba72f3d8dd7L);
    mixed ^= SHUFFLE_SALT;
    mixed ^= mixed >>> 30;
    mixed *= 0xbf58476d1ce4e5b9L;
    mixed ^= mixed >>> 27;
    mixed *= 0x94d049bb133111ebL;
    return mixed ^ (mixed >>> 31);
  }

  private static long signature(Map<ColumnCoordinate, OverworldColumnDebugTrace> traces) {
    long signature = 0xcbf29ce484222325L;
    for (Map.Entry<ColumnCoordinate, OverworldColumnDebugTrace> entry : traces.entrySet()) {
      signature = (signature ^ entry.getKey().hashCode()) * 0x100000001b3L;
      signature = (signature ^ entry.getValue().hashCode()) * 0x100000001b3L;
    }
    return signature;
  }

  private static long percentile(List<Long> observations, double probability) {
    List<Long> sorted = observations.stream().sorted().toList();
    int index = (int) StrictMath.ceil(probability * sorted.size()) - 1;
    return sorted.get(Math.max(0, index));
  }

  private record ColumnCoordinate(long blockX, long blockZ) {}

  /** Immutable benchmark result; only invariant booleans are suitable for automated gates. */
  public record Report(
      long worldSeed,
      long chunkX,
      long chunkZ,
      int warmIterations,
      long shuffleSeed,
      int columnsPerChunk,
      boolean generationOrderStable,
      boolean warmResultStable,
      boolean seamStable,
      int seamColumnsChecked,
      long coldNanos,
      long shuffledNanos,
      long warmNanosP50,
      long warmNanosP95,
      long stableSignature) {
    public Report {
      if (warmIterations < 1
          || columnsPerChunk != COLUMNS_PER_CHUNK
          || seamColumnsChecked != SEAM_COLUMNS_CHECKED
          || coldNanos < 0
          || shuffledNanos < 0
          || warmNanosP50 < 0
          || warmNanosP95 < warmNanosP50) {
        throw new IllegalArgumentException("worldgen benchmark report values are invalid");
      }
    }

    public String stableSignatureHex() {
      return Long.toUnsignedString(stableSignature, 16);
    }
  }
}
