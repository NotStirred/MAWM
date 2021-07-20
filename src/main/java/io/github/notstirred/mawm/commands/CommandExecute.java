package io.github.notstirred.mawm.commands;

import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandExecute extends CommandBase {
    @Override
    public String getName() {
        return "execute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.execute.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(MAWM.isQueueMode) {
            if (!((IFreezableWorld) sender.getEntityWorld()).hasDeferredRequests())
                throw new CommandException("mawm.command.execute.no_tasks");
            ((IFreezableWorld) sender.getEntityWorld()).requestTasksExecute();
        }
    }
}
