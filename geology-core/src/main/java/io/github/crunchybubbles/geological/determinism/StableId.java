package io.github.crunchybubbles.geological.determinism;

import java.util.Arrays;
import java.util.HexFormat;

/** A collision-checked-in-context 128-bit identifier derived from a full HMAC-SHA-256 output. */
public final class StableId implements Comparable<StableId> {
  public static final int BYTE_LENGTH = 16;

  private final byte[] bytes;

  private StableId(byte[] bytes) {
    if (bytes.length != BYTE_LENGTH) {
      throw new IllegalArgumentException("Stable IDs must contain exactly 128 bits");
    }
    this.bytes = bytes.clone();
  }

  public static StableId first128(byte[] digest) {
    if (digest.length < BYTE_LENGTH) {
      throw new IllegalArgumentException("digest is shorter than 128 bits");
    }
    return new StableId(Arrays.copyOf(digest, BYTE_LENGTH));
  }

  public static StableId parse(String value) {
    return new StableId(HexFormat.of().parseHex(value));
  }

  public byte[] bytes() {
    return bytes.clone();
  }

  @Override
  public int compareTo(StableId other) {
    return Arrays.compareUnsigned(bytes, other.bytes);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof StableId id && Arrays.equals(bytes, id.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return HexFormat.of().formatHex(bytes);
  }
}
