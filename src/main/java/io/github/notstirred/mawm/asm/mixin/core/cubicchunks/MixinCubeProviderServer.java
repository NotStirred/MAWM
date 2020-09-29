package io.github.notstirred.mawm.asm.mixin.core.cubicchunks;

import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessCubeWatcher;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessPlayerCubeMap;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IPlayerChunkMapEntry;
import io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = CubeProviderServer.class, remap = false)
public abstract class MixinCubeProviderServer extends ChunkProviderServer implements ICubeProviderServer, IFreezableCubeProviderServer {

    @Shadow private WorldServer worldServer;
    @Shadow private ICubeIO cubeIO;

    @Shadow @Nullable protected abstract Chunk postProcessColumn(int columnX, int columnZ, @Nullable Chunk column, ICubeProviderServer.Requirement req);

    @Shadow protected abstract void onCubeLoaded(@Nullable Cube cube, Chunk column);

    @Shadow @Nullable protected abstract Cube postCubeLoadAttempt(int cubeX, int cubeY, int cubeZ, @Nullable Cube cube, Chunk column, ICubeProviderServer.Requirement req);

    @Shadow @Nonnull private XYZMap<Cube> cubeMap;
    private Cube blankCube;
    private Chunk emptyColumn;

    private XZMap<IColumn> newFrozenColumns = new XZMap<>(0.75f, 1000);
    private XYZMap<Cube> newFrozenCubes = new XYZMap<>(0.75f, 1000);

    private static CubePrimer barrierPrimer = createBarrierCubePrimer();

    public MixinCubeProviderServer(WorldServer worldObjIn, IChunkLoader chunkLoaderIn, IChunkGenerator chunkGeneratorIn) {
        super(worldObjIn, chunkLoaderIn, chunkGeneratorIn);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void on$init(WorldServer worldServer, ICubeGenerator cubeGen, CallbackInfo ci) {
        emptyColumn = new EmptyChunk(worldServer, 0, 0);
        blankCube = new BlankCube(emptyColumn);
    }

    //If cube is read frozen, return null
    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite
    @Override @Nullable
    public Chunk getLoadedColumn(int columnX, int columnZ) {
        Chunk chunk = this.loadedChunks.get(ChunkPos.asLong(columnX, columnZ));
        if(chunk == null)
            return (Chunk) newFrozenColumns.get(columnX,columnZ);
        return chunk;
    }

    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite
    @Override @Nullable
    public Chunk getColumn(int columnX, int columnZ, ICubeProviderServer.Requirement req) {
        Chunk column = this.getLoadedChunk(columnX, columnZ);
        if(column != null) {
            return column;
        }
        //TODO: if column is in frozen area, return emptyColumn
        if(((IFreezableWorld)worldServer).isColumnFrozen(columnX, columnZ)) {
            Chunk chunk = (Chunk) newFrozenColumns.get(columnX, columnZ);
            if(chunk == null) {
                chunk = new Chunk(worldServer, new ChunkPrimer(), columnX, columnZ);
                newFrozenColumns.put((IColumn)chunk);
                chunk.onLoad();
            }
            return chunk;
        }

        if (req == ICubeProviderServer.Requirement.GET_CACHED) {
            return column;
        }

        column = AsyncWorldIOExecutor.syncColumnLoad(worldServer, cubeIO, columnX, columnZ);
        column = postProcessColumn(columnX, columnZ, column, req);

        return column;
    }

    //If cube is read frozen, return null
    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite
    @Override @Nullable
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        Cube cube = this.cubeMap.get(cubeX, cubeY, cubeZ);
        if(cube != null) {
            return cube;
        }
        return (Cube) newFrozenCubes.get(cubeX,cubeY,cubeZ);
    }

    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite
    @Override @Nullable
    public Cube getCube(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req) {
        Cube cube = getLoadedCube(cubeX, cubeY, cubeZ);

        if (req == ICubeProviderServer.Requirement.GET_CACHED ||
                (cube != null && req.compareTo(ICubeProviderServer.Requirement.GENERATE) <= 0)) {
            return cube;
        }

        // try to get the Column
        Chunk column = getColumn(cubeX, cubeZ, req);
        if (column == null) {
            return cube; // Column did not reach req, so Cube also does not
        }
        if(((IFreezableWorld)worldServer).isCubeFrozen(cubeX, cubeY, cubeZ)) {
            cube = newFrozenCubes.get(cubeX,cubeY,cubeZ);
            if(cube == null) {
                cube = createBarrierCube(column, cubeX, cubeY, cubeZ);
                newFrozenCubes.put(cube);
                cube.onLoad();
            }
            return cube;
        }

        if (cube == null) {
            // a little hack to fix StackOverflowError when loading TileEntities, as Cube methods are now redirected into IColumn
            // Column needs cube to be loaded to add TileEntity, so make CubeProvider contain it already
            cube = AsyncWorldIOExecutor.syncCubeLoad(worldServer, cubeIO, (CubeProviderServer)(Object)this, cubeX, cubeY, cubeZ);
            onCubeLoaded(cube, column);
        }
        return postCubeLoadAttempt(cubeX, cubeY, cubeZ, cube, column, req);
    }

    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite
    public void asyncGetCube(int cubeX, int cubeY, int cubeZ, Requirement req, Consumer<Cube> callback) {
        Cube cube = getLoadedCube(cubeX, cubeY, cubeZ);
        if (req == Requirement.GET_CACHED || (cube != null && req.compareTo(Requirement.GENERATE) <= 0)) {
            callback.accept(cube);
            return;
        }

        if(((IFreezableWorld)worldServer).isCubeFrozen(cubeX, cubeY, cubeZ)) {
            if(cube == null) {
                Chunk column = getColumn(cubeX, cubeZ, req);
                if(column == null) {
                    callback.accept(null);
                    return;
                }
                cube = createBarrierCube(column, cubeX, cubeY, cubeZ);
                newFrozenCubes.put(cube);
                cube.onLoad();
            }
            callback.accept(cube);
            return;
        }
        if (cube == null) {
            AsyncWorldIOExecutor.queueCubeLoad(worldServer, cubeIO, (CubeProviderServer)(Object)this, cubeX, cubeY, cubeZ, loaded -> {
                Chunk col = getLoadedColumn(cubeX, cubeZ);
                if (col != null) {
                    onCubeLoaded(loaded, col);
                    loaded = postCubeLoadAttempt(cubeX, cubeY, cubeZ, loaded, col, req);
                }
                callback.accept(loaded);
            });
        }
    }

    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite
    public void asyncGetColumn(int columnX, int columnZ, Requirement req, Consumer<Chunk> callback) {
        Chunk column = getLoadedColumn(columnX, columnZ);
        if (column != null || req == Requirement.GET_CACHED) {
            callback.accept(column);
            return;
        }

        if(((IFreezableWorld)worldServer).isColumnFrozen(columnX, columnZ)) {
            Chunk frozenColumn = (Chunk) newFrozenColumns.get(columnX, columnZ);
            if(frozenColumn == null) {
                frozenColumn = new Chunk(worldServer, columnX, columnZ);
                newFrozenColumns.put((IColumn) frozenColumn);
                frozenColumn.onLoad();
            }
            callback.accept(frozenColumn);
            return;
        }
        AsyncWorldIOExecutor.queueColumnLoad(worldServer, cubeIO, columnX, columnZ, col -> {
            col = postProcessColumn(columnX, columnZ, col, req);
            callback.accept(col);
        });
    }

    @Inject(method = "tryUnloadColumn", at = @At("HEAD"), cancellable = true)
    private void on$tryUnloadColumn(Chunk column, CallbackInfoReturnable<Boolean> cir) {
        if(((IFreezableWorld)worldServer).isColumnFrozen(column)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void unfreeze() {
        PlayerCubeMap map = ((PlayerCubeMap)worldServer.getPlayerChunkMap());

        Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap = new IdentityHashMap<>(newFrozenCubes.getSize());
        Map<IColumn, List<EntityPlayerMP>> columnPlayerMap = new IdentityHashMap<>(newFrozenCubes.getSize());

        newFrozenCubes.iterator().forEachRemaining(cube -> {
            CubeWatcher cubeWatcher = map.getCubeWatcher(cube.getCoords());
            if(cubeWatcher != null) {
                ObjectArrayList<EntityPlayerMP> players = ((AccessCubeWatcher) cubeWatcher).getPlayers().clone();
                cubePlayerMap.put(cube, players);
                players.forEach(((AccessCubeWatcher) cubeWatcher)::invokeRemovePlayer);
            }
            cube.onUnload();
        });
        this.newFrozenCubes = new XYZMap<>(0.75f, 1000);

        newFrozenColumns.iterator().forEachRemaining(col -> {
            ColumnWatcher columnWatcher = map.getColumnWatcher(((Chunk)col).getPos());
            if(columnWatcher != null) {
                List<EntityPlayerMP> players = new ArrayList<>(((IPlayerChunkMapEntry) columnWatcher).getPlayerList());
                columnPlayerMap.put(col, players);
                players.forEach(columnWatcher:: removePlayer);
            }
            ((Chunk) col).onUnload();
        });
        newFrozenColumns.clear();
        columnPlayerMap.forEach((col, players) -> {
            ColumnWatcher columnWatcher = ((AccessPlayerCubeMap)map).invokeGetOrCreateColumnWatcher(new ChunkPos(col.getX(), col.getZ()));
            players.forEach(columnWatcher::addPlayer);
        });

        cubePlayerMap.forEach((cube, players) -> {
            CubeWatcher cubeWatcher = ((AccessPlayerCubeMap) map).invokeGetOrCreateCubeWatcher(cube.getCoords());
            players.forEach(((AccessCubeWatcher) cubeWatcher)::invokeAddPlayer);
        });
    }

    private Cube createBarrierCube(Chunk column, int cubeX, int cubeY, int cubeZ) {
        Cube cube = new Cube(column, cubeY, barrierPrimer);
        cube.setClientCube();
        return cube;
    }
    private static CubePrimer createBarrierCubePrimer() {
        CubePrimer primer = new CubePrimer();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    primer.setBlockState(x, y, z, Blocks.BARRIER.getDefaultState());
                }
            }
        }
        return primer;
    }
}
