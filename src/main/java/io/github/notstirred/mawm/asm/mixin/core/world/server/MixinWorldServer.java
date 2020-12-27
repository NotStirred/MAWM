package io.github.notstirred.mawm.asm.mixin.core.world.server;

import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.commands.DualSourceCommandContext;
import io.github.notstirred.mawm.converter.MAWMConverter;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.notstirred.mawm.util.MutablePair;
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
    private boolean undoTasksExecuteRequested = false;

    List<MutablePair<ICommandSender, EditTask>> activePlayerTaskPairs = new ArrayList<>();
    List<MutablePair<ICommandSender, EditTask>> deferredPlayerTaskPairs = new ArrayList<>();

    List<MutablePair<ICommandSender, EditTask>> activeUndoPlayerTaskPairs = new ArrayList<>();
    List<MutablePair<ICommandSender, EditTask>> deferredUndoPlayerTaskPairs = new ArrayList<>();

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
    public void requestUndoTasksExecute() {
        undoTasksExecuteRequested = true;
    }

    @Override
    public boolean isTasksExecuteRequested() {
        return tasksExecuteRequested;
    }

    @Override
    public boolean isUndoTasksExecuteRequested() {
        return undoTasksExecuteRequested;
    }

    @Override
    public void convertCommand() {
        tasksExecuteRequested = false;
        manipulateStage = ManipulateStage.STARTED;

        addDeferredTasks();
        activePlayerTaskPairs.forEach(pair -> {
            if (pair.getKey() instanceof EntityPlayer)
                MAWM.INSTANCE.playerDidTask((EntityPlayer) pair.getKey(), pair.getValue());
        });

        this.addFreezeRegionsForTasks(activePlayerTaskPairs);

        isDstSavingLocked = true;
        isDstSaveAddingLocked = true;

        ((IFreezableCubeProviderServer) chunkProvider).addSrcCubesToSave();
        isSrcSaveAddingLocked = true;
        manipulateStage = ManipulateStage.WAITING_SRC_SAVE;
        //Continued in worldTick event on WAITING_SRC_SAVE
    }
    @Override
    public void undoConvertCommand() {
        undoTasksExecuteRequested = false;
        manipulateStage = ManipulateStage.STARTED;

        addDeferredUndoTasks();

        this.addFreezeRegionsForTasks(activeUndoPlayerTaskPairs);

        isDstSavingLocked = true;
        isDstSaveAddingLocked = true;

        ((IFreezableCubeProviderServer) chunkProvider).addSrcCubesToSave();
        isSrcSaveAddingLocked = true;
        manipulateStage = ManipulateStage.WAITING_SRC_SAVE_UNDO;
        //Continued in worldTick event on WAITING_SRC_SAVE_UNDO
    }

    @Override
    public void startConverter() {
        HeadlessCommandContext context = new HeadlessCommandContext();

        Path srcWorld = saveHandler.getWorldDirectory().toPath();
        Path dstWorld = Paths.get(saveHandler.getWorldDirectory().getParent() + "/mawmWorkingWorld");

        context.setSrcWorld(srcWorld);
        context.setDstWorld(dstWorld);

        List<EditTask> tasks = new ArrayList<>();
        activePlayerTaskPairs.forEach(pair -> tasks.add(pair.getValue()));

        long startTime = System.nanoTime();

        MAWMConverter.convert(context, tasks, () -> {
                manipulateStage = ManipulateStage.CONVERT_FINISHED;
                double conversionTime = (System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1);

                int playerCount = new HashSet<>(Arrays.asList(activePlayerTaskPairs.toArray())).size();

                int cubeCount = 0;
                for (MutablePair<ICommandSender, EditTask> playerTaskPair : activePlayerTaskPairs) {
                    BoundingBox sourceBox = playerTaskPair.getValue().getSourceBox();
                    Vector3i taskDimensions = sourceBox.getMaxPos().sub(sourceBox.getMinPos());
                    cubeCount += (taskDimensions.getX()+1) * (taskDimensions.getY()+1) * (taskDimensions.getZ()+1);
                    //TODO: add per-task-type cube counting
                }

                double cubesPerSecond = cubeCount / conversionTime;
                double blocksPerSecond = (cubeCount * (16 * 16 * 16)) / conversionTime;
                activePlayerTaskPairs.forEach(pair ->
                    pair.getKey().sendMessage(TextComponentHelper.createComponentTranslation(pair.getKey(),
                        "mawm.execute.completed.stats",
                        activePlayerTaskPairs.size(),
                        playerCount,
                        conversionTime,
                        Math.floor(cubesPerSecond),
                        Math.floor(blocksPerSecond)
                    ))
                );
            },
            throwable -> activePlayerTaskPairs.forEach(pair -> {
                pair.getKey().sendMessage(TextComponentHelper.createComponentTranslation(pair.getKey(), "mawm.execute.error.converter_error"));
                MAWM.LOGGER.fatal("Unrecoverable converter error!", throwable);
            })
        );
    }

    @Override
    public void startUndoConverter() {
        DualSourceCommandContext context = new DualSourceCommandContext();

        Path srcWorld = Paths.get(saveHandler.getWorldDirectory().getAbsolutePath());
        String worldName = saveHandler.getWorldDirectory().getName();
        Path backupWorldDir = Paths.get(srcWorld.getParent() + "/" + worldName + ".bak");

        context.setFallbackWorld(srcWorld);
        context.setPriorityWorld(backupWorldDir);
        context.setDstWorld(srcWorld);

        List<EditTask> tasks = new ArrayList<>();
        activeUndoPlayerTaskPairs.forEach(pair -> tasks.add(pair.getValue()));

        long startTime = System.nanoTime();

        MAWMConverter.convertUndo(context, tasks, () -> {
            manipulateStage = ManipulateStage.CONVERT_FINISHED;
            double conversionTime = (System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1);

            int playerCount = new HashSet<>(Arrays.asList(activePlayerTaskPairs.toArray())).size();

            activeUndoPlayerTaskPairs.forEach(pair ->
                pair.getKey().sendMessage(TextComponentHelper.createComponentTranslation(pair.getKey(),
                    "mawm.undo.completed.stats",
                    activeUndoPlayerTaskPairs.size(),
                    playerCount,
                    conversionTime
                ))
            );

        }, (throwable -> activeUndoPlayerTaskPairs.forEach(pair -> {
            pair.getKey().sendMessage(TextComponentHelper.createComponentTranslation(pair.getKey(), "mawm.undo.error.converter_error"));
            MAWM.LOGGER.fatal("Unrecoverable converter error!", throwable);
        })));
    }

    @Override
    public void swapModifiedRegionFilesForTasks() {
        File saveLoc = saveHandler.getWorldDirectory();
        File workingLoc = Paths.get(saveLoc.getParent() + "/mawmWorkingWorld").toFile();

        String worldDirName = saveLoc.getName();

        Path backupLoc = Paths.get(saveLoc.getParent() + "/" +  worldDirName + ".bak");

        if (!Files.exists(backupLoc)) {
            try {
                Files.createDirectories(backupLoc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        activePlayerTaskPairs.forEach(task -> getRegionFilesModifiedByTask(task.getValue()).forEach((vec) -> {
            Path workingLocPath = Paths.get(workingLoc + "/region3d/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");
            if(!Files.exists(workingLocPath))
                return;

            String backRegionDir = backupLoc + "/region3d/";
            File f = new File(backRegionDir);
            f.mkdir();

            Path dstVecPath = Paths.get(saveLoc + "/region3d/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");
            Path bakVecPath = Paths.get(backRegionDir + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");


            try {
                Files.delete(bakVecPath);
                MAWM.LOGGER.debug("Deleted existing backup region file");
            } catch(IOException e) {
                MAWM.LOGGER.debug("No backup region file existed to delete at " + bakVecPath);
            }
            try {
                Files.move(dstVecPath, bakVecPath);
                MAWM.LOGGER.debug("Moved world region file into backup loc");

                try {
                    Files.move(workingLocPath, dstVecPath, StandardCopyOption.REPLACE_EXISTING);
                    MAWM.LOGGER.debug("Moved output region into world loc");
                } catch (IOException e) {
                    MAWM.LOGGER.debug("No region file exists at " + workingLocPath + ", assuming converter had empty region at that location, skipping." + e.getMessage());
                }
            } catch (IOException e) {
                MAWM.LOGGER.error("Cancelling command execution! Couldn't find existing region file to move to backups at " + dstVecPath + ". " + e.getMessage());
                e.printStackTrace();
                task.getKey().sendMessage(TextComponentHelper.createComponentTranslation(task.getKey(),
                    "mawm.execute.error.missing_existing_region",
                    ("(" + vec.getX() + ", " + vec.getY() + ", " + vec.getZ() + ")")
                ));
            }
        }));
    }

    private Set<Vector3i> getRegionFilesModifiedByTask(EditTask task) {
        Set<Vector3i> vectors = new HashSet<>();

        switch(task.getType()) {
            case MOVE:
                vectors.addAll(getRegionFilesWithinBoundingBox(task.getSourceBox().add(task.getOffset())));
                break;
            case CUT:
                vectors.addAll(getRegionFilesWithinBoundingBox(task.getSourceBox()));
                if(task.getOffset() != null) {
                    vectors.addAll(getRegionFilesWithinBoundingBox(task.getSourceBox().add(task.getOffset())));
                }
                break;
            case COPY:
                if(task.getOffset() != null) {
                    vectors.addAll(getRegionFilesWithinBoundingBox(task.getSourceBox().add(task.getOffset())));
                }
                break;
            case SET:
            case REPLACE:
            case KEEP:
            case REMOVE:
                vectors.addAll(getRegionFilesWithinBoundingBox(task.getSourceBox()));
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + task.getType());
        }
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

    @Override
    public void addFreezeRegionsForTasks(List<MutablePair<ICommandSender, EditTask>> tasks) {
        //TODO: fix commands that don't have a src freeze box, such as cut 0 0 0 15 15 15
        //TODO: SET & REPLACE don't need a SrcFreezeBox
        tasks.forEach(pair -> {
            EditTask task = pair.getValue();
            addSrcFreezeBox(new FreezableBox(task.getSourceBox().getMinPos(), task.getSourceBox().getMaxPos()));
            if(task.getOffset() != null) {
                addDstFreezeBox(new FreezableBox(
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
                case REPLACE:
                case SET:
                    addDstFreezeBox(new FreezableBox(task.getSourceBox().getMinPos(), task.getSourceBox().getMaxPos()));
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + task.getType());
            }
        });
    }

    @Override
    public List<MutablePair<ICommandSender, EditTask>> getDeferredTasks() {
        return deferredPlayerTaskPairs;
    }

    @Override
    public void addDeferredTasks() {
        activePlayerTaskPairs.clear();
        activePlayerTaskPairs.addAll(deferredPlayerTaskPairs);
        deferredPlayerTaskPairs.clear();
    }
    @Override
    public void addDeferredUndoTasks() {
        activeUndoPlayerTaskPairs.clear();
        activeUndoPlayerTaskPairs.addAll(deferredUndoPlayerTaskPairs);
        deferredUndoPlayerTaskPairs.clear();
    }

    @Override
    public void addTask(ICommandSender sender, EditTask task) {
        deferredPlayerTaskPairs.add(new MutablePair<>(sender, task));
    }

    @Override
    public void addUndoTask(ICommandSender sender, List<EditTask> undoTasks) {
        undoTasks.forEach(task -> deferredUndoPlayerTaskPairs.add(new MutablePair<>(sender, task)));
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