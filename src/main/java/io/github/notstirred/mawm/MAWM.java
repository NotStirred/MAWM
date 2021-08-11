package io.github.notstirred.mawm;

import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.asm.mixininterfaces.IRegionCubeIO;
import io.github.notstirred.mawm.commands.MAWMCommands;
import io.github.notstirred.mawm.commands.debug.CommandConvert;
import io.github.notstirred.mawm.commands.debug.CommandFreeze;
import io.github.notstirred.mawm.commands.debug.CommandFreezeBox;
import io.github.notstirred.mawm.commands.debug.CommandUnfreeze;
import io.github.notstirred.mawm.converter.task.TaskRequest;
import io.github.notstirred.mawm.converter.task.source.SimpleTaskSource;
import io.github.notstirred.mawm.util.LimitedFifoQueue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
Stop dst cubes from being saved
Stop new dst cubes & cols adding to saved

Add all src cubes & cols to be saved
Stop new src cubes & cols adding to saved
Wait for all src cubes & cols to be saved
Stop src cubes from being saved
flush

do src freeze

do converter stuff

unfreeze source cubes

freeze dst
flush
move files
unfreeze, drop and reload dst

allow src cubes & cols adding to saved
allow src cubes & cols being saved
allow dst cubes & cols adding to saved
allow dst cubes & cols being saved
 */

/* TODO: changes to the converter
custom reader to allow for piping nbt data straight to it on save with a higher priority than the one read from disk
 */

@Mod(
        modid = MAWM.MOD_ID,
        name = MAWM.MOD_NAME,
        version = MAWM.VERSION,
        acceptableRemoteVersions = "*"
)
@Mod.EventBusSubscriber(modid = MAWM.MOD_ID)
public class MAWM {
    public SimpleTaskSource workingDirectory;
    public Path backupDirectory;
    public static final String MOD_ID = "mawm";
    public static final String MOD_NAME = "Massively Asynchronous World Editor";
    public static final String VERSION = "1.0-SNAPSHOT";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static boolean isQueueMode = false;

    private final Map<UUID, LimitedFifoQueue<TaskRequest>> playerTaskHistory = new HashMap<>();

    @Mod.Instance(MOD_ID)
    public static MAWM INSTANCE;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent evt)
    {
        playerTaskHistory.clear();

        String mainWorldDirectory = evt.getServer().getWorld(0).getSaveHandler().getWorldDirectory().toPath().toAbsolutePath().toString();
        workingDirectory = new SimpleTaskSource(Paths.get(mainWorldDirectory, "mawm/workingdir"));
        backupDirectory = Paths.get(mainWorldDirectory, "mawm/backups");
        evt.registerServerCommand(new CommandFreezeBox());
        evt.registerServerCommand(new CommandFreeze());
        evt.registerServerCommand(new CommandUnfreeze());
        evt.registerServerCommand(new CommandConvert());
        evt.registerServerCommand(new MAWMCommands());
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if(event.world.isRemote) return;
        //TODO: fix issues with event.world possibly not being the world the player is in (nether, end, etc)
        IFreezableWorld world = ((IFreezableWorld) event.world);

        if(world.getManipulateStage() == IFreezableWorld.ManipulateStage.READY) {
            if(world.isTasksExecuteRequested())
                world.taskStart();
            else if(world.isUndoRedoTasksExecuteRequested())
                world.undoRedoStart();
        }
        if(world.getManipulateStage() == IFreezableWorld.ManipulateStage.WAITING_SRC_SAVE) {
            IRegionCubeIO regionCubeIO = ((IRegionCubeIO) ((AccessCubeProviderServer) ((WorldServer) event.world).getChunkProvider()).getCubeIO());
            if (!regionCubeIO.hasFrozenSrcColumnsToBeSaved() && !regionCubeIO.hasFrozenSrcCubesToBeSaved()) {
                try {
                    world.setSrcSavingLocked(true);
                    SharedCachedRegionProvider.clearRegions();
                    LOGGER.trace("REGIONS CLEARED");
                    world.setSrcFrozen(true);
                    world.setManipulateStage(IFreezableWorld.ManipulateStage.CONVERTING);
                    world.startConverter();
                } catch (Exception e) {
                    LOGGER.fatal(e);
                }
            } else {
                LOGGER.trace("waiting for affected cubes & columns to be saved");
            }
        } else if(((IFreezableWorld)event.world).getManipulateStage() == IFreezableWorld.ManipulateStage.WAITING_SRC_SAVE_UNDO_REDO) {
            IRegionCubeIO regionCubeIO = ((IRegionCubeIO) ((AccessCubeProviderServer) ((WorldServer) event.world).getChunkProvider()).getCubeIO());
            if (!regionCubeIO.hasFrozenSrcColumnsToBeSaved() && !regionCubeIO.hasFrozenSrcCubesToBeSaved()) {
                try {
                    world.setSrcSavingLocked(true);
                    SharedCachedRegionProvider.clearRegions();
                    LOGGER.trace("REGIONS CLEARED");
                    world.setSrcFrozen(true);
                    world.setManipulateStage(IFreezableWorld.ManipulateStage.CONVERTING_UNDOREDO);
                    world.startUndoRedoConverter();
                } catch (Exception e) {
                    LOGGER.fatal(e);
                }
            } else {
                LOGGER.trace("waiting for affected cubes & columns to be saved");
            }
        }
        if(world.getManipulateStage() == IFreezableWorld.ManipulateStage.CONVERT_FINISHED || world.getManipulateStage() == IFreezableWorld.ManipulateStage.CONVERT_UNDOREDO_FINISHED) {
            world.setSrcFrozen(false);
            world.setDstFrozen(true);
            try {
                SharedCachedRegionProvider.clearRegions();
            } catch (IOException e) {
                LOGGER.fatal(e);
            }

            if(world.getManipulateStage() == IFreezableWorld.ManipulateStage.CONVERT_FINISHED) {
                ((IFreezableWorld) event.world).swapModifiedRegionFilesForTasks();
            }

            LOGGER.trace("Region files copied");

            world.setSrcSavingLocked(false);
            world.setSrcSaveAddingLocked(false);
            world.setDstSavingLocked(false);
            world.setDstSaveAddingLocked(false);
            IRegionCubeIO cubeIO = (IRegionCubeIO) ((AccessCubeProviderServer) ((WorldServer) event.world).getChunkProvider()).getCubeIO();
            cubeIO.flushDeferred();

            ((IFreezableCubeProviderServer) event.world.getChunkProvider()).reload();

            if(world.hasDeferredRequests()) //If there were any deferred tasks, execute them.
                world.taskStart();
            else if(world.hasDeferredUndoRedoRequests())
                world.undoRedoStart();
            else
                world.setManipulateStage(IFreezableWorld.ManipulateStage.READY);
        }
    }

    public Map<UUID, LimitedFifoQueue<TaskRequest>> getPlayerTaskHistory() {
        return playerTaskHistory;
    }

    public void playerDidTask(TaskRequest taskRequest) {
        playerTaskHistory.computeIfAbsent(((EntityPlayer) taskRequest.getSender()).getUniqueID(), id -> new LimitedFifoQueue<>(10)).push(taskRequest);
    }
}
