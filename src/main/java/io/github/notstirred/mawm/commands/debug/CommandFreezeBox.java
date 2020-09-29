package io.github.notstirred.mawm.commands.debug;

import cubicchunks.converter.lib.util.BoundingBox;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

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
        BoundingBox box;
        try {
            box = new BoundingBox(Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]),
                    Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]),
                    Integer.parseInt(args[5]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new CommandException("mawm.command.debug.invalidboundingbox");
        }

        ((IFreezableWorld) sender.getEntityWorld()).freezeBox(box);

    }
}
