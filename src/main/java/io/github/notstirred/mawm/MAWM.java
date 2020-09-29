package io.github.notstirred.mawm;

import io.github.notstirred.mawm.commands.debug.CommandFreeze;
import io.github.notstirred.mawm.commands.debug.CommandFreezeBox;
import io.github.notstirred.mawm.commands.debug.CommandUnfreeze;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* TODO: everything
RegionCubeIO
    - freeze
    - do converter stuff
drop -> reload all writeFrozenCubes and writeFrozenColumns
 */

@Mod(
        modid = MAWM.MOD_ID,
        name = MAWM.MOD_NAME,
        version = MAWM.VERSION
)
@Mod.EventBusSubscriber(modid = MAWM.MOD_ID)
public class MAWM {

    public static final String MOD_ID = "mawm";
    public static final String MOD_NAME = "Massively Asynchronous World Editor";
    public static final String VERSION = "1.0-SNAPSHOT";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static MAWM INSTANCE;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent evt)
    {
        evt.registerServerCommand(new CommandFreezeBox());
        evt.registerServerCommand(new CommandFreeze());
        evt.registerServerCommand(new CommandUnfreeze());
    }
}
