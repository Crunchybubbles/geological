package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.surface.SurfaceSample;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Transient raster query result with an overlapping border sample for seam comparison. */
public final class AtlasTile {
  private final TileKey key;
  private final List<SurfaceSample> samples;

  public AtlasTile(TileKey key, List<SurfaceSample> samples) {
    this.key = key;
    this.samples = List.copyOf(samples);
    int expected = Math.multiplyExact(key.samplesPerSide(), key.samplesPerSide());
    if (this.samples.size() != expected) {
      throw new IllegalArgumentException("tile sample count does not match its key");
    }
  }

  public TileKey key() {
    return key;
  }

  public SurfaceSample sample(int x, int z) {
    int side = key.samplesPerSide();
    if (x < 0 || z < 0 || x >= side || z >= side) {
      throw new IndexOutOfBoundsException("tile sample index is outside the raster");
    }
    return samples.get(z * side + x);
  }

  public List<SurfaceSample> samples() {
    return samples;
  }

  public String digest() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream output = new DataOutputStream(bytes)) {
        output.writeLong(key.originX());
        output.writeLong(key.originZ());
        output.writeInt(key.intervals());
        output.writeInt(key.spacing());
        for (SurfaceSample sample : samples) {
          output.write(sample.bedrock().provinceId().bytes());
          output.write(sample.bedrock().rockBodyId().bytes());
          output.writeInt(sample.surfaceMaterial().ordinal());
          output.writeInt(sample.surfaceOverprint().ordinal());
          output.writeLong(quantize(sample.fields().elevation()));
          output.writeLong(quantize(sample.fields().uplift()));
          output.writeLong(quantize(sample.fields().weatheringDepth()));
          output.writeLong(quantize(sample.fields().drainage().flowAccumulation()));
          output.writeBoolean(sample.fields().outcrop());
          output.writeBoolean(sample.fields().drainage().sourceLinkedPlacer());
        }
      }
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
    } catch (IOException exception) {
      throw new IllegalStateException("in-memory tile serialization failed", exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }

  private static long quantize(double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("cannot digest a non-finite tile field");
    }
    return (long) StrictMath.rint(value * 4096.0);
  }
}
