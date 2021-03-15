package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.converter.lib.util.edittask.SetEditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.input.CubeWandHandler;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandSet extends CommandBase {
    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.set.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = null;
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            player = (EntityPlayer) sender.getCommandSenderEntity();
        }
        if (player != null) { //command came from a player
            MutablePair<Vector3i, Vector3i> positions = CubeWandHandler.getWandLocationsForPlayer(player);
            if (positions.getKey() == null)
                throw new CommandException("mawm.cubewand.no_cubewandpos1");
            if (positions.getValue() == null)
                throw new CommandException("mawm.cubewand.no_cubewandpos2");

            BoundingBox box = new BoundingBox(
                    positions.getKey(),
                    positions.getValue()
            );

            if (args.length < 1) {
                throw new CommandException("mawm.command.set.no_args");
            }

            Block block = CommandBase.getBlockByText(sender, args[0]);
            IBlockState state = block.getDefaultState();

            @SuppressWarnings("deprecation")
            int id = Block.BLOCK_STATE_IDS.get(state);
            ((IFreezableWorld) sender.getEntityWorld()).addTask(sender, new SetEditTask(box, (byte)(id >> 4 & 255), (byte)(id & 15)));
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestTasksExecute();
        }
    }
}
