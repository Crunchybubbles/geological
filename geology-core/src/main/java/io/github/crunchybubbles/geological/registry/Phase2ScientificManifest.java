package io.github.crunchybubbles.geological.registry;

import io.github.crunchybubbles.geological.petrology.MaterialCatalogSnapshot;
import io.github.crunchybubbles.geological.petrology.Phase2MaterialCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Composite identity manifest retaining the frozen Phase 1 registry plus Phase 2 materials. */
public final class Phase2ScientificManifest {
  private static final MaterialCatalogSnapshot MATERIALS = Phase2MaterialCatalog.snapshot();
  private static final String CANONICAL_JSON =
      "{\"canonical_schema\":\"geological:scientific_manifest:v1\","
          + "\"base_registry_digest\":\""
          + Phase1ScientificRegistry.snapshot().digest()
          + "\",\"material_catalog_digest\":\""
          + MATERIALS.digest()
          + "\"}";
  private static final String DIGEST = "sha256:" + sha256(CANONICAL_JSON);

  private Phase2ScientificManifest() {}

  public static RegistrySnapshot baseRegistry() {
    return Phase1ScientificRegistry.snapshot();
  }

  public static MaterialCatalogSnapshot materials() {
    return MATERIALS;
  }

  public static String canonicalJson() {
    return CANONICAL_JSON;
  }

  public static String digest() {
    return DIGEST;
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("required SHA-256 implementation is unavailable", exception);
    }
  }
}
