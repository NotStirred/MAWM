package io.github.notstirred.mawm.asm.mixin.core.cubicchunks;

import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessCubeWatcher;
import io.github.notstirred.mawm.asm.mixin.core.server.AccessPlayerChunkMapEntry;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessPlayerCubeMap;
import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.util.ticket.AccessTicketList;
import io.github.notstirred.mawm.asm.mixininterfaces.IColumnWatcher;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCubeProviderServer;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IPlayerChunkMapEntry;
import io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Consumer;

@Mixin(value = CubeProviderServer.class)
public abstract class MixinCubeProviderServer extends ChunkProviderServer implements ICubeProviderServer, IFreezableCubeProviderServer {

    @Shadow(remap = false)
    private Chunk currentlyLoadingColumn;

    @Shadow(remap = false)
    private ICubeIO cubeIO;

    @Shadow(remap = false)
    @Nullable
    protected abstract Chunk postProcessColumn(int columnX, int columnZ, @Nullable Chunk column, ICubeProviderServer.Requirement req, boolean force);

    @Shadow(remap = false)
    protected abstract void onCubeLoaded(@Nullable Cube cube, Chunk column);

    @Shadow(remap = false)
    @Nullable
    protected abstract Cube postCubeLoadAttempt(int cubeX, int cubeY, int cubeZ, @Nullable Cube cube, Chunk column, ICubeProviderServer.Requirement req, boolean forceNow);

    @Shadow(remap = false)
    @Nonnull
    private XYZMap<Cube> cubeMap;



    private XZMap<IColumn> newFrozenColumns = new XZMap<>(0.75f, 1000);
    private XYZMap<Cube> newFrozenCubes = new XYZMap<>(0.75f, 1000);

    private static CubePrimer barrierPrimer = createBarrierCubePrimer();

    public MixinCubeProviderServer(WorldServer worldObjIn, IChunkLoader chunkLoaderIn, IChunkGenerator chunkGeneratorIn) {
        super(worldObjIn, chunkLoaderIn, chunkGeneratorIn);
    }

    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite(remap = false)
    @Override
    @Nullable
    public Chunk getLoadedColumn(int columnX, int columnZ) {
        Chunk chunk = this.loadedChunks.get(ChunkPos.asLong(columnX, columnZ));
        if (chunk == null)
            return (Chunk) newFrozenColumns.get(columnX, columnZ);
        return chunk;
    }

    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite(remap = false)
    @Nullable
    private Chunk getColumn(int columnX, int columnZ, ICubeProviderServer.Requirement req, boolean forceNow) {
        Chunk column = this.getLoadedChunk(columnX, columnZ);
        if (column != null) {
            return column;
        }
        if (((IFreezableWorld) this.world).isColumnDst(columnX, columnZ, true)) {
            Chunk chunk = (Chunk) newFrozenColumns.get(columnX, columnZ);
            if (chunk == null) {
                chunk = new Chunk(world, new ChunkPrimer(), columnX, columnZ);
                newFrozenColumns.put((IColumn) chunk);
                chunk.onLoad();
            }
            return chunk;
        }

        if (req == ICubeProviderServer.Requirement.GET_CACHED) {
            return column;
        }
        column = AsyncWorldIOExecutor.syncColumnLoad(world, cubeIO, columnX, columnZ, col -> currentlyLoadingColumn = col);
        column = postProcessColumn(columnX, columnZ, column, req, forceNow);

        return column;
    }

    //If cube is read frozen, return null

    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite(remap = false)
    @Override
    @Nullable
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        Cube cube = this.cubeMap.get(cubeX, cubeY, cubeZ);
        if (cube != null) {
            return cube;
        }
        return newFrozenCubes.get(cubeX, cubeY, cubeZ);
    }

    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite(remap = false)
    @Nullable
    private Cube getCube(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req, boolean forceNow) {
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
        if (((IFreezableWorld) world).isCubeDst(cubeX, cubeY, cubeZ, true)) {
            cube = newFrozenCubes.get(cubeX, cubeY, cubeZ);
            if (cube == null) {
                cube = createBarrierCube(column, cubeX, cubeY, cubeZ);
                newFrozenCubes.put(cube);
                cube.onLoad();
            }
            return cube;
        }

        if (cube == null) {
            // a little hack to fix StackOverflowError when loading TileEntities, as Cube methods are now redirected into IColumn
            // Column needs cube to be loaded to add TileEntity, so make CubeProvider contain it already
            cube = AsyncWorldIOExecutor.syncCubeLoad(world, cubeIO, (CubeProviderServer) (Object) this, cubeX, cubeY, cubeZ);
            onCubeLoaded(cube, column);
        }
        return postCubeLoadAttempt(cubeX, cubeY, cubeZ, cube, column, req, forceNow);
    }

    /**
     * @author NotStirred
     * @reason freezable cubes
     */
    @Overwrite(remap = false)
    public void asyncGetCube(int cubeX, int cubeY, int cubeZ, Requirement req, Consumer<Cube> callback) {
        Cube cube = getLoadedCube(cubeX, cubeY, cubeZ);
        if (req == Requirement.GET_CACHED || (cube != null && req.compareTo(Requirement.GENERATE) <= 0)) {
            callback.accept(cube);
            return;
        }

        if (((IFreezableWorld) world).isCubeDst(cubeX, cubeY, cubeZ, true)) {
            if (cube == null) {
                Chunk column = getColumn(cubeX, cubeZ, req);
                if (column == null) {
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
            AsyncWorldIOExecutor.queueCubeLoad(world, cubeIO, (CubeProviderServer) (Object) this, cubeX, cubeY, cubeZ, loaded -> {
                Chunk col = getLoadedColumn(cubeX, cubeZ);
                if (col != null) {
                    onCubeLoaded(loaded, col);
                    loaded = postCubeLoadAttempt(cubeX, cubeY, cubeZ, loaded, col, req, false);
                }
                callback.accept(loaded);
            });
        }
    }

    /**
     * @author NotStirred
     * @reason freezable columns
     */
    @Overwrite(remap = false)
    public void asyncGetColumn(int columnX, int columnZ, Requirement req, Consumer<Chunk> callback) {
        Chunk column = getLoadedColumn(columnX, columnZ);
        if (column != null || req == Requirement.GET_CACHED) {
            callback.accept(column);
            return;
        }

        if (((IFreezableWorld) world).isColumnDst(columnX, columnZ, true)) {
            Chunk frozenColumn = (Chunk) newFrozenColumns.get(columnX, columnZ);
            if (frozenColumn == null) {
                frozenColumn = new Chunk(world, columnX, columnZ);
                //noinspection ConstantConditions
                newFrozenColumns.put((IColumn) frozenColumn);
                frozenColumn.onLoad();
            }
            callback.accept(frozenColumn);
            return;
        }

        AsyncWorldIOExecutor.queueColumnLoad(world, cubeIO, columnX, columnZ, col -> {
            col = postProcessColumn(columnX, columnZ, col, req, false);
            callback.accept(col);
        }, col -> currentlyLoadingColumn = col);
    }

    @Inject(method = "tryUnloadColumn", at = @At("HEAD"), cancellable = true, remap = false)
    private void on$tryUnloadColumn(Chunk column, CallbackInfoReturnable<Boolean> cir) {
        if (((IFreezableWorld) world).isColumnFrozen(column)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void reload() {
        PlayerCubeMap playerCubeMap = (PlayerCubeMap) world.getPlayerChunkMap();
        List<Map.Entry<CubePos, TicketList>> dstCubesToReload = new ArrayList<>(1000);
        Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap = new IdentityHashMap<>(1000);
        Map<IColumn, List<EntityPlayerMP>> columnPlayerMap = new IdentityHashMap<>(1000);

        unfreezeUnloadBarrier(playerCubeMap, dstCubesToReload, cubePlayerMap, columnPlayerMap);
        MAWM.LOGGER.trace("Barrier unloaded");
        unfreezeUnloadDst(playerCubeMap, dstCubesToReload, cubePlayerMap, columnPlayerMap);
        MAWM.LOGGER.trace("Dst unloaded");

        ((IFreezableWorld) world).setDstFrozen(false);

        ((IFreezableWorld) world).clearSrcBoxes();
        ((IFreezableWorld) world).clearDstBoxes();

        ((IFreezableWorld) world).setManipulateStage(IFreezableWorld.ManipulateStage.RELOADING_CUBES);

        unfreezeReloadBarrier(playerCubeMap, cubePlayerMap, columnPlayerMap);
        MAWM.LOGGER.trace("Barrier reloaded");
        unfreezeReloadDst(dstCubesToReload);
        MAWM.LOGGER.trace("Dst reloaded");
    }

    @Override
    public void unfreezeUnloadBarrier(PlayerCubeMap map, List<Map.Entry<CubePos, TicketList>> dstCubesToReload, Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap, Map<IColumn, List<EntityPlayerMP>> columnPlayerMap) {
        newFrozenCubes.iterator().forEachRemaining(cube -> {
            MAWM.LOGGER.trace("Barrier Cube unloaded at pos " + cube.getCoords());
            CubeWatcher cubeWatcher = map.getCubeWatcher(cube.getCoords());
            if (cubeWatcher != null) {
                ObjectArrayList<EntityPlayerMP> players = ((AccessCubeWatcher) cubeWatcher).getPlayers().clone();
                cubePlayerMap.put(cube, players);
                players.forEach(((AccessCubeWatcher) cubeWatcher)::invokeRemovePlayer);
            }
            dstCubesToReload.add(new SimpleEntry<>(cube.getCoords(), cube.getTickets()));
            cube.onUnload();
            cube.getColumn().removeCube(cube.getY());
        });
        this.newFrozenCubes = new XYZMap<>(0.75f, 1000);

        newFrozenColumns.iterator().forEachRemaining(col -> {
            ColumnWatcher columnWatcher = map.getColumnWatcher(((Chunk) col).getPos());
            if (columnWatcher != null) {
                List<EntityPlayerMP> players = new ArrayList<>(((IPlayerChunkMapEntry) columnWatcher).getPlayerList());
                columnPlayerMap.put(col, players);
                players.forEach(columnWatcher::removePlayer);
            }
            ((Chunk) col).onUnload();
        });
        newFrozenColumns.clear();
    }
    @Override
    public void unfreezeReloadBarrier(PlayerCubeMap map, Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap, Map<IColumn, List<EntityPlayerMP>> columnPlayerMap) {
        columnPlayerMap.forEach((col, players) -> {
            ColumnWatcher columnWatcher = ((AccessPlayerCubeMap) map).invokeGetOrCreateColumnWatcher(new ChunkPos(col.getX(), col.getZ()));
            players.forEach(((IColumnWatcher)columnWatcher)::addPlayerNoChunkUnload);

        });

        cubePlayerMap.forEach((cube, players) -> {
            MAWM.LOGGER.trace("Barrier Cube reloaded at pos " + cube.getCoords());
            CubeWatcher cubeWatcher = ((AccessPlayerCubeMap)map).invokeGetOrCreateCubeWatcher(cube.getCoords());
            players.forEach(((AccessCubeWatcher) cubeWatcher)::invokeAddPlayer);
        });
    }

    @Override
    public void unfreezeUnloadDst(PlayerCubeMap map, List<Map.Entry<CubePos, TicketList>> dstCubesToReload, Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap, Map<IColumn, List<EntityPlayerMP>> columnPlayerMap) {
        for (Iterator<Cube> iterator = cubeMap.iterator(); iterator.hasNext(); ) {
            Cube cube = iterator.next();
            if (!((IFreezableWorld) world).isCubeDst(cube, false))
                continue;
            MAWM.LOGGER.trace("DST Cube unloaded at pos " + cube.getCoords());
            CubeWatcher cubeWatcher = map.getCubeWatcher(cube.getCoords());
            if (cubeWatcher != null) {
                ObjectArrayList<EntityPlayerMP> players = ((AccessCubeWatcher) cubeWatcher).getPlayers().clone();
                cubePlayerMap.put(cube, players);
                players.forEach(((AccessCubeWatcher) cubeWatcher)::invokeRemovePlayer);
            }
            cube.onUnload();
            cube.getColumn().removeCube(cube.getY());
            iterator.remove();

            dstCubesToReload.add(new SimpleEntry<>(cube.getCoords(), cube.getTickets()));
        }

        for (Iterator<Chunk> iterator = loadedChunks.values().iterator(); iterator.hasNext(); ) {
            Chunk chunk = iterator.next();
            if (!((IFreezableWorld) world).isColumnDst(chunk, false))
                continue;

            ColumnWatcher columnWatcher = map.getColumnWatcher(chunk.getPos());
            if (columnWatcher != null) {
                List<EntityPlayerMP> players = new ArrayList<>(((AccessPlayerChunkMapEntry) columnWatcher).getPlayerList());
                columnPlayerMap.put((IColumn) chunk, players);
                players.forEach(((IColumnWatcher)columnWatcher)::removePlayerNoChunkUnload);
            }
            chunk.onUnload();
            iterator.remove();
        }
    }
    @Override
    public void unfreezeReloadDst(List<Map.Entry<CubePos, TicketList>> dstCubesToReload) {
        dstCubesToReload.forEach(
                (pair) -> {
                    MAWM.LOGGER.trace("DST Cube reloaded at pos " + pair.getKey());
                    this.asyncGetCube(pair.getKey().getX(), pair.getKey().getY(), pair.getKey().getZ(), Requirement.LOAD,
                        (cube) -> ((AccessTicketList) pair.getValue()).getTickets().forEach((iticket) -> {
                            if(cube != null)
                                cube.getTickets().add(iticket);
                        }));
                }
        );
    }

    @Override
    public void addSrcCubesToSave() {
        for (Cube cube : this.cubeMap) {
            if (((IFreezableWorld) world).isCubeSrc(cube, false)) {
                if (cube.needsSaving()) { // save the Cube, if it needs saving
                    this.cubeIO.saveCube(cube);
                }
            }
        }

        for (Chunk chunk : loadedChunks.values()) {
            if (((IFreezableWorld) world).isColumnSrc(chunk, false)) {
                if (chunk.needsSaving(true)) { // save the Cube, if it needs saving
                    this.cubeIO.saveColumn(chunk);

                }
            }
        }
    }

    @SuppressWarnings("unused")
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
