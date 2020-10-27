package io.github.notstirred.mawm.asm.mixininterfaces;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public interface IColumnWatcher {
    void addPlayerNoChunkUnload(EntityPlayerMP player);

    // CHECKED: 1.10.2-12.18.1.2092//TODO: remove it, the only different line is sending packet
    void removePlayerNoChunkUnload(EntityPlayerMP player);
}
