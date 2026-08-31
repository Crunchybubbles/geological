package io.github.crunchybubbles.geological.cli;

import io.github.crunchybubbles.geological.atlas.GeologyAtlas;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.ColumnPlanBudget;
import io.github.crunchybubbles.geological.query.ColumnPlanComplexity;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase1World;
import io.github.crunchybubbles.geological.query.TileKey;
import io.github.crunchybubbles.geological.query.TileQueryEngine;
import io.github.crunchybubbles.geological.registry.RegistrySnapshot;
import io.github.crunchybubbles.geological.registry.ScientificRegistryCompiler;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class AtlasMeasurements {
  private final long seed;

  AtlasMeasurements(long seed) {
    this.seed = seed;
  }

  Path measure(Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    GeologyQueryEngine query = Phase1World.create(seed);
    GeologyAtlas atlas = query.atlas();
    TileQueryEngine tiles = new TileQueryEngine(query);
    Point2 center = atlas.provinceAt(new Point2(0.0, 0.0)).site();
    List<TileKey> keys = keysAround(center);

    for (int index = 0; index < 3; index++) {
      tiles.query(keys.get(index));
    }

    query.clearCaches();
    long memoryBefore = usedHeapBytes();
    long coldAllocationBefore = currentThreadAllocatedBytes();
    long coldStart = System.nanoTime();
    List<String> coldDigests = keys.stream().map(tiles::query).map(tile -> tile.digest()).toList();
    long coldNanos = System.nanoTime() - coldStart;
    long coldAllocatedBytes = allocationDelta(coldAllocationBefore, currentThreadAllocatedBytes());
    long memoryAfterCold = usedHeapBytes();
    GeologyAtlas.CacheSizes cacheSizes = atlas.cacheSizes();

    long warmAllocationBefore = currentThreadAllocatedBytes();
    long warmStart = System.nanoTime();
    List<String> warmDigests = keys.stream().map(tiles::query).map(tile -> tile.digest()).toList();
    long warmNanos = System.nanoTime() - warmStart;
    long warmAllocatedBytes = allocationDelta(warmAllocationBefore, currentThreadAllocatedBytes());
    if (!coldDigests.equals(warmDigests)) {
      throw new IllegalStateException(
          "cold and warm measurement queries produced different geology");
    }

    List<TileKey> reverseKeys = new ArrayList<>(keys);
    Collections.reverse(reverseKeys);
    query.clearCaches();
    long shuffledStart = System.nanoTime();
    reverseKeys.forEach(tiles::query);
    long reverseOrderNanos = System.nanoTime() - shuffledStart;

    long sampleCount =
        (long) keys.size() * keys.getFirst().samplesPerSide() * keys.getFirst().samplesPerSide();
    long liveDelta = memoryAfterCold - memoryBefore;
    Province referenceProvince = referenceProvince(query);
    Point3 contactLocal =
        referenceProvince
            .geometry()
            .pushForward(referenceProvince.geometry().porphyryCenter(), new AgeKey(92.0, 0));
    Point3 contactWorld = referenceProvince.frame().toWorld(contactLocal);
    ColumnRequest contactRequest = new ColumnRequest(contactWorld.x(), contactWorld.z(), -64, 320);
    ColumnRequest uniformRequest = mostAdaptiveRequest(query, referenceProvince);
    Map<String, Object> contactMeasurement = columnTiming(query, contactRequest, 128);
    Map<String, Object> uniformMeasurement = columnTiming(query, uniformRequest, 128);
    Point3 vmsLocal =
        referenceProvince
            .geometry()
            .pushForward(referenceProvince.geometry().vmsCenter(), new AgeKey(241.0, 0));
    Point3 vmsWorld = referenceProvince.frame().toWorld(vmsLocal);
    Point2 faultWorld =
        referenceProvince
            .frame()
            .toWorld(new Point2(referenceProvince.geometry().fault().planeU(), 0.0));
    List<Map<String, Object>> chunkPlans =
        List.of(
            chunkTiming(query, "porphyry_contact", contactRequest, 16),
            chunkTiming(
                query, "vms_center", new ColumnRequest(vmsWorld.x(), vmsWorld.z(), -64, 320), 16),
            chunkTiming(
                query,
                "fault_damage_zone",
                new ColumnRequest(faultWorld.x(), faultWorld.z(), -64, 320),
                16),
            chunkTiming(query, "background", uniformRequest, 16));
    Map<String, Object> registryMeasurement = registryTiming(128);
    Map<String, Object> report =
        JsonWriter.object(
            "measurementKind",
            "engineering_observation_not_microbenchmark",
            "worldSeed",
            seed,
            "modelVersion",
            Phase1World.MODEL_VERSION,
            "scientificDigest",
            Phase1World.SCIENTIFIC_DIGEST,
            "javaRuntime",
            System.getProperty("java.runtime.version"),
            "os",
            System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            "processors",
            Runtime.getRuntime().availableProcessors(),
            "registryCompile",
            registryMeasurement,
            "tiles",
            keys.size(),
            "surfaceSamplesPerPass",
            sampleCount,
            "cold",
            timingJson(coldNanos, sampleCount, coldAllocatedBytes),
            "warm",
            timingJson(warmNanos, sampleCount, warmAllocatedBytes),
            "reverseOrderCold",
            timingJson(reverseOrderNanos, sampleCount, -1L),
            "approximateSignedLiveHeapDeltaBytes",
            liveDelta,
            "heapUsedAfterColdBytes",
            memoryAfterCold,
            "descriptorCacheSizes",
            JsonWriter.object(
                "macroSites", cacheSizes.macroSites(),
                "provinceSites", cacheSizes.provinceSites(),
                "macroDomains", cacheSizes.macroDomains(),
                "provinces", cacheSizes.provinces(),
                "spatialIndexes", query.spatialIndexCacheSize()),
            "columns",
            JsonWriter.object(
                "iterationsPerColumn", 128,
                "porphyryContact", contactMeasurement,
                "background", uniformMeasurement),
            "chunkPlans",
            chunkPlans,
            "tileDigests",
            coldDigests);
    Path reportPath = outputDirectory.resolve("measurements.json");
    JsonWriter.write(reportPath, report);
    return reportPath;
  }

  private static Province referenceProvince(GeologyQueryEngine query) {
    Point2 origin = new Point2(0.0, 0.0);
    return query
        .atlas()
        .provincesIntersecting(new Bounds2D(-8192.0, -8192.0, 8192.0, 8192.0))
        .stream()
        .filter(province -> province.grammar() == ProvinceGrammar.EXHUMED_FERTILE_RIFT_TO_ARC)
        .min(
            Comparator.comparingDouble(
                    (Province province) -> province.site().squaredDistance(origin))
                .thenComparing(Province::id))
        .orElseThrow();
  }

  private static Map<String, Object> columnTiming(
      GeologyQueryEngine query, ColumnRequest request, int iterations) {
    query.clearCaches();
    long allocatedBefore = currentThreadAllocatedBytes();
    long start = System.nanoTime();
    ColumnQueryResult result = null;
    for (int iteration = 0; iteration < iterations; iteration++) {
      result = query.column(request);
    }
    long nanos = System.nanoTime() - start;
    long allocated = allocationDelta(allocatedBefore, currentThreadAllocatedBytes());
    if (result == null) {
      throw new IllegalStateException("column measurement did not run");
    }
    return JsonWriter.object(
        "totalNanos",
        nanos,
        "nanosecondsPerColumn",
        nanos / (double) iterations,
        "allocatedBytes",
        allocated < 0 ? null : allocated,
        "allocatedBytesPerColumn",
        allocated < 0 ? null : allocated / (double) iterations,
        "runs",
        result.runs().size(),
        "candidates",
        result.candidates().size(),
        "pointEvaluations",
        result.pointEvaluations(),
        "skippedPointEvaluations",
        result.skippedPointEvaluations());
  }

  private static Map<String, Object> registryTiming(int iterations) {
    RegistrySnapshot source = Phase1World.scientificSnapshot();
    long allocatedBefore = currentThreadAllocatedBytes();
    long start = System.nanoTime();
    RegistrySnapshot result = null;
    ScientificRegistryCompiler compiler = new ScientificRegistryCompiler();
    for (int iteration = 0; iteration < iterations; iteration++) {
      result = compiler.compile(source.citations(), source.schemas(), source.definitions());
    }
    long nanos = System.nanoTime() - start;
    long allocated = allocationDelta(allocatedBefore, currentThreadAllocatedBytes());
    if (result == null || !result.digest().equals(source.digest())) {
      throw new IllegalStateException("registry compile measurement changed effective content");
    }
    return JsonWriter.object(
        "iterations",
        iterations,
        "canonicalUtf8Bytes",
        source.canonicalJson().getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
        "nanosecondsPerCompile",
        nanos / (double) iterations,
        "allocatedBytesPerCompile",
        allocated < 0 ? null : allocated / (double) iterations,
        "digest",
        result.digest());
  }

  private static Map<String, Object> chunkTiming(
      GeologyQueryEngine query, String label, ColumnRequest center, int iterations) {
    long originX = chunkOrigin(center.x());
    long originZ = chunkOrigin(center.z());
    query.clearCaches();
    long coldAllocationBefore = currentThreadAllocatedBytes();
    long coldStart = System.nanoTime();
    ChunkExecution cold = executeChunk(query, originX, originZ);
    long coldNanos = System.nanoTime() - coldStart;
    long coldAllocated = allocationDelta(coldAllocationBefore, currentThreadAllocatedBytes());

    executeChunk(query, originX, originZ);
    List<Long> warmNanos = new ArrayList<>();
    List<Long> warmAllocations = new ArrayList<>();
    ChunkExecution latest = null;
    for (int iteration = 0; iteration < iterations; iteration++) {
      long allocationBefore = currentThreadAllocatedBytes();
      long start = System.nanoTime();
      latest = executeChunk(query, originX, originZ);
      warmNanos.add(System.nanoTime() - start);
      long allocated = allocationDelta(allocationBefore, currentThreadAllocatedBytes());
      if (allocated >= 0) {
        warmAllocations.add(allocated);
      }
    }
    if (latest == null || cold.signature() != latest.signature()) {
      throw new IllegalStateException(label + " chunk plan changed between cold and warm queries");
    }
    return JsonWriter.object(
        "label",
        label,
        "originBlockX",
        originX,
        "originBlockZ",
        originZ,
        "columns",
        256,
        "columnHeight",
        384,
        "coldNanos",
        coldNanos,
        "coldAllocatedBytes",
        coldAllocated < 0 ? null : coldAllocated,
        "warmIterations",
        iterations,
        "warmNanosP50",
        percentile(warmNanos, 0.50),
        "warmNanosP95",
        percentile(warmNanos, 0.95),
        "warmNanosP99",
        percentile(warmNanos, 0.99),
        "warmAllocatedBytesP50",
        warmAllocations.isEmpty() ? null : percentile(warmAllocations, 0.50),
        "warmAllocatedBytesP95",
        warmAllocations.isEmpty() ? null : percentile(warmAllocations, 0.95),
        "warmAllocatedBytesP99",
        warmAllocations.isEmpty() ? null : percentile(warmAllocations, 0.99),
        "complexity",
        JsonWriter.object(
            "maximumCandidatesPerColumn", latest.maximumCandidates(),
            "maximumTransitionsPerColumn", latest.maximumTransitions(),
            "maximumPointEvaluationsPerColumn", latest.maximumPointEvaluations(),
            "maximumMaterialRunsPerColumn", latest.maximumMaterialRuns(),
            "totalCandidates", latest.totalCandidates(),
            "totalPointEvaluations", latest.totalPointEvaluations(),
            "totalMaterialRuns", latest.totalMaterialRuns(),
            "diagnosticViolationCount", latest.diagnosticViolations()),
        "resultSignature",
        Long.toUnsignedString(latest.signature(), 16));
  }

  private static ChunkExecution executeChunk(GeologyQueryEngine query, long originX, long originZ) {
    long signature = 0xcbf29ce484222325L;
    int totalCandidates = 0;
    int totalEvaluations = 0;
    int totalRuns = 0;
    int maximumCandidates = 0;
    int maximumTransitions = 0;
    int maximumEvaluations = 0;
    int maximumRuns = 0;
    int diagnosticViolations = 0;
    for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
      for (int offsetX = 0; offsetX < 16; offsetX++) {
        ColumnQueryResult result =
            query.column(
                new ColumnRequest(originX + offsetX + 0.5, originZ + offsetZ + 0.5, -64, 320));
        ColumnPlanComplexity complexity = result.complexity();
        totalCandidates += complexity.candidates();
        totalEvaluations += complexity.pointEvaluations();
        totalRuns += complexity.materialRuns();
        maximumCandidates = StrictMath.max(maximumCandidates, complexity.candidates());
        maximumTransitions = StrictMath.max(maximumTransitions, complexity.transitions());
        maximumEvaluations = StrictMath.max(maximumEvaluations, complexity.pointEvaluations());
        maximumRuns = StrictMath.max(maximumRuns, complexity.materialRuns());
        diagnosticViolations += complexity.violations(ColumnPlanBudget.PHASE1_REVIEW).size();
        signature = (signature ^ result.hashCode()) * 0x100000001b3L;
      }
    }
    return new ChunkExecution(
        signature,
        totalCandidates,
        totalEvaluations,
        totalRuns,
        maximumCandidates,
        maximumTransitions,
        maximumEvaluations,
        maximumRuns,
        diagnosticViolations);
  }

  private static long chunkOrigin(double coordinate) {
    long block = (long) StrictMath.floor(coordinate);
    return Math.floorDiv(block, 16L) * 16L;
  }

  private static long percentile(List<Long> observations, double percentile) {
    if (observations.isEmpty() || !(percentile > 0.0 && percentile <= 1.0)) {
      throw new IllegalArgumentException("percentile observations and probability are required");
    }
    List<Long> sorted = observations.stream().sorted().toList();
    int index = (int) StrictMath.ceil(percentile * sorted.size()) - 1;
    return sorted.get(index);
  }

  private static ColumnRequest mostAdaptiveRequest(GeologyQueryEngine query, Province province) {
    ColumnQueryResult best = null;
    for (int u = -900; u <= 900; u += 300) {
      for (int v = -900; v <= 900; v += 300) {
        Point2 world = province.frame().toWorld(new Point2(u, v));
        ColumnQueryResult candidate =
            query.column(new ColumnRequest(world.x(), world.z(), -64, 320));
        if (candidate.provinceId().equals(province.id())
            && (best == null
                || candidate.skippedPointEvaluations() > best.skippedPointEvaluations())) {
          best = candidate;
        }
      }
    }
    if (best == null) {
      throw new IllegalStateException("could not find a uniform reference column");
    }
    return best.request();
  }

  private static List<TileKey> keysAround(Point2 center) {
    long baseX = (long) StrictMath.floor(center.x()) - 1024;
    long baseZ = (long) StrictMath.floor(center.z()) - 1024;
    List<TileKey> keys = new ArrayList<>();
    for (int z = 0; z < 4; z++) {
      for (int x = 0; x < 4; x++) {
        keys.add(new TileKey(baseX + x * 512L, baseZ + z * 512L, 32, 16));
      }
    }
    return List.copyOf(keys);
  }

  private static Map<String, Object> timingJson(long nanos, long samples, long allocatedBytes) {
    return JsonWriter.object(
        "totalNanos",
        nanos,
        "milliseconds",
        nanos / 1_000_000.0,
        "nanosecondsPerSurfaceSample",
        nanos / (double) samples,
        "currentThreadAllocatedBytes",
        allocatedBytes < 0 ? null : allocatedBytes,
        "allocatedBytesPerSurfaceSample",
        allocatedBytes < 0 ? null : allocatedBytes / (double) samples);
  }

  private static long usedHeapBytes() {
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    return memory.getHeapMemoryUsage().getUsed();
  }

  private static long currentThreadAllocatedBytes() {
    java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
    if (!(platformBean instanceof com.sun.management.ThreadMXBean allocationBean)
        || !allocationBean.isThreadAllocatedMemorySupported()) {
      return -1L;
    }
    if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
      allocationBean.setThreadAllocatedMemoryEnabled(true);
    }
    return allocationBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
  }

  private static long allocationDelta(long before, long after) {
    return before < 0 || after < before ? -1L : after - before;
  }

  private record ChunkExecution(
      long signature,
      int totalCandidates,
      int totalPointEvaluations,
      int totalMaterialRuns,
      int maximumCandidates,
      int maximumTransitions,
      int maximumPointEvaluations,
      int maximumMaterialRuns,
      int diagnosticViolations) {}
}
