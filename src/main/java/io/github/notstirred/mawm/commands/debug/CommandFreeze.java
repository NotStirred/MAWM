package io.github.notstirred.mawm.commands.debug;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.TextComponentHelper;

public class CommandFreeze extends CommandBase {
    @Override
    public String getName() {
        return "freeze";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "iaf hswreihbvo";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        ((IFreezableWorld) sender.getEntityWorld()).freeze();
        sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.debug.freeze.success"));
    }
}