package io.github.crunchybubbles.geological.determinism;

import io.github.crunchybubbles.geological.model.CellKey;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Frozen inputs from which every descriptor identity and random stream is derived. */
public final class WorldIdentity {
  public static final String KEY_SCHEMA = "geological:key:v1";

  private final long worldSeed;
  private final String modelVersion;
  private final String scientificDigest;
  private final String dimensionProfileId;
  private final byte[] rootKey;

  public WorldIdentity(
      long worldSeed, String modelVersion, String scientificDigest, String dimensionProfileId) {
    this.worldSeed = worldSeed;
    this.modelVersion = requireText(modelVersion, "modelVersion");
    this.scientificDigest = requireText(scientificDigest, "scientificDigest");
    this.dimensionProfileId = requireText(dimensionProfileId, "dimensionProfileId");
    byte[] seedKey =
        CanonicalCbor.encodeTuple(
            "geological:world-seed-key:v1",
            ByteBuffer.allocate(Long.BYTES).putLong(worldSeed).array());
    this.rootKey =
        hmac(
            seedKey,
            CanonicalCbor.encodeTuple(
                "geological:root-key:v1",
                this.modelVersion,
                this.scientificDigest,
                this.dimensionProfileId));
  }

  public long worldSeed() {
    return worldSeed;
  }

  public String modelVersion() {
    return modelVersion;
  }

  public String scientificDigest() {
    return scientificDigest;
  }

  public String dimensionProfileId() {
    return dimensionProfileId;
  }

  public RandomStream stream(
      String namespace, String objectType, CellKey homeCell, long localIndex) {
    return new RandomStream(
        rootKey,
        dimensionProfileId,
        requireText(namespace, "namespace"),
        requireText(objectType, "objectType"),
        Objects.requireNonNull(homeCell, "homeCell"),
        localIndex);
  }

  public ObjectRandomStream objectStream(String namespace, String objectType, StableId objectId) {
    return new ObjectRandomStream(
        rootKey,
        dimensionProfileId,
        requireText(namespace, "namespace"),
        requireText(objectType, "objectType"),
        Objects.requireNonNull(objectId, "objectId"));
  }

  static byte[] hmac(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(message);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException(
          "Required HmacSHA256 implementation is unavailable", exception);
    }
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must be present");
    }
    return value;
  }
}
