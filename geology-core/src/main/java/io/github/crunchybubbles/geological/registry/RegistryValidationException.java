package io.github.crunchybubbles.geological.registry;

import java.util.List;

/** Fatal registry compile failure; invalid scientific content cannot create a world. */
public final class RegistryValidationException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  private final transient List<RegistryDiagnostic> diagnostics;

  RegistryValidationException(List<RegistryDiagnostic> diagnostics) {
    super(message(diagnostics));
    this.diagnostics = List.copyOf(diagnostics);
  }

  public List<RegistryDiagnostic> diagnostics() {
    return diagnostics;
  }

  private static String message(List<RegistryDiagnostic> diagnostics) {
    return diagnostics.stream()
        .sorted()
        .map(diagnostic -> diagnostic.path() + ": " + diagnostic.message())
        .reduce("scientific registry validation failed", (left, right) -> left + "\n" + right);
  }
}
