package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.world.server;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IFreezableWorld, ICubicWorldServer {

    @Shadow @Final private PlayerChunkMap playerChunkMap;

    private List<FreezableBox> srcFreezeBoxes = new ArrayList<>();
    private List<FreezableBox> dstFreezeBoxes = new ArrayList<>();

    IFreezableWorld.ManipulateStage manipulateStage = ManipulateStage.NONE;

    private boolean isSrcFrozen = false;
    private boolean isDstFrozen = false;
    private boolean isSrcSavingLocked = false;
    private boolean isSrcSaveAddingLocked = false;
    private boolean isDstSavingLocked = false;
    private boolean isDstSaveAddingLocked = false;

    PlayerCubeMap playerCubeMap;
    List<Map.Entry<CubePos, TicketList>> dstCubesToReload;
    Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap;
    Map<IColumn, List<EntityPlayerMP>> columnPlayerMap;

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