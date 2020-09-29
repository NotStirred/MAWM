package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.world.server;

import cubicchunks.converter.lib.util.BoundingBox;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IFreezableWorld, ICubicWorldServer {
    @Shadow public abstract ChunkProviderServer getChunkProvider();

    List<BoundingBox> freezeBoxes = new ArrayList<>();

    @Override
    public boolean isCubeFrozen(Cube cube) {
        return isCubeFrozen(cube.getX(), cube.getY(), cube.getZ());
    }
    @Override
    public boolean isCubeFrozen(int x, int y, int z) {
        return freezeBoxes.stream().anyMatch(b -> b.intersects(x, y, z));
    }
    
    @Override
    public boolean isColumnFrozen(Chunk column) {
        return isColumnFrozen(column.x, column.z);
    }
    @Override
    public boolean isColumnFrozen(int x, int z) {
        return freezeBoxes.stream().anyMatch(b -> b.columnIntersects(x, z));
    }

    @Override
    public void freezeBox(BoundingBox box) {
        this.freezeBoxes.add(box);
    }
    @Override
    public void unfreeze() {
        this.freezeBoxes = new ArrayList<>();
        ((IFreezableCubeProviderServer)this.getCubeCache()).unfreeze();
    }
}