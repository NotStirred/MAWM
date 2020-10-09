package io.github.notstirred.mawm.asm.mixin.core.world.server;

import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IFreezableWorld, ICubicWorldServer {
    List<EditTask> tasks = new ArrayList<>();

    private List<FreezableBox> srcFreezeBoxes = new ArrayList<>();
    private List<FreezableBox> dstFreezeBoxes = new ArrayList<>();

    IFreezableWorld.ManipulateStage manipulateStage = ManipulateStage.NONE;

    private boolean isSrcFrozen = false;
    private boolean isDstFrozen = false;
    private boolean isSrcSavingLocked = false;
    private boolean isSrcSaveAddingLocked = false;
    private boolean isDstSavingLocked = false;
    private boolean isDstSaveAddingLocked = false;

    private static final Vector3i CC_REGION_SIZE = new Vector3i(16, 16, 16);

    @Override
    public void swapModifiedRegionFilesForTasks() {
        tasks.forEach(task -> getRegionFilesModifiedByTask(task).forEach((vec) -> {

                File saveLoc = ((WorldServer)(Object)this).getSaveHandler().getWorldDirectory();
                File workingLoc = Paths.get(((WorldServer)(Object)this).getSaveHandler().getWorldDirectory().getParent() + "/mawmWorkingWorld").toFile();

                Path backupLoc = Paths.get(saveLoc.getAbsolutePath() + "/region3d.bak/");
                if(!Files.exists(backupLoc)) {
                    try {
                        Files.createDirectories(backupLoc);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }

                String dstVecPath = saveLoc.getAbsolutePath() + "/region3d/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr";
                Path bakVecPath = Paths.get(saveLoc.getAbsolutePath() + "/region3d.bak/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr");
            try {
                if(Files.exists(bakVecPath)) {
                    Files.delete(bakVecPath);
                    MAWM.LOGGER.info("Deleted existing backup region file");
                }
                Files.move(Paths.get(dstVecPath), bakVecPath);
                MAWM.LOGGER.info("Moved world region file into backup loc");
                Files.move(Paths.get(workingLoc.getAbsolutePath() + "/region3d/" + vec.getX() + "." + vec.getY() + "." + vec.getZ() + ".3dr"),
                        Paths.get(dstVecPath));
                MAWM.LOGGER.info("Moved output region into world loc");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private Set<Vector3i> getRegionFilesModifiedByTask(EditTask task) {
        Set<Vector3i> vectors = new HashSet<>();

        switch(task.getType()) {
            case MOVE:
                vectors.add(task.getSourceBox().asRegionCoords(CC_REGION_SIZE).getMinPos().add(vec3iAsRegionCoords(task.getOffset(), CC_REGION_SIZE)));
                break;
            case CUT:
                vectors.add(task.getSourceBox().asRegionCoords(CC_REGION_SIZE).getMinPos());
                if(task.getOffset() != null) {
                    vectors.add(task.getSourceBox().asRegionCoords(CC_REGION_SIZE).getMinPos().add(vec3iAsRegionCoords(task.getOffset(), CC_REGION_SIZE)));
                }
                break;
            case COPY:
                if(task.getOffset() != null) {
                    vectors.add(task.getSourceBox().asRegionCoords(CC_REGION_SIZE).getMinPos().add(vec3iAsRegionCoords(task.getOffset(), CC_REGION_SIZE)));
                }
                break;
            case SET:
            case REPLACE:
            case KEEP:
            case REMOVE:
                vectors.add(task.getSourceBox().asRegionCoords(CC_REGION_SIZE).getMinPos());
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + task.getType());
        }

        return vectors;
    }

    private Vector3i vec3iAsRegionCoords(Vector3i vec, Vector3i regionSize) {
        //TODO: add this to Vector3i in the converter
        return new Vector3i(Math.floorDiv(vec.getX(), regionSize.getX()),
                Math.floorDiv(vec.getY(), regionSize.getY()),
                Math.floorDiv(vec.getZ(), regionSize.getZ()));
    }

    @Override
    public void addFreezeRegionsForTasks() {
        //TODO: fix commands that don't have a src freeze box, such as cut 0 0 0 15 15 15
        //TODO: SET doesn't need a SrcFreezeBox
        tasks.forEach(task -> {
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
    public List<EditTask> getTasks() {
        return tasks;
    }
    @Override
    public void addTask(EditTask task) {
        tasks.add(task);
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