package io.github.crunchybubbles.geological;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NeoForge entry point for the platform adapter module. */
@Mod(GeologicalMod.MOD_ID)
public final class GeologicalMod {
  public static final String MOD_ID = "geological";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  public GeologicalMod(IEventBus modBus) {
    GeologicalWorldgenRegistries.register(modBus);
    LOGGER.info("Geological Overworld worldgen adapter initialized");
  }
}
