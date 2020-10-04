package io.github.notstirred.mawm.asm.mixin.core.cubicchunks;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableCube;
import io.github.notstirred.mawm.ticket.FrozenTicket;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(value = Cube.class, remap = false)
public class MixinCube implements IFreezableCube {

    @Shadow @Final @Nonnull private TicketList tickets;
    private static FrozenTicket frozenTicket = new FrozenTicket();

    @Override
    public void freeze() {
        this.tickets.add(frozenTicket);
    }

    @Override
    public void unFreeze() {
        this.tickets.remove(frozenTicket);
    }
}
