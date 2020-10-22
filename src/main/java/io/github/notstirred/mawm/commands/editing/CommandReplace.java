package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BlockEditTask;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
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

public class CommandReplace extends CommandBase {
    @Override
    public String getName() {
        return "replace";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.replace.usage";
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

            if (args.length < 2) {
                throw new CommandException("mawm.command.replace.no_args");
            }

            Block inBlock = CommandBase.getBlockByText(sender, args[0]);
            IBlockState inState = inBlock.getDefaultState();
            @SuppressWarnings("deprecation")
            int inId = Block.BLOCK_STATE_IDS.get(inState);

            Block outBlock = CommandBase.getBlockByText(sender, args[1]);
            IBlockState outState = outBlock.getDefaultState();
            @SuppressWarnings("deprecation")
            int outId = Block.BLOCK_STATE_IDS.get(outState);

            ((IFreezableWorld) sender.getEntityWorld()).addTask(sender, new BlockEditTask(box, null, EditTask.Type.REPLACE,
                    (byte) (inId >> 4 & 255), (byte) (inId & 15),
                    (byte) (outId >> 4 & 255), (byte) (outId & 15)
            ));
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestTasksExecute();
        }
    }
}
