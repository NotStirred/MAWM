package io.github.notstirred.mawm;

import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.asm.mixininterfaces.IRegionCubeIO;
import io.github.notstirred.mawm.commands.debug.CommandConvert;
import io.github.notstirred.mawm.commands.debug.CommandFreeze;
import io.github.notstirred.mawm.commands.debug.CommandFreezeBox;
import io.github.notstirred.mawm.commands.debug.CommandUnfreeze;
import io.github.notstirred.mawm.converter.MAWMConverter;
import io.github.notstirred.mawm.util.FreezableBox;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    private static Path srcWorld = Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/New World");
    private static Path dstWorld = Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/mawmWorkingDir");

    List<EditTask> tasks = new ArrayList<>();

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent evt)
    {
        evt.registerServerCommand(new CommandFreezeBox());
        evt.registerServerCommand(new CommandFreeze());
        evt.registerServerCommand(new CommandUnfreeze());
        evt.registerServerCommand(new CommandConvert());

        tasks.add(new EditTask(new BoundingBox(0, 0, 0, 15, 15, 15), new Vector3i(-16, 0,0), EditTask.Type.CUT));
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if(event.world.isRemote) return;

        if(((IFreezableWorld) event.world).getManipulateStage() == IFreezableWorld.ManipulateStage.WAITING_SRC_SAVE) {
            LOGGER.info(event.world.provider.getDimension());
            IRegionCubeIO regionCubeIO = ((IRegionCubeIO) ((AccessCubeProviderServer) ((WorldServer) event.world).getChunkProvider()).getCubeIO());
            if (!regionCubeIO.hasFrozenSrcColumnsToBeSaved() && !regionCubeIO.hasFrozenSrcCubesToBeSaved()) {
                try {
                    ((IFreezableWorld) event.world).setSrcSavingLocked(true);
                    SharedCachedRegionProvider.clearRegions();
                    LOGGER.info("REGIONS CLEARED");
                    ((IFreezableWorld) event.world).setSrcFrozen(true);
                    ((IFreezableWorld) event.world).setManipulateStage(IFreezableWorld.ManipulateStage.CONVERTING);
                    startConverter((WorldServer) event.world);
                } catch (Exception e) {
                    LOGGER.fatal(e);
                }
            } else {
                LOGGER.info("waiting for affected cubes & columns to be saved");
            }
        }
        if(((IFreezableWorld) event.world).getManipulateStage() == IFreezableWorld.ManipulateStage.CONVERT_FINISHED) {
            LOGGER.info(event.world.provider.getDimension());
            ((IFreezableWorld) event.world).setSrcFrozen(false);
            ((IFreezableWorld) event.world).setDstFrozen(true);
            try {
                SharedCachedRegionProvider.clearRegions();
            } catch (IOException e) {
                LOGGER.fatal(e);
            }
            try {
                Files.delete(Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/New World/region3d/-1.0.0.3dr"));
                Files.delete(Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/New World/region3d/0.0.0.3dr"));
                Files.copy(Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/mawmWorkingDir/region3d/-1.0.0.3dr"),
                        Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/New World/region3d/-1.0.0.3dr"));
                Files.copy(Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/mawmWorkingDir/region3d/0.0.0.3dr"),
                        Paths.get("/home/tom/Documents/dev/java/minecraft/MAWM (copy)/run/saves/New World/region3d/0.0.0.3dr"));
                LOGGER.info("Region files copied");
                ((IFreezableWorld) event.world).setManipulateStage(IFreezableWorld.ManipulateStage.REGION_SWAP_FINISHED);
            } catch(IOException e) {
                e.printStackTrace();
            }
            ((IFreezableWorld) event.world).setSrcSavingLocked(false);
            ((IFreezableWorld) event.world).setSrcSaveAddingLocked(false);
            ((IFreezableWorld) event.world).setDstSavingLocked(false);
            ((IFreezableWorld) event.world).setDstSaveAddingLocked(false);
            ((IFreezableCubeProviderServer) event.world.getChunkProvider()).reload();

            ((IFreezableWorld) event.world).setManipulateStage(IFreezableWorld.ManipulateStage.NONE);
        }
    }

    public void convertCommand(WorldServer world) {
        addFreezeRegionsForTasks(world);
        ((IFreezableWorld) world).setDstSavingLocked(true);
        ((IFreezableWorld) world).setDstSaveAddingLocked(true);

        ((IFreezableCubeProviderServer) world.getChunkProvider()).addSrcCubesToSave();
        ((IFreezableWorld) world).setSrcSaveAddingLocked(true);
        ((IFreezableWorld) world).setManipulateStage(IFreezableWorld.ManipulateStage.WAITING_SRC_SAVE);
        //Continued in worldTick event on WAITING_SRC_SAVE
    }

    public static void startConverter(WorldServer world) {
        HeadlessCommandContext context = new HeadlessCommandContext();
        context.setSrcWorld(srcWorld);
        context.setDstWorld(dstWorld);
        context.setConverterName("Relocating");
        context.setInFormat("CubicChunks");
        context.setOutFormat("CubicChunks");
        LOGGER.info(world.provider.getDimension());
        MAWMConverter.convert(context, INSTANCE.tasks, () ->
                ((IFreezableWorld) world).setManipulateStage(IFreezableWorld.ManipulateStage.CONVERT_FINISHED));
    }

    private void addFreezeRegionsForTasks(WorldServer world) {
        tasks.forEach(task -> {
            ((IFreezableWorld) world).addSrcFreezeBox(new FreezableBox(task.getSourceBox().getMinPos(), task.getSourceBox().getMaxPos()));
            if(task.getOffset() != null) {
                ((IFreezableWorld) world).addDstFreezeBox(new FreezableBox(
                        task.getSourceBox().getMinPos().add(task.getOffset()),
                        task.getSourceBox().getMaxPos().add(task.getOffset())
                ));
            }

            switch (task.getType()) {
                case NONE:
                case KEEP:
                case COPY:
                    break;
                case CUT:
                case MOVE:
                case REMOVE:
                    ((IFreezableWorld) world).addDstFreezeBox(new FreezableBox(task.getSourceBox().getMinPos(), task.getSourceBox().getMaxPos()));
                    break;
            }
        });
    }
}
