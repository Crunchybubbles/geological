package io.github.crunchybubbles.geological;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldColumnDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservation;
import io.github.crunchybubbles.geological.worldgen.OverworldExplorationObservationPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldMapDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldSectionDebugTrace.Axis;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Read-only server commands exposing deterministic Overworld geology provenance overlays. */
public final class GeologicalCommands {
  private static final int MAX_MAP_RADIUS = 8;
  private static final int MAX_SECTION_LENGTH = 64;
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
