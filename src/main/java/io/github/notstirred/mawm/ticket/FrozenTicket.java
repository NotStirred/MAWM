package io.github.notstirred.mawm.ticket;

import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;

public class FrozenTicket implements ITicket {
    @Override
    public boolean shouldTick() {
        return false;
    }
}
