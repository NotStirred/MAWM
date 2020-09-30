package io.github.notstirred.mawm.commands.debug;

import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.FreezableBox;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.TextComponentHelper;

public class CommandFreezeBox extends CommandBase {
    @Override
    public String getName() {
        return "freezebox";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "iaf hswreihbvo";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        FreezableBox box;
        try {
            box = new FreezableBox(args[0].equals("dst") ? FreezableBox.Type.DESTINATION : FreezableBox.Type.SOURCE,
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]),
                    Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]),
                    Integer.parseInt(args[5]),
                    Integer.parseInt(args[6]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new CommandException("mawm.command.debug.invalidboundingbox");
        }
        ((IFreezableWorld) sender.getEntityWorld()).addFreezeBox(box);
        sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.debug.freezebox.success"));
    }
}
