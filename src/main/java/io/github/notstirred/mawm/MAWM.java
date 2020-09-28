package io.github.notstirred.mawm;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/* TODO: everything
Column
	- readFrozen flag
	- writeFrozen flag
Cube
	- readFrozen flag
	- writeFrozen flag
CubeWatcher
	- isFrozen flag

PlayerCubeMap
	- tick
		l342 filter frozen ColumnWatchers
		l370 filter frozen ColumnWatchers
ChunkGc
	- don't unload frozen cubes
RegionCubeIO
	- for windows bin everything, need a custom SimpleRegionProvider
		- freeze
		- flush columnsToSave & cubesToSave
		- close regions (if windows is bad)
		- do converter stuff
	- for linux, we fine
	    - freeze
	    - do converter stuff
CubeProviderServer
    - anything that wants a cube NOW give it BlankCube (server side instance)
 */

@Mod(
        modid = MAWM.MOD_ID,
        name = MAWM.MOD_NAME,
        version = MAWM.VERSION
)
public class MAWM {

    public static final String MOD_ID = "mawm";
    public static final String MOD_NAME = "Massively Asynchronous World Editor";
    public static final String VERSION = "1.0-SNAPSHOT";

    public static Logger LOGGER;

    /**
     * This is the instance of your mod as created by Forge. It will never be null.
     */
    @Mod.Instance(MOD_ID)
    public static MAWM INSTANCE;

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    /**
     * This is the second initialization event. Register custom recipes
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {

    }
}
