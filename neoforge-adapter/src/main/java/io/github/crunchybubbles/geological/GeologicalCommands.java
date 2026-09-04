package io.github.crunchybubbles.geological;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.DrillCoreLog;
import io.github.crunchybubbles.geological.worldgen.ExplorationMapSnapshot;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebook;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebookEntry;
import io.github.crunchybubbles.geological.worldgen.ExplorationSampleKind;
import io.github.crunchybubbles.geological.worldgen.GeochemicalAnomalyEstimate;
import io.github.crunchybubbles.geological.worldgen.HandSampleIdentification;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldColumnDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldDrillCorePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservation;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservationPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldGeochemicalAnomalyPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldGlacialTransportPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldGreisenColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldGreisenInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldGreisenPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldHandSamplePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldLateriteColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldLateriteInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldLateritePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldMapDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfaceColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfaceInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldPaleosurfacePlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryPlacerPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSecondaryWeatheringPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSample;
import io.github.crunchybubbles.geological.worldgen.OverworldSedimentSampler;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnInterval;
import io.github.crunchybubbles.geological.worldgen.OverworldSkarnPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldVerticalSectionPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldVerticalSectionTrace;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server commands exposing deterministic Overworld geology overlays and player notebooks. */
public final class GeologicalCommands {
  private static final int MAX_MAP_RADIUS = 8;
  private static final int MAX_SECTION_LENGTH = 64;
  private static final int MAX_NOTEBOOK_DISPLAY_ENTRIES = 8;
  private static final int MAX_NOTEBOOK_MAP_MARKERS = 32;
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require("minecraft:overworld");
  private static final WorldgenSnapshot SNAPSHOT = WorldgenSnapshot.forProfile(OVERWORLD);

  private GeologicalCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }

  /** Registers the command tree; kept separate so its shape can be tested without a live server. */
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    Objects.requireNonNull(dispatcher, "command dispatcher")
        .register(
            Commands.literal("geology")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("here").executes(GeologicalCommands::showHere))
                .then(
                    Commands.literal("observations")
                        .executes(GeologicalCommands::showObservationsHere))
                .then(
                    Commands.literal("secondary")
                        .executes(GeologicalCommands::showSecondaryWeatheringHere))
                .then(Commands.literal("laterite").executes(GeologicalCommands::showLateriteHere))
                .then(Commands.literal("placers").executes(GeologicalCommands::showPlacersHere))
                .then(
                    Commands.literal("paleosurface")
                        .executes(GeologicalCommands::showPaleosurfaceHere))
                .then(Commands.literal("glacial").executes(GeologicalCommands::showGlacialHere))
                .then(Commands.literal("greisen").executes(GeologicalCommands::showGreisenHere))
                .then(Commands.literal("skarn").executes(GeologicalCommands::showSkarnHere))
                .then(
                    Commands.literal("hand-sample")
                        .executes(GeologicalCommands::showHandSampleHere))
                .then(
                    Commands.literal("soil")
                        .executes(context -> showSedimentHere(context, ExplorationSampleKind.SOIL)))
                .then(
                    Commands.literal("stream-sediment")
                        .executes(
                            context ->
                                showSedimentHere(context, ExplorationSampleKind.STREAM_SEDIMENT)))
                .then(
                    Commands.literal("heavy-mineral")
                        .executes(
                            context ->
                                showSedimentHere(context, ExplorationSampleKind.HEAVY_MINERAL)))
                .then(
                    Commands.literal("anomaly")
                        .then(
                            Commands.argument("kind", StringArgumentType.word())
                                .executes(GeologicalCommands::showAnomalyHere)))
                .then(
                    Commands.literal("drill")
                        .then(
                            Commands.argument(
                                    "depth",
                                    IntegerArgumentType.integer(
                                        1, OverworldDrillCorePlanner.MAX_CORE_DEPTH_BLOCKS))
                                .executes(GeologicalCommands::showDrillHere)))
                .then(
                    Commands.literal("vertical-section")
                        .then(
                            Commands.argument("axis", StringArgumentType.word())
                                .then(
                                    Commands.argument(
                                            "length",
                                            IntegerArgumentType.integer(
                                                1, OverworldVerticalSectionTrace.MAX_LENGTH))
                                        .then(
                                            Commands.argument(
                                                    "depth",
                                                    IntegerArgumentType.integer(
                                                        1,
                                                        OverworldDrillCorePlanner
                                                            .MAX_CORE_DEPTH_BLOCKS))
                                                .executes(
                                                    GeologicalCommands::showVerticalSectionHere)))))
                .then(
                    Commands.literal("notebook")
                        .executes(GeologicalCommands::showNotebookHere)
                        .then(
                            Commands.literal("record")
                                .executes(context -> recordNotebookHere(context, ""))
                                .then(
                                    Commands.argument("note", StringArgumentType.greedyString())
                                        .executes(
                                            context ->
                                                recordNotebookHere(
                                                    context,
                                                    StringArgumentType.getString(
                                                        context, "note")))))
                        .then(
                            Commands.literal("map")
                                .then(
                                    Commands.argument(
                                            "radius",
                                            IntegerArgumentType.integer(
                                                0, ExplorationMapSnapshot.MAX_RADIUS_BLOCKS))
                                        .executes(GeologicalCommands::showNotebookMapHere)))
                        .then(
                            Commands.literal("forget")
                                .then(
                                    Commands.argument("entryId", StringArgumentType.word())
                                        .executes(GeologicalCommands::forgetNotebookHere))))
                .then(
                    Commands.literal("column")
                        .then(
                            Commands.argument("x", IntegerArgumentType.integer())
                                .then(
                                    Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(GeologicalCommands::showColumn))))
                .then(
                    Commands.literal("map")
                        .then(
                            Commands.argument(
                                    "radius", IntegerArgumentType.integer(0, MAX_MAP_RADIUS))
                                .executes(GeologicalCommands::showMapHere)))
                .then(
                    Commands.literal("section")
                        .then(
                            Commands.argument("axis", StringArgumentType.word())
                                .then(
                                    Commands.argument(
                                            "length",
                                            IntegerArgumentType.integer(1, MAX_SECTION_LENGTH))
                                        .executes(GeologicalCommands::showSectionHere)))));
  }

  private static int showHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    return showColumn(context, position.getX(), position.getZ());
  }

  private static int showObservationsHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldRegolithPlanner regolith =
          planner(source.getLevel(), position.getX(), position.getZ());
      List<OverworldExplorationObservation> observations =
          OverworldExplorationObservationPlanner.from(regolith)
              .plan(position.getX(), position.getZ());
      String summary =
          observations.isEmpty()
              ? "observations none at=(%d,%d)".formatted(position.getX(), position.getZ())
              : observations.stream()
                  .map(OverworldExplorationObservation::summary)
                  .collect(Collectors.joining("; "));
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology observations unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showSecondaryWeatheringHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldSecondaryWeatheringColumnPlan plan =
          OverworldSecondaryWeatheringPlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      OverworldSecondaryWeatheringInterval interval = plan.at(position.getY()).orElse(null);
      String summary =
          interval == null
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizon=none"
              : plan.summary() + " " + interval.summary();
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology secondary weathering unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showLateriteHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldLateriteColumnPlan plan =
          OverworldLateritePlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      OverworldLateriteInterval interval = plan.at(position.getY()).orElse(null);
      String summary =
          interval == null
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizon=none"
              : plan.summary() + " " + interval.summary();
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology laterite unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showPlacersHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldSecondaryPlacerColumnPlan plan =
          OverworldSecondaryPlacerPlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      String atY =
          plan.intervals().stream()
              .filter(
                  interval ->
                      position.getY() >= interval.minYInclusive()
                          && position.getY() < interval.maxYExclusive())
              .map(OverworldSecondaryPlacerInterval::summary)
              .collect(Collectors.joining("; "));
      String summary =
          atY.isBlank()
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") intervals=none"
              : plan.summary() + " " + atY;
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology placers unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showPaleosurfaceHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldPaleosurfaceColumnPlan plan =
          OverworldPaleosurfacePlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      String atY =
          plan.at(position.getY()).stream()
              .map(OverworldPaleosurfaceInterval::summary)
              .collect(Collectors.joining("; "));
      String summary =
          atY.isBlank()
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizons=none"
              : plan.summary() + " " + atY;
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology paleosurface unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showGlacialHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldGlacialTransportColumnPlan plan =
          OverworldGlacialTransportPlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      OverworldGlacialTransportInterval interval = plan.at(position.getY()).orElse(null);
      String summary =
          interval == null
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizon=none"
              : plan.summary() + " " + interval.summary();
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology glacial unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showGreisenHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldGreisenColumnPlan plan =
          OverworldGreisenPlanner.from(planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      OverworldGreisenInterval interval = plan.at(position.getY()).orElse(null);
      String summary =
          interval == null
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizon=none"
              : plan.summary() + " " + interval.summary();
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology greisen unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showSkarnHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldSkarnColumnPlan plan =
          OverworldSkarnPlanner.from(planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      OverworldSkarnInterval interval = plan.at(position.getY()).orElse(null);
      String summary =
          interval == null
              ? plan.summary()
                  + " at=("
                  + position.getX()
                  + ","
                  + position.getY()
                  + ","
                  + position.getZ()
                  + ") horizon=none"
              : plan.summary() + " " + interval.summary();
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(Component.literal("geology skarn unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showHandSampleHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      HandSampleIdentification identification =
          OverworldHandSamplePlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .identifySurface(position.getX(), position.getZ());
      source.sendSuccess(() -> Component.literal(identification.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology hand-sample unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showSedimentHere(
      CommandContext<CommandSourceStack> context, ExplorationSampleKind kind) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    try {
      OverworldSedimentSample sample =
          OverworldSedimentSampler.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .sample(kind, position.getX(), position.getZ());
      source.sendSuccess(() -> Component.literal(sample.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal(
              "geology "
                  + kind.name().toLowerCase(Locale.ROOT)
                  + " unavailable: "
                  + exception.getMessage()));
      return 0;
    }
  }

  private static int showAnomalyHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    ExplorationSampleKind kind;
    try {
      kind = parseSampleKind(StringArgumentType.getString(context, "kind"));
    } catch (IllegalArgumentException exception) {
      source.sendFailure(
          Component.literal("geology anomaly unavailable: " + exception.getMessage()));
      return 0;
    }
    try {
      OverworldGeochemicalAnomalyPlanner anomaly =
          OverworldGeochemicalAnomalyPlanner.from(
              OverworldSedimentSampler.from(
                  planner(source.getLevel(), position.getX(), position.getZ())));
      GeochemicalAnomalyEstimate estimate =
          switch (kind) {
            case SOIL -> anomaly.estimateSoil(position.getX(), position.getZ());
            case STREAM_SEDIMENT ->
                anomaly.estimateStreamSediment(position.getX(), position.getZ());
            case HEAVY_MINERAL -> anomaly.estimateHeavyMineral(position.getX(), position.getZ());
          };
      source.sendSuccess(() -> Component.literal(estimate.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology anomaly unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showDrillHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    CommandSourceStack source = context.getSource();
    int depth = IntegerArgumentType.getInteger(context, "depth");
    try {
      DrillCoreLog log =
          OverworldDrillCorePlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .logSurface(position.getX(), position.getZ(), depth);
      String summary =
          log.summary()
              + " "
              + log.intervals().stream()
                  .map(interval -> interval.summary())
                  .collect(Collectors.joining("; "));
      source.sendSuccess(() -> Component.literal(summary), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(Component.literal("geology drill unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showVerticalSectionHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    String axisName = StringArgumentType.getString(context, "axis");
    int length = IntegerArgumentType.getInteger(context, "length");
    int depth = IntegerArgumentType.getInteger(context, "depth");
    int offset = length / 2;
    Axis axis;
    try {
      axis = Axis.valueOf(axisName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      context
          .getSource()
          .sendFailure(Component.literal("geology vertical-section axis must be X or Z"));
      return 0;
    }
    long originX = axis == Axis.X ? (long) position.getX() - offset : position.getX();
    long originZ = axis == Axis.Z ? (long) position.getZ() - offset : position.getZ();
    try {
      OverworldVerticalSectionTrace trace =
          OverworldVerticalSectionPlanner.from(
                  planner(context.getSource().getLevel(), position.getX(), position.getZ()))
              .section(axis, originX, originZ, length, depth);
      context.getSource().sendSuccess(() -> Component.literal(trace.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      context
          .getSource()
          .sendFailure(
              Component.literal("geology vertical-section unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showNotebookHere(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      source.sendFailure(Component.literal("geology notebook is player-only"));
      return 0;
    }
    try {
      GeologicalDiscoverySavedData data = GeologicalDiscoverySavedData.get(source.getLevel());
      ExplorationNotebook notebook = data.notebook(player.getUUID());
      String entries =
          notebook.entries().stream()
              .limit(MAX_NOTEBOOK_DISPLAY_ENTRIES)
              .map(ExplorationNotebookEntry::summary)
              .collect(Collectors.joining("; "));
      String suffix = entries.isBlank() ? "" : " entries=[" + entries + "]";
      boolean compatible = data.isCompatibleWithCurrentSnapshot();
      source.sendSuccess(
          () -> Component.literal(notebook.summary() + " compatible=" + compatible + suffix),
          false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology notebook unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int recordNotebookHere(CommandContext<CommandSourceStack> context, String note) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      source.sendFailure(Component.literal("geology notebook is player-only"));
      return 0;
    }
    BlockPos position = BlockPos.containing(source.getPosition());
    try {
      List<OverworldExplorationObservation> observations =
          OverworldExplorationObservationPlanner.from(
                  planner(source.getLevel(), position.getX(), position.getZ()))
              .plan(position.getX(), position.getZ());
      if (observations.isEmpty()) {
        source.sendFailure(Component.literal("geology notebook found no observable evidence here"));
        return 0;
      }
      List<ExplorationNotebookEntry> entries =
          observations.stream()
              .map(
                  observation ->
                      ExplorationNotebookEntry.fromObservation(
                          observation, note, OVERWORLD.profileId()))
              .toList();
      GeologicalDiscoverySavedData data = GeologicalDiscoverySavedData.get(source.getLevel());
      ExplorationNotebook notebook = data.record(player.getUUID(), entries);
      source.sendSuccess(
          () ->
              Component.literal(
                  "notebook recorded="
                      + entries.size()
                      + " total="
                      + notebook.entries().size()
                      + " digest="
                      + notebook.digest()),
          false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology notebook unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showNotebookMapHere(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      source.sendFailure(Component.literal("geology notebook map is player-only"));
      return 0;
    }
    BlockPos position = BlockPos.containing(source.getPosition());
    int radius = IntegerArgumentType.getInteger(context, "radius");
    try {
      ExplorationNotebook notebook =
          GeologicalDiscoverySavedData.get(source.getLevel()).notebook(player.getUUID());
      ExplorationMapSnapshot map = notebook.map(position.getX(), position.getZ(), radius);
      String markers =
          map.markers().stream()
              .limit(MAX_NOTEBOOK_MAP_MARKERS)
              .map(
                  marker ->
                      "%s@(%d,%d)"
                          .formatted(marker.evidenceKind(), marker.blockX(), marker.blockZ()))
              .collect(Collectors.joining(", "));
      String suffix = markers.isBlank() ? "" : " markers=[" + markers + "]";
      source.sendSuccess(() -> Component.literal(map.summary() + suffix), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology notebook map unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int forgetNotebookHere(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      source.sendFailure(Component.literal("geology notebook is player-only"));
      return 0;
    }
    String value = StringArgumentType.getString(context, "entryId");
    try {
      boolean removed =
          GeologicalDiscoverySavedData.get(source.getLevel())
              .forget(
                  player.getUUID(),
                  io.github.crunchybubbles.geological.determinism.StableId.parse(value));
      if (!removed) {
        source.sendFailure(Component.literal("geology notebook entry not found: " + value));
        return 0;
      }
      source.sendSuccess(() -> Component.literal("notebook forgot=" + value), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology notebook unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static ExplorationSampleKind parseSampleKind(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "soil" -> ExplorationSampleKind.SOIL;
      case "stream", "stream-sediment", "stream_sediment" -> ExplorationSampleKind.STREAM_SEDIMENT;
      case "heavy", "heavy-mineral", "heavy_mineral" -> ExplorationSampleKind.HEAVY_MINERAL;
      default -> throw new IllegalArgumentException("sample kind must be soil, stream, or heavy");
    };
  }

  private static int showColumn(CommandContext<CommandSourceStack> context) {
    return showColumn(
        context,
        IntegerArgumentType.getInteger(context, "x"),
        IntegerArgumentType.getInteger(context, "z"));
  }

  private static int showColumn(
      CommandContext<CommandSourceStack> context, int blockX, int blockZ) {
    CommandSourceStack source = context.getSource();
    try {
      OverworldColumnDebugTrace trace = trace(source.getLevel(), blockX, blockZ);
      source.sendSuccess(() -> Component.literal(trace.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(Component.literal("geology debug unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showMapHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    int radius = IntegerArgumentType.getInteger(context, "radius");
    return showMap(context, position.getX(), position.getZ(), radius);
  }

  private static int showMap(
      CommandContext<CommandSourceStack> context, int centerX, int centerZ, int radius) {
    CommandSourceStack source = context.getSource();
    try {
      OverworldMapDebugTrace trace = map(source.getLevel(), centerX, centerZ, radius);
      source.sendSuccess(() -> Component.literal(trace.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(Component.literal("geology map unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static int showSectionHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    String axis = StringArgumentType.getString(context, "axis");
    int length = IntegerArgumentType.getInteger(context, "length");
    return showSection(context, position.getX(), position.getZ(), axis, length);
  }

  private static int showSection(
      CommandContext<CommandSourceStack> context,
      int centerX,
      int centerZ,
      String axis,
      int length) {
    CommandSourceStack source = context.getSource();
    try {
      OverworldSectionDebugTrace trace = section(source.getLevel(), centerX, centerZ, axis, length);
      source.sendSuccess(() -> Component.literal(trace.summary()), false);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      source.sendFailure(
          Component.literal("geology section unavailable: " + exception.getMessage()));
      return 0;
    }
  }

  private static OverworldColumnDebugTrace trace(ServerLevel level, int blockX, int blockZ) {
    return trace(planner(level, blockX, blockZ), blockX, blockZ);
  }

  private static OverworldColumnDebugTrace trace(
      OverworldRegolithPlanner regolith, long blockX, long blockZ) {
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(regolith.baseTerrain()).plan(blockX, blockZ);
    OverworldRegolithColumnPlan surface = regolith.plan(blockX, blockZ);
    return OverworldColumnDebugTrace.from(base, air, surface);
  }

  private static OverworldMapDebugTrace map(
      ServerLevel level, int centerX, int centerZ, int radius) {
    if (radius < 0 || radius > MAX_MAP_RADIUS) {
      throw new IllegalArgumentException("map radius must be between 0 and " + MAX_MAP_RADIUS);
    }
    OverworldRegolithPlanner planner = planner(level, centerX, centerZ);
    List<OverworldColumnDebugTrace> columns = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
    for (long blockX = (long) centerX - radius; blockX <= (long) centerX + radius; blockX++) {
      for (long blockZ = (long) centerZ - radius; blockZ <= (long) centerZ + radius; blockZ++) {
        columns.add(trace(planner, blockX, blockZ));
      }
    }
    return new OverworldMapDebugTrace(centerX, centerZ, radius, columns);
  }

  private static OverworldSectionDebugTrace section(
      ServerLevel level, int centerX, int centerZ, String axisName, int length) {
    if (length < 1 || length > MAX_SECTION_LENGTH) {
      throw new IllegalArgumentException(
          "section length must be between 1 and " + MAX_SECTION_LENGTH);
    }
    Axis axis;
    try {
      axis = Axis.valueOf(axisName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("section axis must be X or Z", exception);
    }
    int offset = length / 2;
    long originX = axis == Axis.X ? (long) centerX - offset : centerX;
    long originZ = axis == Axis.Z ? (long) centerZ - offset : centerZ;
    OverworldRegolithPlanner planner = planner(level, centerX, centerZ);
    List<OverworldColumnDebugTrace> columns = new ArrayList<>(length);
    for (int index = 0; index < length; index++) {
      columns.add(
          trace(
              planner,
              originX + (axis == Axis.X ? index : 0),
              originZ + (axis == Axis.Z ? index : 0)));
    }
    return new OverworldSectionDebugTrace(axis, originX, originZ, length, columns);
  }

  private static OverworldRegolithPlanner planner(ServerLevel level, int blockX, int blockZ) {
    Objects.requireNonNull(level, "server level");
    if (!level.dimension().equals(Level.OVERWORLD)) {
      throw new IllegalArgumentException(
          "the geology debug command currently supports Overworld only");
    }
    ChunkPos chunk = new ChunkPos(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    WorldgenExecutionContext execution =
        GeologicalWorldgenAdapter.regolithSurfaceContext(
            level.getSeed(), Level.OVERWORLD, chunk, SNAPSHOT, Runnable::run);
    return OverworldRegolithPlanner.from(execution);
  }
}
