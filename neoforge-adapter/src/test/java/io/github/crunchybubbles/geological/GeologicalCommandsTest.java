package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class GeologicalCommandsTest {
  @Test
  void registersReadOnlyHereAndColumnDebugPaths() {
    CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    GeologicalCommands.register(dispatcher);

    var geology = dispatcher.getRoot().getChild("geology");
    assertNotNull(geology);
    assertNotNull(geology.getChild("here"));
    assertNotNull(geology.getChild("column"));
    assertNotNull(geology.getChild("column").getChild("x"));
    assertNotNull(geology.getChild("column").getChild("x").getChild("z"));
  }
}
