package io.github.notstirred.mawm.asm.mixin.core.cubicchunks;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableColumn;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Chunk.class)
public class MixinChunkColumn implements IFreezableColumn {

    @Override
    public void freeze() { }
    @Override
    public void unFreeze() {}
}
