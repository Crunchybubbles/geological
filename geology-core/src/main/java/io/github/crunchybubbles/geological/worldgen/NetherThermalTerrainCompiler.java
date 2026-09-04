package io.github.crunchybubbles.geological.worldgen;

import io.github.crunchybubbles.geological.determinism.Noise2D;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.CellKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic 3-D Nether roof/floor/lava terrain compiler with no Overworld assumptions. */
public final class NetherThermalTerrainCompiler {
  private static final DimensionGeologyProfile PROFILE =
      DimensionGeologyProfiles.require("minecraft:the_nether");
  private static final int MIN_Y = PROFILE.verticalEnvelope().minimumY();
  private static final int MAX_Y_EXCLUSIVE = PROFILE.verticalEnvelope().maximumY() + 1;
  private static final int PROVINCE_CELL_BLOCKS = 512;
  private final WorldIdentity identity;
  private final Noise2D floorNoise;
  private final Noise2D roofNoise;
  private final Noise2D bridgeNoise;
  private final Noise2D lavaNoise;

  private NetherThermalTerrainCompiler(WorldIdentity identity) {
    this.identity = Objects.requireNonNull(identity, "world identity");
    if (!PROFILE.profileId().equals(identity.dimensionProfileId())
        || !PROFILE.version().equals(identity.modelVersion())
        || !PROFILE.scientificDigest().equals(identity.scientificDigest())) {
      throw new IllegalArgumentException(
          "Nether compiler identity does not match the Nether profile");
    }
    floorNoise = new Noise2D(identity, "nether-floor");
    roofNoise = new Noise2D(identity, "nether-roof");
    bridgeNoise = new Noise2D(identity, "nether-bridges");
    lavaNoise = new Noise2D(identity, "nether-lava");
  }

  public static NetherThermalTerrainCompiler from(WorldIdentity identity) {
    return new NetherThermalTerrainCompiler(identity);
  }

  public NetherThermalChunkPlan plan(long chunkX, long chunkZ) {
    long minX = Math.multiplyExact(chunkX, 16L);
    long minZ = Math.multiplyExact(chunkZ, 16L);
    ChunkBlockBounds bounds =
        new ChunkBlockBounds(minX, MIN_Y, minZ, minX + 16L, MAX_Y_EXCLUSIVE, minZ + 16L);
    List<NetherThermalColumnPlan> columns = new ArrayList<>(256);
    for (long blockX = bounds.minX(); blockX < bounds.maxXExclusive(); blockX++) {
      for (long blockZ = bounds.minZ(); blockZ < bounds.maxZExclusive(); blockZ++) {
        columns.add(planColumn(blockX, blockZ));
      }
    }
    return new NetherThermalChunkPlan(chunkX, chunkZ, bounds, columns);
  }

  public NetherThermalColumnPlan planColumn(long blockX, long blockZ) {
    NetherThermalProvinceState province = provinceAt(blockX, blockZ);
    double floorValue = floorNoise.fractal(blockX + 0.5, blockZ + 0.5, 192.0, 3);
    double roofValue = roofNoise.fractal(blockX + 0.5, blockZ + 0.5, 256.0, 3);
    int floorY = clamp(-54 + (int) StrictMath.round(floorValue * 22.0), MIN_Y + 1, 24);
    int roofY =
        clamp(77 + (int) StrictMath.round(roofValue * 42.0), floorY + 16, MAX_Y_EXCLUSIVE - 1);
    int lavaLevelY =
        clamp(
            -31 + (int) StrictMath.round(lavaNoise.value(blockX, blockZ, 128.0) * 7.0),
            MIN_Y,
            roofY - 2);
    boolean bridge = bridgeNoise.value(blockX, blockZ, 96.0) > 0.52;
    List<NetherTerrainInterval> solid = new ArrayList<>();
    solid.add(new NetherTerrainInterval(MIN_Y, floorY + 1));
    if (bridge) {
      int bridgeCenter = floorY + (roofY - floorY) / 2;
      solid.add(new NetherTerrainInterval(bridgeCenter - 2, bridgeCenter + 3));
    }
    solid.add(new NetherTerrainInterval(roofY, MAX_Y_EXCLUSIVE));
    List<NetherTerrainInterval> lava = List.of();
    int lavaMin = floorY + 1;
    int lavaMax = Math.min(roofY, lavaLevelY + 1);
    if (lavaMax > lavaMin) {
      lava = List.of(new NetherTerrainInterval(lavaMin, lavaMax));
    }
    return new NetherThermalColumnPlan(
        blockX,
        blockZ,
        province.provinceId(),
        province.kind(),
        floorY,
        roofY,
        lavaLevelY,
        solid,
        lava,
        bridge);
  }

  public NetherThermalProvinceState provinceAt(long blockX, long blockZ) {
    long provinceCellX = Math.floorDiv(blockX, PROVINCE_CELL_BLOCKS);
    long provinceCellZ = Math.floorDiv(blockZ, PROVINCE_CELL_BLOCKS);
    CellKey cell = new CellKey("nether:province", provinceCellX, provinceCellZ);
    var stream = identity.stream("geological", "nether-thermal-province", cell, 0);
    int kindIndex =
        stream.boundedInt(
            "province-kind", 0, NetherThermalProvinceState.NetherProvinceKind.values().length);
    StableId provinceId = stream.stableId();
    StableId basementId =
        identity.stream("geological", "nether-refractory-basement", cell, 0).stableId();
    StableId magmaId = identity.stream("geological", "nether-magma-province", cell, 0).stableId();
    long heat = 620_000L + Math.round(stream.unitDouble("heat", 0) * 380_000L);
    long volatilePotential = 420_000L + Math.round(stream.unitDouble("volatiles", 0) * 580_000L);
    return new NetherThermalProvinceState(
        provinceId,
        basementId,
        magmaId,
        NetherThermalProvinceState.NetherProvinceKind.values()[kindIndex],
        provinceCellX,
        provinceCellZ,
        heat,
        volatilePotential);
  }

  public DimensionGeologyProfile profile() {
    return PROFILE;
  }

  /** Returns the immutable world identity used for every Nether field and descriptor. */
  public WorldIdentity worldIdentity() {
    return identity;
  }

  private static int clamp(int value, int minimum, int maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }
}
