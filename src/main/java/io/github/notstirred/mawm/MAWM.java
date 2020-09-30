package io.github.notstirred.mawm;

import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.MixinCubeProviderServer;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.asm.mixininterfaces.IRegionCubeIO;
import io.github.notstirred.mawm.commands.debug.CommandFreeze;
import io.github.notstirred.mawm.commands.debug.CommandFreezeBox;
import io.github.notstirred.mawm.commands.debug.CommandUnfreeze;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.RegionCubeIO;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/* TODO: everything


before starting converter stuffs
    - wait 'til all dst frozen cubes&columns are saved
    - wait 'til force save frozen src cubes&columns
    - call SharedCachedRegionProvider#clearRegions

do converter stuffs

after ending converter stuffs
    - remove frozen cubes+columns
    - drop -> reload all dstCubes and dstColumns (async)
        - resend reloaded to client
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

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if(event.world.isRemote) return;
        if(((IFreezableWorld) event.world).isFrozen()) {
            IRegionCubeIO regionCubeIO = ((IRegionCubeIO) ((AccessCubeProviderServer) ((WorldServer) event.world).getChunkProvider()).getCubeIO());
            if (!regionCubeIO.hasFrozenColumnsToBeSaved() && !regionCubeIO.hasFrozenCubesToBeSaved()) {
                try {
                    SharedCachedRegionProvider.clearRegions();
                    LOGGER.info("REGIONS CLEARED");
                } catch (IOException e) {
                    LOGGER.fatal(e);
                }
                //Do converter stuff
            }
        }
    }
}
