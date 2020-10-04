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

@Mixin(value = RegionCubeIO.class, remap = false)
public abstract class MixinRegionCubeIO implements IRegionCubeIO {

    @Shadow @Nonnull private World world;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nonnull private ConcurrentMap<CubePos, Object> cubesToSave;
    @Shadow @Nonnull private ConcurrentMap<ChunkPos, Object> columnsToSave;

    //saving
    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save2d(Lcubicchunks/regionlib/impl/EntryLocation2D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipSrcColumn(SaveCubeColumns save, EntryLocation2D entry, ByteBuffer data) {
        if(((IFreezableWorld) world).isSrcSavingLocked()) {
            if (((IFreezableWorld) world).is2dRegionSrc(entry)) {
                return;
            }
        }
        if(((IFreezableWorld) world).isDstSavingLocked()) {
            if(((IFreezableWorld) world).is2dRegionDst(entry)) {
                return;
            }
        }
        try {
            save.save2d(entry, data);
        } catch (Throwable t) {
            LOGGER.error(String.format("Unable to write column (%d, %d)", entry.getEntryX(), entry.getEntryZ()), t);
        }
    }
    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save3d(Lcubicchunks/regionlib/impl/EntryLocation3D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipSrcCube(SaveCubeColumns save, EntryLocation3D entry, ByteBuffer data) {
        if(((IFreezableWorld) world).isSrcSavingLocked()) {
            if (((IFreezableWorld) world).is3dRegionSrc(entry)) {
                return;
            }
        }
        if(((IFreezableWorld) world).isDstSavingLocked()) {
            if(((IFreezableWorld) world).is3dRegionDst(entry)) {
                return;
            }
        }
        try {
            save.save3d(entry, data);
        } catch (Throwable t) {
            LOGGER.error(String.format("Unable to write cube %d, %d, %d", entry.getEntryX(), entry.getEntryY(), entry.getEntryZ()), t);
        }
    }

    //adding to save
    @Redirect(method = "saveColumn", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveColumn$skipFrozenColumn(ConcurrentMap<Object, Object> concurrentMap, Object chunkPos, Object saveEntry) {
        if(((IFreezableWorld) world).isSrcSaveAddingLocked()) {
            if (((IFreezableWorld) world).isColumnSrc((ChunkPos) chunkPos, false)) {
                return null;
            }
        }
        if(((IFreezableWorld) world).isDstSaveAddingLocked()) {
            if (((IFreezableWorld) world).isColumnDst((ChunkPos) chunkPos, false)) {
                return null;
            }
        }
        return concurrentMap.put(chunkPos, saveEntry);
    }
    @Redirect(method = "saveCube", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveCube$skipFrozenCube(ConcurrentMap<Object, Object> concurrentMap, Object cubePos, Object saveEntry) {
        if(((IFreezableWorld) world).isSrcSaveAddingLocked()) {
            if (((IFreezableWorld) world).isCubeSrc((CubePos) cubePos, false)) {
                return null;
            }
        }
        if(((IFreezableWorld) world).isDstSaveAddingLocked()) {
            if (((IFreezableWorld) world).isCubeDst((CubePos) cubePos, false)) {
                return null;
            }
        }
        return concurrentMap.put(cubePos, saveEntry);
    }

    @Override
    public boolean hasFrozenSrcCubesToBeSaved() {
        return cubesToSave.keySet().stream().anyMatch(cubePos -> ((IFreezableWorld) world).isCubeSrc(cubePos, false));
    }
    @Override
    public boolean hasFrozenSrcColumnsToBeSaved() {
        return columnsToSave.keySet().stream().anyMatch(chunkPos -> ((IFreezableWorld) world).isColumnSrc(chunkPos, false));
    }
}
