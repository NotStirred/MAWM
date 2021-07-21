package io.github.notstirred.mawm.asm.mixin.core.world.server;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.edittask.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.commands.DualSourceCommandContext;
import io.github.notstirred.mawm.converter.task.MergeTaskRequest;
import io.github.notstirred.mawm.converter.task.TaskRequest;
import io.github.notstirred.mawm.converter.MAWMConverter;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.server.command.TextComponentHelper;
import org.spongepowered.asm.mixin.Mixin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World implements IFreezableWorld, ICubicWorldServer {

    protected MixinWorldServer(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    private boolean tasksExecuteRequested = false;
    private boolean undoRedoTasksExecuteRequested = false;

    //Only one task should be executed at once due to undo states requiring that. Optionally the player could request it for a large operation
    TaskRequest activeTaskRequest;
    LinkedList<TaskRequest> taskRequests = new LinkedList<>();

    MergeTaskRequest activeUndoRedoRequest;
    LinkedList<MergeTaskRequest> undoRedoRequests = new LinkedList<>();

    private List<FreezableBox> srcFreezeBoxes = new ArrayList<>();
    private List<FreezableBox> dstFreezeBoxes = new ArrayList<>();

    IFreezableWorld.ManipulateStage manipulateStage = ManipulateStage.READY;

    private boolean isSrcFrozen = false;
    private boolean isDstFrozen = false;
    private boolean isSrcSavingLocked = false;
    private boolean isSrcSaveAddingLocked = false;
    private boolean isDstSavingLocked = false;
    private boolean isDstSaveAddingLocked = false;

    private static final Vector3i CC_REGION_SIZE = new Vector3i(16, 16, 16);

    @Override
    public void requestTasksExecute() {
        tasksExecuteRequested = true;
    }

    @Override
    public void requestUndoRedoTasksExecute() {
        undoRedoTasksExecuteRequested = true;
    }

    @Override
    public boolean isTasksExecuteRequested() {
        return tasksExecuteRequested;
    }

    @Override
    public boolean isUndoRedoTasksExecuteRequested() {
        return undoRedoTasksExecuteRequested;
    }

    @Override
    public void taskStart() {
        tasksExecuteRequested = false;
        manipulateStage = ManipulateStage.STARTED;

        addDeferredRelocateTasks();

        this.addFreezeRegionsForTasks(activeTaskRequest.getTasks());

        isDstSavingLocked = true;
        isDstSaveAddingLocked = true;

        ((IFreezableCubeProviderServer) chunkProvider).addSrcCubesToSave();
        isSrcSaveAddingLocked = true;
        manipulateStage = ManipulateStage.WAITING_SRC_SAVE;
        //Continued in worldTick event on WAITING_SRC_SAVE
    }

    @Override
    public void undoRedoStart() {
        undoRedoTasksExecuteRequested = false;
        manipulateStage = ManipulateStage.STARTED;

        addDeferredMergeTasks();

        this.addFreezeRegionsForTasks(activeUndoRedoRequest.getTasks());

        isDstSavingLocked = true;
        isDstSaveAddingLocked = true;

        ((IFreezableCubeProviderServer) chunkProvider).addSrcCubesToSave();
        isSrcSaveAddingLocked = true;
        manipulateStage = ManipulateStage.WAITING_SRC_SAVE_UNDO_REDO;
        //Continued in worldTick event on WAITING_SRC_SAVE_UNDO_REDO
    }

    @Override
    public void startConverter() {
        TaskRequest taskRequest = this.activeTaskRequest;

        long startTime = System.nanoTime();

        MAWMConverter.convert(taskRequest, () -> {
                ICommandSender sender = taskRequest.getSender();
                if (sender instanceof EntityPlayer) {
                    MAWM.INSTANCE.playerDidTask(taskRequest);
                }

                setManipulateStage(ManipulateStage.CONVERT_FINISHED);
                double conversionTime = (System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1);

                sendConverterStats(sender, conversionTime);
            },
            throwable -> {
                ICommandSender sender = taskRequest.getSender();
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.execute.error.converter_error"));
                MAWM.LOGGER.fatal("Unrecoverable converter error!", throwable);
            }
        );
    }

    @Override
    public void startUndoRedoConverter() {
        MergeTaskRequest taskRequest = this.activeUndoRedoRequest;

        long startTime = System.nanoTime();

        MAWMConverter.convertUndoRedo(taskRequest, () -> {
                setManipulateStage(ManipulateStage.CONVERT_UNDOREDO_FINISHED);
                double conversionTime = (System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1);

                ICommandSender sender = taskRequest.getSender();
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender,
                    "mawm.execute_undoredo.completed.stats",
                    taskRequest.getTasks().size(),
                    1,
                    conversionTime
                ));
            },
            throwable -> {
                ICommandSender sender = taskRequest.getSender();
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.execute_undoredo.error.converter_error"));
                MAWM.LOGGER.fatal("Unrecoverable converter error!", throwable);
            }
        );
    }

    private void sendConverterStats(ICommandSender sender, double conversionTime) {
        int cubeCount = 0;
        //TODO: add per-task-type cube counting
        for (EditTask completedTask : activeTaskRequest.getTasks()) {
            for (BoundingBox box : completedTask.getSrcBoxes()) {
                Vector3i taskDimensions = box.getMaxPos().sub(box.getMinPos());
                cubeCount += (taskDimensions.getX() + 1) * (taskDimensions.getY() + 1) * (taskDimensions.getZ() + 1);
            }
        }

        double cubesPerSecond = cubeCount / conversionTime;
        double blocksPerSecond = (cubeCount * (16 * 16 * 16)) / conversionTime;
        sender.sendMessage(TextComponentHelper.createComponentTranslation(sender,
            "mawm.execute.completed.stats",
            activeTaskRequest.getTasks().size(),
            1,
            conversionTime,
            Math.floor(cubesPerSecond),
            Math.floor(blocksPerSecond)
        ));
    }

    @Override
    public void swapModifiedRegionFilesForTasks() {
        TaskRequest taskRequest = this.activeTaskRequest;

        File saveLoc = taskRequest.getSrcTaskSource().getPath().toFile(); //saveHandler.getWorldDirectory().getAbsoluteFile();
        File dstLoc = taskRequest.getDstTaskSource().getPath().toFile(); //Paths.get(saveLoc.getParent() + "/mawmWorkingWorld").toFile();

        Path backupLoc = Paths.get(MAWM.INSTANCE.backupDirectory + "");

        if (!Files.exists(backupLoc)) {
            try {
                Files.createDirectories(backupLoc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ICommandSender sender = taskRequest.getSender();
        EntityPlayer player = (EntityPlayer) sender;

        int head = MAWM.INSTANCE.getPlayerTaskHistory().get(player.getUniqueID()).getHead();

        String dstLocRegion = dstLoc + "/region3d/";
        String backRegionDir = backupLoc + "/" + player.getUniqueID() + "/" + head + "/region3d/";
        String backCurrentRegionDir = backupLoc + "/" + player.getUniqueID() + "/" + (head+1) + "/region3d/";
        new File(backRegionDir).mkdirs();
        new File(backCurrentRegionDir).mkdirs();

        taskRequest.getTasks().forEach(task -> getRegionFilesModifiedByTask(task).forEach((vec) -> {
            Path dstLocPath = Paths.get(dstLocRegion + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");

            if(!Files.exists(dstLocPath))
                return; //if the converter didn't output a region at this position

            Path worldVecPath = Paths.get(saveLoc + "/region3d/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");
            Path bakVecPath = Paths.get(backRegionDir + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");
            Path bakCurrentVecPath = Paths.get(backCurrentRegionDir + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");

            //Delete existing backup region
            try {
                Files.delete(bakVecPath);
                MAWM.LOGGER.trace("Deleted existing backup region file");
            } catch(IOException e) {
                MAWM.LOGGER.trace("No backup region file existed to delete at " + bakVecPath);
            }

            //Move src region to the backup location
            try {
                if(taskRequest.shouldMoveFilesBackToSrc())
                    Files.move(worldVecPath, bakVecPath);
                else
                    Files.copy(worldVecPath, bakVecPath, StandardCopyOption.REPLACE_EXISTING);

                MAWM.LOGGER.trace("Moved world region file into backup loc");
            } catch (IOException e) {
                MAWM.LOGGER.trace("Couldn't find existing region file to move to backups at " + worldVecPath + ". This is ok if the region didn't exist before. " + e.getMessage());
            }

            try {
                Files.copy(dstLocPath, bakCurrentVecPath, StandardCopyOption.REPLACE_EXISTING);
                if(taskRequest.shouldMoveFilesBackToSrc())
                    Files.move(dstLocPath, worldVecPath, StandardCopyOption.REPLACE_EXISTING);
                MAWM.LOGGER.trace("Moved output region into world loc");
            } catch (IOException e) {
                MAWM.LOGGER.trace("No region file exists at " + dstLocPath + ", assuming converter had empty region at that location, skipping." + e.getMessage());
            }
        }));
    }

    private Set<Vector3i> getRegionFilesModifiedByTask(EditTask task) {
        Set<Vector3i> vectors = new HashSet<>();

        task.getDstBoxes().forEach(box -> vectors.addAll(getRegionFilesWithinBoundingBox(box)));

        return vectors;
    }

    private Set<Vector3i> getRegionFilesWithinBoundingBox(BoundingBox boundingBox) {
        Set<Vector3i> affectedRegions = new HashSet<>();

        BoundingBox regionBox = boundingBox.asRegionCoords(CC_REGION_SIZE);

        for(int x = regionBox.getMinPos().getX(); x <= regionBox.getMaxPos().getX(); x++) {
            for(int y = regionBox.getMinPos().getY(); y <= regionBox.getMaxPos().getY(); y++) {
                for(int z = regionBox.getMinPos().getZ(); z <= regionBox.getMaxPos().getZ(); z++) {
                    affectedRegions.add(new Vector3i(x, y, z));
                }
            }
        }
        return affectedRegions;
    }

    public void addFreezeRegionsForTasks(List<EditTask> taskRequests) {
        taskRequests.forEach(task -> {
            task.getSrcBoxes().forEach(box -> addSrcFreezeBox(new FreezableBox(box.getMinPos(), box.getMaxPos())));
            task.getDstBoxes().forEach(box -> addDstFreezeBox(new FreezableBox(box.getMinPos(), box.getMaxPos())));
        });
    }

    @Override
    public boolean hasDeferredRequests() {
        return !taskRequests.isEmpty();
    }
    @Override
    public boolean hasDeferredUndoRedoRequests() {
        return !undoRedoRequests.isEmpty();
    }

    public void addDeferredRelocateTasks() {
        activeTaskRequest = taskRequests.removeFirst();
    }
    public void addDeferredMergeTasks() {
        activeUndoRedoRequest = undoRedoRequests.removeFirst();
    }

    @Override
    public void addTask(TaskRequest taskRequest) {
        taskRequests.add(taskRequest);
    }

    @Override
    public void addUndoRedoTask(MergeTaskRequest taskRequest) {
        undoRedoRequests.add(taskRequest);
    }

    @Override
    public ManipulateStage getManipulateStage() {
        return this.manipulateStage;
    }
    @Override
    public void setManipulateStage(ManipulateStage stage) {
        this.manipulateStage = stage;
    }

    @Override
    public boolean isSrcSavingLocked() {
        return isSrcSavingLocked;
    }
    @Override
    public void setSrcSavingLocked(boolean state) {
        isSrcSavingLocked = state;
    }

    @Override
    public boolean isSrcSaveAddingLocked() {
        return isSrcSaveAddingLocked;
    }
    @Override
    public void setSrcSaveAddingLocked(boolean state) {
        isSrcSaveAddingLocked = state;
    }

    @Override
    public boolean isDstSavingLocked() {
        return isDstSavingLocked;
    }
    @Override
    public void setDstSavingLocked(boolean state) {
        isDstSavingLocked = state;
    }

    @Override
    public boolean isDstSaveAddingLocked() {
        return isDstSaveAddingLocked;
    }
    @Override
    public void setDstSaveAddingLocked(boolean state) {
        isDstSaveAddingLocked = state;
    }

    @Override
    public boolean isSrcFrozen() {
        return isSrcFrozen;
    }
    @Override
    public void setSrcFrozen(boolean state) {
        isSrcFrozen = state;
    }

    @Override
    public boolean isDstFrozen() {
        return isDstFrozen;
    }
    @Override
    public void setDstFrozen(boolean state) {
        isDstFrozen = state;
    }

    @Override
    public boolean isCubeFrozen(int x, int y, int z) {
        if(isSrcFrozen || isDstFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z)) || dstFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeDst(int x, int y, int z, boolean checkFrozen) {
        if(checkFrozen) {
            if (isDstFrozen) {
                return dstFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
            }
        }
        else
            return dstFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeSrc(int x, int y, int z, boolean checkFrozen) {
        if(checkFrozen) {
            if (isSrcFrozen) {
                return srcFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
            }
        }
        else
            return dstFreezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isColumnFrozen(int x, int z) {
        if(isSrcFrozen || isDstFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z)) || dstFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnDst(int x, int z, boolean checkFrozen) {
        if(checkFrozen) {
            if (isDstFrozen)
                return dstFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        } else
            return dstFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnSrc(int x, int z, boolean checkFrozen) {
        if(checkFrozen) {
            if (isSrcFrozen)
                return srcFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        } else
            return srcFreezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        return false;
    }

    @Override
    public boolean is2dRegionDst(EntryLocation2D entry) {
        if(isDstFrozen)
            return dstFreezeBoxes.stream().anyMatch(b -> b.is2dRegionFrozen(entry));
        return false;
    }
    @Override
    public boolean is2dRegionSrc(EntryLocation2D entry) {
        if(isSrcFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.is2dRegionFrozen(entry));
        return false;
    }
    @Override
    public boolean is2dRegionFrozen(EntryLocation2D entry) {
        if(isSrcFrozen || isDstFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.is2dRegionFrozen(entry)) || dstFreezeBoxes.stream().anyMatch(b -> b.is2dRegionFrozen(entry));
        return false;
    }

    @Override
    public boolean is3dRegionDst(EntryLocation3D entry) {
        if(isDstFrozen)
            return dstFreezeBoxes.stream().anyMatch(b -> b.is3dRegionFrozen(entry));
        return false;
    }
    @Override
    public boolean is3dRegionSrc(EntryLocation3D entry) {
        if(isSrcFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.is3dRegionFrozen(entry));
        return false;
    }
    @Override
    public boolean is3dRegionFrozen(EntryLocation3D entry) {
        if(isSrcFrozen || isDstFrozen)
            return srcFreezeBoxes.stream().anyMatch(b -> b.is3dRegionFrozen(entry)) || dstFreezeBoxes.stream().anyMatch(b -> b.is3dRegionFrozen(entry));
        return false;
    }

    @Override
    public void addSrcFreezeBox(FreezableBox box) {
        this.srcFreezeBoxes.add(box);
    }
    @Override
    public void addDstFreezeBox(FreezableBox box) {
        this.dstFreezeBoxes.add(box);
    }

    @Override
    public void clearSrcBoxes() {
        srcFreezeBoxes = new ArrayList<>();
    }
    @Override
    public void clearDstBoxes() {
        dstFreezeBoxes = new ArrayList<>();
    }
}