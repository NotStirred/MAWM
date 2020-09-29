package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CubeWatcher.class, remap = false)
public interface AccessCubeWatcher {
    @Accessor ObjectArrayList<EntityPlayerMP> getPlayers();
    @Invoker void invokeRemovePlayer(EntityPlayerMP player);
    @Invoker void invokeAddPlayer(EntityPlayerMP player);
}
