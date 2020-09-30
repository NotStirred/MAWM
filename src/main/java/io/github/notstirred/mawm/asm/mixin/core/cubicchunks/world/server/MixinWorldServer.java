package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.world.server;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.MixinCubeProviderServer;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessPlayerCubeMap;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IFreezableWorld, ICubicWorldServer {
    @Shadow public abstract ChunkProviderServer getChunkProvider();

    @Shadow @Final private PlayerChunkMap playerChunkMap;
    private List<FreezableBox> freezeBoxes = new ArrayList<>();
    private boolean isFrozen = false;

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public boolean isCubeFrozen(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeDst(Cube cube) {
        if(isFrozen)
            return isCubeDst(cube.getX(), cube.getY(), cube.getZ());
        return false;
    }
    @Override
    public boolean isCubeDst(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeReadFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeSrc(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeWriteFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isColumnFrozen(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnDst(Chunk column) {
        if(isFrozen)
            return isColumnDst(column.x, column.z);
        return false;
    }
    @Override
    public boolean isColumnDst(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnReadFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnSrc(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnWriteFrozen(x, z));
        return false;
    }

    @Override
    public boolean is2dRegionDst(EntryLocation2D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is2dRegionReadFrozen(entry));
        return false;
    }
    @Override
    public boolean is2dRegionSrc(EntryLocation2D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is2dRegionWriteFrozen(entry));
        return false;
    }
    @Override
    public boolean is2dRegionFrozen(EntryLocation2D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is2dRegionFrozen(entry));
        return false;
    }

    @Override
    public boolean is3dRegionDst(EntryLocation3D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is3dRegionReadFrozen(entry));
        return false;
    }
    @Override
    public boolean is3dRegionSrc(EntryLocation3D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is3dRegionWriteFrozen(entry));
        return false;
    }
    @Override
    public boolean is3dRegionFrozen(EntryLocation3D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is3dRegionFrozen(entry));
        return false;
    }

    @Override
    public void addFreezeBox(FreezableBox box) {
        this.freezeBoxes.add(box);
    }
    @Override
    public void freeze() {
        isFrozen = true;
        ((IFreezableCubeProviderServer) this.getCubeCache()).freeze();
    }
    @Override
    public void unfreeze() {
        PlayerCubeMap map = ((PlayerCubeMap) playerChunkMap);

        List<Map.Entry<CubePos, TicketList>> dstCubesToReload = new ArrayList<>(1000);

        Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap = new IdentityHashMap<>(1000);
        Map<IColumn, List<EntityPlayerMP>> columnPlayerMap = new IdentityHashMap<>(1000);

        ((IFreezableCubeProviderServer) getCubeCache()).unfreezeUnload(map, dstCubesToReload, cubePlayerMap, columnPlayerMap);

        isFrozen = false;
        this.freezeBoxes = new ArrayList<>();

        ((IFreezableCubeProviderServer) getCubeCache()).unfreezeReload((AccessPlayerCubeMap) map, dstCubesToReload, cubePlayerMap, columnPlayerMap);

    }
}