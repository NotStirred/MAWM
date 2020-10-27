package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(value = PlayerCubeMap.class, remap = false)
public interface AccessPlayerCubeMap {
    @Invoker CubeWatcher invokeGetOrCreateCubeWatcher(@Nonnull CubePos cubePos);
    @Invoker ColumnWatcher invokeGetOrCreateColumnWatcher(ChunkPos chunkPos);

    @Accessor VanillaNetworkHandler getVanillaNetworkHandler();
}
