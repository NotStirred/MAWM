package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server;

import io.github.notstirred.mawm.asm.mixininterfaces.IColumnWatcher;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IPlayerChunkMapEntry;
import io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = ColumnWatcher.class, remap = false)
public abstract class MixinColumnWatcher extends PlayerChunkMapEntry implements IColumnWatcher {
    @Shadow protected abstract IPlayerChunkMapEntry self();

    @Shadow public abstract int getX();

    @Shadow public abstract int getZ();

    @Shadow @Nonnull private PlayerCubeMap playerCubeMap;

    private final List<EntityPlayerMP> playersWithColumn = new ArrayList<>(); //These are the players who already know of this column, and don't need to be resent an empty one

    public MixinColumnWatcher(PlayerChunkMap mapIn, int chunkX, int chunkZ) {
        super(mapIn, chunkX, chunkZ);
    }

    @Override
    public void addPlayerNoChunkUnload(EntityPlayerMP player) {
        assert this.getChunk() == null || this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (self().getPlayerList().contains(player)) {
            CubicChunks.LOGGER.debug("[MAWM](addPlayerNoChunkUnload) Failed to expand player. {} already is in chunk {}, {}", player,
                this.getPos().x,
                this.getPos().z);
            return;
        }
        if (self().getPlayerList().isEmpty()) {
            self().setLastUpdateInhabitedTime(playerCubeMap.getWorldServer().getTotalWorldTime());
        }

        self().getPlayerList().add(player);
        playersWithColumn.add(player);
    }

    @Override
    public void removePlayerNoChunkUnload(EntityPlayerMP player) {
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (!self().getPlayerList().contains(player)) {
            return;
        }
        if (this.getChunk() == null) {
            self().getPlayerList().remove(player);
            if (self().getPlayerList().isEmpty()) {
                if (self().isLoading()) {
                    AsyncWorldIOExecutor.dropQueuedColumnLoad(
                        playerCubeMap.getWorldServer(), getPos().x, getPos().z, (c) -> self().getLoadedRunnable().run());
                }
                this.playerCubeMap.removeEntry(this);
            }
            return;
        }

        self().getPlayerList().remove(player);

        MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(this.getChunk(), player));

        if (self().getPlayerList().isEmpty()) {
            playerCubeMap.removeEntry((ColumnWatcher)(Object)this);
        }
    }

    @Redirect(remap = true, require = 1, method = "sendToPlayers", at = @At(value = "INVOKE", target = "Lio/github/opencubicchunks/cubicchunks/core/asm/mixin/core/common/IPlayerChunkMapEntry;getPlayerList()Ljava/util/List;"))
    private List<EntityPlayerMP> sendToPlayers$onGetPlayerList(IPlayerChunkMapEntry iPlayerChunkMapEntry) {
        List<EntityPlayerMP> players = new ArrayList<>(iPlayerChunkMapEntry.getPlayerList());
        players.removeAll(playersWithColumn);
        this.playersWithColumn.clear();
        return players;
    }
}
