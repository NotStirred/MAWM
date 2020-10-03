package io.github.notstirred.mawm.commands;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.input.WandHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.AbstractMap;

public class CommandCut extends CommandBase {
    @Override
    public String getName() {
        return "cut";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.cut.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = null;
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            player = (EntityPlayer) sender.getCommandSenderEntity();
        }
        if (player != null) { //command came from a player
            AbstractMap.SimpleEntry<BlockPos, BlockPos> positions = WandHandler.getWandLocationsForPlayer(player);
            if (positions.getKey() == null)
                throw new CommandException("mawm.command.cut.no_wandpos1");
            if (positions.getValue() == null)
                throw new CommandException("mawm.command.cut.no_wandpos2");

            BoundingBox box = MAWMCommands.toCubeBox(
                    new Vector3i(
                            positions.getKey().getX(),
                            positions.getKey().getY(),
                            positions.getKey().getZ()
                    ),
                    new Vector3i(
                            positions.getValue().getX(),
                            positions.getValue().getY(),
                            positions.getValue().getZ()
                    )
            );

            Vector3i offset = null;

            if (args.length == 3) { //doing a cut from wandpos1 to wandpos2 with an offset
                offset = new Vector3i(
                        Integer.parseInt(args[0]),
                        Integer.parseInt(args[1]),
                        Integer.parseInt(args[2])
                );

            } else if (args.length == 0) {
            } //doing a cut from wandpos1 to wandpos2 WITHOUT an offset

            ((IFreezableWorld) sender.getEntityWorld()).addTask(new EditTask(box, offset, EditTask.Type.CUT));
        }
        sender.sendMessage(new TextComponentTranslation("mawm.command.cut.queued"));
    }
}
