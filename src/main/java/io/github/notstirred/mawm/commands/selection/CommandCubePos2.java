package io.github.notstirred.mawm.commands.selection;

import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.input.CubeWandHandler;
import io.github.notstirred.mawm.util.MathUtil;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.TextComponentHelper;

public class CommandCubePos2 extends CommandBase {
    @Override
    public String getName() {
        return "cubepos2";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.cubepos2.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();

            if (args.length != 3)
                throw new CommandException("mawm.command.cubepos2.invalid_args");

            MutablePair<Vector3i, Vector3i> positions = CubeWandHandler.getWandLocationsForPlayer(player);

            try {
                positions.setValue(new Vector3i(MathUtil.parseInt(args[0]), MathUtil.parseInt(args[1]), MathUtil.parseInt(args[2])));
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.cubepos2.set", args[0], args[1], args[2]));
            } catch(NumberFormatException e) {
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.cubepos2.invalid_args"));
            }
        }
    }
}