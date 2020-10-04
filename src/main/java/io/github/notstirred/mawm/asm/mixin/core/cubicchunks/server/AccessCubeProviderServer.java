package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CubeProviderServer.class, remap = false)
public interface AccessCubeProviderServer {
    @Accessor ICubeIO getCubeIO();
}
