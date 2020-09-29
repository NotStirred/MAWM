package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.world.server;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.FreezableBox;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IFreezableWorld, ICubicWorldServer {
    @Shadow public abstract ChunkProviderServer getChunkProvider();

    private List<FreezableBox> freezeBoxes = new ArrayList<>();
    private boolean isFrozen = false;

    @Override
    public boolean isCubeFrozen(Cube cube) {
        if(isFrozen)
            return isCubeFrozen(cube.getX(), cube.getY(), cube.getZ());
        return false;
    }
    @Override
    public boolean isCubeFrozen(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeReadFrozen(Cube cube) {
        if(isFrozen)
            return isCubeReadFrozen(cube.getX(), cube.getY(), cube.getZ());
        return false;
    }
    @Override
    public boolean isCubeReadFrozen(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeReadFrozen(x, y, z));
        return false;
    }

    @Override
    public boolean isCubeWriteFrozen(Cube cube) {
        if(isFrozen)
            return isCubeWriteFrozen(cube.getX(), cube.getY(), cube.getZ());
        return false;
    }
    @Override
    public boolean isCubeWriteFrozen(int x, int y, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isCubeWriteFrozen(x, y, z));
        return false;
    }
    
    @Override
    public boolean isColumnFrozen(Chunk column) {
        if(isFrozen)
            return isColumnFrozen(column.x, column.z);
        return false;
    }
    @Override
    public boolean isColumnFrozen(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnReadFrozen(Chunk column) {
        if(isFrozen)
            return isColumnReadFrozen(column.x, column.z);
        return false;
    }
    @Override
    public boolean isColumnReadFrozen(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnReadFrozen(x, z));
        return false;
    }

    @Override
    public boolean isColumnWriteFrozen(Chunk column) {
        if(isFrozen)
            return isColumnWriteFrozen(column.x, column.z);
        return false;
    }
    @Override
    public boolean isColumnWriteFrozen(int x, int z) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.isColumnWriteFrozen(x, z));
        return false;
    }

    @Override
    public boolean is2dRegionReadFrozen(EntryLocation2D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is2dRegionReadFrozen(entry));
        return false;
    }
    @Override
    public boolean is2dRegionWriteFrozen(EntryLocation2D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is2dRegionWriteFrozen(entry));
        return false;
    }

    @Override
    public boolean is3dRegionReadFrozen(EntryLocation3D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is3dRegionReadFrozen(entry));
        return false;
    }
    @Override
    public boolean is3dRegionWriteFrozen(EntryLocation3D entry) {
        if(isFrozen)
            return freezeBoxes.stream().anyMatch(b -> b.is3dRegionWriteFrozen(entry));
        return false;
    }

    @Override
    public void addFreezeBox(FreezableBox box) {
        this.freezeBoxes.add(box);
    }
    @Override
    public void freeze() {
        isFrozen = true;
    }
    @Override
    public void unfreeze() {
        isFrozen = false;
        this.freezeBoxes = new ArrayList<>();
        ((IFreezableCubeProviderServer)this.getCubeCache()).unfreeze();
    }
}