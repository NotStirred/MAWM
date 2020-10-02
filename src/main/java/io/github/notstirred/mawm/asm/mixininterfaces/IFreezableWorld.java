package io.github.notstirred.mawm.asm.mixininterfaces;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public interface IFreezableWorld {
    void clearSrcBoxes();

    void clearDstBoxes();

    enum ManipulateStage { NONE, WAITING_SRC_SAVE, CONVERTING, CONVERT_FINISHED, REGION_SWAP_FINISHED, RELOADING_CUBES }

    ManipulateStage getManipulateStage();
    void setManipulateStage(ManipulateStage stage);

    boolean isSrcSavingLocked();
    void setSrcSavingLocked(boolean state);

    boolean isSrcSaveAddingLocked();
    void setSrcSaveAddingLocked(boolean state);

    boolean isDstSavingLocked();
    void setDstSavingLocked(boolean state);

    boolean isDstSaveAddingLocked();
    void setDstSaveAddingLocked(boolean state);

    boolean isSrcFrozen();
    void setSrcFrozen(boolean state);

    boolean isDstFrozen();
    void setDstFrozen(boolean state);

    default boolean isCubeSrc(Cube cube, boolean checkFrozen) { return isCubeSrc(cube.getX(), cube.getY(), cube.getZ(), checkFrozen); }
    default boolean isCubeSrc(CubePos cubePos, boolean checkFrozen) { return isCubeSrc(cubePos.getX(), cubePos.getY(), cubePos.getZ(), checkFrozen); }
    boolean isCubeSrc(int x, int y, int z, boolean checkFrozen);
    default boolean isCubeDst(Cube cube, boolean checkFrozen) { return isCubeDst(cube.getX(), cube.getY(), cube.getZ(), checkFrozen); }
    default boolean isCubeDst(CubePos cubePos, boolean checkFrozen) { return isCubeDst(cubePos.getX(), cubePos.getY(), cubePos.getZ(), checkFrozen); }
    boolean isCubeDst(int x, int y, int z, boolean checkFrozen);
    default boolean isCubeFrozen(Cube cube) { return isCubeFrozen(cube.getX(), cube.getY(), cube.getZ()); }
    default boolean isCubeFrozen(CubePos cubePos) { return isCubeFrozen(cubePos.getX(), cubePos.getY(), cubePos.getZ()); }
    boolean isCubeFrozen(int x, int y, int z);


    default boolean isColumnSrc(Chunk column, boolean checkFrozen) { return isColumnSrc(column.x, column.z, checkFrozen); }
    default boolean isColumnSrc(ChunkPos chunkPos, boolean checkFrozen) { return isColumnSrc(chunkPos.x, chunkPos.z, checkFrozen); }
    boolean isColumnSrc(int x, int z, boolean checkFrozen);
    default boolean isColumnDst(ChunkPos chunkPos, boolean checkFrozen) { return isColumnDst(chunkPos.x, chunkPos.z, checkFrozen); }
    default boolean isColumnDst(Chunk column, boolean checkFrozen) { return isColumnDst(column.x, column.z, checkFrozen); }
    boolean isColumnDst(int x, int z, boolean checkFrozen);
    default boolean isColumnFrozen(Chunk column) { return isColumnFrozen(column.x, column.z); }
    default boolean isColumnFrozen(ChunkPos chunkPos) { return isColumnFrozen(chunkPos.x, chunkPos.z); }
    boolean isColumnFrozen(int x, int z);

    boolean is2dRegionSrc(EntryLocation2D entry);
    boolean is2dRegionDst(EntryLocation2D entry);
    boolean is2dRegionFrozen(EntryLocation2D entry);

    boolean is3dRegionSrc(EntryLocation3D entry);
    boolean is3dRegionDst(EntryLocation3D entry);
    boolean is3dRegionFrozen(EntryLocation3D entry);

    void addSrcFreezeBox(FreezableBox box);
    void addDstFreezeBox(FreezableBox box);
}
