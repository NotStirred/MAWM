package io.github.notstirred.mawm.commands.debug;

import io.github.notstirred.mawm.MAWM;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.server.command.TextComponentHelper;

public class CommandConvert extends CommandBase {
    @Override
    public String getName() {
        return "convert";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "iaf hswreihbvo";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        MAWM.INSTANCE.convertCommand((WorldServer) sender.getEntityWorld());

        sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.debug.convert.success"));
    }
}
