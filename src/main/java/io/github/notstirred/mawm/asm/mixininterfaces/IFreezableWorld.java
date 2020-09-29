package io.github.notstirred.mawm.asm.mixininterfaces;

import cubicchunks.converter.lib.util.BoundingBox;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.chunk.Chunk;

public interface IFreezableWorld {
    boolean isCubeFrozen(Cube cube);

    boolean isCubeFrozen(int x, int y, int z);

    boolean isColumnFrozen(Chunk column);

    boolean isColumnFrozen(int x, int z);

    void freezeBox(BoundingBox box);
    void unfreeze();
}
