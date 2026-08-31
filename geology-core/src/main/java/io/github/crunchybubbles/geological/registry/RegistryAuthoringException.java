package io.github.crunchybubbles.geological.registry;

/** Fatal syntax or shape error in an authored registry document. */
public final class RegistryAuthoringException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  RegistryAuthoringException(String source, String path, String message) {
    super(source + ":" + path + ": " + message);
  }

  RegistryAuthoringException(String source, String message, Throwable cause) {
    super(source + ": " + message, cause);
  }
}
