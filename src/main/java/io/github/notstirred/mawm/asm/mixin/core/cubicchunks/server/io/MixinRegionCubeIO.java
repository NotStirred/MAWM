package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.io;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.RegionCubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
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
public abstract class MixinRegionCubeIO {

    @Shadow @Nonnull private World world;

    @Shadow @Final private static Logger LOGGER;

    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save2d(Lcubicchunks/regionlib/impl/EntryLocation2D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipFrozenColumn(SaveCubeColumns save, EntryLocation2D entry, ByteBuffer data) {
        if(!((IFreezableWorld) world).is2dRegionWriteFrozen(entry)) {
            try {
                save.save2d(entry, data);
            } catch (Throwable t) {
                LOGGER.error(String.format("Unable to write column (%d, %d)", entry.getEntryX(), entry.getEntryZ()), t);
            }
        }
    }
    @Redirect(method = "writeNextIO", at = @At(value = "INVOKE", target = "Lcubicchunks/regionlib/impl/SaveCubeColumns;save3d(Lcubicchunks/regionlib/impl/EntryLocation3D;Ljava/nio/ByteBuffer;)V"))
    private void writeNextIO$skipFrozenCube(SaveCubeColumns save, EntryLocation3D entry, ByteBuffer data) {
        if(!((IFreezableWorld) world).is3dRegionWriteFrozen(entry)) {
            try {
                save.save3d(entry, data);
            } catch (Throwable t) {
                LOGGER.error(String.format("Unable to write cube %d, %d, %d", entry.getEntryX(), entry.getEntryY(), entry.getEntryZ()), t);
            }
        }
    }

    @Redirect(method = "saveColumn", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveColumn$skipFrozenColumn(ConcurrentMap concurrentMap, Object column, Object saveEntry) {
        if(!((IFreezableWorld) world).isColumnWriteFrozen((Chunk) column)) {
            return concurrentMap.put(((Chunk) column).getPos(), saveEntry);
        }
        return null;
    }
    @Redirect(method = "saveCube", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object saveColumn$skipFrozenCube(ConcurrentMap concurrentMap, Object cube, Object saveEntry) {
        if(!((IFreezableWorld) world).isCubeWriteFrozen((Cube) cube)) {
            return concurrentMap.put(((Cube) cube).getCoords(), saveEntry);
        }
        return null;
    }
}
