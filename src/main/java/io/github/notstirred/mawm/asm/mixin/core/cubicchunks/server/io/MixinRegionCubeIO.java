package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.io;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.asm.mixininterfaces.IRegionCubeIO;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.RegionCubeIO;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

@Mixin(RegionCubeIO.class)
public abstract class MixinRegionCubeIO implements IRegionCubeIO {

    @Shadow @Nonnull private World world;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nonnull private ConcurrentMap<CubePos, Object> cubesToSave;

    @Shadow @Nonnull private ConcurrentMap<ChunkPos, Object> columnsToSave;

    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save2d(Lcubicchunks/regionlib/impl/EntryLocation2D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipSrcColumn(SaveCubeColumns save, EntryLocation2D entry, ByteBuffer data) {
        if(!((IFreezableWorld) world).is2dRegionFrozen(entry)) {
            try {
                save.save2d(entry, data);
            } catch (Throwable t) {
                LOGGER.error(String.format("Unable to write column (%d, %d)", entry.getEntryX(), entry.getEntryZ()), t);
            }
        }
    }
    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save3d(Lcubicchunks/regionlib/impl/EntryLocation3D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipSrcCube(SaveCubeColumns save, EntryLocation3D entry, ByteBuffer data) {
        if(!((IFreezableWorld) world).is3dRegionFrozen(entry)) {
            try {
                save.save3d(entry, data);
            } catch (Throwable t) {
                LOGGER.error(String.format("Unable to write cube %d, %d, %d", entry.getEntryX(), entry.getEntryY(), entry.getEntryZ()), t);
            }
        }
    }

    @Redirect(method = "saveColumn", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveColumn$skipFrozenColumn(ConcurrentMap<Object, Object> concurrentMap, Object chunkPos, Object saveEntry) {
        if(!((IFreezableWorld) world).isColumnFrozen((ChunkPos) chunkPos)) {
            return concurrentMap.put(chunkPos, saveEntry);
        }
        return null;
    }
    @Redirect(method = "saveCube", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveColumn$skipFrozenCube(ConcurrentMap<Object, Object> concurrentMap, Object cubePos, Object saveEntry) {
        if(!((IFreezableWorld) world).isCubeFrozen((CubePos) cubePos)) {
            return concurrentMap.put(cubePos, saveEntry);
        }
        return null;
    }

    @Override
    public boolean hasFrozenCubesToBeSaved() {
        return cubesToSave.keySet().stream().anyMatch(cubePos -> ((IFreezableWorld) world).isCubeFrozen(cubePos));
    }
    @Override
    public boolean hasFrozenColumnsToBeSaved() {
        return columnsToSave.keySet().stream().anyMatch(chunkPos -> ((IFreezableWorld) world).isColumnFrozen(chunkPos));
    }
}
