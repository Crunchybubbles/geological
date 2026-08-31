package io.github.crunchybubbles.geological.registry;

/** Precise validation failure tied to a logical schema path. */
public record RegistryDiagnostic(String path, String message)
    implements Comparable<RegistryDiagnostic>, java.io.Serializable {
  private static final long serialVersionUID = 1L;

  public RegistryDiagnostic {
    if (path == null || path.isBlank() || message == null || message.isBlank()) {
      throw new IllegalArgumentException("registry diagnostic must be complete");
    }
  }

  @Override
  public int compareTo(RegistryDiagnostic other) {
    int pathOrder = path.compareTo(other.path);
    return pathOrder != 0 ? pathOrder : message.compareTo(other.message);
  }
}
