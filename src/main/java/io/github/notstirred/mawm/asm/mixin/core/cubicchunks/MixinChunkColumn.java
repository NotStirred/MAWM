package io.github.notstirred.mawm.asm.mixin.core.cubicchunks;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableColumn;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Chunk.class)
public class MixinChunkColumn implements IFreezableColumn {

    private boolean isColumnReadFrozen = false;
    private boolean isColumnWriteFrozen = false;

    @Override
    public boolean isColumnReadFrozen() {
        return isColumnReadFrozen;
    }

    @Override
    public boolean isColumnWriteFrozen() {
        return isColumnWriteFrozen;
    }

    @Override
    public void freeze() { }
    @Override
    public void unFreeze() {}
}
