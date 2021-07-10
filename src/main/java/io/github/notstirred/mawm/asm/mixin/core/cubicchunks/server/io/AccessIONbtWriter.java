package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.io;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "io.github.opencubicchunks.cubicchunks.core.server.chunkio.IONbtWriter", remap = false)
public interface AccessIONbtWriter {
    @Invoker static NBTTagCompound invokeWrite(Chunk column) { throw new IllegalStateException("Mixin failed to apply"); }
    @Invoker static NBTTagCompound invokeWrite(Cube cube) { throw new IllegalStateException("Mixin failed to apply"); }
}
