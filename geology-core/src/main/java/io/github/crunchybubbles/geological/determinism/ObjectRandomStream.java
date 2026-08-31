package io.github.crunchybubbles.geological.determinism;

import java.nio.ByteBuffer;

/** Stateless domain-separated random access keyed by an already established stable object ID. */
public final class ObjectRandomStream {
  private static final long UNIT_DOUBLE_BITS = 1L << 53;

  private final byte[] rootKey;
  private final String dimensionProfileId;
  private final String namespace;
  private final String objectType;
  private final StableId objectId;

  ObjectRandomStream(
      byte[] rootKey,
      String dimensionProfileId,
      String namespace,
      String objectType,
      StableId objectId) {
    this.rootKey = rootKey.clone();
    this.dimensionProfileId = dimensionProfileId;
    this.namespace = namespace;
    this.objectType = objectType;
    this.objectId = objectId;
  }

  public byte[] bytes(String purpose, long counter) {
    return digest(requirePurpose(purpose), counter, 0);
  }

  public double unitDouble(String purpose, long counter) {
    long bits = ByteBuffer.wrap(bytes(purpose, counter)).getLong() >>> 11;
    return bits / (double) UNIT_DOUBLE_BITS;
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
            "geological:object-stream:v1",
            dimensionProfileId,
            namespace,
            objectType,
            objectId.bytes(),
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
