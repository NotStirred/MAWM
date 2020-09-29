package io.github.notstirred.mawm;

import com.google.common.eventbus.Subscribe;
import io.github.notstirred.mawm.commands.debug.CommandFreezeBox;
import io.github.notstirred.mawm.commands.debug.CommandUnfreeze;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.command.ForgeCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* TODO: everything
CubeWatcher
	- isFrozen flag

PlayerCubeMap
	- tick
		l342 filter frozen ColumnWatchers
		l370 filter frozen ColumnWatchers
RegionCubeIO
	- for windows bin everything, need a custom SimpleRegionProvider
		- freeze
		- flush columnsToSave & cubesToSave
		- close regions (if windows is bad)
		- do converter stuff
	- for linux, we fine
	    - freeze
	    - do converter stuff
drop newFrozenCubes and Columns
drop -> reload all writeFrozenCubes and Columns
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
        evt.registerServerCommand(new CommandUnfreeze());
    }
}
