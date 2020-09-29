package io.github.notstirred.mawm.commands.debug;


import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandUnfreeze extends CommandBase {
    @Override
    public String getName() {
        return "unfreezebox";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "iaf hswreihbvo";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        ((IFreezableWorld) sender.getEntityWorld()).unfreeze();
    }
}