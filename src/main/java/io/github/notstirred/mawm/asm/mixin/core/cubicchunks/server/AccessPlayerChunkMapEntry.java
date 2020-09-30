package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PlayerChunkMapEntry.class)
public interface AccessPlayerChunkMapEntry {

    @Accessor("players") List<EntityPlayerMP> getPlayerList();

}
