package io.github.crunchybubbles.geological.worldgen;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable scientific/configuration/presentation snapshot supplied to worldgen workers. */
public record WorldgenSnapshot(
    String modelVersion,
    String scientificDigest,
    String configurationDigest,
    String presentationDigest,
    String scaleProfileId) {
  public WorldgenSnapshot {
    requireText(modelVersion, "worldgen model version");
    requireDigest(scientificDigest, "scientific digest");
    requireDigest(configurationDigest, "configuration digest");
    requireDigest(presentationDigest, "presentation digest");
    requireText(scaleProfileId, "scale profile ID");
  }

  /** Creates the initial default snapshot for one frozen dimension profile. */
  public static WorldgenSnapshot forProfile(DimensionGeologyProfile profile) {
    Objects.requireNonNull(profile, "dimension geology profile");
    String identity =
        profile.profileId() + "|" + profile.version() + "|" + profile.scaleProfileId();
    return new WorldgenSnapshot(
        profile.version(),
        profile.scientificDigest(),
        digest("geological:worldgen-configuration:v1|" + identity),
        digest("geological:worldgen-presentation:v1|" + identity),
        profile.scaleProfileId());
  }

  public boolean matches(DimensionGeologyProfile profile) {
    Objects.requireNonNull(profile, "dimension geology profile");
    return modelVersion.equals(profile.version())
        && scientificDigest.equals(profile.scientificDigest())
        && scaleProfileId.equals(profile.scaleProfileId());
  }

  private static void requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must be present");
    }
  }

  private static void requireDigest(String value, String label) {
    if (value == null || !value.matches("sha256:[0-9a-f]{64}")) {
      throw new IllegalArgumentException(label + " must be sha256 hex");
    }
  }

  private static String digest(String value) {
    try {
      return "sha256:"
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }
}
