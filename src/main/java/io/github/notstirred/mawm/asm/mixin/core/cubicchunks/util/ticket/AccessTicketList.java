package io.github.notstirred.mawm.asm.mixin.core.cubicchunks.util.ticket;

import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = TicketList.class, remap = false)
public interface AccessTicketList {
    @Accessor List<ITicket> getTickets();
}
