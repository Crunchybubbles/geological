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
      case "worldgen-benchmark" -> {
        Path report = new WorldgenBenchmarkPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 4 worldgen benchmark to " + report);
      }
      case "exploration-telemetry" -> {
        Path report = new ExplorationTelemetryPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 5 exploration telemetry to " + report);
      }
      case "secondary-weathering" -> {
        Path report = new SecondaryWeatheringPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 6 secondary-weathering review to " + report);
      }
      case "laterite" -> {
        Path report = new LateritePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 6 laterite review to " + report);
      }
      case "secondary-placers" -> {
        Path report = new SecondaryPlacerPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 6 secondary-placer review to " + report);
      }
      case "paleosurface" -> {
        Path report = new PaleosurfacePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 6 paleosurface review to " + report);
      }
      case "glacial" -> {
        Path report = new GlacialPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 6 glacial review to " + report);
      }
      case "greisen" -> {
        Path report = new GreisenPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 greisen review to " + report);
      }
      case "skarn" -> {
        Path report = new SkarnPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 skarn review to " + report);
      }
      case "epithermal" -> {
        Path report = new EpithermalPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 epithermal review to " + report);
      }
      case "orogenic-gold" -> {
        Path report = new OrogenicGoldPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 orogenic-gold review to " + report);
      }
      case "basin-hydrothermal" -> {
        Path report = new BasinHydrothermalPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 basin-hydrothermal review to " + report);
      }
      case "uranium" -> {
        Path report = new UraniumPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 uranium review to " + report);
      }
      case "layered-intrusion" -> {
        Path report = new LayeredIntrusionPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 layered-intrusion review to " + report);
      }
      case "carbonatite-kimberlite" -> {
        Path report = new CarbonatiteKimberlitePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 carbonatite-kimberlite review to " + report);
      }
      case "sedimentary-resources" -> {
        Path report = new SedimentaryResourcePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 sedimentary-resource review to " + report);
      }
      case "geothermal" -> {
        Path report = new GeothermalPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 7 geothermal review to " + report);
      }
      case "nether-thermal" -> {
        Path report = new NetherThermalPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 Nether thermal review to " + report);
      }
      case "nether-resources" -> {
        Path report = new NetherResourcePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 Nether material/resource review to " + report);
      }
      case "end-fragments" -> {
        Path report = new EndFragmentPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 End parent-fragment review to " + report);
      }
      case "end-progression" -> {
        Path report = new EndProgressionPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 End progression review to " + report);
      }
      case "dimension-traces" -> {
        Path report = new DimensionWorldgenTracePacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 cross-dimensional adapter trace to " + report);
      }
      case "dimension-compatibility" -> {
        Path report = new DimensionCompatibilityPacketGenerator(seed).generate(output);
        System.out.println("Wrote Phase 8 cross-dimensional compatibility review to " + report);
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
        "Usage: atlas-cli <generate|measure|materials|worldgen-benchmark|exploration-telemetry|secondary-weathering|laterite|secondary-placers|paleosurface|glacial|greisen|skarn|epithermal|orogenic-gold|basin-hydrothermal|uranium|layered-intrusion|carbonatite-kimberlite|sedimentary-resources|geothermal|nether-thermal|nether-resources|end-fragments|end-progression|dimension-traces|dimension-compatibility> [--seed <long>] [--output <directory>]");
  }
}
