package io.github.crunchybubbles.geological.determinism;

import io.github.crunchybubbles.geological.model.CellKey;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic value noise used only to perturb fields inside causal descriptors. */
public final class Noise2D {
  private final WorldIdentity identity;
  private final String fieldId;
  private final int maximumCacheSize;
  private final Map<CellKey, Double> latticeCache;

  public Noise2D(WorldIdentity identity, String fieldId) {
    this(identity, fieldId, 4096);
  }

  public Noise2D(WorldIdentity identity, String fieldId, int maximumCacheSize) {
    this.identity = identity;
    if (fieldId == null || fieldId.isBlank()) {
      throw new IllegalArgumentException("fieldId must be present");
    }
    if (maximumCacheSize < 0) {
      throw new IllegalArgumentException("maximumCacheSize must not be negative");
    }
    this.fieldId = fieldId;
    this.maximumCacheSize = maximumCacheSize;
    latticeCache =
        new LinkedHashMap<>(StrictMath.max(1, maximumCacheSize), 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<CellKey, Double> eldest) {
            return size() > Noise2D.this.maximumCacheSize;
          }
        };
  }

  public double value(double x, double z, double wavelength) {
    if (!(wavelength > 0.0)) {
      throw new IllegalArgumentException("wavelength must be positive");
    }
    double scaledX = x / wavelength;
    double scaledZ = z / wavelength;
    long x0 = floorToLong(scaledX);
    long z0 = floorToLong(scaledZ);
    double fx = fade(scaledX - x0);
    double fz = fade(scaledZ - z0);
    double a = lerp(lattice(x0, z0), lattice(x0 + 1, z0), fx);
    double b = lerp(lattice(x0, z0 + 1), lattice(x0 + 1, z0 + 1), fx);
    return lerp(a, b, fz);
  }

  public double fractal(double x, double z, double wavelength, int octaves) {
    if (octaves <= 0) {
      throw new IllegalArgumentException("octaves must be positive");
    }
    double total = 0.0;
    double amplitude = 1.0;
    double normalizer = 0.0;
    double currentWavelength = wavelength;
    for (int octave = 0; octave < octaves; octave++) {
      total += amplitude * value(x, z, currentWavelength);
      normalizer += amplitude;
      amplitude *= 0.5;
      currentWavelength *= 0.5;
    }
    return total / normalizer;
  }

  public synchronized void clearCache() {
    latticeCache.clear();
  }

  private synchronized double lattice(long x, long z) {
    CellKey key = new CellKey("noise", x, z);
    if (maximumCacheSize > 0) {
      Double existing = latticeCache.get(key);
      if (existing != null) {
        return existing;
      }
    }
    double created =
        identity.stream("geological", "noise/" + fieldId, key, 0).symmetricDouble("value", 0);
    if (maximumCacheSize > 0) {
      latticeCache.put(key, created);
    }
    return created;
  }

  private static long floorToLong(double value) {
    if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
      throw new IllegalArgumentException("noise coordinate is outside the supported range");
    }
    return (long) StrictMath.floor(value);
  }

  private static double fade(double value) {
    return value * value * (3.0 - 2.0 * value);
  }

  private static double lerp(double a, double b, double amount) {
    return a + (b - a) * amount;
  }
}
