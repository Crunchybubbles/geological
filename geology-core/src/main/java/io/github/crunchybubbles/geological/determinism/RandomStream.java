package io.github.crunchybubbles.geological.determinism;

import io.github.crunchybubbles.geological.model.CellKey;
import java.nio.ByteBuffer;

/** Stateless, domain-separated random access to HMAC-SHA-256 output. */
public final class RandomStream {
  private static final long UNIT_DOUBLE_BITS = 1L << 53;

  private final byte[] rootKey;
  private final String dimensionProfileId;
  private final String namespace;
  private final String objectType;
  private final CellKey homeCell;
  private final long localIndex;

  RandomStream(
      byte[] rootKey,
      String dimensionProfileId,
      String namespace,
      String objectType,
      CellKey homeCell,
      long localIndex) {
    this.rootKey = rootKey.clone();
    this.dimensionProfileId = dimensionProfileId;
    this.namespace = namespace;
    this.objectType = objectType;
    this.homeCell = homeCell;
    this.localIndex = localIndex;
  }

  public StableId stableId() {
    return StableId.first128(digest("identity", 0, 0));
  }

  public byte[] bytes(String purpose, long counter) {
    return digest(requirePurpose(purpose), counter, 0);
  }

  public double unitDouble(String purpose, long counter) {
    long bits = ByteBuffer.wrap(bytes(purpose, counter)).getLong() >>> 11;
    return bits / (double) UNIT_DOUBLE_BITS;
  }

  public double symmetricDouble(String purpose, long counter) {
    return unitDouble(purpose, counter) * 2.0 - 1.0;
  }

  public int boundedInt(String purpose, long counter, int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be positive");
    }
    long range = 1L << 31;
    long limit = range - range % bound;
    for (long attempt = 0; attempt < 1_000_000; attempt++) {
      long candidate =
          ByteBuffer.wrap(digest(requirePurpose(purpose), counter, attempt)).getInt()
              & 0x7fff_ffffL;
      if (candidate < limit) {
        return (int) (candidate % bound);
      }
    }
    throw new IllegalStateException("bounded integer rejection sampling did not converge");
  }

  private byte[] digest(String purpose, long counter, long attempt) {
    return WorldIdentity.hmac(
        rootKey,
        CanonicalCbor.encodeTuple(
            WorldIdentity.KEY_SCHEMA,
            dimensionProfileId,
            namespace,
            objectType,
            homeCell.level(),
            homeCell.x(),
            homeCell.z(),
            localIndex,
            purpose,
            counter,
            attempt));
  }

  private static String requirePurpose(String purpose) {
    if (purpose == null || purpose.isBlank()) {
      throw new IllegalArgumentException("purpose must be present");
    }
    return purpose;
  }
}
