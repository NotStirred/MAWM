package io.github.notstirred.mawm.asm.mixininterfaces;

import io.github.notstirred.mawm.asm.mixin.core.cubicchunks.server.AccessPlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.Map;

public interface IFreezableCubeProviderServer {

    void unfreezeReload(AccessPlayerCubeMap map, List<Map.Entry<CubePos, TicketList>> dstCubesToReload, Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap, Map<IColumn, List<EntityPlayerMP>> columnPlayerMap);

    void unfreezeUnload(PlayerCubeMap map, List<Map.Entry<CubePos, TicketList>> dstCubesToReload, Map<Cube, ObjectArrayList<EntityPlayerMP>> cubePlayerMap, Map<IColumn, List<EntityPlayerMP>> columnPlayerMap);

    void freeze();

}
