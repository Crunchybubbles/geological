package io.github.crunchybubbles.geological;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldAirFluidPlanner;
import io.github.crunchybubbles.geological.worldgen.OverworldBaseTerrainColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldColumnDebugTrace;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithColumnPlan;
import io.github.crunchybubbles.geological.worldgen.OverworldRegolithPlanner;
import io.github.crunchybubbles.geological.worldgen.WorldgenExecutionContext;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Read-only server commands exposing deterministic geology provenance for one Overworld column. */
public final class GeologicalCommands {
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
                    Commands.literal("column")
                        .then(
                            Commands.argument("x", IntegerArgumentType.integer())
                                .then(
                                    Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(GeologicalCommands::showColumn)))));
  }

  private static int showHere(CommandContext<CommandSourceStack> context) {
    BlockPos position = BlockPos.containing(context.getSource().getPosition());
    return showColumn(context, position.getX(), position.getZ());
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

  private static OverworldColumnDebugTrace trace(ServerLevel level, int blockX, int blockZ) {
    Objects.requireNonNull(level, "server level");
    if (!level.dimension().equals(Level.OVERWORLD)) {
      throw new IllegalArgumentException(
          "the geology debug command currently supports Overworld only");
    }
    ChunkPos chunk = new ChunkPos(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    WorldgenExecutionContext execution =
        GeologicalWorldgenAdapter.regolithSurfaceContext(
            level.getSeed(), Level.OVERWORLD, chunk, SNAPSHOT, Runnable::run);
    OverworldRegolithPlanner regolith = OverworldRegolithPlanner.from(execution);
    OverworldBaseTerrainColumnPlan base = regolith.baseTerrain().plan(blockX, blockZ);
    OverworldAirFluidColumnPlan air =
        OverworldAirFluidPlanner.from(regolith.baseTerrain()).plan(blockX, blockZ);
    OverworldRegolithColumnPlan surface = regolith.plan(blockX, blockZ);
    return OverworldColumnDebugTrace.from(base, air, surface);
  }
}
