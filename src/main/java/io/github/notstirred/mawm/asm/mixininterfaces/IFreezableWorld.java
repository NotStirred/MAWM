package io.github.notstirred.mawm.asm.mixininterfaces;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.chunk.Chunk;

public interface IFreezableWorld {
    boolean isCubeFrozen(Cube cube);

    boolean isCubeFrozen(int x, int y, int z);

    boolean isCubeReadFrozen(Cube cube);

    boolean isCubeReadFrozen(int x, int y, int z);

    boolean isCubeWriteFrozen(Cube cube);

    boolean isCubeWriteFrozen(int x, int y, int z);

    boolean isColumnFrozen(Chunk column);

    boolean isColumnFrozen(int x, int z);

    boolean isColumnReadFrozen(Chunk column);

    boolean isColumnReadFrozen(int x, int z);

    boolean isColumnWriteFrozen(Chunk column);

    boolean isColumnWriteFrozen(int x, int z);

    void addFreezeBox(FreezableBox box);
    void freeze();
    void unfreeze();

    boolean is2dRegionWriteFrozen(EntryLocation2D entry);
    boolean is2dRegionReadFrozen(EntryLocation2D entry);

    boolean is3dRegionWriteFrozen(EntryLocation3D entry);

    boolean is3dRegionReadFrozen(EntryLocation3D entry);
}
