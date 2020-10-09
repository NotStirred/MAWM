package io.github.notstirred.mawm.commands.selection;

import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.input.CubeWandHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.AbstractMap;

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
        EntityPlayer player = null;
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            player = (EntityPlayer) sender.getCommandSenderEntity();
        }
        if (player != null) { //command came from a player
            if (args.length != 3)
                throw new CommandException("mawm.command.cubepos2.invalid_args");

            AbstractMap.SimpleEntry<Vector3i, Vector3i> positions = CubeWandHandler.getWandLocationsForPlayer(player);

            positions.setValue(new Vector3i(
                    Integer.parseInt(args[0]) << 4,
                    Integer.parseInt(args[1]) << 4,
                    Integer.parseInt(args[2]) << 4)
            );
            sender.sendMessage(new TextComponentTranslation("mawm.command.cubepos2.set", args[0], args[1], args[2]));
        }
    }
}