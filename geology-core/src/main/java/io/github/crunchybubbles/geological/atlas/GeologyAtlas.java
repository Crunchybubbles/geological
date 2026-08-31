package io.github.crunchybubbles.geological.atlas;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.model.DimensionProfile;
import io.github.crunchybubbles.geological.model.Point2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Infinite random-access atlas backed only by optional bounded descriptor caches. */
public final class GeologyAtlas {
  private static final int MAX_BOUNDED_QUERY_CELLS = 4096;

  private final WorldIdentity identity;
  private final DimensionProfile profile;
  private final AtlasCompiler compiler;
  private final DescriptorCache<CellKey, AtlasSite> macroSiteCache;
  private final DescriptorCache<CellKey, AtlasSite> provinceSiteCache;
  private final DescriptorCache<CellKey, MacroDomain> macroCache;
  private final DescriptorCache<CellKey, Province> provinceCache;

  public GeologyAtlas(WorldIdentity identity, DimensionProfile profile) {
    this(
        identity,
        profile,
        new BoundedDescriptorCache<>(256),
        new BoundedDescriptorCache<>(512),
        new BoundedDescriptorCache<>(128),
        new BoundedDescriptorCache<>(256));
  }

  public GeologyAtlas(
      WorldIdentity identity,
      DimensionProfile profile,
      DescriptorCache<CellKey, MacroDomain> macroCache,
      DescriptorCache<CellKey, Province> provinceCache) {
    this(
        identity,
        profile,
        new BoundedDescriptorCache<>(256),
        new BoundedDescriptorCache<>(512),
        macroCache,
        provinceCache);
  }

  public GeologyAtlas(
      WorldIdentity identity,
      DimensionProfile profile,
      DescriptorCache<CellKey, AtlasSite> macroSiteCache,
      DescriptorCache<CellKey, AtlasSite> provinceSiteCache,
      DescriptorCache<CellKey, MacroDomain> macroCache,
      DescriptorCache<CellKey, Province> provinceCache) {
    this.identity = identity;
    this.profile = profile;
    this.compiler = new AtlasCompiler(identity, profile);
    this.macroSiteCache = macroSiteCache;
    this.provinceSiteCache = provinceSiteCache;
    this.macroCache = macroCache;
    this.provinceCache = provinceCache;
  }

  public WorldIdentity identity() {
    return identity;
  }

  public DimensionProfile profile() {
    return profile;
  }

  public MacroDomain macroDomain(CellKey key) {
    return macroCache.get(key, compiler::compileMacroDomain);
  }

  public Province province(CellKey key) {
    return provinceCache.get(key, compiler::compileProvince);
  }

  public MacroDomain macroDomainAt(Point2 point) {
    CellKey key = nearest(point, "macro", AtlasCompiler.MACRO_CELL_SIZE, true);
    return macroDomain(key);
  }

  public Province provinceAt(Point2 point) {
    CellKey key = nearest(point, "province", AtlasCompiler.PROVINCE_CELL_SIZE, false);
    return province(key);
  }

  public List<Province> provincesIntersecting(Bounds2D bounds) {
    CellKey minimum =
        AtlasCompiler.containing(
            "province", new Point2(bounds.minX(), bounds.minZ()), AtlasCompiler.PROVINCE_CELL_SIZE);
    CellKey maximum =
        AtlasCompiler.containing(
            "province", new Point2(bounds.maxX(), bounds.maxZ()), AtlasCompiler.PROVINCE_CELL_SIZE);
    long width = maximum.x() - minimum.x() + 3;
    long height = maximum.z() - minimum.z() + 3;
    if (width <= 0 || height <= 0 || width > MAX_BOUNDED_QUERY_CELLS / height) {
      throw new IllegalArgumentException("bounded atlas query exceeds the Phase 0 safety cap");
    }
    List<Province> candidates = new ArrayList<>((int) (width * height));
    for (long cellX = minimum.x() - 1; cellX <= maximum.x() + 1; cellX++) {
      for (long cellZ = minimum.z() - 1; cellZ <= maximum.z() + 1; cellZ++) {
        candidates.add(province(new CellKey("province", cellX, cellZ)));
      }
    }
    return candidates.stream().sorted(Comparator.comparing(Province::id)).toList();
  }

  public void clearCaches() {
    macroSiteCache.clear();
    provinceSiteCache.clear();
    macroCache.clear();
    provinceCache.clear();
  }

  public CacheSizes cacheSizes() {
    return new CacheSizes(
        macroSiteCache.size(), provinceSiteCache.size(), macroCache.size(), provinceCache.size());
  }

  private CellKey nearest(Point2 point, String level, long cellSize, boolean macro) {
    CellKey containing = AtlasCompiler.containing(level, point, cellSize);
    CellKey bestCell = null;
    StableId bestId = null;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (long offsetX = -1; offsetX <= 1; offsetX++) {
      for (long offsetZ = -1; offsetZ <= 1; offsetZ++) {
        CellKey candidate = new CellKey(level, containing.x() + offsetX, containing.z() + offsetZ);
        AtlasSite site =
            macro
                ? macroSiteCache.get(candidate, compiler::compileMacroSite)
                : provinceSiteCache.get(candidate, compiler::compileProvinceSite);
        StableId id = site.id();
        double distance = point.squaredDistance(site.point());
        if (distance < bestDistance
            || (distance == bestDistance && (bestId == null || id.compareTo(bestId) < 0))) {
          bestCell = candidate;
          bestId = id;
          bestDistance = distance;
        }
      }
    }
    return bestCell;
  }

  public record CacheSizes(int macroSites, int provinceSites, int macroDomains, int provinces) {}
}
