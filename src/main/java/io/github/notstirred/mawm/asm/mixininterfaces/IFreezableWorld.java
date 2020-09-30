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
    boolean isFrozen();

    default boolean isCubeFrozen(Cube cube) {
        return isCubeFrozen(cube.getX(), cube.getY(), cube.getZ());
    }
    default boolean isCubeFrozen(CubePos cubePos) {
        return isCubeFrozen(cubePos.getX(), cubePos.getY(), cubePos.getZ());
    }
    boolean isCubeFrozen(int x, int y, int z);

    boolean isCubeDst(Cube cube);
    boolean isCubeDst(int x, int y, int z);

    default boolean isCubeSrc(Cube cube) {
        return isCubeSrc(cube.getX(), cube.getY(), cube.getZ());
    }
    default boolean isCubeSrc(CubePos cubePos) {
        return isCubeSrc(cubePos.getX(), cubePos.getY(), cubePos.getZ());
    }
    boolean isCubeSrc(int x, int y, int z);

    default boolean isColumnFrozen(Chunk column) {
        return isColumnFrozen(column.x, column.z);
    }
    default boolean isColumnFrozen(ChunkPos chunkPos) {
        return isColumnFrozen(chunkPos.x, chunkPos.z);
    }
    boolean isColumnFrozen(int x, int z);

    boolean isColumnDst(Chunk column);
    boolean isColumnDst(int x, int z);

    default boolean isColumnSrc(Chunk column) {
        return isColumnSrc(column.x, column.z);
    }
    default boolean isColumnSrc(ChunkPos chunkPos) {
        return isColumnSrc(chunkPos.x, chunkPos.z);
    }
    boolean isColumnSrc(int x, int z);

    boolean is2dRegionSrc(EntryLocation2D entry);
    boolean is2dRegionDst(EntryLocation2D entry);
    boolean is2dRegionFrozen(EntryLocation2D entry);

    boolean is3dRegionSrc(EntryLocation3D entry);
    boolean is3dRegionDst(EntryLocation3D entry);
    boolean is3dRegionFrozen(EntryLocation3D entry);

    void addFreezeBox(FreezableBox box);
    void freeze();
    void unfreeze();
}
