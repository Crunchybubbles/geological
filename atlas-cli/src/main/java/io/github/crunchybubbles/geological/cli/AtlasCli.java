package io.github.crunchybubbles.geological.cli;

import java.nio.file.Path;

/** Standalone entry point for geological-core review artifacts and engineering measurements. */
public final class AtlasCli {
  private AtlasCli() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args[0].equals("--help") || args[0].equals("help")) {
      printUsage();
      return;
    }
    String command = args[0];
    long seed = longOption(args, "--seed", 8_675_309L);
    Path output =
        Path.of(stringOption(args, "--output", "build/phase1/" + command))
            .toAbsolutePath()
            .normalize();
    switch (command) {
      case "generate" -> {
        new ReviewPacketGenerator(seed).generate(output);
        System.out.println("Generated geological-core review packet at " + output);
      }
      case "measure" -> {
        Path report = new AtlasMeasurements(seed).measure(output);
        System.out.println("Wrote geological-core measurements to " + report);
      }
      case "materials" -> {
        Path report = new MaterialReviewPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 2 material review to " + report);
      }
      default -> throw new IllegalArgumentException("Unknown command: " + command);
    }
  }

  private static String stringOption(String[] args, String option, String fallback) {
    for (int index = 1; index < args.length; index++) {
      if (args[index].equals(option)) {
        if (index + 1 >= args.length) {
          throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index + 1];
      }
    }
    return fallback;
  }

  private static long longOption(String[] args, String option, long fallback) {
    String value = stringOption(args, option, Long.toString(fallback));
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(option + " must be a signed 64-bit integer", exception);
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: atlas-cli <generate|measure|materials> [--seed <long>] [--output <directory>]");
  }
}
