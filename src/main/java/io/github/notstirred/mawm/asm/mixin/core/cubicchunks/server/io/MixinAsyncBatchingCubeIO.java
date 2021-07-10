package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.io;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.asm.mixininterfaces.IRegionCubeIO;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ThreadedFileIOBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

@Mixin(value = AsyncBatchingCubeIO.class, remap = false)
public abstract class MixinAsyncBatchingCubeIO implements IRegionCubeIO {

    @Shadow @Final @Nonnull protected World world;

    @Shadow @Final protected final Map<ChunkPos, NBTTagCompound> pendingColumns = new ConcurrentHashMap<>();
    @Shadow @Final protected final Map<CubePos, NBTTagCompound> pendingCubes = new ConcurrentHashMap<>();

    @Shadow @Final protected ReadWriteLock lock;

    @Shadow protected abstract void ensureOpen();

    private final ConcurrentSet<Cube> deferredCubes = new ConcurrentSet<>();
    private final ConcurrentSet<Chunk> deferredColumns = new ConcurrentSet<>();

    //TODO: prioritise saving frozen cubes first (probably need SaveEntry shadow class for that first however)
    @Inject(method = "saveCube", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), cancellable = true)
    private void skipFrozenCube(Cube cube, CallbackInfo ci) {
        if(((IFreezableWorld) world).isSrcSaveAddingLocked() || ((IFreezableWorld) world).isDstSaveAddingLocked()) {
            //TODO: optimisation, could check if this region was affected, and only add to deferred if it is
//            if (((IFreezableWorld) world).isCubeSrc((CubePos) cubePos, false) || ((IFreezableWorld) world).isCubeDst((CubePos) cubePos, false)) {
//                return null;
//            }
            if(((IFreezableWorld) world).isCubeDst(cube.getCoords(), false)) {
                //if the cube is dst, it doesn't need to be saved
                //also if the cube is src and dst
                return;
            }
            if(((IFreezableWorld) world).isCubeSrc(cube.getCoords(), false)) {
                //TODO: Send cube data to the converter, and defer saving
            }
            //Always deferring all cubes, which is unnecessary
            deferredCubes.add(cube);
            ci.cancel();
        }
    }

    @Inject(method = "saveColumn", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), cancellable = true)
    private void skipFrozenColumn(Chunk column, CallbackInfo ci) {
        if(((IFreezableWorld) world).isSrcSaveAddingLocked() || ((IFreezableWorld) world).isDstSaveAddingLocked()) {
            //TODO: optimisation, could check if this region was affected, and only add to deferred if it is
//            if (((IFreezableWorld) world).isColumnSrc((ChunkPos) chunkPos, false) || ((IFreezableWorld) world).isColumnDst((ChunkPos) chunkPos, false)) {
//                return null;
//            }
            if(((IFreezableWorld) world).isColumnSrc(column.getPos(), false)) {
                //TODO: Send column data to the converter, and defer saving
            }
            if(((IFreezableWorld) world).isColumnDst(column.getPos(), false)) {
                //if the column is dst, it doesn't need to be saved
                //This is also true if the column is src&dst
                return;
            }

            //Always deferring all columns, which is unnecessary
            deferredColumns.add(column);
            ci.cancel();
        }
    }

    @Override
    public boolean hasFrozenSrcCubesToBeSaved() {
        return pendingCubes.keySet().stream().anyMatch(cubePos -> ((IFreezableWorld) world).isCubeSrc(cubePos, false));
    }
    @Override
    public boolean hasFrozenSrcColumnsToBeSaved() {
        return pendingColumns.keySet().stream().anyMatch(chunkPos -> ((IFreezableWorld) world).isColumnSrc(chunkPos, false));
    }

    @Override
    public void flushDeferredCubes() {
        lock.readLock().lock();
        try {
            this.ensureOpen();

            // NOTE: this function blocks the world thread
            // make it as fast as possible by offloading processing to the IO thread
            // except we have to write the NBT in this thread to avoid problems
            // with concurrent access to world data structures

            // add the column to the save queue
            for (Iterator<Cube> iterator = deferredCubes.iterator(); iterator.hasNext(); ) {
                Cube cube = iterator.next();
                iterator.remove();
                this.pendingCubes.put(cube.getCoords(), AccessIONbtWriter.invokeWrite(cube));
                cube.markSaved();

                // signal the IO thread to process the save queue
                ThreadedFileIOBase.getThreadedIOInstance().queueIO((AsyncBatchingCubeIO) (Object) this);
            }

        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public void flushDeferredColumns() {
        lock.readLock().lock();
        try {
            this.ensureOpen();

            for (Iterator<Chunk> iterator = deferredColumns.iterator(); iterator.hasNext(); ) {
                Chunk column = iterator.next();
                iterator.remove();
                this.pendingColumns.put(column.getPos(), AccessIONbtWriter.invokeWrite(column));
                column.setModified(false);

                // signal the IO thread to process the save queue
                ThreadedFileIOBase.getThreadedIOInstance().queueIO((AsyncBatchingCubeIO) (Object) this);
            }

        } finally {
            lock.readLock().unlock();
        }
    }
}
